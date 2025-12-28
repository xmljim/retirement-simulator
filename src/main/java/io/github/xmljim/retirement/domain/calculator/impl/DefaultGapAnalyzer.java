package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.github.xmljim.retirement.domain.calculator.GapAnalyzer;
import io.github.xmljim.retirement.domain.exception.MissingRequiredFieldException;
import io.github.xmljim.retirement.domain.value.ExpenseBreakdown;
import io.github.xmljim.retirement.domain.value.GapAnalysis;
import io.github.xmljim.retirement.domain.value.IncomeBreakdown;

/**
 * Default implementation of {@link GapAnalyzer}.
 *
 * <p>Provides income vs expense gap analysis with support for:
 * <ul>
 *   <li>Monthly and annual analysis</li>
 *   <li>Tax-aware withdrawal calculations</li>
 *   <li>Year-long projections</li>
 *   <li>Summary statistics</li>
 * </ul>
 */
public class DefaultGapAnalyzer implements GapAnalyzer {

    private static final int SCALE = 2;
    private static final int MONTHS_PER_YEAR = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Override
    public GapAnalysis analyzeMonthly(IncomeBreakdown income, ExpenseBreakdown expenses, LocalDate asOfDate) {
        MissingRequiredFieldException.requireNonNull(income, "income");
        MissingRequiredFieldException.requireNonNull(expenses, "expenses");
        MissingRequiredFieldException.requireNonNull(asOfDate, "asOfDate");

        BigDecimal totalIncome = income.total();
        BigDecimal totalExpenses = expenses.total();

        return GapAnalysis.of(asOfDate, totalIncome, totalExpenses);
    }

    @Override
    public GapAnalysis analyzeAnnual(IncomeBreakdown income, ExpenseBreakdown expenses, LocalDate asOfDate) {
        MissingRequiredFieldException.requireNonNull(income, "income");
        MissingRequiredFieldException.requireNonNull(expenses, "expenses");
        MissingRequiredFieldException.requireNonNull(asOfDate, "asOfDate");

        BigDecimal annualMultiplier = BigDecimal.valueOf(MONTHS_PER_YEAR);
        BigDecimal totalIncome = income.total().multiply(annualMultiplier);
        BigDecimal totalExpenses = expenses.total().multiply(annualMultiplier);

        return GapAnalysis.of(asOfDate, totalIncome, totalExpenses);
    }

    @Override
    public List<GapAnalysis> projectYear(
            Function<LocalDate, IncomeBreakdown> incomeProvider,
            Function<LocalDate, ExpenseBreakdown> expenseProvider,
            LocalDate startDate) {

        MissingRequiredFieldException.requireNonNull(incomeProvider, "incomeProvider");
        MissingRequiredFieldException.requireNonNull(expenseProvider, "expenseProvider");
        MissingRequiredFieldException.requireNonNull(startDate, "startDate");

        List<GapAnalysis> projections = new ArrayList<>();
        LocalDate currentDate = startDate;

        for (int i = 0; i < MONTHS_PER_YEAR; i++) {
            IncomeBreakdown income = incomeProvider.apply(currentDate);
            ExpenseBreakdown expenses = expenseProvider.apply(currentDate);
            GapAnalysis analysis = analyzeMonthly(income, expenses, currentDate);
            projections.add(analysis);
            currentDate = currentDate.plusMonths(1);
        }

        return projections;
    }

    @Override
    public BigDecimal calculateGrossWithdrawal(BigDecimal netNeeded, BigDecimal marginalTaxRate) {
        if (netNeeded == null || netNeeded.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (marginalTaxRate == null || marginalTaxRate.compareTo(BigDecimal.ZERO) <= 0) {
            return netNeeded.setScale(SCALE, ROUNDING);
        }

        if (marginalTaxRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Marginal tax rate must be less than 1.0");
        }

        // gross = net / (1 - taxRate)
        BigDecimal netFactor = BigDecimal.ONE.subtract(marginalTaxRate);
        return netNeeded.divide(netFactor, SCALE, ROUNDING);
    }

    @Override
    public GapAnalysis summarize(List<GapAnalysis> monthlyAnalyses) {
        if (monthlyAnalyses == null || monthlyAnalyses.isEmpty()) {
            return GapAnalysis.empty(LocalDate.now());
        }

        LocalDate firstDate = monthlyAnalyses.get(0).asOfDate();

        BigDecimal totalIncome = monthlyAnalyses.stream()
                .map(GapAnalysis::totalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = monthlyAnalyses.stream()
                .map(GapAnalysis::totalExpenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return GapAnalysis.of(firstDate, totalIncome, totalExpenses);
    }
}
