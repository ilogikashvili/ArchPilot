package com.ilia.gikashvili.archmain.review.state;

import com.ilia.gikashvili.archmain.common.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines which ReviewStatus transitions are legal.
 */
@Component
public class ReviewStateMachine {

    private static final Map<ReviewStatus, Set<ReviewStatus>> TRANSITIONS;

    static {
        TRANSITIONS = new EnumMap<>(ReviewStatus.class);
        TRANSITIONS.put(ReviewStatus.QUEUED, EnumSet.of(ReviewStatus.RUNNING, ReviewStatus.CANCELLED));
        TRANSITIONS.put(ReviewStatus.RUNNING, EnumSet.of(ReviewStatus.COMPLETED, ReviewStatus.FAILED, ReviewStatus.CANCELLED));
        TRANSITIONS.put(ReviewStatus.COMPLETED, EnumSet.noneOf(ReviewStatus.class));
        TRANSITIONS.put(ReviewStatus.FAILED, EnumSet.noneOf(ReviewStatus.class));
        TRANSITIONS.put(ReviewStatus.CANCELLED, EnumSet.noneOf(ReviewStatus.class));
    }

    /**
     * Validate that the transition from → to is legal.
     * Throws ValidationException if not.
     */
    public void requireTransition(ReviewStatus from, ReviewStatus to) {
        Set<ReviewStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ValidationException(
                    "Cannot transition review from " + from + " to " + to);
        }
    }
}
