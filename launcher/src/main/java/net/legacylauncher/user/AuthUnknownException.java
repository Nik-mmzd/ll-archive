package net.legacylauncher.user;

public class AuthUnknownException extends AuthException {
    public AuthUnknownException(Throwable t) {
        super(t, "unknown");
    }
}
