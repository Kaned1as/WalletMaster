package com.adonai.wallet;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Iterator;

/**
 * @author adonai
 */
public class Utils {
    @SuppressWarnings("unchecked") // we know what we want
    public static <T> T getValue(String value, T defaultValue) {
        T result;
        try {
            try {
                final Method valueOf = defaultValue.getClass().getMethod("valueOf", String.class);
                result = (T) valueOf.invoke(null, value);
                return result;
            } catch (NoSuchMethodException e) {
                final Constructor constructor = defaultValue.getClass().getConstructor(String.class);
                result = (T) constructor.newInstance(value);
            }


        } catch (Exception e) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / 160f);
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        final Resources resources = context.getResources();
        final DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    /**
     * Retrieves all keys of enum as string array
     * @param clazz class of enum
     * @return String array
     */
    public static <E extends Enum<E>> String[] allKeys(Class<E> clazz) {
        EnumSet<E> set = EnumSet.allOf(clazz);
        String[] result = new String[set.size()];
        Iterator<E> iter = set.iterator();
        for(int i = 0; i < set.size(); ++i) {
            result[i] = iter.next().toString();
        }
        return result;
    }
}
