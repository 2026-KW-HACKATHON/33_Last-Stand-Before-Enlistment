package com.discushion.posts;

import com.discushion.contracts.post.PostType;
import com.discushion.photos.PhotoAttachments;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

record PostPatch(Field<String> title, Field<String> content, Field<String> topic, Field<Long> regionId,
        boolean detailsSupplied, Field<String> source, Field<String> schedule, Field<String> place,
        Field<String> activityStatus, Field<String> externalParticipationUrl, Field<java.time.Instant> endsAt,
        Field<List<PhotoAttachments.Reference>> photoOrder) {
    record Field<T>(boolean supplied, T value) {
        static <T> Field<T> missing() { return new Field<>(false, null); }
        static <T> Field<T> supplied(T value) { return new Field<>(true, value); }
    }

    static PostPatch parse(Map<String, Object> body, PostType type) {
        if (body == null) throw PostEditFailure.invalid("body");
        Set<String> allowed = new HashSet<>(Set.of("title", "content", "photoOrder"));
        if (type != PostType.VOTE) allowed.addAll(Set.of("topic", "regionId"));
        if (type == PostType.LOCAL_ACTIVITY || type == PostType.VOTE) allowed.add("details");
        for (String name : body.keySet()) if (!allowed.contains(name)) throw type == PostType.VOTE
                ? PostEditFailure.typeInvalid(name) : PostEditFailure.invalid(name);
        Field<String> title = string(body, "title", false), content = string(body, "content", false);
        Field<String> topic = type == PostType.VOTE ? Field.missing() : string(body, "topic", false);
        Field<Long> regionId = type == PostType.VOTE ? Field.missing() : positiveId(body, "regionId");
        Field<List<PhotoAttachments.Reference>> photoOrder = photoOrder(body);
        Object rawDetails = body.get("details");
        boolean detailsSupplied = body.containsKey("details");
        Field<String> source = Field.missing(), schedule = Field.missing(), place = Field.missing();
        Field<String> activityStatus = Field.missing(), externalUrl = Field.missing();
        Field<java.time.Instant> endsAt = Field.missing();
        if (detailsSupplied) {
            if (!(rawDetails instanceof Map<?, ?> details)) throw PostEditFailure.invalid("details");
            if (type == PostType.LOCAL_ACTIVITY) {
                Set<String> activityFields = Set.of("source", "schedule", "place", "activityStatus", "externalParticipationUrl");
                for (Object key : details.keySet()) if (!(key instanceof String name) || !activityFields.contains(name))
                    throw PostEditFailure.typeInvalid("details." + key);
                source = string(details, "source", false); schedule = string(details, "schedule", false);
                place = string(details, "place", false); activityStatus = string(details, "activityStatus", false);
                externalUrl = string(details, "externalParticipationUrl", true);
                if (externalUrl.supplied() && externalUrl.value() != null) {
                    try {
                        var uri = java.net.URI.create(externalUrl.value());
                        if (!uri.isAbsolute() || uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())))
                            throw PostEditFailure.invalid("details.externalParticipationUrl");
                    } catch (IllegalArgumentException invalid) { throw PostEditFailure.invalid("details.externalParticipationUrl"); }
                }
                if (!(source.supplied() || schedule.supplied() || place.supplied() || activityStatus.supplied() || externalUrl.supplied()))
                    throw PostEditFailure.invalid("details");
            } else if (type == PostType.VOTE) {
                for (Object key : details.keySet()) if (!"endsAt".equals(key)) throw PostEditFailure.typeInvalid("details." + key);
                Object end = details.get("endsAt");
                if (!(end instanceof String text)) throw PostEditFailure.invalid("details.endsAt");
                try { endsAt = Field.supplied(OffsetDateTime.parse(text).toInstant()); }
                catch (RuntimeException invalid) { throw PostEditFailure.invalid("details.endsAt"); }
            } else throw PostEditFailure.typeInvalid("details");
        }
        if (!(title.supplied() || content.supplied() || topic.supplied() || regionId.supplied()
                || detailsSupplied || photoOrder.supplied())) throw PostEditFailure.invalid("body");
        return new PostPatch(title, content, topic, regionId, detailsSupplied, source, schedule, place,
                activityStatus, externalUrl, endsAt, photoOrder);
    }

    private static Field<String> string(Map<?, ?> object, String key, boolean nullable) {
        if (!object.containsKey(key)) return Field.missing();
        Object value = object.get(key);
        if (value == null && nullable) return Field.supplied(null);
        if (!(value instanceof String text) || text.isBlank()) throw PostEditFailure.invalid(key);
        return Field.supplied(text);
    }
    private static Field<Long> positiveId(Map<String, Object> object, String key) {
        if (!object.containsKey(key)) return Field.missing();
        Object raw = object.get(key);
        if (!(raw instanceof Number number)) throw PostEditFailure.invalid(key);
        return Field.supplied(exactId(number, key));
    }
    private static Field<List<PhotoAttachments.Reference>> photoOrder(Map<String, Object> object) {
        if (!object.containsKey("photoOrder")) return Field.missing();
        Object raw = object.get("photoOrder");
        if (!(raw instanceof List<?> values)) throw PostEditFailure.invalid("photoOrder");
        List<PhotoAttachments.Reference> references = new ArrayList<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> reference) || reference.size() != 1) throw PostEditFailure.invalid("photoOrder");
            Object key = reference.keySet().iterator().next();
            if (!(key instanceof String name) || (!name.equals("photoId") && !name.equals("fileId")))
                throw PostEditFailure.invalid("photoOrder");
            Object rawId = reference.get(key);
            if (!(rawId instanceof Number number)) throw PostEditFailure.invalid("photoOrder");
            long id = exactId(number, "photoOrder");
            references.add(name.equals("photoId") ? new PhotoAttachments.Reference(id, null)
                    : new PhotoAttachments.Reference(null, id));
        }
        return Field.supplied(List.copyOf(references));
    }
    private static long exactId(Number number, String field) {
        try {
            long value = new java.math.BigDecimal(number.toString()).longValueExact();
            if (value < 1 || value > 9_007_199_254_740_991L) throw PostEditFailure.invalid(field);
            return value;
        } catch (NumberFormatException | ArithmeticException invalid) { throw PostEditFailure.invalid(field); }
    }
}
