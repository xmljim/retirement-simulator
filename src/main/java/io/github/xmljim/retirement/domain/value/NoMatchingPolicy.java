package io.github.xmljim.retirement.domain.value;

import java.math.BigDecimal;

/**
 * A matching policy representing no employer match.
 *
 * <p>This is a singleton implementation used when an employer does not
 * offer any matching contributions.
 */
public final class NoMatchingPolicy implements MatchingPolicy {

    /**
     * Singleton instance.
     */
    static final NoMatchingPolicy INSTANCE = new NoMatchingPolicy();

    private NoMatchingPolicy() {
        // Private constructor for singleton
    }

    @Override
    public BigDecimal calculateEmployerMatch(BigDecimal employeeContributionRate) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean allowsRothMatch() {
        return false;
    }

    @Override
    public String getDescription() {
        return "No employer match";
    }

    @Override
    public String toString() {
        return "NoMatchingPolicy{}";
    }
}
