package com.demo.mcp.client.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI智能哔哩哔哩服务
 * 通过AI分析用户意图，自动调用相应的MCP服务
 */
@Slf4j
@Service
public class AIBilibiliService {

    @Autowired
    private BilibiliClientService bilibiliClientService;

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private static final String SYSTEM_PROMPT = 
            "你是一个哔哩哔哩视频管理助手，专门基于推理分析用户意图并调用MCP服务。禁止联网搜索。\n\n" +
            "可执行的操作：\n" +
            "1. setSessdata - 设置登录信息（当用户提供SESSDATA时）\n" +
            "2. search - 搜索视频（当用户想要搜索某个关键词）\n" +
            "3. searchAndDownload - 搜索并下载（当用户想下载视频但没有BV号时）\n" +
            "4. download - 直接下载（当用户提供了具体的BV号或链接时）\n" +
            "5. serverStatus - 检查服务器状态\n\n" +
            "**严格要求**：必须返回有效的JSON格式：\n" +
            "{\n" +
            "  \"action\": \"操作类型\",\n" +
            "  \"parameters\": {\n" +
            "    \"keyword\": \"搜索关键词\",\n" +
            "    \"sessdata\": \"登录信息\",\n" +
            "    \"videoUrl\": \"视频链接或BV号\"\n" +
            "  },\n" +
            "  \"message\": \"友好的回复信息\"\n" +
            "}\n\n" +
            "分析示例：\n" +
            "- \"下载程序员鱼皮的视频\" → searchAndDownload, keyword=\"程序员鱼皮\"\n" +
            "- \"搜索编程教程\" → search, keyword=\"编程教程\"\n" +
            "- \"设置SESSDATA=abc123\" → setSessdata, sessdata=\"abc123\"\n" +
            "- \"检查状态\" → serverStatus\n\n" +
            "**禁止**：联网搜索、文件操作、外部API调用。只能基于输入文本进行推理分析。";

    /**
     * 智能处理用户输入
     */
    public Map<String, Object> processUserInput(String userInput) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();
        
        try {
            log.info("处理用户输入: {}", userInput);
            
            // 使用AI分析用户意图
            String aiAnalysis = analyzeUserIntent(userInput);
            log.info("AI分析结果: {}", aiAnalysis);
            
            // 解析AI分析结果
            Map<String, Object> plan = parseAIResponse(aiAnalysis);
            log.info("解析后的执行计划: {}", plan);
            
            if (plan == null || plan.isEmpty()) {
                response.put("success", false);
                response.put("message", "抱歉，我无法理解您的请求。请明确说明您想要执行的操作。");
                response.put("steps", steps);
                return response;
            }
            
            // 执行计划
            String action = (String) plan.get("action");
            Map<String, Object> parameters = (Map<String, Object>) plan.get("parameters");
            String message = (String) plan.get("message");
            
            // 安全检查
            if (action == null || action.trim().isEmpty()) {
                action = "error";
            }
            if (parameters == null) {
                parameters = new HashMap<>();
            }
            if (message == null) {
                message = "正在处理您的请求...";
            }
            
            response.put("aiMessage", message);
            response.put("steps", steps);
            
            // 根据动作类型执行相应操作
            switch (action) {
                case "setSessdata":
                    executeSessdataAction(parameters, steps);
                    break;
                case "search":
                    executeSearchAction(parameters, steps);
                    break;
                case "download":
                    executeDownloadAction(parameters, steps);
                    break;
                case "searchAndDownload":
                    executeSearchAndDownloadAction(parameters, steps);
                    break;
                case "status":
                    executeStatusAction(steps);
                    break;
                case "serverStatus":
                    executeServerStatusAction(steps);
                    break;
                case "error":
                    steps.add(createStep("error", "处理错误", message, false));
                    break;
                default:
                    steps.add(createStep("error", "未知操作", "不支持的操作类型: " + action, false));
            }
            
            // 检查是否有失败的步骤
            boolean allSuccess = steps.stream().allMatch(step -> (Boolean) step.get("success"));
            response.put("success", allSuccess);
            response.put("message", allSuccess ? "操作完成" : "操作过程中出现错误");
            
        } catch (Exception e) {
            log.error("处理用户输入失败", e);
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            steps.add(createStep("error", "系统错误", e.getMessage(), false));
        }
        
        return response;
    }

    /**
     * 使用AI分析用户意图
     */
    private String analyzeUserIntent(String userInput) throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(SYSTEM_PROMPT)
                .build();
                
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userInput)
                .build();
                
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
                
        GenerationResult result = gen.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 解析AI响应
     */
    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI响应为空");
                return createDefaultPlan("AI响应为空，请重试");
            }
            
            // 提取JSON部分
            Pattern jsonPattern = Pattern.compile("\\{[^{}]*\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(aiResponse);
            
            if (matcher.find()) {
                String jsonStr = matcher.group();
                Map<String, Object> result = JsonUtils.fromJson(jsonStr, Map.class);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
            
            // 如果没有找到JSON，尝试简单解析
            return parseSimpleResponse(aiResponse);
            
        } catch (Exception e) {
            log.error("解析AI响应失败: " + aiResponse, e);
            return createDefaultPlan("解析AI响应失败: " + e.getMessage());
        }
    }

    /**
     * 创建默认计划
     */
    private Map<String, Object> createDefaultPlan(String message) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("action", "error");
        plan.put("parameters", new HashMap<>());
        plan.put("message", message != null ? message : "处理失败");
        return plan;
    }

    /**
     * 简单解析响应（备用方案）
     */
    private Map<String, Object> parseSimpleResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return createDefaultPlan("响应内容为空");
        }
        
        Map<String, Object> plan = new HashMap<>();
        Map<String, Object> parameters = new HashMap<>();
        
        String lowerResponse = response.toLowerCase();
        
        if (lowerResponse.contains("sessdata")) {
            plan.put("action", "setSessdata");
            // 提取SESSDATA，支持更宽松的模式
            Pattern sessdataPattern = Pattern.compile("sessdata[=：:\\s]*([a-zA-Z0-9%\\-_]+)");
            Matcher matcher = sessdataPattern.matcher(response);
            if (matcher.find() && matcher.group(1) != null) {
                parameters.put("sessdata", matcher.group(1));
            }
        } else if (lowerResponse.contains("搜索") || lowerResponse.contains("search")) {
            plan.put("action", "search");
            String keyword = extractKeywordImproved(response);
            if (keyword != null && !keyword.trim().isEmpty()) {
                parameters.put("keyword", keyword);
            }
        } else if (lowerResponse.contains("下载") || lowerResponse.contains("download")) {
            plan.put("action", "searchAndDownload");
            String keyword = extractKeywordImproved(response);
            if (keyword != null && !keyword.trim().isEmpty()) {
                parameters.put("keyword", keyword);
            }
        } else if (lowerResponse.contains("状态") || lowerResponse.contains("status")) {
            plan.put("action", "serverStatus");
        } else {
            // 如果包含人名，默认为搜索
            String keyword = extractKeywordImproved(response);
            if (keyword != null && !keyword.trim().isEmpty()) {
                plan.put("action", "search");
                parameters.put("keyword", keyword);
            } else {
                plan.put("action", "serverStatus");
            }
        }
        
        plan.put("parameters", parameters);
        plan.put("message", "正在处理您的请求...");
        
        return plan;
    }

    /**
     * 提取关键词
     */
    private String extractKeyword(String text) {
        // 简单的关键词提取逻辑
        String[] commonWords = {"搜索", "下载", "的", "视频", "找", "查找"};
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            boolean isCommon = false;
            for (String common : commonWords) {
                if (word.contains(common)) {
                    isCommon = true;
                    break;
                }
            }
            if (!isCommon && word.length() > 1) {
                return word;
            }
        }
        return null;
    }

    /**
     * 改进的关键词提取
     */
    private String extractKeywordImproved(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 移除常见的停用词
        String[] stopWords = {"设置", "搜索", "下载", "的", "视频", "找", "查找", "所有", "全部", 
                              "我想", "我要", "帮我", "请", "可以", "能", "吗", "呢", "吧"};
        
        // 首先尝试匹配人名或UP主名字
        String[] knownCreators = {"程序员鱼皮", "鱼皮", "李鱼皮"};
        for (String creator : knownCreators) {
            if (text.contains(creator)) {
                return creator;
            }
        }
        
        // 分词并过滤
        String[] words = text.replaceAll("[，。！？；：、]", " ").split("\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            word = word.trim();
            if (word.length() < 2) continue;
            
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equals(stopWord) || word.contains(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            
            if (!isStopWord) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word);
            }
        }
        
        String extracted = result.toString().trim();
        return extracted.isEmpty() ? null : extracted;
    }

    /**
     * 执行SESSDATA设置
     */
    private void executeSessdataAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String sessdata = (String) parameters.get("sessdata");
        if (sessdata == null) {
            steps.add(createStep("setSessdata", "设置登录信息", "未提供SESSDATA", false));
            return;
        }
        
        steps.add(createStep("setSessdata", "设置登录信息", "正在设置SESSDATA...", true));
        String result = bilibiliClientService.setSessdata(sessdata);
        log.info("设置SESSDATA结果: {}", result);
        steps.add(createStep("setSessdata", "设置结果", result, !result.contains("❌")));
    }

    /**
     * 执行搜索操作
     */
    private void executeSearchAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String keyword = (String) parameters.get("keyword");
        if (keyword == null) {
            steps.add(createStep("search", "搜索视频", "未提供搜索关键词", false));
            return;
        }
        
        steps.add(createStep("search", "搜索视频", "正在搜索: " + keyword, true));
        String result = bilibiliClientService.searchVideos(keyword);
        log.info("搜索结果: {}", result);
        steps.add(createStep("search", "搜索结果", result, !result.contains("❌")));
    }

    /**
     * 执行下载操作
     */
    private void executeDownloadAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String videoUrl = (String) parameters.get("videoUrl");
        if (videoUrl == null) {
            steps.add(createStep("download", "下载视频", "未提供视频链接或BV号", false));
            return;
        }
        
        steps.add(createStep("download", "创建下载任务", "正在创建下载任务: " + videoUrl, true));
        String result = bilibiliClientService.createDownloadTask(videoUrl);
        steps.add(createStep("download", "下载结果", result, !result.contains("❌")));
    }

    /**
     * 执行搜索并下载操作
     */
    private void executeSearchAndDownloadAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String keyword = (String) parameters.get("keyword");
        if (keyword == null) {
            steps.add(createStep("searchAndDownload", "搜索并下载", "未提供搜索关键词", false));
            return;
        }
        
        // 先搜索
        steps.add(createStep("search", "搜索视频", "正在搜索: " + keyword, true));
        String searchResult = bilibiliClientService.searchVideos(keyword);
        
        if (searchResult.contains("❌")) {
            steps.add(createStep("search", "搜索失败", searchResult, false));
            return;
        }
        
        steps.add(createStep("search", "搜索成功", searchResult, true));
        
        // 从搜索结果中提取BV号
        List<String> bvids = extractBVIds(searchResult);
        
        if (bvids.isEmpty()) {
            steps.add(createStep("extract", "提取视频信息", "未能从搜索结果中提取到BV号", false));
            return;
        }
        
        // 下载找到的视频
        for (String bvid : bvids) {
            steps.add(createStep("download", "创建下载任务", "正在下载: " + bvid, true));
            String downloadResult = bilibiliClientService.createDownloadTask(bvid);
            steps.add(createStep("download", "下载结果", downloadResult, !downloadResult.contains("❌")));
            
            if (downloadResult.contains("❌")) {
                break; // 遇到错误就停止
            }
        }
    }

    /**
     * 执行状态查询
     */
    private void executeStatusAction(List<Map<String, Object>> steps) {
        steps.add(createStep("status", "检查连接状态", "正在检查连接状态...", true));
        String result = bilibiliClientService.getConnectionStatus();
        steps.add(createStep("status", "连接状态", result, !result.contains("❌")));
    }

    /**
     * 执行服务器状态查询
     */
    private void executeServerStatusAction(List<Map<String, Object>> steps) {
        steps.add(createStep("serverStatus", "检查服务器状态", "正在检查服务器状态...", true));
        String result = bilibiliClientService.getServerStatus();
        steps.add(createStep("serverStatus", "服务器状态", result, !result.contains("❌")));
    }

    /**
     * 从搜索结果中提取BV号
     */
    private List<String> extractBVIds(String searchResult) {
        List<String> bvids = new ArrayList<>();
        Pattern bvPattern = Pattern.compile("BV[0-9A-Za-z]{10}");
        Matcher matcher = bvPattern.matcher(searchResult);
        
        while (matcher.find()) {
            bvids.add(matcher.group());
        }
        
        return bvids;
    }

    /**
     * 创建步骤对象
     */
    private Map<String, Object> createStep(String type, String title, String content, boolean success) {
        Map<String, Object> step = new HashMap<>();
        step.put("type", type);
        step.put("title", title);
        step.put("content", content);
        step.put("success", success);
        step.put("timestamp", System.currentTimeMillis());
        return step;
    }
} 