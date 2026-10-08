package com.discushion.contracts.identity;

import java.time.Instant;
import java.util.Optional;

public record LocalMember(long userId, Optional<Instant> registrationCompletedAt) {}
