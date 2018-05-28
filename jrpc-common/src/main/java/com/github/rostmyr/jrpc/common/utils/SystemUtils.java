package com.github.rostmyr.jrpc.common.utils;

/**
 * Rostyslav Myroshnychenko
 * on 21.05.2018.
 */
public final class SystemUtils {
    private SystemUtils() {
        // util class
    }

    /**
     * Checks whether the underlying OS is Linux
     *
     * @return {@code true} if it is otherwise returns {@code false}
     */
    public static boolean isLinux() {
        String osName = System.getProperty("os.name");
        return osName != null && osName.contains("nux");
    }
}
