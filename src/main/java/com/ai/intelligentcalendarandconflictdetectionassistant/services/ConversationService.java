package com.ai.intelligentcalendarandconflictdetectionassistant.services;

import com.ai.intelligentcalendarandconflictdetectionassistant.mapper.ConversationMapper;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.Conversation;
import com.ai.intelligentcalendarandconflictdetectionassistant.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private UserService userService;
    /**
     * 保存对话记录到数据库
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param aiResponse AI响应
     * @param intent 识别的意图
     * @param entities 提取的实体
     * @param successful 是否处理成功
     * @return 保存的对话记录
     */
    public Conversation saveConversation(Long userId, String sessionId, String userMessage, String aiResponse,
                                         String intent, String entities, Boolean successful) {
        // 确保用户存在，如果不存在则创建
        String username = "user_" + (userId != null ? userId : System.currentTimeMillis());
        User user = userService.ensureUserExists(userId, username);

        // 如果传入的userId为null，使用新创建的用户ID
        if (userId == null && user.getId() != null) {
            userId = user.getId();
        }
        Conversation conversation = new Conversation();
        conversation.setUserId(userId);
        conversation.setSessionId(sessionId);
        conversation.setUserMessage(userMessage);
        conversation.setAiResponse(aiResponse);
        conversation.setIntent(intent);
        conversation.setEntities(entities);
        conversation.setSuccessful(successful);
        conversation.setCreatedAt(LocalDateTime.now());

        System.out.println("准备插入对话记录: userId=" + userId + ", sessionId=" + sessionId +
                ", userMessage=" + userMessage);

        conversationMapper.insert(conversation);

        System.out.println("对话记录插入完成，生成的ID: " + conversation.getId());
        return conversation;
    }

    /**
     * 根据ID查找对话记录
     * @param id 对话ID
     * @return 对话记录
     */
    public Conversation findById(Long id) {
        return conversationMapper.findById(id);
    }

    /**
     * 根据会话ID获取对话历史
     * @param sessionId 会话ID
     * @return 对话历史列表
     */
    public List<Conversation> getConversationHistory(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return new ArrayList<>();
        }
        List<Conversation> history = conversationMapper.findBySessionIdOrderByCreatedAtDesc(sessionId);
        System.out.println("获取到 " + (history != null ? history.size() : 0) + " 条历史记录，sessionId: " + sessionId);
        return history != null ? history : new ArrayList<>();
    }

    /**
     * 根据用户ID获取对话历史
     * @param userId 用户ID
     * @return 对话历史列表
     */
    public List<Conversation> getConversationHistoryByUser(Long userId) {
        return conversationMapper.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除对话记录
     * @param id 对话ID
     */
    public void deleteConversation(Long id) {
        conversationMapper.deleteById(id);
    }
    
    /**
     * 获取用户的所有会话ID列表
     * @param userId 用户ID
     * @return 会话ID列表
     */
    public List<String> getAllSessionIdsByUser(Long userId) {
        return conversationMapper.findDistinctSessionIdsByUserId(userId);
    }

}
