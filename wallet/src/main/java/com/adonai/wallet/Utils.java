package com.adonai.wallet;

import java.lang.reflect.Method;

/**
 * @author adonai
 */
public class Utils {
    public static <T extends Object> T getValue(String value, T defaultValue) {
        T result;
        try {

            final Method valueOf = defaultValue.getClass().getMethod("valueOf", String.class);
            result = (T) valueOf.invoke(null, value);
            return result;
        } catch (Exception e) {
            result = defaultValue;
        }
        return result;
    }
}
