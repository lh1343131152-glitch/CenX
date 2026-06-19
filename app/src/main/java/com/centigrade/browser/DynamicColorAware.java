package com.centigrade.browser;

/**
 * 动态取色状态变化回调接口
 * 实现此接口的 Activity 会在动态取色开关变化时收到通知
 */
public interface DynamicColorAware {
    /**
     * 当动态取色状态变化时调用
     * @param enabled 当前动态取色是否开启
     */
    void onDynamicColorChanged(boolean enabled);
}
