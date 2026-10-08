package com.discushion.neighbor;

import java.time.OffsetDateTime;
import java.util.List;

record NeighborView(List<VerifiedRegion> verifiedRegions,int maxVerifiedRegions,TargetRegion targetRegion) {
    record VerifiedRegion(long id,String name,OffsetDateTime verifiedAt) {}
    /** Qualification only: features still recheck the target state and permissions at action time. */
    record TargetRegion(long regionId,boolean isNeighborVerified) {}
}
