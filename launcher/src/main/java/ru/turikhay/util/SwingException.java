package ru.turikhay.util;

public class SwingException extends RuntimeException {
    public SwingException(Throwable cause) {
        super(cause);
    }

    public Throwable unpackException() {
        if(getCause() instanceof SuppressedSwingException) {
            return getCause().getCause();
        }
        return getCause();
    }
}