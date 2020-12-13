package ru.turikhay.tlauncher.configuration;

import ru.turikhay.exceptions.ParseException;
import ru.turikhay.util.IntegerArray;
import ru.turikhay.util.StringUtil;
import ru.turikhay.util.U;

import java.util.UUID;

class PlainParser {
    public static void parse(Object plainValue, Object defaultValue) throws ParseException {
        if (defaultValue != null) {
            if (plainValue == null) {
                throw new ParseException("Value is NULL");
            } else {
                String value = plainValue.toString();

                try {
                    if (defaultValue instanceof Integer) {
                        Integer.parseInt(value);
                    } else if (defaultValue instanceof Boolean) {
                        StringUtil.parseBoolean(value);
                    } else if (defaultValue instanceof Double) {
                        Double.parseDouble(value);
                    } else if (defaultValue instanceof Long) {
                        Long.parseLong(value);
                    } else if (defaultValue instanceof IntegerArray) {
                        IntegerArray.parseIntegerArray(value);
                    } else if (defaultValue instanceof Configuration.ActionOnLaunch) {
                        if (!Configuration.ActionOnLaunch.parse(value)) {
                            throw new ParseException("Cannot parse ActionOnLaunch");
                        }
                    } else if (defaultValue instanceof Configuration.LoggerType) {
                        if (!Configuration.LoggerType.parse(value)) {
                            throw new ParseException("Cannot parse LoggerType");
                        }
                    } else if (defaultValue instanceof UUID) {
                        U.requireNotNull(UUID.fromString(value), "uuid");
                    }

                } catch (Exception var4) {
                    if (var4 instanceof ParseException) {
                        throw (ParseException) var4;
                    } else {
                        throw new ParseException("Cannot parse input value!", var4);
                    }
                }
            }
        }
    }
}