package com.discushion.identity;

import com.discushion.contracts.identity.CurrentActorProvider;
import com.discushion.contracts.identity.VerifiedActor;
import java.util.Optional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/** Server-owned servlet attribute; neither request headers/body nor thread inheritance is used. */
public final class RequestActorContext implements CurrentActorProvider {
    private static final String ATTRIBUTE = RequestActorContext.class.getName() + ".verifiedActor";

    @Override
    public Optional<VerifiedActor> current() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        return Optional.ofNullable((VerifiedActor) attributes.getRequest().getAttribute(ATTRIBUTE));
    }

    static void set(HttpServletRequest request, VerifiedActor actor) { request.setAttribute(ATTRIBUTE, actor); }
    static void clear(HttpServletRequest request) { request.removeAttribute(ATTRIBUTE); }
}
