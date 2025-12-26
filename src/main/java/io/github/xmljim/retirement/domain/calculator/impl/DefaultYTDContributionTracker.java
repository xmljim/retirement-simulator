package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.xmljim.retirement.domain.calculator.IrsContributionRules;
import io.github.xmljim.retirement.domain.calculator.YTDContributionTracker;
import io.github.xmljim.retirement.domain.config.IrsContributionLimits;
import io.github.xmljim.retirement.domain.enums.AccountType;
import io.github.xmljim.retirement.domain.enums.LimitCategory;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ContributionRecord;
import io.github.xmljim.retirement.domain.value.YTDSummary;

/**
 * Immutable implementation of YTD contribution tracking.
 *
 * <p>This implementation stores contribution records and provides
 * aggregation by year and limit category. It is immutable - calling
 * {@link #recordContribution} returns a new tracker instance.
 *
 * <p>Internally tracks:
 * <ul>
 *   <li>All contribution records by year</li>
 *   <li>Aggregated personal contributions by year and category</li>
 *   <li>Aggregated employer contributions by year and category</li>
 * </ul>
 */
public class DefaultYTDContributionTracker implements YTDContributionTracker {

    private final IrsContributionRules irsRules;
    private final Map<Integer, List<ContributionRecord>> recordsByYear;
    private final Map<Integer, Map<LimitCategory, BigDecimal>> personalByYearCategory;
    private final Map<Integer, Map<LimitCategory, BigDecimal>> employerByYearCategory;

    /**
     * Creates an empty tracker.
     *
     * @param irsRules the IRS rules for limit calculations
     */
    public DefaultYTDContributionTracker(IrsContributionRules irsRules) {
        MissingRequiredFieldException.requireNonNull(irsRules, "irsRules");
        this.irsRules = irsRules;
        this.recordsByYear = Collections.emptyMap();
        this.personalByYearCategory = Collections.emptyMap();
        this.employerByYearCategory = Collections.emptyMap();
    }

    // Private constructor for immutable copy
    private DefaultYTDContributionTracker(
            IrsContributionRules irsRules,
            Map<Integer, List<ContributionRecord>> recordsByYear,
            Map<Integer, Map<LimitCategory, BigDecimal>> personalByYearCategory,
            Map<Integer, Map<LimitCategory, BigDecimal>> employerByYearCategory) {
        this.irsRules = irsRules;
        this.recordsByYear = recordsByYear;
        this.personalByYearCategory = personalByYearCategory;
        this.employerByYearCategory = employerByYearCategory;
    }

    @Override
    public YTDContributionTracker recordContribution(ContributionRecord record) {
        MissingRequiredFieldException.requireNonNull(record, "record");

        // Create new records map with the new record added
        Map<Integer, List<ContributionRecord>> newRecords = new HashMap<>(recordsByYear);
        List<ContributionRecord> yearRecords = new ArrayList<>(
            newRecords.getOrDefault(record.year(), Collections.emptyList()));
        yearRecords.add(record);
        newRecords.put(record.year(), Collections.unmodifiableList(yearRecords));

        // Update aggregates
        LimitCategory category = record.getLimitCategory();
        Map<Integer, Map<LimitCategory, BigDecimal>> newPersonal =
            new HashMap<>(personalByYearCategory);
        Map<Integer, Map<LimitCategory, BigDecimal>> newEmployer =
            new HashMap<>(employerByYearCategory);

        if (category != null) {
            if (record.isPersonal()) {
                updateAggregate(newPersonal, record.year(), category, record.amount());
            } else {
                updateAggregate(newEmployer, record.year(), category, record.amount());
            }
        }

        return new DefaultYTDContributionTracker(
            irsRules,
            Collections.unmodifiableMap(newRecords),
            Collections.unmodifiableMap(newPersonal),
            Collections.unmodifiableMap(newEmployer)
        );
    }

    private void updateAggregate(
            Map<Integer, Map<LimitCategory, BigDecimal>> aggregates,
            int year,
            LimitCategory category,
            BigDecimal amount) {

        Map<LimitCategory, BigDecimal> yearMap = new EnumMap<>(LimitCategory.class);
        Map<LimitCategory, BigDecimal> existing = aggregates.get(year);
        if (existing != null) {
            yearMap.putAll(existing);
        }
        BigDecimal current = yearMap.getOrDefault(category, BigDecimal.ZERO);
        yearMap.put(category, current.add(amount));
        aggregates.put(year, Collections.unmodifiableMap(yearMap));
    }

    @Override
    public BigDecimal getYTDPersonalContributions(int year, LimitCategory category) {
        Map<LimitCategory, BigDecimal> yearMap = personalByYearCategory.get(year);
        if (yearMap == null) {
            return BigDecimal.ZERO;
        }
        return yearMap.getOrDefault(category, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getYTDEmployerContributions(int year, LimitCategory category) {
        Map<LimitCategory, BigDecimal> yearMap = employerByYearCategory.get(year);
        if (yearMap == null) {
            return BigDecimal.ZERO;
        }
        return yearMap.getOrDefault(category, BigDecimal.ZERO);
    }

    @Override
    public List<ContributionRecord> getRecordsForYear(int year) {
        return recordsByYear.getOrDefault(year, Collections.emptyList());
    }

    @Override
    public YTDSummary getSummary(int year, int age, boolean hasSpouse) {
        return getSummary(year, age, hasSpouse, irsRules);
    }

    @Override
    public YTDSummary getSummary(int year, int age, boolean hasSpouse, IrsContributionRules rules) {
        YTDSummary.Builder builder = YTDSummary.builder(year);

        for (LimitCategory category : LimitCategory.values()) {
            BigDecimal personal = getYTDPersonalContributions(year, category);
            BigDecimal employer = getYTDEmployerContributions(year, category);
            BigDecimal limit = calculateLimit(category, year, age, hasSpouse, rules);
            BigDecimal remaining = limit.subtract(personal).max(BigDecimal.ZERO);

            builder.personalContribution(category, personal)
                   .employerContribution(category, employer)
                   .limit(category, limit)
                   .remainingRoom(category, remaining);
        }

        return builder.build();
    }

    /**
     * Calculates the applicable limit for a category.
     */
    private BigDecimal calculateLimit(
            LimitCategory category,
            int year,
            int age,
            boolean hasSpouse,
            IrsContributionRules rules) {

        return switch (category) {
            case EMPLOYER_401K -> rules.calculateAnnualContributionLimit(year, age, AccountType.TRADITIONAL_401K);
            case IRA -> calculateIraLimit(year, age, rules);
            case HSA -> calculateHsaLimit(year, age, hasSpouse, rules);
        };
    }

    private BigDecimal calculateIraLimit(int year, int age, IrsContributionRules rules) {
        // IRA uses different limits than 401k - need to get from IrsContributionLimits
        // For now, use the IRS rules which should handle IRA limits
        // Note: IRA doesn't have super catch-up, just standard catch-up at 50+
        if (rules instanceof Secure2ContributionRules secure2Rules) {
            return getIraLimitFromConfig(secure2Rules, year, age);
        }
        // Fallback - shouldn't happen in practice
        return rules.calculateAnnualContributionLimit(year, age, AccountType.TRADITIONAL_IRA);
    }

    private BigDecimal getIraLimitFromConfig(Secure2ContributionRules rules, int year, int age) {
        // Access the underlying IrsContributionLimits for IRA-specific limits
        // This is a workaround since IrsContributionRules is designed for 401k primarily
        try {
            var field = Secure2ContributionRules.class.getDeclaredField("limits");
            field.setAccessible(true);
            IrsContributionLimits limits = (IrsContributionLimits) field.get(rules);
            var iraLimits = limits.getIraLimitsForYear(year);
            BigDecimal base = iraLimits.baseLimit();
            if (age >= 50) {
                return base.add(iraLimits.catchUpLimit());
            }
            return base;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Fallback to default behavior
            return rules.calculateAnnualContributionLimit(year, age, AccountType.TRADITIONAL_IRA);
        }
    }

    private BigDecimal calculateHsaLimit(int year, int age, boolean hasSpouse, IrsContributionRules rules) {
        // HSA limits need to come from IrsContributionLimits
        if (rules instanceof Secure2ContributionRules secure2Rules) {
            return getHsaLimitFromConfig(secure2Rules, year, age, hasSpouse);
        }
        // Fallback
        return BigDecimal.ZERO;
    }

    private BigDecimal getHsaLimitFromConfig(Secure2ContributionRules rules, int year, int age, boolean hasSpouse) {
        try {
            var field = Secure2ContributionRules.class.getDeclaredField("limits");
            field.setAccessible(true);
            IrsContributionLimits limits = (IrsContributionLimits) field.get(rules);
            var hsaLimits = limits.getHsaLimitsForYear(year);
            BigDecimal base = hasSpouse ? hsaLimits.familyLimit() : hsaLimits.individualLimit();
            // HSA catch-up starts at age 55
            if (age >= 55) {
                return base.add(hsaLimits.catchUpLimit());
            }
            return base;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return BigDecimal.ZERO;
        }
    }
}
