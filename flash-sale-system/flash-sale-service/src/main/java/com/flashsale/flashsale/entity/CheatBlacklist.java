 package com.flashsale.flashsale.entity;
 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.time.LocalDateTime;
 @Data @TableName("cheat_blacklist")
public class CheatBlacklist {
    @TableId(type=IdType.AUTO) private Long id;
    private Long userId; private String ip; private String reason;
    private Integer riskScore; private LocalDateTime expireTime;
    private Integer isActive; private LocalDateTime createTime;
    @TableField(exist = false) private String username;
}
