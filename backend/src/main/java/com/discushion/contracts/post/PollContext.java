package com.discushion.contracts.post;

import java.time.Instant;
import java.util.List;

public record PollContext(long pollId, Instant endsAt, List<Long> optionIds) {
    public PollContext {
        optionIds = List.copyOf(optionIds);
    }
}
