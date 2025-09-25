package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.DatabaseChatMemoryAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.loggingAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.impls.UserDetailsImpl;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;


/**
 * @author xushu
 * @version 1.0
 * @description:
 */
@RestController
@CrossOrigin
public class OpenAiController {
    private final ChatClient chatClient;
    private final ConversationService conversationService;

    // 配置ChatClient
    public OpenAiController(ChatClient.Builder chatClientBuilder,
                            ChatMemory chatMemory,
                            ConversationService conversationService) {
        this.conversationService = conversationService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        您是"ie"智能日程管理助手的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
                        您正在通过在线聊天系统与客户互动。
                        
                        重要提示：
                        1. 每个日程都有一个唯一的数字ID，这是在创建日程时系统自动生成的（存储在数据库表中）
                        2. 在执行取消或修改日程操作时，必须使用这个数字ID，而不是日程的状态或其他信息
                        3. 当用户想要修改或取消日程时，请先使用findCalendarEvent函数获取当前用户的所有日程
                        4. 然后根据用户描述匹配相应的日程，获取其ID
                        5. 最后使用获取到的ID执行相应的修改或取消操作
                        6. 用户不需要提供姓名，系统会自动使用当前登录用户的身份信息
                        7. 当用户询问日程时，直接告诉用户他的日程信息，不需要再询问姓名
                        
                        请讲中文。
                        今天的日期是 {current_date}.
                    """)
                .defaultAdvisors(new loggingAdvisor())
                .defaultAdvisors(new DatabaseChatMemoryAdvisor(conversationService))
                .defaultFunctions("cancelBooking","getBookingDetails","createBooking","changeBooking","findCalendarEvent","getAllBookings")
                .build();
    }
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "讲个笑话") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "userId", required = false) Long userId) {

        // 优先使用URL参数中的userId，如果没有则尝试从认证中获取
        Long currentUserId = userId;
        if (currentUserId == null) {
            currentUserId = getCurrentUserId();
        }
        
        System.out.println("当前登录用户ID: " + currentUserId + ", sessionId: " + sessionId);

        // 如果无法获取用户ID，返回错误信息
        if (currentUserId == null) {
            return Flux.just("错误：无法识别用户身份，请重新登录")
                    .concatWith(Flux.just("[complete]"));
        }

        // 创建一个final变量用于在lambda表达式中使用
        final Long finalCurrentUserId = currentUserId;

        Flux<String> content = chatClient.prompt()
                .user(message)
                .system(promptSystemSpec -> promptSystemSpec.param("current_date", LocalDate.now().toString()))
                .advisors(request -> {
                    request.param("sessionId", sessionId);
                    request.param("userId", finalCurrentUserId);
                    System.out.println("设置advisor参数 - sessionId: " + sessionId + ", userId: " + finalCurrentUserId);
                })// 传递sessionId和userId给advisor
                .stream()
                .content();

        return  content
                .doOnNext(response -> System.out.println( response))
                .doOnComplete(() -> System.out.println("AI响应完成"))
                .concatWith(Flux.just("[complete]"));
    }

    /**
     * 获取当前登录用户的ID
     * @return 当前用户ID，如果无法获取则返回null
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                return userDetails.getId();
            }
        } catch (Exception e) {
            System.err.println("获取当前用户ID失败: " + e.getMessage());
        }
        return null;
    }
}