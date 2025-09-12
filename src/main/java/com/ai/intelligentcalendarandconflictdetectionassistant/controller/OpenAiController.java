package com.ai.intelligentcalendarandconflictdetectionassistant.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author xushu
 * @version 1.0
 * @description:
 */
@RestController
@CrossOrigin
public class OpenAiController  {


    @CrossOrigin
    @GetMapping(value = "/ai/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public String generateStreamAsString(@RequestParam(value = "message", defaultValue = "讲个笑话") String message) {
        return null;
        // todo 从这开始
    }



}
