package io.github.xmljim.retirement.domain.value;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import io.github.xmljim.retirement.domain.enums.MaritalStatus;

/**
 * Marriage information for Social Security benefit eligibility.
 *
 * <p>This record captures the details needed to determine eligibility for:
 * <ul>
 *   <li>Spousal benefits (requires 1+ year marriage)</li>
 *   <li>Divorced spouse benefits (requires 10+ year marriage, 2+ years divorced)</li>
 *   <li>Survivor benefits (requires 9+ months marriage)</li>
 * </ul>
 *
 * <p>All date fields use {@link Optional} to clearly express optionality.
 * Use the static factory methods for convenient creation:
 * <pre>{@code
 * // Single person
 * MarriageInfo single = MarriageInfo.single();
 *
 * // Married couple
 * MarriageInfo married = MarriageInfo.married(LocalDate.of(2010, 6, 15));
 *
 * // Divorced
 * MarriageInfo divorced = MarriageInfo.divorced(
 *     LocalDate.of(2000, 5, 1),
 *     LocalDate.of(2015, 3, 20)
 * );
 *
 * // Widowed
 * MarriageInfo widowed = MarriageInfo.widowed(
 *     LocalDate.of(1990, 8, 12),
 *     LocalDate.of(2023, 11, 5)
 * );
 * }</pre>
 *
 * @param status the current marital status
 * @param marriageDate the date of marriage (empty for SINGLE)
 * @param divorceDate the date of divorce (only for DIVORCED status)
 * @param spouseDeathDate the date spouse died (only for WIDOWED status)
 * @param livingWithSpouse whether currently living with spouse (affects MFS filing)
 *
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying7.html">SSA Spousal Benefits</a>
 * @see <a href="https://www.ssa.gov/benefits/retirement/planner/applying6.html">SSA Divorced Spouse</a>
 * @see <a href="https://www.ssa.gov/benefits/survivors/">SSA Survivor Benefits</a>
 */
public record MarriageInfo(
    MaritalStatus status,
    Optional<LocalDate> marriageDate,
    Optional<LocalDate> divorceDate,
    Optional<LocalDate> spouseDeathDate,
    boolean livingWithSpouse
) {

    /**
     * Canonical constructor with validation.
     */
    public MarriageInfo {
        if (status == null) {
            throw new IllegalArgumentException("Marital status cannot be null");
        }
        marriageDate = marriageDate != null ? marriageDate : Optional.empty();
        divorceDate = divorceDate != null ? divorceDate : Optional.empty();
        spouseDeathDate = spouseDeathDate != null ? spouseDeathDate : Optional.empty();
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a MarriageInfo for a single (never married) person.
     *
     * @return MarriageInfo with SINGLE status
     */
    public static MarriageInfo single() {
        return new MarriageInfo(
            MaritalStatus.SINGLE,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            false
        );
    }

    /**
     * Creates a MarriageInfo for a married person.
     *
     * @param marriageDate the date of marriage
     * @return MarriageInfo with MARRIED status
     */
    public static MarriageInfo married(LocalDate marriageDate) {
        return new MarriageInfo(
            MaritalStatus.MARRIED,
            Optional.of(marriageDate),
            Optional.empty(),
            Optional.empty(),
            true
        );
    }

    /**
     * Creates a MarriageInfo for a married person with living arrangement specified.
     *
     * @param marriageDate the date of marriage
     * @param livingTogether whether currently living with spouse
     * @return MarriageInfo with MARRIED status
     */
    public static MarriageInfo married(LocalDate marriageDate, boolean livingTogether) {
        return new MarriageInfo(
            MaritalStatus.MARRIED,
            Optional.of(marriageDate),
            Optional.empty(),
            Optional.empty(),
            livingTogether
        );
    }

    /**
     * Creates a MarriageInfo for a divorced person.
     *
     * @param marriageDate the date of original marriage
     * @param divorceDate the date of divorce
     * @return MarriageInfo with DIVORCED status
     */
    public static MarriageInfo divorced(LocalDate marriageDate, LocalDate divorceDate) {
        return new MarriageInfo(
            MaritalStatus.DIVORCED,
            Optional.of(marriageDate),
            Optional.of(divorceDate),
            Optional.empty(),
            false
        );
    }

    /**
     * Creates a MarriageInfo for a widowed person.
     *
     * @param marriageDate the date of original marriage
     * @param spouseDeathDate the date spouse died
     * @return MarriageInfo with WIDOWED status
     */
    public static MarriageInfo widowed(LocalDate marriageDate, LocalDate spouseDeathDate) {
        return new MarriageInfo(
            MaritalStatus.WIDOWED,
            Optional.of(marriageDate),
            Optional.empty(),
            Optional.of(spouseDeathDate),
            false
        );
    }

    // ==================== Marriage Duration Methods ====================

    /**
     * Calculates the duration of marriage in months.
     *
     * <p>For divorced/widowed, calculates from marriage date to divorce/death date.
     * For married, calculates from marriage date to the specified date.
     *
     * @param asOf the reference date for calculation
     * @return the marriage duration in months, or 0 if never married
     */
    public int getMarriageDurationMonths(LocalDate asOf) {
        return marriageDate
            .map(start -> {
                LocalDate end = getMarriageEndDate().orElse(asOf);
                Period period = Period.between(start, end);
                return period.getYears() * 12 + period.getMonths();
            })
            .orElse(0);
    }

    /**
     * Gets the effective end date of the marriage.
     *
     * @return divorce date if divorced, death date if widowed, empty if still married
     */
    public Optional<LocalDate> getMarriageEndDate() {
        return switch (status) {
            case DIVORCED -> divorceDate;
            case WIDOWED -> spouseDeathDate;
            case MARRIED, SINGLE -> Optional.empty();
        };
    }

    /**
     * Checks if the marriage meets a minimum duration requirement.
     *
     * @param minimumMonths the minimum months required
     * @param asOf the reference date
     * @return true if marriage duration >= minimumMonths
     */
    public boolean meetsMinimumMarriageDuration(int minimumMonths, LocalDate asOf) {
        return getMarriageDurationMonths(asOf) >= minimumMonths;
    }

    // ==================== Spousal Benefit Eligibility ====================

    /**
     * Checks eligibility for spousal benefits (married persons).
     *
     * <p>Requirements:
     * <ul>
     *   <li>Currently married</li>
     *   <li>Married at least 12 months</li>
     * </ul>
     *
     * @param asOf the reference date
     * @return true if eligible for spousal benefits
     */
    public boolean isEligibleForSpousalBenefits(LocalDate asOf) {
        return status == MaritalStatus.MARRIED
            && meetsMinimumMarriageDuration(12, asOf);
    }

    // ==================== Divorced Spouse Eligibility ====================

    /**
     * Calculates the years since divorce.
     *
     * @param asOf the reference date
     * @return years since divorce, or 0 if not divorced
     */
    public int getYearsSinceDivorce(LocalDate asOf) {
        return divorceDate
            .map(divorce -> Period.between(divorce, asOf).getYears())
            .orElse(0);
    }

    /**
     * Checks if divorced long enough for ex-spouse benefits.
     *
     * @param minimumYears the minimum years required (typically 2)
     * @param asOf the reference date
     * @return true if divorced >= minimumYears
     */
    public boolean isDivorcedMinimumYears(int minimumYears, LocalDate asOf) {
        return status == MaritalStatus.DIVORCED
            && getYearsSinceDivorce(asOf) >= minimumYears;
    }

    /**
     * Checks eligibility for divorced spouse benefits.
     *
     * <p>Requirements:
     * <ul>
     *   <li>Currently divorced (not remarried before 60)</li>
     *   <li>Marriage lasted at least 10 years</li>
     *   <li>Divorced for at least 2 years</li>
     * </ul>
     *
     * @param asOf the reference date
     * @return true if eligible for divorced spouse benefits
     */
    public boolean isEligibleForDivorcedSpouseBenefits(LocalDate asOf) {
        return status == MaritalStatus.DIVORCED
            && meetsMinimumMarriageDuration(120, asOf)  // 10 years
            && isDivorcedMinimumYears(2, asOf);
    }

    // ==================== Survivor Benefit Eligibility ====================

    /**
     * Checks eligibility for survivor benefits (widowed persons).
     *
     * <p>Requirements:
     * <ul>
     *   <li>Widowed status</li>
     *   <li>Marriage lasted at least 9 months (exception for accidents)</li>
     * </ul>
     *
     * @param asOf the reference date
     * @return true if eligible for survivor benefits
     */
    public boolean isEligibleForSurvivorBenefits(LocalDate asOf) {
        return status == MaritalStatus.WIDOWED
            && meetsMinimumMarriageDuration(9, asOf);
    }

    /**
     * Checks if remarriage occurred after a certain age (affects survivor benefit retention).
     *
     * <p>Survivors who remarry after age 60 retain their survivor benefits.
     *
     * @param ageMonthsAtRemarriage the age in months when remarriage occurred
     * @param thresholdMonths the age threshold (typically 720 = 60 years)
     * @return true if remarriage was at or after the threshold age
     */
    public boolean remarriedAfterAge(int ageMonthsAtRemarriage, int thresholdMonths) {
        return ageMonthsAtRemarriage >= thresholdMonths;
    }
}
