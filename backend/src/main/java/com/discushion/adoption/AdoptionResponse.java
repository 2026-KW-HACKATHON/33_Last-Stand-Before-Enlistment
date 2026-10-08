package com.discushion.adoption;

import java.time.Instant;

record AdoptionResponse(long id, long postId, String institutionName, Instant adoptedAt) {}
