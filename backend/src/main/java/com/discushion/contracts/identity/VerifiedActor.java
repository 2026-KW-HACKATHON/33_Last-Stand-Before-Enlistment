package com.discushion.contracts.identity;

import java.util.Optional;

public record VerifiedActor(String privySubject, Optional<LocalMember> member) {}
