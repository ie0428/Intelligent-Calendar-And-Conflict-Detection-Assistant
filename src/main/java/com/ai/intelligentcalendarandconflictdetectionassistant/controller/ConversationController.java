package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/user/{userId}")
    public List<Conversation> getConversationsByUser(@PathVariable Long userId) {
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
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    @GetMapping("/sessions/user/{userId}")
    public List<String> getAllSessionsByUser(@PathVariable Long userId) {
        return conversationService.getAllSessionIdsByUser(userId);
    }
}
