# Persistence & Security Architecture Design

**Status:** Draft
**Date:** December 29, 2025

---

## Overview

This document defines the persistence and security architecture for the Retirement Portfolio Simulator, designed to be implemented prior to the API layer.

## Milestone Structure

| Milestone | Focus |
|-----------|-------|
| M10 | Persistence Layer |
| M11 | Security Layer |
| M12 | API Layer (was M10) |
| M13 | UI - React (was M11) |

---

## M10: Persistence Layer

### Technology Stack

- **Database:** PostgreSQL (primary target)
- **SQL Framework:** jOOQ (type-safe SQL DSL)
- **Migrations:** Flyway
- **Connection Pool:** HikariCP
- **Integration Testing:** Testcontainers + Podman

### Architecture

```
Domain Objects → Repository Interfaces → jOOQ Implementations → PostgreSQL
```

### Build Process

```
1. Flyway migrates schema
2. jOOQ generates code from schema
3. Maven compiles with generated code
```

### Entity Model

| Entity | Key Fields | Encrypted Fields |
|--------|------------|------------------|
| User | id, email, passwordHash | - |
| PersonProfile | id, userId, name, dob, spouseId | ssn |
| Account | id, profileId, type, balance, allocation | accountNumber |
| SocialSecurityBenefit | id, profileId, fraBenefit, claimingAge | - |
| Pension | id, profileId, monthlyBenefit, paymentForm | - |
| Annuity | id, profileId, type, monthlyPayment | - |
| WorkingIncome | id, profileId, salary, colaRate | - |
| OtherIncome | id, profileId, type, amount | - |
| RecurringExpense | id, profileId, category, amount, frequency | - |
| OneTimeExpense | id, profileId, category, amount, targetDate | - |
| Scenario | id, userId, name, config (TEXT) | - |
| SimulationResult | id, scenarioId, resultData (TEXT) | - |

### Storage Strategy

**Design Principle:** Maximize database portability by avoiding PostgreSQL-specific features (e.g., JSONB). This enables testing with Testcontainers against real PostgreSQL while keeping options open for future database migrations.

**Approach by Data Type:**

| Data Type | Storage | Rationale |
|-----------|---------|-----------|
| Core domain entities | Normalized tables | Full SQL query capability, referential integrity |
| Income sources | Normalized tables (per type) | Need to query/aggregate by type |
| Expenses | Normalized tables | Category queries, budget aggregation |
| Scenario config | TEXT (JSON via Jackson) | Complex nested config, read/write as unit |
| Simulation results | TEXT (JSON via Jackson) | Large blob (2-5MB), read/write as unit |

**TEXT Column Strategy:**

For complex configuration and results data, we store JSON-serialized content in TEXT columns:

- Application serializes/deserializes via Jackson
- Database treats it as opaque text (no JSON queries needed)
- Works on any RDBMS (PostgreSQL, MySQL, H2, etc.)
- Typical size: 2-5MB for simulation results (30+ years of monthly data)

```java
// Scenario config stored as TEXT, serialized by Jackson
@Entity
public class Scenario {
    private UUID id;
    private String name;

    @Column(columnDefinition = "TEXT")
    private String config;  // JSON serialized ScenarioConfig
}
```

**Future Consideration:** If query performance on large TEXT columns becomes an issue, simulation results could be moved to external file storage (S3, local filesystem) with path references in the database.

### Repository Pattern

```java
public interface PersonProfileRepository {
    Optional<PersonProfile> findById(UUID id);
    List<PersonProfile> findByUserId(UUID userId);
    PersonProfile save(PersonProfile profile);
    void delete(UUID id);
}

// jOOQ implementation
public class JooqPersonProfileRepository implements PersonProfileRepository {
    private final DSLContext dsl;
    // Type-safe queries with compile-time checking
}
```

---

## M11: Security Layer

### Technology Stack

- **Framework:** Spring Security
- **Authentication:** JWT (JSON Web Tokens)
- **Password Hashing:** BCrypt
- **Encryption:** AES-256-GCM
- **Key Management:** PKCS12 Keystore

### Key Management Architecture

All secrets are centralized in a PKCS12 keystore file, minimizing the attack surface to a single file and one password:

```
┌─────────────────────────────────────────────────────────┐
│           Environment Variables (minimal)               │
│  KEYSTORE_PATH=/secure/path/retirement.p12             │
│  KEYSTORE_PASSWORD=*****                                │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                 PKCS12 Keystore File                    │
│  ┌─────────────────────────────────────────────────┐   │
│  │ retirement-aes-key      (AES-256 SecretKey)     │   │
│  │ retirement-aes-key-v2   (for key rotation)      │   │
│  │ jwt-signing-key         (HMAC for JWT)          │   │
│  │ db-credentials          (database auth)         │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

**Benefits of Centralized Secrets:**
- Single secrets location (one file to protect, backup, rotate)
- Only one credential exposed in environment variables
- No database passwords in config files or env vars
- Consistent rotation process for all secrets
- Easier secrets management in containerized deployments

**Why PKCS12 over JKS:**
- JKS deprecated since Java 9
- PKCS12 is industry standard, cross-platform
- Supports symmetric keys (AES) for field encryption

**Keystore Setup (one-time):**
```bash
# Generate AES-256 key for field encryption
keytool -genseckey -alias retirement-aes-key \
  -keyalg AES -keysize 256 \
  -storetype PKCS12 \
  -keystore retirement.p12 \
  -storepass <password>

# Generate HMAC key for JWT signing
keytool -genseckey -alias jwt-signing-key \
  -keyalg HmacSHA256 -keysize 256 \
  -storetype PKCS12 \
  -keystore retirement.p12 \
  -storepass <password>
```

**Database Credentials Storage:**

Database credentials are stored programmatically using the Java KeyStore API:

```java
// Store DB credentials (during setup/deployment)
KeyStore keyStore = KeyStore.getInstance("PKCS12");
keyStore.load(new FileInputStream(keystorePath), keystorePassword);

// Store as a SecretKey (username:password encoded as bytes)
String credentials = dbUsername + ":" + dbPassword;
SecretKey credKey = new SecretKeySpec(credentials.getBytes(UTF_8), "RAW");
keyStore.setKeyEntry("db-credentials", credKey, keystorePassword, null);
keyStore.store(new FileOutputStream(keystorePath), keystorePassword);

// Retrieve at application startup
SecretKey key = (SecretKey) keyStore.getKey("db-credentials", keystorePassword);
String[] parts = new String(key.getEncoded(), UTF_8).split(":");
String username = parts[0];
String password = parts[1];
```

**DataSource Configuration:**

Spring's DataSource is configured programmatically after keystore initialization:

```java
@Configuration
public class DataSourceConfig {
    @Bean
    public DataSource dataSource(KeyStoreService keyStoreService) {
        String[] credentials = keyStoreService.getDatabaseCredentials();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/retirement");
        config.setUsername(credentials[0]);
        config.setPassword(credentials[1]);
        return new HikariDataSource(config);
    }
}
```

**Key Rotation Support:**
- Add new key with versioned alias (e.g., `retirement-aes-key-v2`)
- Update application configuration to use new alias
- Old data can still be decrypted by loading previous key alias
- Database credentials rotation: update keystore entry, restart application

### Authentication Flow

**Strategy: API Key + JWT (Two-Layer Authentication)**

1. **Client credentials** - Proves request is from legitimate UI app
2. **User JWT** - Proves which user is making the request

```
┌─────────────────────────────────────────────────────────┐
│                     API Request                         │
│  Headers:                                               │
│    X-Client-Id: retirement-ui-v1                       │
│    X-Client-Secret: <api-key>                          │
│    Authorization: Bearer <user-jwt>                     │
│  Cookie:                                                │
│    refresh_token=<httponly-secure-cookie>              │
└─────────────────────────────────────────────────────────┘
```

### Token Structure

**Access Token (JWT, 15-minute expiry):**
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "roles": ["USER"],
  "clientId": "retirement-ui-v1",
  "iat": 1703800000,
  "exp": 1703800900
}
```

**Refresh Token:**
- Stored in HttpOnly, Secure, SameSite cookie
- 7-day expiry
- Used to obtain new access token without re-login
- Can be revoked server-side (stored in DB)

### Password Policy

```
- Minimum 12 characters
- At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
- BCrypt hashing (cost factor 12)
- Optional: Check against common password lists
```

### Password Reset Flow

```
┌──────┐     1. POST /auth/forgot     ┌──────────┐
│  UI  │ ───────────────────────────► │   API    │
└──────┘     { email }                └────┬─────┘
                                           │ 2. Generate reset token
                                           │    (UUID, 15-min expiry)
                                           │    Store hash in DB
                                           ▼
┌──────┐     3. Reset link            ┌──────────┐
│ User │ ◄────────────────────────    │  Email   │
└──┬───┘                              └──────────┘
   │ 4. Click link
   ▼
┌──────┐  5. POST /auth/reset         ┌──────────┐
│  UI  │ ───────────────────────────► │   API    │
└──────┘  { token, newPassword }      └────┬─────┘
                                           │ 6. Validate token
                                           │    Update password
                                           │    Invalidate all sessions
                                           ▼
                                      [Password Changed]
```

### Email Service Configuration

Pluggable email provider with console fallback for development:

```yaml
security:
  email:
    provider: console  # console | smtp | sendgrid
    from-address: noreply@retirement-simulator.com
    smtp:
      host: ${SMTP_HOST:smtp.gmail.com}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
    sendgrid:
      api-key: ${SENDGRID_API_KEY}
```

### Admin Seeding

First admin created via seed script with multiple options:

```bash
# Option 1: Password from environment variable
ADMIN_EMAIL=admin@example.com \
ADMIN_PASSWORD=SecurePass123! \
  java -jar retirement.jar --seed-admin

# Option 2: Password from keystore
java -jar retirement.jar --seed-admin \
  --email admin@example.com \
  --keystore /path/to/retirement.p12

# Option 3: Generate password (displayed once)
java -jar retirement.jar --seed-admin --email admin@example.com
# Output: Admin created. Password: xK9#mP2$vL7@nQ4
#         (This password will not be shown again)
```

### Rate Limiting

Configurable per-endpoint rate limiting:

```yaml
security:
  rate-limiting:
    enabled: true
    default:
      requests-per-minute: 60
      requests-per-hour: 1000
    endpoints:
      /api/auth/login:
        requests-per-minute: 10
      /api/auth/forgot-password:
        requests-per-minute: 3
      /api/auth/reset-password:
        requests-per-minute: 5
      /api/simulations/run:
        requests-per-minute: 5
```

### Account Lockout (Brute Force Protection)

Protect against brute force attacks by temporarily locking accounts after repeated failed login attempts:

```yaml
security:
  lockout:
    enabled: true
    max-failed-attempts: 5
    lockout-duration-minutes: 15
    reset-after-success: true  # Reset counter after successful login
```

**Behavior:**
- After 5 failed attempts: Account locked for 15 minutes
- Successful login: Reset failed attempt counter
- Locked account: Returns generic "invalid credentials" (no information disclosure)
- Admin can manually unlock accounts

### Audit Logging

All sensitive operations are logged for compliance and forensics:

```yaml
security:
  audit:
    enabled: true
    retention-days: 365  # Keep logs for 1 year
    log-request-body: false  # Don't log sensitive request data
    log-ip-address: true
    log-user-agent: true
```

**Audited Actions:**

| Category | Actions |
|----------|---------|
| Authentication | LOGIN, LOGOUT, FAILED_LOGIN, PASSWORD_CHANGE, PASSWORD_RESET_REQUEST, PASSWORD_RESET_COMPLETE |
| User Management | USER_CREATED, USER_UPDATED, USER_DISABLED, USER_DELETED |
| Data Access | VIEW_PROFILE, VIEW_ACCOUNT, VIEW_SCENARIO, VIEW_SIMULATION_RESULT |
| Data Modification | CREATE_*, UPDATE_*, DELETE_* |
| Admin | CLIENT_REGISTERED, CLIENT_DISABLED, ACCOUNT_UNLOCKED |

**Audit Log Entry:**
```json
{
  "id": "uuid",
  "timestamp": "2025-12-29T10:30:00Z",
  "userId": "user-uuid",
  "clientId": "retirement-ui-v1",
  "action": "UPDATE_ACCOUNT",
  "resourceType": "ACCOUNT",
  "resourceId": "account-uuid",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "details": { "field": "balance", "oldValue": "100000", "newValue": "105000" }
}
```

### Session Management

Users can view and revoke their active sessions:

**Endpoints:**
```
GET    /api/auth/sessions           → List active sessions for current user
DELETE /api/auth/sessions/{id}      → Revoke specific session
DELETE /api/auth/sessions           → Revoke all sessions (logout everywhere)
```

**Session Info Returned:**
```json
{
  "id": "session-uuid",
  "clientName": "Retirement UI",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0 (Macintosh...)",
  "lastActivity": "2025-12-29T10:30:00Z",
  "createdAt": "2025-12-29T08:00:00Z",
  "current": true
}
```

### CORS Policy

Restrict API access to authorized origins:

```yaml
security:
  cors:
    enabled: true
    allowed-origins:
      - https://retirement-simulator.com
      - https://app.retirement-simulator.com
      - http://localhost:3000  # Dev only, remove in production
    allowed-methods:
      - GET
      - POST
      - PUT
      - DELETE
      - OPTIONS
    allowed-headers:
      - Authorization
      - X-Client-Id
      - X-Client-Secret
      - Content-Type
    expose-headers:
      - X-Request-Id
    allow-credentials: true
    max-age: 3600
```

### Security Headers

Standard security headers applied to all responses:

```yaml
security:
  headers:
    content-security-policy: "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'"
    x-frame-options: DENY
    x-content-type-options: nosniff
    x-xss-protection: "1; mode=block"
    strict-transport-security: "max-age=31536000; includeSubDomains"
    referrer-policy: strict-origin-when-cross-origin
    permissions-policy: "geolocation=(), microphone=(), camera=()"
```

### Encryption Service

```java
public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}

@EncryptedField // Marker for fields requiring encryption
private String socialSecurityNumber;
```

### Security Layers

1. **Authentication:** JWT validation on every request
2. **Authorization:** Role-based (@PreAuthorize)
3. **Resource Access:** Users can only access their own data
4. **Encryption at Rest:** TDE + field-level for PII
5. **Encryption in Transit:** HTTPS/TLS

### Protected Endpoints

| Endpoint Pattern | Required Role |
|-----------------|---------------|
| /api/auth/** | Public |
| /api/users/** | USER |
| /api/admin/** | ADMIN |
| /api/profiles/** | USER (own data) |
| /api/scenarios/** | USER (own data) |

---

## Database Schema

```sql
-- =============================================
-- CORE TABLES
-- =============================================

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    -- Account lockout fields
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- SECURITY & AUTH TABLES
-- =============================================

CREATE TABLE api_clients (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) UNIQUE NOT NULL,  -- e.g., 'retirement-ui-v1'
    client_secret_hash VARCHAR(255) NOT NULL, -- BCrypt hashed
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES api_clients(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,  -- SHA-256 hash of token
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    -- Session tracking for session management
    ip_address VARCHAR(45),            -- IPv6 compatible
    user_agent TEXT,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,  -- SHA-256 hash of token
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id),  -- Nullable for unauthenticated actions
    client_id UUID REFERENCES api_clients(id),
    action VARCHAR(100) NOT NULL,       -- LOGIN, LOGOUT, UPDATE_ACCOUNT, etc.
    resource_type VARCHAR(100),         -- USER, PERSON_PROFILE, ACCOUNT, SCENARIO
    resource_id UUID,
    ip_address VARCHAR(45),             -- IPv6 compatible
    user_agent TEXT,
    details TEXT,                       -- JSON with additional context
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- PERSON PROFILES
-- =============================================

CREATE TABLE person_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    spouse_id UUID REFERENCES person_profiles(id),  -- Self-referential for couples
    name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    ssn_encrypted VARCHAR(500),  -- AES-256-GCM encrypted
    retirement_date DATE,
    life_expectancy INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- INVESTMENT ACCOUNTS
-- =============================================

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL,  -- 401K, IRA, ROTH_IRA, ROTH_401K, HSA, TAXABLE
    account_number_encrypted VARCHAR(500),  -- AES-256-GCM encrypted
    balance DECIMAL(15,2) NOT NULL DEFAULT 0,
    -- Asset allocation
    stock_allocation DECIMAL(5,4) NOT NULL DEFAULT 0,
    bond_allocation DECIMAL(5,4) NOT NULL DEFAULT 0,
    cash_allocation DECIMAL(5,4) NOT NULL DEFAULT 0,
    -- Return rates
    pre_retirement_return DECIMAL(5,4),
    post_retirement_return DECIMAL(5,4),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_allocation CHECK (stock_allocation + bond_allocation + cash_allocation = 1.0)
);

-- =============================================
-- INCOME SOURCES (Normalized per type)
-- =============================================

CREATE TABLE social_security_benefits (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    fra_benefit DECIMAL(10,2) NOT NULL,  -- Full Retirement Age monthly benefit
    claiming_age INT NOT NULL,
    cola_rate DECIMAL(5,4) DEFAULT 0.02,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pensions (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    monthly_benefit DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    payment_form VARCHAR(50) NOT NULL,  -- SINGLE_LIFE, JOINT_100, JOINT_75, JOINT_50
    survivor_percentage DECIMAL(5,2),
    cola_rate DECIMAL(5,4) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE annuities (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    annuity_type VARCHAR(50) NOT NULL,  -- FIXED_IMMEDIATE, FIXED_DEFERRED, VARIABLE
    monthly_payment DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    cola_rate DECIMAL(5,4) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE working_income (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    employer_name VARCHAR(255),
    annual_salary DECIMAL(12,2) NOT NULL,
    cola_rate DECIMAL(5,4) DEFAULT 0.03,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE other_income (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    income_type VARCHAR(50) NOT NULL,  -- RENTAL, PART_TIME, ROYALTIES, DIVIDENDS, BUSINESS
    monthly_amount DECIMAL(10,2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    inflation_rate DECIMAL(5,4) DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- EXPENSES
-- =============================================

CREATE TABLE recurring_expenses (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,  -- HOUSING, FOOD, HEALTHCARE, TRAVEL, etc.
    amount DECIMAL(10,2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,  -- MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
    start_date DATE NOT NULL,
    end_date DATE,
    inflation_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',  -- GENERAL, HEALTHCARE, HOUSING, NONE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE one_time_expenses (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES person_profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    target_date DATE NOT NULL,
    adjust_for_inflation BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- SCENARIOS & SIMULATION RESULTS
-- =============================================

CREATE TABLE scenarios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    config TEXT NOT NULL,  -- JSON serialized ScenarioConfig (Jackson)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE simulation_results (
    id UUID PRIMARY KEY,
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    simulation_type VARCHAR(50) NOT NULL,  -- DETERMINISTIC, MONTE_CARLO, HISTORICAL
    run_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result_data TEXT NOT NULL,  -- JSON serialized results (2-5MB typical)
    summary_metrics TEXT,  -- JSON serialized summary for quick access
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================
-- INDICES
-- =============================================

CREATE INDEX idx_person_profiles_user_id ON person_profiles(user_id);
CREATE INDEX idx_accounts_profile_id ON accounts(profile_id);
CREATE INDEX idx_ss_benefits_profile_id ON social_security_benefits(profile_id);
CREATE INDEX idx_pensions_profile_id ON pensions(profile_id);
CREATE INDEX idx_annuities_profile_id ON annuities(profile_id);
CREATE INDEX idx_working_income_profile_id ON working_income(profile_id);
CREATE INDEX idx_other_income_profile_id ON other_income(profile_id);
CREATE INDEX idx_recurring_expenses_profile_id ON recurring_expenses(profile_id);
CREATE INDEX idx_one_time_expenses_profile_id ON one_time_expenses(profile_id);
CREATE INDEX idx_scenarios_user_id ON scenarios(user_id);
CREATE INDEX idx_simulation_results_scenario_id ON simulation_results(scenario_id);

-- Security indices
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at) WHERE NOT revoked;
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token_hash ON password_reset_tokens(token_hash);

-- Audit log indices
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
```

---

## Multi-Tenancy (Deferred)

Design abstractions to support future multi-tenancy:
- Repository interfaces don't expose tenant details
- Could add tenant discriminator or schema-per-tenant later
- Current design: single-tenant (user-based isolation)

---

## Testing Strategy

### Integration Testing with Testcontainers + Podman

We use Testcontainers with Podman (not Docker) for integration tests against real PostgreSQL. This avoids Docker Desktop licensing issues while providing full database fidelity.

**Why Podman:**
- Fully open source (no commercial licensing)
- Docker-compatible API
- Rootless containers for better security
- Pre-installed on GitHub Actions Ubuntu runners

**Podman Configuration (macOS):**

```bash
# Initialize and start Podman machine
podman machine init
podman machine start

# Enable Docker-compatible socket
podman machine set --rootful
podman machine stop && podman machine start

# Set environment for Testcontainers
export DOCKER_HOST="unix://$HOME/.local/share/containers/podman/machine/podman.sock"
export TESTCONTAINERS_RYUK_DISABLED=true
```

**Test Configuration:**

```java
@Testcontainers
class RepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("retirement_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

**CI/CD (GitHub Actions):**

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Podman for Testcontainers
        run: |
          systemctl --user start podman.socket
          echo "DOCKER_HOST=unix:///run/user/$UID/podman/podman.sock" >> $GITHUB_ENV
          echo "TESTCONTAINERS_RYUK_DISABLED=true" >> $GITHUB_ENV

      - name: Run tests
        run: mvn verify
```

**Maven Dependencies:**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

---

## Future Considerations

### Alternative Data Stores

The current design uses PostgreSQL with TEXT columns for large JSON blobs. If requirements change, consider:

| Scenario | Alternative | Migration Path |
|----------|-------------|----------------|
| Simulation results > 10MB | File storage (S3/local) | Store path in DB, data in files |
| Need graph queries (relationships) | Neo4j | Add graph layer alongside RDBMS |
| Need document flexibility | MongoDB | Migrate scenario/results to document store |
| High read scalability | Read replicas | PostgreSQL streaming replication |

### Data Migration Strategy

If moving large TEXT data to external storage:

```java
public interface SimulationResultStorage {
    void store(UUID resultId, SimulationResult result);
    SimulationResult retrieve(UUID resultId);
}

// Current: DatabaseSimulationResultStorage (TEXT column)
// Future:  S3SimulationResultStorage or FileSystemSimulationResultStorage
```

The repository interface remains unchanged; only the storage implementation changes.

---

## Issue Breakdown

### M10: Persistence Layer (~21 points)

| # | Title | Points |
|---|-------|--------|
| 1 | Setup PostgreSQL + Flyway + jOOQ build pipeline | 5 |
| 2 | Create database schema migrations (users, profiles, accounts) | 5 |
| 3 | Implement repository interfaces for domain entities | 3 |
| 4 | Implement jOOQ repository implementations | 5 |
| 5 | Add comprehensive persistence tests | 3 |

### M11: Security Layer (~43 points)

| # | Title | Points |
|---|-------|--------|
| 1 | Implement KeyStoreService for centralized secrets management | 5 |
| 2 | Setup Spring Security with JWT + API Key filter chain | 5 |
| 3 | Implement user registration with password policy | 3 |
| 4 | Implement login with access token + refresh token (HttpOnly cookie) | 5 |
| 5 | Implement password reset flow with email service | 5 |
| 6 | Implement API client registration and validation | 3 |
| 7 | Implement EncryptionService for field-level encryption | 3 |
| 8 | Add role-based and resource-based authorization | 3 |
| 9 | Implement configurable rate limiting | 3 |
| 10 | Implement account lockout (brute force protection) | 2 |
| 11 | Implement audit logging service | 3 |
| 12 | Add session management endpoints | 2 |
| 13 | Configure CORS and security headers | 2 |
| 14 | Create admin seed script | 2 |
| 15 | Add security integration tests | 3 |

---

## Future Security Enhancements (Post-MVP)

| Feature | Priority | Description |
|---------|----------|-------------|
| Two-factor authentication (2FA) | Medium | TOTP via authenticator app (Google Authenticator, Authy) |
| Login activity notifications | Low | Email alerts on login from new device/location |
| Data export (GDPR) | Medium | Allow users to export all their data as JSON |
| Account deletion | Medium | "Delete my account" with full data cascade |
| Dependency scanning | High | OWASP Dependency Check integrated in CI pipeline |
| Penetration testing | High | Professional security audit before production |

---

## Dependencies

- M10 must complete before M11 (security needs user persistence)
- M11 must complete before M12 (API needs authentication)
