package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.domain.calculator.IncomeAggregator;
import io.github.xmljim.retirement.domain.value.Annuity;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;
import io.github.xmljim.retirement.domain.value.IncomeSources;
import io.github.xmljim.retirement.domain.value.OtherIncome;
import io.github.xmljim.retirement.domain.value.Pension;

/**
 * Default implementation of income aggregation.
 *
 * <p>Aggregates income from all configured sources for a given date,
 * applying COLA/inflation adjustments and filtering by active date ranges.
 */
@Service
public class DefaultIncomeAggregator implements IncomeAggregator {

    @Override
    public IncomeBreakdown getMonthlyIncome(IncomeSources sources, LocalDate asOfDate) {
        if (sources == null || asOfDate == null) {
            return IncomeBreakdown.empty(asOfDate != null ? asOfDate : LocalDate.now());
        }

        IncomeBreakdown.Builder builder = IncomeBreakdown.builder()
            .asOfDate(asOfDate);

        // Working income (salary) - earned income
        sources.getWorkingIncome().ifPresent(income -> {
            BigDecimal salary = income.getMonthlySalary(asOfDate);
            builder.addToSalary(salary);
            builder.addToEarnedIncome(salary);
        });

        // Social Security - partially taxable, passive for earnings test purposes
        sources.getSocialSecurityBenefit().ifPresent(benefit -> {
            BigDecimal ssBenefit = benefit.getMonthlyBenefit(asOfDate);
            builder.addToSocialSecurity(ssBenefit);
            builder.addToPassiveIncome(ssBenefit);
        });

        // Pensions - passive income
        for (Pension pension : sources.getPensions()) {
            BigDecimal pensionBenefit = pension.getMonthlyBenefit(asOfDate);
            builder.addToPension(pensionBenefit);
            builder.addToPassiveIncome(pensionBenefit);
        }

        // Annuities - passive income
        for (Annuity annuity : sources.getAnnuities()) {
            BigDecimal annuityBenefit = annuity.getMonthlyBenefitAsOf(asOfDate);
            builder.addToAnnuity(annuityBenefit);
            builder.addToPassiveIncome(annuityBenefit);
        }

        // Other income - earned or passive depending on type
        for (OtherIncome other : sources.getOtherIncomes()) {
            BigDecimal otherAmount = other.getMonthlyIncome(asOfDate);
            builder.addToOther(otherAmount);
            if (other.isEarnedIncome()) {
                builder.addToEarnedIncome(otherAmount);
            } else {
                builder.addToPassiveIncome(otherAmount);
            }
        }

        return builder.build();
    }

    @Override
    public IncomeBreakdown combineForCouple(IncomeBreakdown primary, IncomeBreakdown secondary) {
        if (primary == null && secondary == null) {
            return IncomeBreakdown.empty(LocalDate.now());
        }
        if (primary == null) {
            return secondary;
        }
        if (secondary == null) {
            return primary;
        }

        return new IncomeBreakdown(
            primary.asOfDate(),
            primary.salary().add(secondary.salary()),
            primary.socialSecurity().add(secondary.socialSecurity()),
            primary.pension().add(secondary.pension()),
            primary.annuity().add(secondary.annuity()),
            primary.other().add(secondary.other()),
            primary.total().add(secondary.total()),
            primary.earnedIncome().add(secondary.earnedIncome()),
            primary.passiveIncome().add(secondary.passiveIncome())
        );
    }
}
