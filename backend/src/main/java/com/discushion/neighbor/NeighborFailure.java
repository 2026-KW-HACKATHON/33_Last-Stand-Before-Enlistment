package com.discushion.neighbor;

final class NeighborFailure extends RuntimeException {
    final String code;final int status;
    NeighborFailure(String code,int status) {super(code);this.code=code;this.status=status;}
}
