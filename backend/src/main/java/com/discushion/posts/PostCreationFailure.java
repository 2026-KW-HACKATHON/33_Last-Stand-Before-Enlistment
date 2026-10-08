package com.discushion.posts;

final class PostCreationFailure extends RuntimeException {
    PostCreationFailure() { super("VALIDATION_ERROR"); }
}
