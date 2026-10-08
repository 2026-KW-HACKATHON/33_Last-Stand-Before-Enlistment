package com.discushion.officer;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

class OfficerAgendaQueryTests {
    @Test void defaultsToAllWithTwentyItems() {
        var query = OfficerAgendaQuery.parse(new LinkedMultiValueMap<>());
        assertThat(query.scope()).isEqualTo(OfficerAgendaScope.ALL);
        assertThat(query.regionId()).isNull();
        assertThat(query.size()).isEqualTo(20);
    }

    @Test void acceptsRegionAdoptedScopeAndMaximumPageSize() {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("scope", "ADOPTED");
        params.add("regionId", "15");
        params.add("size", "100");
        var query = OfficerAgendaQuery.parse(params);
        assertThat(query.scope()).isEqualTo(OfficerAgendaScope.ADOPTED);
        assertThat(query.regionId()).isEqualTo(15L);
        assertThat(query.size()).isEqualTo(100);
    }

    @Test void rejectsUnknownRepeatedAndOutOfRangeParameters() {
        var unknown = new LinkedMultiValueMap<String, String>();
        unknown.add("userId", "1");
        assertThatThrownBy(() -> OfficerAgendaQuery.parse(unknown)).isInstanceOf(OfficerAgendaFailure.class);

        var repeated = new LinkedMultiValueMap<String, String>();
        repeated.add("scope", "ALL"); repeated.add("scope", "ADOPTED");
        assertThatThrownBy(() -> OfficerAgendaQuery.parse(repeated)).isInstanceOf(OfficerAgendaFailure.class);

        var tooLarge = new LinkedMultiValueMap<String, String>();
        tooLarge.add("size", "101");
        assertThatThrownBy(() -> OfficerAgendaQuery.parse(tooLarge)).isInstanceOf(OfficerAgendaFailure.class);
    }

    @Test void cursorIsBoundToFiltersAndInstitution() {
        var filter = new OfficerAgendaCursor.Filter(42, OfficerAgendaScope.ALL, 15L);
        var position = new OfficerAgendaCursor.Position(7, java.time.Instant.parse("2026-10-08T10:00:00Z"), 101);
        var cursor = OfficerAgendaCursor.encode(filter, position);
        assertThat(OfficerAgendaCursor.decode(cursor, filter)).isEqualTo(position);
        assertThatThrownBy(() -> OfficerAgendaCursor.decode(cursor,
                new OfficerAgendaCursor.Filter(43, OfficerAgendaScope.ALL, 15L)))
                .isInstanceOf(OfficerAgendaFailure.class);
        assertThatThrownBy(() -> OfficerAgendaCursor.decode(cursor,
                new OfficerAgendaCursor.Filter(42, OfficerAgendaScope.ADOPTED, 15L)))
                .isInstanceOf(OfficerAgendaFailure.class);
        assertThatThrownBy(() -> OfficerAgendaCursor.decode(cursor,
                new OfficerAgendaCursor.Filter(42, OfficerAgendaScope.ALL, null)))
                .isInstanceOf(OfficerAgendaFailure.class);
    }
}
