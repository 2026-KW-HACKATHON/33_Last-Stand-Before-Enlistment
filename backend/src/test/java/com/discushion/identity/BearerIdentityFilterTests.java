package com.discushion.identity;

import com.discushion.contracts.identity.LocalMember;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.assertj.core.api.Assertions.*;
import static com.discushion.identity.IdentityFailure.Reason.*;

class BearerIdentityFilterTests {
    private final RequestActorContext actors = new RequestActorContext();

    private MockHttpServletRequest request(String header) {
        var request = new MockHttpServletRequest("GET", "/api/v1/test-only");
        request.setServletPath("/api/v1/test-only");
        if(header != null) request.addHeader("Authorization", header);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }

    @Test void suppliesVerifiedSubjectAndServerMemberOnlyDuringRequest() throws Exception {
        var filter = new BearerIdentityFilter(token -> "did:privy:verified", subject -> {
            assertThat(subject).isEqualTo("did:privy:verified");
            return Optional.of(new LocalMember(42, Optional.of(Instant.EPOCH)));
        });
        var request = request("Bearer synthetic-token");
        request.addParameter("userId", "99");
        filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
            assertThat(actors.current().orElseThrow().member().orElseThrow().userId()).isEqualTo(42));
        assertThat(actors.current()).isEmpty();
        assertThat(new ThreadResult(actors).value()).isEmpty();
    }

    @Test void anonymousRequestDoesNotBecomeAMember() throws Exception {
        var filter = new BearerIdentityFilter(token -> {throw new AssertionError();}, subject -> {throw new AssertionError();});
        filter.doFilter(request(null), new MockHttpServletResponse(), (req, res) -> assertThat(actors.current()).isEmpty());
    }

    @Test void invalidTokenNeverFallsBackToAnonymousOrLeaksToken() throws Exception {
        var filter = new BearerIdentityFilter(token -> {throw new IdentityFailure(INVALID_TOKEN);}, subject -> {throw new AssertionError();});
        var response = new MockHttpServletResponse();
        filter.doFilter(request("Bearer synthetic-secret-token"), response, (req, res) -> {throw new AssertionError();});
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED", "traceId", "details")
            .doesNotContain("synthetic-secret-token", "did:privy", "stackTrace");
        assertThat(actors.current()).isEmpty();
    }

    @Test void rejectsEmptyWrongSchemeWhitespaceAndDuplicateHeaders() throws Exception {
        var filter = new BearerIdentityFilter(token -> {throw new AssertionError();}, subject -> {throw new AssertionError();});
        for(String value : new String[]{"Bearer ", "Basic abc", "Bearer a b"}) {
            var response = new MockHttpServletResponse();
            filter.doFilter(request(value), response, (req, res) -> {throw new AssertionError();});
            assertThat(response.getStatus()).isEqualTo(401);
        }
        var duplicate = request("Bearer one");
        duplicate.addHeader("Authorization", "Bearer two");
        var response = new MockHttpServletResponse();
        filter.doFilter(duplicate, response, (req, res) -> {throw new AssertionError();});
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void keyOutageIs503RatherThan401AndCleanupSurvivesHandlerFailure() throws Exception {
        var broken = new BearerIdentityFilter(token -> {throw new IdentityFailure(PROVIDER_UNAVAILABLE);}, subject -> Optional.empty());
        var response = new MockHttpServletResponse();
        broken.doFilter(request("Bearer synthetic"), response, (req, res) -> {throw new AssertionError();});
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("AUTH_PROVIDER_UNAVAILABLE");
        var working = new BearerIdentityFilter(token -> "did:privy:verified", subject -> Optional.empty());
        var request = request("Bearer synthetic");
        assertThatThrownBy(() -> working.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            assertThat(actors.current()).isPresent();
            throw new IllegalStateException("test-only failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(actors.current()).isEmpty();
    }

    @Test void unregisteredAndIncompleteSnapshotsRemainDistinct() throws Exception {
        var unregistered = new BearerIdentityFilter(token -> "did:privy:verified", subject -> Optional.empty());
        unregistered.doFilter(request("Bearer synthetic"), new MockHttpServletResponse(), (req, res) ->
            assertThat(actors.current().orElseThrow().member()).isEmpty());
        var incomplete = new BearerIdentityFilter(token -> "did:privy:verified", subject -> Optional.of(new LocalMember(42, Optional.empty())));
        incomplete.doFilter(request("Bearer synthetic"), new MockHttpServletResponse(), (req, res) ->
            assertThat(actors.current().orElseThrow().member().orElseThrow().registrationCompletedAt()).isEmpty());
    }

    @Test void databaseFailureIsNotHiddenAsUnregisteredOrGuest() throws Exception {
        var filter = new BearerIdentityFilter(token -> "did:privy:verified", subject -> {
            throw new org.springframework.dao.DataAccessResourceFailureException("synthetic outage");
        });
        var request = request("Bearer synthetic");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {throw new AssertionError();});
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("INTERNAL_ERROR").doesNotContain("synthetic outage");
        assertThat(actors.current()).isEmpty();
    }

    private record ThreadResult(Optional<com.discushion.contracts.identity.VerifiedActor> value) {
        ThreadResult(RequestActorContext actors) throws InterruptedException { this(readOnOtherThread(actors)); }
        private static Optional<com.discushion.contracts.identity.VerifiedActor> readOnOtherThread(RequestActorContext actors) throws InterruptedException {
            var result = new java.util.concurrent.atomic.AtomicReference<Optional<com.discushion.contracts.identity.VerifiedActor>>();
            var thread = new Thread(() -> result.set(actors.current()));
            thread.start(); thread.join(); return result.get();
        }
    }
}
