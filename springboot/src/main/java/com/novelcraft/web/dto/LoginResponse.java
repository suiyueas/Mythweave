package com.novelcraft.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String avatar;
        /** 角色: admin-管理员 user-普通用户 */
        private String role;
        /** VIP等级 0-普通 1-白银 2-黄金 3-钻石 */
        private Integer vipLevel;
        /** VIP到期时间，null 表示未开通 */
        private java.time.LocalDateTime vipExpireAt;
        /** 最近一次VIP购买时间 */
        private java.time.LocalDateTime vipPurchasedAt;
        /** VIP状态 none-未开通 active-生效中 expired-已过期 */
        private String vipStatus;
    }
}