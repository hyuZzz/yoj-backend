package com.yuhi.yojbackendcommon.constant;

/**
 * 用户常量
 *
 * @author yuhi
 */
public interface UserConstant {

    /**
     * 盐值，混淆密码
     */
    String SALT = "yuhi";

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";


    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    /**
     * 默认头像
     */
    //String USER_DEFAULT_AVATAR = "https://p3-passport.byteimg.com/img/user-avatar/2ea9106b748a0b88d5bfcf517a4dc2ef~180x180.awebp";
    String USER_DEFAULT_AVATAR = "https://yoj1.oss-cn-hangzhou.aliyuncs.com/2023-12-09/gt.png";
}
