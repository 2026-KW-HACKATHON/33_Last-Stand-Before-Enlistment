package com.discushion.contracts.identity;

import java.util.Optional;

/** Supplies only the identity verified for the current request. Invalid tokens must fail. */
public interface CurrentActorProvider {
    Optional<VerifiedActor> current();
}
