package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.DatabaseChatMemoryAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.loggingAdvisor;
import com.ai.intelligentcalendarandconflictdetectionassistant.services.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
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
                        在提供有关创建日程或取消日程的信息之前，您必须已知
                        以下信息：用户的姓名，日程操作在哪一天
                        创建日程时必须得到会议开始和结束时间以及会议主题
                        在询问用户之前，请检查消息历史记录以获取此信息。
                        
                        重要提示：
                        1. 每个日程都有一个唯一的数字ID，这是在创建日程时系统自动生成的（存储在数据库表中）
                        2. 在执行取消或修改日程操作时，必须使用这个数字ID，而不是日程的状态或其他信息
                        3. 当用户想要修改或取消日程时，请先使用findCalendarEvent函数获取该用户的所有日程
                        4. 然后根据用户描述匹配相应的日程，获取其ID
                        5. 最后使用获取到的ID执行相应的修改或取消操作
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
            @RequestParam(value = "sessionId", required = false) String sessionId) {

        Flux<String> content = chatClient.prompt()
                .user(message)
                .system(promptSystemSpec -> promptSystemSpec.param("current_date", LocalDate.now().toString()))
                .advisors(request -> {
                    request.param("sessionId", sessionId);
                    System.out.println("设置advisor参数 - sessionId: " + sessionId);
                })// 传递sessionId给advisor
                .stream()
                .content();

        return  content
                .doOnNext(response -> System.out.println( response))
                .doOnComplete(() -> System.out.println("AI响应完成"))
                .concatWith(Flux.just("[complete]"));
    }
}
