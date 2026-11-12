package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 运行期可改的系统设置（键值对）。
 */
@Data
@TableName("app_setting")
public class AppSetting {
    @TableId(value = "setting_key", type = IdType.INPUT)
    private String settingKey;

    @TableField("setting_value")
    private String settingValue;

    private Date updatedAt;
}
