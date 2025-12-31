package io.github.xmljim.retirement.simulation.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.xmljim.retirement.domain.exception.ValidationException;

/**
 * Market return assumptions for simulation.
 *
 * <p>Configures how investment returns are calculated based on the
 * selected {@link SimulationMode}:
 * <ul>
 *   <li>Deterministic - uses {@code expectedReturn}</li>
 *   <li>Monte Carlo - uses {@code expectedReturn} and {@code returnStdDev}</li>
 *   <li>Historical - uses {@code historicalReturns} sequence</li>
 * </ul>
 *
 * @param mode the simulation mode determining return calculation
 * @param expectedReturn expected annual return (used by deterministic and Monte Carlo)
 * @param returnStdDev standard deviation of returns (used by Monte Carlo)
 * @param historicalReturns sequence of historical returns (used by historical mode)
 *
 * @see SimulationMode
 * @see SimulationLevers
 */
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "List is made unmodifiable in compact constructor"
)
public record MarketLevers(
        SimulationMode mode,
        BigDecimal expectedReturn,
        BigDecimal returnStdDev,
        List<BigDecimal> historicalReturns
) {

    /** Default expected return: 7.0%. */
    public static final BigDecimal DEFAULT_EXPECTED_RETURN = new BigDecimal("0.07");

    /** Default return standard deviation: 15.0%. */
    public static final BigDecimal DEFAULT_RETURN_STD_DEV = new BigDecimal("0.15");

    /**
     * Compact constructor with validation and defensive copying.
     */
    public MarketLevers {
        if (mode == null) {
            mode = SimulationMode.DETERMINISTIC;
        }
        if (expectedReturn == null) {
            expectedReturn = DEFAULT_EXPECTED_RETURN;
        }
        if (returnStdDev == null) {
            returnStdDev = DEFAULT_RETURN_STD_DEV;
        }
        historicalReturns = historicalReturns == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(historicalReturns));

        // Validate mode-specific requirements
        if (mode == SimulationMode.HISTORICAL && historicalReturns.isEmpty()) {
            throw new ValidationException(
                    "Historical mode requires non-empty historicalReturns", "historicalReturns");
        }
    }

    /**
     * Creates deterministic market levers with a fixed return rate.
     *
     * @param rate the fixed annual return rate
     * @return market levers for deterministic simulation
     */
    public static MarketLevers deterministic(BigDecimal rate) {
        return new MarketLevers(
                SimulationMode.DETERMINISTIC,
                rate,
                BigDecimal.ZERO,
                Collections.emptyList()
        );
    }

    /**
     * Creates Monte Carlo market levers with mean and standard deviation.
     *
     * @param mean the expected return (mean of distribution)
     * @param stdDev the standard deviation of returns
     * @return market levers for Monte Carlo simulation
     */
    public static MarketLevers monteCarlo(BigDecimal mean, BigDecimal stdDev) {
        return new MarketLevers(
                SimulationMode.MONTE_CARLO,
                mean,
                stdDev,
                Collections.emptyList()
        );
    }

    /**
     * Creates historical market levers with actual return sequence.
     *
     * @param returns the historical returns sequence
     * @return market levers for historical backtesting
     * @throws ValidationException if returns list is empty
     */
    public static MarketLevers historical(List<BigDecimal> returns) {
        return new MarketLevers(
                SimulationMode.HISTORICAL,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                returns
        );
    }

    /**
     * Creates market levers with default assumptions (deterministic 7%).
     *
     * @return levers with default values
     */
    public static MarketLevers withDefaults() {
        return deterministic(DEFAULT_EXPECTED_RETURN);
    }

    /**
     * Indicates whether this configuration uses stochastic returns.
     *
     * @return true if Monte Carlo mode
     */
    public boolean isStochastic() {
        return mode.isStochastic();
    }

    /**
     * Indicates whether this configuration uses historical data.
     *
     * @return true if historical mode
     */
    public boolean usesHistoricalData() {
        return mode.usesHistoricalData();
    }
}
