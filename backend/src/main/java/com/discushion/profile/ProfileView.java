package com.discushion.profile;

import java.time.OffsetDateTime;
import java.util.List;

record ProfileView(long id,String email,Profile profile,List<Region> neighborVerifiedRegions,
                   Institution institutionVerification,boolean institutionVerified) {
    record Region(long id,String name) {}
    record Profile(String nickname,String bio,String profileImageUrl,List<String> residentAttributes,Region activityRegion) {}
    record Institution(String status,String institutionName,Region responsibleRegion,
                       OffsetDateTime completedAt,OffsetDateTime validUntil,boolean isActive) {}
}
