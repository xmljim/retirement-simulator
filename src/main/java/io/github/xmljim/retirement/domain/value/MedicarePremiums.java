package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

/**
 * Result of Medicare premium calculation.
 *
 * <p>Contains all Medicare premium components including:
 * <ul>
 *   <li>Part A premium (typically $0 for premium-free Medicare)</li>
 *   <li>Part B base premium</li>
 *   <li>Part B IRMAA surcharge (if applicable)</li>
 *   <li>Part D IRMAA surcharge (if applicable)</li>
 * </ul>
 *
 * <p>IRMAA (Income-Related Monthly Adjustment Amount) is an additional
 * premium charged to higher-income beneficiaries. It is based on MAGI
 * from 2 years prior (e.g., 2025 premiums use 2023 MAGI).
 *
 * @param partAPremium Part A monthly premium ($0 if premium-free, else $285 or $518)
 * @param partBBasePremium Part B base monthly premium (e.g., $185 in 2025)
 * @param partBIrmaa Part B IRMAA surcharge ($0-$444/month based on MAGI)
 * @param partDIrmaa Part D IRMAA surcharge ($0-$86/month based on MAGI)
 * @param bracket the IRMAA bracket tier
 * @param year the Medicare year
 *
 * @see <a href="https://www.medicare.gov/basics/costs/medicare-costs">Medicare Costs</a>
 */
public record MedicarePremiums(
    BigDecimal partAPremium,
    BigDecimal partBBasePremium,
    BigDecimal partBIrmaa,
    BigDecimal partDIrmaa,
    IrmaaBracket bracket,
    int year
) {

    /**
     * IRMAA bracket tiers based on MAGI thresholds.
     *
     * <p>Higher brackets pay higher premiums. Bracket 0 is standard
     * (no IRMAA), while Bracket 5 is the highest IRMAA tier.
     */
    public enum IrmaaBracket {
        /**
         * Standard premium - no IRMAA (lowest income tier).
         */
        BRACKET_0(0, "Standard"),

        /**
         * First IRMAA tier - moderate income.
         */
        BRACKET_1(1, "First Tier"),

        /**
         * Second IRMAA tier.
         */
        BRACKET_2(2, "Second Tier"),

        /**
         * Third IRMAA tier.
         */
        BRACKET_3(3, "Third Tier"),

        /**
         * Fourth IRMAA tier.
         */
        BRACKET_4(4, "Fourth Tier"),

        /**
         * Fifth IRMAA tier - highest income (maximum premium).
         */
        BRACKET_5(5, "Highest Tier");

        private final int level;
        private final String description;

        IrmaaBracket(int level, String description) {
            this.level = level;
            this.description = description;
        }

        /**
         * Returns the numeric bracket level (0-5).
         *
         * @return the bracket level
         */
        public int getLevel() {
            return level;
        }

        /**
         * Returns a human-readable description.
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the bracket for a given level.
         *
         * @param level the bracket level (0-5)
         * @return the corresponding bracket
         * @throws IllegalArgumentException if level is out of range
         */
        public static IrmaaBracket fromLevel(int level) {
            for (IrmaaBracket bracket : values()) {
                if (bracket.level == level) {
                    return bracket;
                }
            }
            throw new IllegalArgumentException("Invalid IRMAA bracket level: " + level);
        }

        /**
         * Returns whether this bracket has IRMAA surcharges.
         *
         * @return true if bracket level is greater than 0
         */
        public boolean hasIrmaa() {
            return level > 0;
        }
    }

    /**
     * Returns the total Part B monthly premium (base + IRMAA).
     *
     * @return the total Part B premium
     */
    public BigDecimal getTotalPartBPremium() {
        return partBBasePremium.add(partBIrmaa);
    }

    /**
     * Returns the total monthly Medicare premium (Part A + Part B + Part D IRMAA).
     *
     * <p>Note: This does not include Part D plan premium, only the IRMAA surcharge.
     *
     * @return the total monthly premium
     */
    public BigDecimal getTotalMonthly() {
        return partAPremium
            .add(partBBasePremium)
            .add(partBIrmaa)
            .add(partDIrmaa);
    }

    /**
     * Returns the total annual Medicare cost.
     *
     * @return the monthly total multiplied by 12
     */
    public BigDecimal getTotalAnnual() {
        return getTotalMonthly().multiply(BigDecimal.valueOf(12));
    }

    /**
     * Returns whether this result includes IRMAA surcharges.
     *
     * @return true if bracket is above BRACKET_0
     */
    public boolean hasIrmaa() {
        return bracket.hasIrmaa();
    }

    /**
     * Returns the total IRMAA surcharge (Part B + Part D).
     *
     * @return the combined monthly IRMAA amount
     */
    public BigDecimal getTotalIrmaa() {
        return partBIrmaa.add(partDIrmaa);
    }

    /**
     * Creates a premium result with no IRMAA (standard bracket).
     *
     * @param partAPremium the Part A premium
     * @param partBBase the Part B base premium
     * @param year the Medicare year
     * @return a MedicarePremiums with bracket 0 and no IRMAA
     */
    public static MedicarePremiums standard(
            BigDecimal partAPremium,
            BigDecimal partBBase,
            int year) {
        return new MedicarePremiums(
            partAPremium,
            partBBase,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            IrmaaBracket.BRACKET_0,
            year
        );
    }

    /**
     * Creates a premium result with IRMAA surcharges.
     *
     * @param partAPremium the Part A premium
     * @param partBBase the Part B base premium
     * @param partBIrmaa the Part B IRMAA surcharge
     * @param partDIrmaa the Part D IRMAA surcharge
     * @param bracket the IRMAA bracket
     * @param year the Medicare year
     * @return a MedicarePremiums with the specified values
     */
    public static MedicarePremiums withIrmaa(
            BigDecimal partAPremium,
            BigDecimal partBBase,
            BigDecimal partBIrmaa,
            BigDecimal partDIrmaa,
            IrmaaBracket bracket,
            int year) {
        return new MedicarePremiums(
            partAPremium,
            partBBase,
            partBIrmaa,
            partDIrmaa,
            bracket,
            year
        );
    }
}
