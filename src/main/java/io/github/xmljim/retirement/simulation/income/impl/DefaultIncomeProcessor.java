package io.github.xmljim.retirement.simulation.income.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import io.github.xmljim.retirement.domain.enums.SimulationPhase;
import io.github.xmljim.retirement.simulation.income.IncomeProcessor;
import io.github.xmljim.retirement.simulation.income.IncomeProfile;
import io.github.xmljim.retirement.simulation.income.MonthlyIncome;

/**
 * Default implementation of IncomeProcessor.
 *
 * <p>Processes income based on simulation phase:
 * <ul>
 *   <li><b>ACCUMULATION:</b> Includes salary income for working persons</li>
 *   <li><b>DISTRIBUTION:</b> Includes SS, pension, annuity, and other income</li>
 *   <li><b>SURVIVOR:</b> Same as distribution, adjusted for survivor status</li>
 * </ul>
 *
 * <p>Income is calculated for each person individually, then aggregated.
 * Each income source has its own start date and COLA rules.
 */
public class DefaultIncomeProcessor implements IncomeProcessor {

    @Override
    public MonthlyIncome process(List<IncomeProfile> incomeProfiles, YearMonth month, SimulationPhase phase) {
        if (incomeProfiles == null || incomeProfiles.isEmpty()) {
            return MonthlyIncome.zero();
        }

        MonthlyIncome.Builder builder = MonthlyIncome.builder();
        LocalDate date = month.atDay(1);

        for (IncomeProfile profile : incomeProfiles) {
            processProfile(profile, date, phase, builder);
        }

        return builder.build();
    }

    /**
     * Processes a single income profile.
     *
     * @param profile the income profile
     * @param date    the date to calculate income for
     * @param phase   the simulation phase
     * @param builder the builder to accumulate results
     */
    private void processProfile(
            IncomeProfile profile,
            LocalDate date,
            SimulationPhase phase,
            MonthlyIncome.Builder builder) {

        // Process salary income (only during accumulation, before retirement)
        if (phase == SimulationPhase.ACCUMULATION) {
            processSalaryIncome(profile, date, builder);
        }

        // Process retirement income sources
        // SS, pensions, annuities, other income are available in all phases
        // but only if their start date has passed
        processSocialSecurity(profile, date, builder);
        processPensions(profile, date, builder);
        processAnnuities(profile, date, builder);
        processOtherIncome(profile, date, builder);
    }

    /**
     * Processes salary income for a profile.
     */
    private void processSalaryIncome(
            IncomeProfile profile,
            LocalDate date,
            MonthlyIncome.Builder builder) {

        profile.getWorkingIncome()
            .filter(wi -> wi.isActiveOn(date))
            .map(wi -> wi.getMonthlySalary(date))
            .ifPresent(builder::addSalaryIncome);
    }

    /**
     * Processes Social Security income for a profile.
     */
    private void processSocialSecurity(
            IncomeProfile profile,
            LocalDate date,
            MonthlyIncome.Builder builder) {

        profile.getSocialSecurity()
            .map(ss -> ss.getMonthlyBenefit(date))
            .filter(benefit -> benefit.compareTo(BigDecimal.ZERO) > 0)
            .ifPresent(builder::addSocialSecurityIncome);
    }

    /**
     * Processes pension income for a profile.
     */
    private void processPensions(
            IncomeProfile profile,
            LocalDate date,
            MonthlyIncome.Builder builder) {

        profile.pensions().stream()
            .map(pension -> pension.getMonthlyBenefit(date))
            .filter(benefit -> benefit.compareTo(BigDecimal.ZERO) > 0)
            .forEach(builder::addPensionIncome);
    }

    /**
     * Processes annuity income for a profile.
     */
    private void processAnnuities(
            IncomeProfile profile,
            LocalDate date,
            MonthlyIncome.Builder builder) {

        profile.annuities().stream()
            .map(annuity -> annuity.getMonthlyBenefitAsOf(date))
            .filter(benefit -> benefit.compareTo(BigDecimal.ZERO) > 0)
            .forEach(builder::addAnnuityIncome);
    }

    /**
     * Processes other income sources for a profile.
     */
    private void processOtherIncome(
            IncomeProfile profile,
            LocalDate date,
            MonthlyIncome.Builder builder) {

        profile.otherIncomes().stream()
            .map(other -> other.getMonthlyIncome(date))
            .filter(income -> income.compareTo(BigDecimal.ZERO) > 0)
            .forEach(builder::addOtherIncome);
    }
}
