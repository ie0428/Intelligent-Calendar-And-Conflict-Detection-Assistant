package com.ai.intelligentcalendarandconflictdetectionassistant.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private String timezone;
    private String language;
    private String avatarUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
