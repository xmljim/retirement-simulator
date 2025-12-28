package io.github.xmljim.retirement.domain.calculator.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.calculator.FederalTaxCalculator;
import io.github.xmljim.retirement.domain.config.FederalTaxRules;
import io.github.xmljim.retirement.domain.config.FederalTaxRules.BracketConfig;
import io.github.xmljim.retirement.domain.enums.FilingStatus;
import io.github.xmljim.retirement.domain.value.TaxBracket;
import io.github.xmljim.retirement.domain.value.TaxCalculationResult;
import io.github.xmljim.retirement.domain.value.TaxCalculationResult.BracketTax;

/**
 * Default implementation of federal income tax calculator.
 *
 * <p>Calculates federal tax using progressive tax brackets with
 * chained CPI indexing for future year projections.
 *
 * @see FederalTaxCalculator
 * @see FederalTaxRules
 */
@Service
public class DefaultFederalTaxCalculator implements FederalTaxCalculator {

    private static final int SCALE = 10;
    private static final int AGE_65 = 65;
    private static final BigDecimal FIFTY = new BigDecimal("50");

    private final FederalTaxRules rules;

    /**
     * Creates a calculator with Spring-managed rules.
     *
     * @param rules the federal tax rules configuration
     */
    @Autowired
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring-managed beans")
    public DefaultFederalTaxCalculator(FederalTaxRules rules) {
        this.rules = rules;
    }

    /**
     * Creates a calculator with default rules.
     */
    public DefaultFederalTaxCalculator() {
        this.rules = new FederalTaxRules();
    }

    @Override
    public TaxCalculationResult calculateTax(
            BigDecimal grossIncome,
            FilingStatus filingStatus,
            int age,
            int year) {

        BigDecimal standardDeduction = getStandardDeduction(filingStatus, age, year);
        return buildTaxResult(grossIncome, standardDeduction, filingStatus, year);
    }

    @Override
    public TaxCalculationResult calculateTaxMfj(
            BigDecimal grossIncome,
            int age,
            int spouseAge,
            int year) {

        BigDecimal standardDeduction = getStandardDeductionMfj(age, spouseAge, year);
        return buildTaxResult(grossIncome, standardDeduction, FilingStatus.MARRIED_FILING_JOINTLY, year);
    }

    private TaxCalculationResult buildTaxResult(
            BigDecimal grossIncome,
            BigDecimal standardDeduction,
            FilingStatus filingStatus,
            int year) {

        BigDecimal taxableIncome = grossIncome.subtract(standardDeduction).max(BigDecimal.ZERO);

        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxCalculationResult.noTax(grossIncome, standardDeduction, filingStatus, year);
        }

        List<TaxBracket> brackets = getBrackets(filingStatus, year);
        List<BracketTax> bracketBreakdown = calculateBracketBreakdown(taxableIncome, brackets);

        BigDecimal federalTax = bracketBreakdown.stream()
            .map(BracketTax::taxInBracket)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal effectiveRate = calculateEffectiveRate(federalTax, grossIncome);
        BigDecimal marginalRate = getMarginalTaxRate(taxableIncome, filingStatus, year);

        return new TaxCalculationResult(
            grossIncome,
            standardDeduction,
            taxableIncome,
            federalTax,
            effectiveRate,
            marginalRate,
            filingStatus,
            year,
            bracketBreakdown
        );
    }

    @Override
    public BigDecimal getStandardDeduction(FilingStatus filingStatus, int age, int year) {
        BigDecimal baseDeduction = projectValueWithChainedCpi(
            rules.getStandardDeduction(filingStatus),
            rules.getBaseYear(),
            year
        );

        if (age >= AGE_65) {
            BigDecimal additional = projectValueWithChainedCpi(
                rules.getAge65AdditionalDeduction(filingStatus),
                rules.getBaseYear(),
                year
            );
            baseDeduction = baseDeduction.add(additional);
        }

        return baseDeduction;
    }

    @Override
    public BigDecimal getStandardDeductionMfj(int age, int spouseAge, int year) {
        BigDecimal baseDeduction = projectValueWithChainedCpi(
            rules.getStandardDeduction(FilingStatus.MARRIED_FILING_JOINTLY),
            rules.getBaseYear(),
            year
        );

        BigDecimal additional = rules.getAge65AdditionalDeduction(FilingStatus.MARRIED_FILING_JOINTLY);
        int count65Plus = (age >= AGE_65 ? 1 : 0) + (spouseAge >= AGE_65 ? 1 : 0);

        if (count65Plus > 0) {
            BigDecimal projectedAdditional = projectValueWithChainedCpi(
                additional.multiply(BigDecimal.valueOf(count65Plus)),
                rules.getBaseYear(),
                year
            );
            baseDeduction = baseDeduction.add(projectedAdditional);
        }

        return baseDeduction;
    }

    @Override
    public List<TaxBracket> getBrackets(FilingStatus filingStatus, int year) {
        List<BracketConfig> configBrackets = rules.getBrackets(filingStatus);
        AtomicReference<BigDecimal> previousUpper = new AtomicReference<>(BigDecimal.ZERO);

        return IntStream.range(0, configBrackets.size())
            .mapToObj(i -> {
                BracketConfig config = configBrackets.get(i);
                BigDecimal lowerBound = previousUpper.get();
                BigDecimal upperBound = config.getUpperBound()
                    .map(upper -> projectValueWithChainedCpi(upper, rules.getBaseYear(), year))
                    .orElse(null);

                if (upperBound != null) {
                    previousUpper.set(upperBound);
                }

                return TaxBracket.of(config.getRate(), lowerBound, upperBound);
            })
            .toList();
    }

    @Override
    public BigDecimal getEffectiveTaxRate(
            BigDecimal grossIncome,
            FilingStatus filingStatus,
            int age,
            int year) {

        TaxCalculationResult result = calculateTax(grossIncome, filingStatus, age, year);
        return result.effectiveRate();
    }

    @Override
    public BigDecimal getMarginalTaxRate(
            BigDecimal taxableIncome,
            FilingStatus filingStatus,
            int year) {

        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        List<TaxBracket> brackets = getBrackets(filingStatus, year);

        return brackets.stream()
            .filter(bracket -> bracket.containsIncome(taxableIncome))
            .findFirst()
            .map(TaxBracket::rate)
            .orElseGet(() -> brackets.isEmpty() ? BigDecimal.ZERO : brackets.getLast().rate());
    }

    @Override
    public BigDecimal projectValueWithChainedCpi(
            BigDecimal baseValue,
            int baseYear,
            int targetYear) {

        if (baseValue == null || baseValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        int yearsElapsed = targetYear - baseYear;
        if (yearsElapsed <= 0) {
            return roundToNearest50(baseValue);
        }

        BigDecimal multiplier = BigDecimal.ONE.add(rules.getChainedCpiRate())
            .pow(yearsElapsed, java.math.MathContext.DECIMAL128);

        BigDecimal projected = baseValue.multiply(multiplier);
        return roundToNearest50(projected);
    }

    private List<BracketTax> calculateBracketBreakdown(
            BigDecimal taxableIncome,
            List<TaxBracket> brackets) {

        return brackets.stream()
            .map(bracket -> BracketTax.of(bracket.rate(), bracket.getIncomeInBracket(taxableIncome)))
            .filter(bracketTax -> bracketTax.incomeInBracket().compareTo(BigDecimal.ZERO) > 0)
            .toList();
    }

    private BigDecimal calculateEffectiveRate(BigDecimal tax, BigDecimal grossIncome) {
        if (grossIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return tax.divide(grossIncome, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal roundToNearest50(BigDecimal value) {
        return value.divide(FIFTY, 0, RoundingMode.HALF_UP).multiply(FIFTY);
    }
}
