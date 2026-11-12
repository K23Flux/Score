package com.score.entity.bo;

import lombok.Data;

/**
 * 在线保存 GitHub OAuth App 凭据。字段为 null 表示不修改。
 */
@Data
public class GithubSettingsBo {
    private String clientId;
    /** 新密钥；为空且 clearClientSecret=false 时保持原值 */
    private String clientSecret;
    private Boolean clearClientSecret;
}
