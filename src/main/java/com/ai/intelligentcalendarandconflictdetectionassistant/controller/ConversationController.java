package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID
     * @throws SecurityException 如果用户未认证
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new SecurityException("用户未认证");
    }

    @GetMapping("/user/current")
    public List<Conversation> getCurrentUserConversations() {
        Long userId = getCurrentUserId();
        return conversationService.getConversationHistoryByUser(userId);
    }

    @GetMapping("/user/{userId}")
    public List<Conversation> getConversationsByUser(@PathVariable Long userId) {
        // 验证当前用户是否有权限访问该用户的数据
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new SecurityException("无权访问其他用户的对话记录");
        }
        return conversationService.getConversationHistoryByUser(userId);
    }

    @GetMapping("/session/{sessionId}")
    public List<Conversation> getConversationsBySession(@PathVariable String sessionId) {
        return conversationService.getConversationHistory(sessionId);
    }

    @DeleteMapping("/{id}")
    public void deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
    }
    
    /**
     * 获取当前用户的所有会话ID列表
     * @return 会话ID列表
     */
    @GetMapping("/sessions/current")
    public List<String> getCurrentUserSessions() {
        Long userId = getCurrentUserId();
        return conversationService.getAllSessionIdsByUser(userId);
    }

    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    @GetMapping("/sessions/user/{userId}")
    public List<String> getAllSessionsByUser(@PathVariable Long userId) {
        // 验证当前用户是否有权限访问该用户的数据
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new SecurityException("无权访问其他用户的会话列表");
        }
        return conversationService.getAllSessionIdsByUser(userId);
    }
}
