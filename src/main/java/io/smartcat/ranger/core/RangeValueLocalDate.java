package io.smartcat.ranger.core;

import java.time.LocalDate;

import io.smartcat.ranger.distribution.Distribution;
import io.smartcat.ranger.distribution.UniformDistribution;

/**
 * Randomly generates {@link LocalDate} value within specified range.
 */
public class RangeValueLocalDate extends Value<LocalDate> {

    private final LocalDate beginning;
    private final LocalDate end;
    private final boolean useEdgeCases;
    private final Distribution distribution;

    private boolean beginningEdgeCaseUsed = false;
    private boolean endEdgeCaseUsed = false;

    /**
     * Constructs range value with specified <code>range</code>. <code>useEdgeCases</code> is set to
     * <code>true</code> and <code>distribution</code> is set to {@link UniformDistribution}.
     *
     * @param beginning The beginning of the range.
     * @param end The end of the range.
     */
    public RangeValueLocalDate(LocalDate beginning, LocalDate end) {
        this(beginning, end, true);
    }

    /**
     * Constructs range value with specified <code>range</code> and <code>useEdgeCases</code>.
     * <code>distribution</code> is set to {@link UniformDistribution}.
     *
     * @param beginning The beginning of the range.
     * @param end The end of the range.
     * @param useEdgeCases Indicates whether to create edge cases as first two values or not.
     */
    public RangeValueLocalDate(LocalDate beginning, LocalDate end, boolean useEdgeCases) {
        this(beginning, end, useEdgeCases, new UniformDistribution());
    }

    /**
     * Constructs range value with specified <code>range</code>, <code>useEdgeCases</code> and
     * <code>distribution</code>.
     *
     * @param beginning The beginning of the range.
     * @param end The end of the range.
     * @param useEdgeCases Indicates whether to create edge cases as first two values or not.
     * @param distribution Distribution to use for value selection.
     */
    public RangeValueLocalDate(LocalDate beginning, LocalDate end, boolean useEdgeCases, Distribution distribution) {
        if (beginning == null) {
            throw new IllegalArgumentException("Beginning cannot be null.");
        }
        if (end == null) {
            throw new IllegalArgumentException("End cannot be null.");
        }
        if (!isRangeIncreasing(beginning, end)) {
            throw new InvalidRangeBoundsException("End of the range must be greater than the beginning of the range.");
        }
        if (distribution == null) {
            throw new IllegalArgumentException("Distribution cannot be null.");
        }
        this.beginning = beginning;
        this.end = end;
        this.useEdgeCases = useEdgeCases;
        this.distribution = distribution;
    }

    @Override
    protected void eval() {
        if (useEdgeCases && !beginningEdgeCaseUsed) {
            beginningEdgeCaseUsed = true;
            val = beginning;
            return;
        }
        if (useEdgeCases && !endEdgeCaseUsed) {
            endEdgeCaseUsed = true;
            val = end.minusDays(1);
            return;
        }
        val = LocalDate.ofEpochDay(distribution.nextLong(beginning.toEpochDay(), end.toEpochDay()));
    }

    private boolean isRangeIncreasing(LocalDate beginning, LocalDate end) {
        return beginning.compareTo(end) < 0;
    }
}
