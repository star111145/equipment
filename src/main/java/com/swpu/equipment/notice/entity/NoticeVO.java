package com.swpu.equipment.notice.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
