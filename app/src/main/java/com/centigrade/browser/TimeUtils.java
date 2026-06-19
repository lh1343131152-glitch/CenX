package com.centigrade.browser;

/**
 * 时间工具类
 * 用于判断系统时间是否明显异常，避免 HTTPS 证书因为设备时间错误而误判。
 */
public class TimeUtils {

    private TimeUtils() {
    }

    public static boolean isSystemTimeAbnormal() {
        long now = System.currentTimeMillis();

        // 2020-01-01 00:00:00 UTC，低于此值通常说明设备时间异常
        long min = 1577836800000L;

        // 2100-01-01 00:00:00 UTC，高于此值也视为异常
        long max = 4102444800000L;

        return now < min || now > max;
    }
}
