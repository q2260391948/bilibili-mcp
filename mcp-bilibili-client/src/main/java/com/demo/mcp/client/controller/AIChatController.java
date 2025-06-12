package com.demo.mcp.client.controller;

import com.demo.mcp.client.service.SimpleAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    @Autowired
    @Qualifier("simpleAIService")
    private SimpleAIService aiService;

    /**
     * 处理用户聊天输入
     */
    @PostMapping("/chat")
    public Map<String, Object> processChat(@RequestBody Map<String, String> request) {
        try {
            String userInput = request.get("message");
            if (userInput == null || userInput.trim().isEmpty()) {
                throw new IllegalArgumentException("消息内容不能为空");
            }
            
            log.info("收到用户消息: {}", userInput);
            
            // 使用AI服务处理用户输入
            Map<String, Object> response = aiService.processUserInput(userInput.trim());
            
            log.info("处理完成，成功: {}", response.get("success"));
            return response;
            
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            return Map.of(
                "success", false,
                "message", "处理失败: " + e.getMessage(),
                "steps", java.util.List.of()
            );
        }
    }

    /**
     * 获取AI助手的介绍信息
     */
    @GetMapping("/info")
    public Map<String, Object> getAIInfo() {
        return Map.of(
            "name", "哔哩哔哩AI助手",
            "version", "1.0.0",
            "description", "我可以帮助您管理哔哩哔哩视频，包括搜索、下载等功能。您可以用自然语言告诉我您想要做什么。",
            "capabilities", java.util.List.of(
                "设置登录信息（SESSDATA）",
                "搜索视频",
                "下载视频",
                "查询下载状态",
                "检查系统状态"
            )
        );
    }
} 