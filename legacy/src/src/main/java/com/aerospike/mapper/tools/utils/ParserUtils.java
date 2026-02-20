package com.aerospike.mapper.tools.utils;

public class ParserUtils {
    private static final ParserUtils instance = new ParserUtils();

    public static ParserUtils getInstance() {
        return instance;
    }

    private ParserUtils() {
    }

    public String get(String value) {
        return parseString(value);
    }

    private String parseString(String value) {
        if (value == null || value.length() <= 3) {
            return value;
        }
        if ((value.startsWith("${") || value.startsWith("#{")) && value.endsWith("}")) {
            // Strip off the identifying tokens and split into value and default
            String[] values = value.substring(2, value.length() - 1).split(":");

            String translatedValue;
            if (value.startsWith("${")) {
                translatedValue = System.getProperty(values[0]);
            } else {
                translatedValue = System.getenv(values[0]);
            }

            if (translatedValue != null) {
                return translatedValue;
            }
            if (values.length > 1) {
                // A default was provided, use it.
                return values[1];
            }
            // No environment/property variable was found, no default, return null
            return null;
        }
        return value;
    }
}
