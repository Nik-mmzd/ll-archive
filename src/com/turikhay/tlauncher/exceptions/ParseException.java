package com.turikhay.tlauncher.exceptions;

public class ParseException extends RuntimeException {
   private static final long serialVersionUID = -3231272464953548141L;

   public ParseException(String string) {
      super(string);
   }

   public ParseException(String message, Throwable cause) {
      super(message, cause);
   }

   public ParseException(Throwable cause) {
      super(cause);
   }

   protected ParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }
}