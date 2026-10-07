package com.discushion.login;

final class LoginInputFailure extends RuntimeException {
    LoginInputFailure() { super("VALIDATION_ERROR"); }
}
