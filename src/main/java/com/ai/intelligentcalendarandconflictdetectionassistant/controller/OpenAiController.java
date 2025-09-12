package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import com.ai.intelligentcalendarandconflictdetectionassistant.advisor.loggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
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
public class OpenAiController  {
    private  final ChatClient chatClient;

    //配置ChatClient
    public OpenAiController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
						您是“ie”智能日程管理助手的客户聊天支持代理。请以友好、乐于助人且愉快的方式来回复。
					   您正在通过在线聊天系统与客户互动。
					   在提供有关创建日程或取消日程的信息之前，您必须已知
                       以下信息：用户的姓名，日程操作在哪一天
                       在询问用户之前，请检查消息历史记录以获取此信息。
					   请讲中文。
					   今天的日期是 {current_date}.
					""")
                .defaultAdvisors(new PromptChatMemoryAdvisor(chatMemory))
                .defaultAdvisors(new loggingAdvisor())
                .defaultFunctions("cancelBooking","getBookingDetails")
                .build();
    }
    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        Flux<String> content = chatClient.prompt()//流式对话
                .user(message)
                .system(promptSystemSpec -> promptSystemSpec.param("current_date", LocalDate.now().toString()))
                .stream()
                .content();

        return  content
                .doOnNext(System.out::println)//解决sse长连接重复请求问题
                .concatWith(Flux.just("[complete]"));

    }



}
