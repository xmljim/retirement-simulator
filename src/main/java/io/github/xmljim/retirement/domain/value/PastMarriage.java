package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import io.github.xmljim.retirement.domain.enums.MarriageEndReason;
import io.github.xmljim.retirement.domain.model.PersonProfile;

/**
 * Represents a past marriage that has ended.
 *
 * <p>Used for calculating divorced spouse and survivor benefits from
 * ex-spouses. The ex-spouse can be either a fully modeled PersonProfile
 * or just their Social Security FRA benefit amount.
 *
 * <p>SSA Rules:
 * <ul>
 *   <li>Divorced spouse benefits: Marriage must have lasted 10+ years</li>
 *   <li>Survivor benefits: Marriage must have lasted 9+ months</li>
 * </ul>
 *
 * @param exSpouse the ex-spouse profile if fully modeled in the simulation
 * @param exSpouseFraBenefit the ex-spouse's FRA benefit if known (used when profile not available)
 * @param marriageDate when the marriage began
 * @param endDate when the marriage ended
 * @param endReason how the marriage ended (DIVORCED or SPOUSE_DIED)
 */
public record PastMarriage(
        Optional<PersonProfile> exSpouse,
        Optional<BigDecimal> exSpouseFraBenefit,
        LocalDate marriageDate,
        LocalDate endDate,
        MarriageEndReason endReason
) {

    private static final int DIVORCED_SPOUSE_MIN_MARRIAGE_MONTHS = 120; // 10 years
    private static final int SURVIVOR_MIN_MARRIAGE_MONTHS = 9;

    /**
     * Creates a PastMarriage with a fully modeled ex-spouse.
     *
     * @param exSpouse the ex-spouse profile
     * @param marriageDate when the marriage began
     * @param endDate when the marriage ended
     * @param endReason how the marriage ended
     * @return a new PastMarriage
     */
    public static PastMarriage withProfile(
            PersonProfile exSpouse,
            LocalDate marriageDate,
            LocalDate endDate,
            MarriageEndReason endReason) {
        return new PastMarriage(
            Optional.of(exSpouse),
            Optional.empty(),
            marriageDate,
            endDate,
            endReason
        );
    }

    /**
     * Creates a PastMarriage with just the ex-spouse's FRA benefit.
     *
     * <p>Use this when the ex-spouse is not fully modeled in the simulation
     * but their Social Security benefit is known.
     *
     * @param fraBenefit the ex-spouse's FRA benefit amount
     * @param marriageDate when the marriage began
     * @param endDate when the marriage ended
     * @param endReason how the marriage ended
     * @return a new PastMarriage
     */
    public static PastMarriage withBenefit(
            BigDecimal fraBenefit,
            LocalDate marriageDate,
            LocalDate endDate,
            MarriageEndReason endReason) {
        return new PastMarriage(
            Optional.empty(),
            Optional.of(fraBenefit),
            marriageDate,
            endDate,
            endReason
        );
    }

    /**
     * Returns the duration of the marriage in months.
     *
     * @return marriage duration in months
     */
    public int getDurationMonths() {
        return (int) ChronoUnit.MONTHS.between(marriageDate, endDate);
    }

    /**
     * Returns the duration of the marriage in years.
     *
     * @return marriage duration in years
     */
    public int getDurationYears() {
        return (int) ChronoUnit.YEARS.between(marriageDate, endDate);
    }

    /**
     * Checks if this marriage qualifies for divorced spouse benefits.
     *
     * <p>Requires:
     * <ul>
     *   <li>Marriage lasted 10+ years</li>
     *   <li>Marriage ended in divorce</li>
     * </ul>
     *
     * @return true if potentially eligible for divorced spouse benefits
     */
    public boolean qualifiesForDivorcedSpouseBenefits() {
        return endReason == MarriageEndReason.DIVORCED
            && getDurationMonths() >= DIVORCED_SPOUSE_MIN_MARRIAGE_MONTHS;
    }

    /**
     * Checks if this marriage qualifies for survivor benefits.
     *
     * <p>Requires:
     * <ul>
     *   <li>Marriage lasted 9+ months</li>
     *   <li>Spouse died</li>
     * </ul>
     *
     * @return true if potentially eligible for survivor benefits
     */
    public boolean qualifiesForSurvivorBenefits() {
        return endReason == MarriageEndReason.SPOUSE_DIED
            && getDurationMonths() >= SURVIVOR_MIN_MARRIAGE_MONTHS;
    }

    /**
     * Returns the ex-spouse's FRA benefit, either from the profile or directly provided.
     *
     * @return optional containing the FRA benefit if available
     */
    public Optional<BigDecimal> getExSpouseFraBenefitAmount() {
        if (exSpouseFraBenefit.isPresent()) {
            return exSpouseFraBenefit;
        }
        // If we have a profile, we'd need to get their SS benefit from elsewhere
        // This is a limitation - the PersonProfile doesn't directly store FRA benefit
        return Optional.empty();
    }

    /**
     * Checks if the divorce was final at least the specified number of years ago.
     *
     * @param years minimum years since divorce
     * @param asOf the date to check against
     * @return true if divorced at least the specified years ago
     */
    public boolean divorcedAtLeastYearsAgo(int years, LocalDate asOf) {
        if (endReason != MarriageEndReason.DIVORCED) {
            return false;
        }
        return ChronoUnit.YEARS.between(endDate, asOf) >= years;
    }
}
