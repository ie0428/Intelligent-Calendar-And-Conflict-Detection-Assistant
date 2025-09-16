package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Conversation {
    private Long id;
    private Long userId;
    private String sessionId;
    private String userMessage;
    private String aiResponse;
    private String intent;
    private String entities; // JSON格式存储
    private Boolean successful;
    private LocalDateTime createdAt;
}
