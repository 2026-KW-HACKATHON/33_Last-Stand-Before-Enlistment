package com.discushion.posts;

import com.discushion.contracts.post.PostType;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

record PostCreationCommand(PostType type, String topic, long regionId, String title, String content,
        Activity activity, Vote vote, List<Long> photoFileIds) {
    PostCreationCommand { photoFileIds = List.copyOf(photoFileIds); }

    record Activity(String source, String schedule, String place, String status, String externalUrl) {}
    record Vote(String question, List<String> options, Instant endsAt) {
        Vote { options = List.copyOf(options); }
    }

    static PostCreationCommand parse(Map<String, Object> body) {
        if (body == null || !body.keySet().stream().allMatch(Set.of("type", "topic", "regionId", "title", "content", "details", "photoFileIds")::contains))
            throw new PostCreationFailure();
        PostType type = enumValue(PostType.class, body.get("type"));
        String topic = text(body.get("topic"));
        if (!Set.of("TRANSPORTATION", "HOUSING", "SAFETY", "WELFARE", "LIVING_INFORMATION", "ENVIRONMENT", "OTHER").contains(topic))
            throw new PostCreationFailure();
        long regionId = positiveId(body.get("regionId"));
        String title = text(body.get("title"));
        String content = text(body.get("content"));
        List<Long> photos = body.containsKey("photoFileIds") ? ids(body.get("photoFileIds")) : List.of();
        Map<String, Object> details = body.containsKey("details") ? object(body.get("details")) : Map.of();

        Activity activity = null;
        Vote vote = null;
        if (type == PostType.LOCAL_AGENDA) {
            if (!details.isEmpty()) throw new PostCreationFailure();
        } else if (type == PostType.LOCAL_ACTIVITY) {
            if (!details.keySet().stream().allMatch(Set.of("source", "schedule", "place", "activityStatus", "externalParticipationUrl")::contains))
                throw new PostCreationFailure();
            String source = text(details.get("source"));
            String schedule = text(details.get("schedule"));
            String place = text(details.get("place"));
            String status = enumValue(Set.of("SCHEDULED", "IN_PROGRESS", "ENDED", "CANCELED"), details.get("activityStatus"));
            String url = optionalText(details.get("externalParticipationUrl"));
            if (details.containsKey("externalParticipationUrl") && url == null) throw new PostCreationFailure();
            if (url != null && !httpUrl(url)) throw new PostCreationFailure();
            activity = new Activity(source, schedule, place, status, url);
        } else {
            if (!details.keySet().stream().allMatch(Set.of("question", "options", "endsAt")::contains))
                throw new PostCreationFailure();
            String question = text(details.get("question"));
            List<String> options = textList(details.get("options"));
            if (options.size() < 2 || options.size() > 10) throw new PostCreationFailure();
            if (new HashSet<>(options).size() != options.size()) throw new PostCreationFailure();
            Instant endsAt;
            try { endsAt = OffsetDateTime.parse(text(details.get("endsAt"))).toInstant(); }
            catch (RuntimeException invalid) { throw new PostCreationFailure(); }
            vote = new Vote(question, options, endsAt);
        }
        if (photos.size() > 10 || new HashSet<>(photos).size() != photos.size()) throw new PostCreationFailure();
        return new PostCreationCommand(type, topic, regionId, title, content, activity, vote, photos);
    }

    private static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> raw)) throw new PostCreationFailure();
        var result = new java.util.LinkedHashMap<String, Object>();
        for (var entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new PostCreationFailure();
            result.put(key, entry.getValue());
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    private static List<Long> ids(Object value) {
        if (!(value instanceof List<?> list)) throw new PostCreationFailure();
        List<Long> result = new ArrayList<>();
        for (Object item : list) result.add(positiveId(item));
        return List.copyOf(result);
    }

    private static List<String> textList(Object value) {
        if (!(value instanceof List<?> list)) throw new PostCreationFailure();
        List<String> result = new ArrayList<>();
        for (Object item : list) result.add(text(item).strip());
        return List.copyOf(result);
    }

    private static long positiveId(Object value) {
        try {
            long id;
            if (value instanceof BigDecimal decimal) id = decimal.longValueExact();
            else if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) id = ((Number) value).longValue();
            else throw new ArithmeticException();
            if (id < 1 || id > 9_007_199_254_740_991L) throw new ArithmeticException();
            return id;
        } catch (RuntimeException invalid) { throw new PostCreationFailure(); }
    }

    private static String text(Object value) {
        if (!(value instanceof String text) || text.isBlank()) throw new PostCreationFailure();
        return text;
    }

    private static String optionalText(Object value) {
        if (value == null) return null;
        return text(value);
    }

    private static boolean httpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute() && uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (RuntimeException invalid) { return false; }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, Object value) {
        try { return Enum.valueOf(type, text(value)); }
        catch (RuntimeException invalid) { throw new PostCreationFailure(); }
    }

    private static String enumValue(Set<String> allowed, Object value) {
        String result = text(value);
        if (!allowed.contains(result)) throw new PostCreationFailure();
        return result;
    }
}
