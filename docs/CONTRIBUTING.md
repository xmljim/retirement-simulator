# Contributing to Retirement Portfolio Simulator

Thank you for your interest in contributing to the Retirement Portfolio Simulator! This document provides guidelines and standards for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Branch Strategy](#branch-strategy)
- [Commit Standards](#commit-standards)
- [Pull Request Process](#pull-request-process)
- [Code Review Guidelines](#code-review-guidelines)
- [Definition of Done](#definition-of-done)

---

## Code of Conduct

- Be respectful and inclusive in all interactions
- Provide constructive feedback during code reviews
- Focus on the code, not the person
- Ask questions when something is unclear
- Help others learn and grow

---

## Getting Started

### Prerequisites

- Java 25 or higher
- Maven 3.9+
- Git
- IDE (IntelliJ IDEA recommended)

### Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/<your-username>/retirement-simulator.git
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/xmljim/retirement-simulator.git
   ```
4. Build the project:
   ```bash
   mvn clean install
   ```
5. Run tests to verify setup:
   ```bash
   mvn test
   ```

### IDE Configuration

- Import the project as a Maven project
- Configure code style settings (see [CODE_STYLE.md](CODE_STYLE.md))
- Enable Checkstyle plugin with project configuration
- Configure EditorConfig plugin

---

## Development Workflow

### 1. Sync with Upstream

Before starting work, ensure your local repository is up to date:

```bash
git checkout develop
git fetch upstream
git merge upstream/develop
```

### 2. Create a Feature Branch

Create a branch from `develop` following the naming convention:

```bash
git checkout -b feature/<issue-number>-<short-description>
```

### 3. Make Changes

- Write code following our [Code Style](CODE_STYLE.md) guidelines
- Write tests following our [Testing Standards](TESTING.md)
- Keep commits atomic and focused

### 4. Test Locally

Before pushing, ensure all checks pass:

```bash
# Run all tests
mvn test

# Run full verification (tests + style + analysis)
mvn verify
```

### 5. Push and Create PR

```bash
git push origin feature/<issue-number>-<short-description>
```

Then create a Pull Request on GitHub.

---

## Branch Strategy

### Branch Types

| Branch | Purpose | Merges From | Merges To |
|--------|---------|-------------|-----------|
| `main` | Production-ready code | `release/*`, hotfix | - |
| `develop` | Integration branch | `feature/*`, `bugfix/*` | `release/*` |
| `feature/<issue>-<desc>` | New features | `develop` | `develop` |
| `bugfix/<issue>-<desc>` | Bug fixes | `develop` | `develop` |
| `release/<version>` | Release preparation | `develop` | `main`, `develop` |
| `hotfix/<issue>-<desc>` | Production fixes | `main` | `main`, `develop` |

### Branch Naming Convention

```
<type>/<issue-number>-<short-description>
```

**Examples:**
- `feature/42-implement-static-withdrawal-strategy`
- `bugfix/123-fix-contribution-limit-calculation`
- `release/1.0.0`
- `hotfix/456-fix-ss-calculation-error`

### Rules

- Never commit directly to `main` or `develop`
- Always work in feature/bugfix branches
- Keep branches short-lived (< 1 week ideal)
- Delete branches after merging

---

## Commit Standards

### Commit Message Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `style` | Code style (formatting, no logic change) |
| `refactor` | Code refactoring (no feature/fix) |
| `test` | Adding or updating tests |
| `chore` | Build, CI, tooling changes |
| `perf` | Performance improvement |

### Scope

The scope indicates the module or area affected:
- `domain` - Domain model classes
- `simulation` - Simulation engine
- `api` - REST API layer
- `ui` - React UI
- `contrib` - Contribution calculations
- `distrib` - Distribution strategies
- `income` - Income modeling
- `expense` - Expense modeling

### Subject

- Use imperative mood ("Add feature" not "Added feature")
- No period at the end
- Maximum 50 characters
- Capitalize first letter

### Body (Optional)

- Wrap at 72 characters
- Explain what and why, not how
- Reference related issues

### Footer

- Reference issues: `Fixes #123` or `Closes #123`
- Breaking changes: `BREAKING CHANGE: description`

### Examples

```
feat(distrib): implement static withdrawal strategy

Add the 4% rule static withdrawal strategy with inflation adjustment.
The strategy calculates initial withdrawal from starting balance and
adjusts annually for inflation.

Fixes #42
```

```
fix(contrib): correct catch-up contribution age check

The age check was using >= 50 but should trigger in the year the
person turns 50, not after their birthday.

Fixes #123
```

```
refactor(domain): extract PersonProfile from PortfolioParameters

Split the monolithic PortfolioParameters class to separate concerns.
PersonProfile now owns personal details while Portfolio manages
investment accounts.

Part of #7
```

---

## Pull Request Process

### Before Creating a PR

- [ ] All tests pass locally (`mvn test`)
- [ ] Code style checks pass (`mvn checkstyle:check`)
- [ ] Static analysis passes (`mvn spotbugs:check pmd:check`)
- [ ] Coverage thresholds met
- [ ] Branch is up to date with `develop`
- [ ] Commits are clean and well-organized

### PR Title Format

Use the same format as commit messages:

```
<type>(<scope>): <description>
```

### PR Description Template

```markdown
## Summary
Brief description of the changes.

## Related Issues
Fixes #<issue-number>

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated (if applicable)
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated (if applicable)
- [ ] No new warnings introduced
```

### PR Requirements

1. **Linked Issue**: Every PR must reference an issue
2. **Passing CI**: All GitHub Actions checks must pass
3. **Code Review**: At least one approval required
4. **Up to Date**: Branch must be current with target branch
5. **No Conflicts**: Merge conflicts must be resolved

### Merge Strategy

- Use **Squash and Merge** for feature branches
- Use **Merge Commit** for release branches
- Delete source branch after merge

---

## Code Review Guidelines

### For Authors

- Keep PRs focused and reasonably sized (< 400 lines ideal)
- Provide context in the PR description
- Respond to feedback promptly
- Be open to suggestions

### For Reviewers

Review for:

1. **Correctness**: Does the code do what it's supposed to?
2. **Design**: Does it follow SOLID principles and project patterns?
3. **Readability**: Is the code clear and self-documenting?
4. **Testing**: Are tests adequate and meaningful?
5. **Performance**: Any obvious performance issues?
6. **Security**: Any potential security vulnerabilities?

### Review Checklist

- [ ] Code follows [Code Style](CODE_STYLE.md) guidelines
- [ ] SOLID principles applied appropriately
- [ ] Appropriate design patterns used
- [ ] Unit tests included and passing
- [ ] Test coverage adequate for changes
- [ ] No commented-out code
- [ ] No TODOs without linked issues
- [ ] Error handling is appropriate
- [ ] No magic numbers or strings
- [ ] Documentation updated if needed

### Feedback Guidelines

- Be specific and actionable
- Explain the "why" behind suggestions
- Distinguish between required changes and suggestions
- Use prefixes:
  - `[Required]` - Must fix before merge
  - `[Suggestion]` - Nice to have, author's discretion
  - `[Question]` - Seeking clarification
  - `[Nitpick]` - Minor style preference

---

## Definition of Done

An issue is considered **Done** when:

### Code Complete
- [ ] All acceptance criteria met
- [ ] Code compiles without warnings
- [ ] Code follows style guidelines
- [ ] No TODOs left in code (unless tracked as new issues)

### Testing Complete
- [ ] Unit tests written and passing
- [ ] Integration tests written (if applicable)
- [ ] Coverage thresholds maintained
- [ ] Edge cases considered and tested

### Quality Gates Passed
- [ ] Checkstyle passes
- [ ] SpotBugs passes (no high-priority bugs)
- [ ] PMD passes (no priority 1-2 violations)
- [ ] No security vulnerabilities introduced

### Review Complete
- [ ] PR reviewed and approved
- [ ] All review feedback addressed
- [ ] CI pipeline passes

### Documentation
- [ ] Code is self-documenting or commented appropriately
- [ ] Public APIs have Javadoc
- [ ] README/docs updated if needed

### Merged
- [ ] PR merged to target branch
- [ ] Source branch deleted
- [ ] Issue closed and linked to PR

---

## Questions?

If you have questions about contributing:
1. Check existing documentation
2. Search closed issues/PRs
3. Open a discussion or issue

We're happy to help you contribute!
