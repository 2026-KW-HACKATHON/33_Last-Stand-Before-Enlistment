package com.discushion.identity;

import com.discushion.contracts.identity.LocalMember;
import com.discushion.contracts.identity.VerifiedActor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.web.filter.OncePerRequestFilter;
import static com.discushion.identity.IdentityFailure.Reason.INVALID_TOKEN;

/** Optional bearer authentication. Features decide anonymous/member/shared-guest access. */
public final class BearerIdentityFilter extends OncePerRequestFilter {
    private final AccessTokenVerifier tokens;
    private final Function<String, Optional<LocalMember>> members;
    private final JsonMapper json = JsonMapper.builder().build();

    public BearerIdentityFilter(AccessTokenVerifier tokens, Function<String, Optional<LocalMember>> members) {
        this.tokens = tokens;
        this.members = members;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/") || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        RequestActorContext.clear(request);
        try {
            var headers = Collections.list(request.getHeaders("Authorization"));
            if (!headers.isEmpty()) {
                if (headers.size() != 1) throw new IdentityFailure(INVALID_TOKEN);
                String header = headers.get(0);
                if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) throw new IdentityFailure(INVALID_TOKEN);
                String token = header.substring(7);
                if (token.isBlank() || token.chars().anyMatch(Character::isWhitespace)) throw new IdentityFailure(INVALID_TOKEN);
                String subject = tokens.verify(token);
                RequestActorContext.set(request, new VerifiedActor(subject, members.apply(subject)));
            }
        } catch (IdentityFailure failure) {
            RequestActorContext.clear(request);
            response.setStatus(IdentityErrorResponse.status(failure));
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            if (failure.reason() == INVALID_TOKEN) response.setHeader("WWW-Authenticate", "Bearer");
            json.writeValue(response.getOutputStream(), IdentityErrorResponse.from(failure));
            return;
        } catch (RuntimeException failure) {
            RequestActorContext.clear(request);
            response.setStatus(500);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            json.writeValue(response.getOutputStream(), IdentityErrorResponse.internal());
            return;
        }
        try { chain.doFilter(request, response); }
        finally { RequestActorContext.clear(request); }
    }
}
