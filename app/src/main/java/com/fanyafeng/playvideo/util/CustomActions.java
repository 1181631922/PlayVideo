package com.fanyafeng.playvideo.util;

/**
 * Created by zgw on 16/5/12 17:48.
 */
public class CustomActions {

    private static final String PACKAGE_NAME = "com.grape.wine";

    /**
     * 用户已经登录
     */
    public static final String ACTION_USER_LOGINED = PACKAGE_NAME + ".user_logined";

    /**
     * 用户已经登出
     */
    public static final String ACTION_USER_LOGOUT = PACKAGE_NAME + ".user_logout";

    /**
     * 用户信息改变
     */
    public static final String ACTION_PROFILE_CHANGED = PACKAGE_NAME + ".profile_changed";

    /**
     * 收藏状态发生改变
     */
    public static final String ACTION_COLLECT_CHANGED = PACKAGE_NAME + ".collect_changed";

    /**
     * 支付订单成功
     */
    public static final String ACTION_PAY_SUCCESS = PACKAGE_NAME + ".pay_success";

    /**
     * 收到信鸽消息推送
     */
    public static final String ACTION_MESSAGE_RECEIVED = PACKAGE_NAME + ".receive_message";

    /**
     * 刷新消息红点
     */
    public static final String ACTION_MESSAGE_DOT_UPDATE = PACKAGE_NAME + ".update_message_dot";

    /**
     * 关闭所有webview
     */
    public static final String ACTION_CLOSE_WEBVIEW = PACKAGE_NAME + ".close_webview";
}
