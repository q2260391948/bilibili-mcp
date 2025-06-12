package com.demo.mcp.client.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简化的AI服务，基于关键词匹配，不依赖外部AI
 */
@Slf4j
@Service("simpleAIService")
public class SimpleAIService {

    @Autowired
    private BilibiliClientService bilibiliClientService;

    /**
     * 处理用户输入
     */
    public Map<String, Object> processUserInput(String userInput) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();
        
        try {
            log.info("处理用户输入: {}", userInput);
            
            // 直接基于关键词分析用户意图
            Map<String, Object> plan = analyzeUserIntent(userInput);
            log.info("分析结果: {}", plan);
            
            if (plan == null || plan.isEmpty()) {
                response.put("success", false);
                response.put("message", "抱歉，我无法理解您的请求。");
                response.put("steps", steps);
                return response;
            }
            
            String action = (String) plan.get("action");
            Map<String, Object> parameters = (Map<String, Object>) plan.get("parameters");
            String message = (String) plan.get("message");
            
            response.put("aiMessage", message);
            response.put("steps", steps);
            
            // 执行相应操作
            switch (action) {
                case "setSessdata":
                    executeSessdataAction(parameters, steps);
                    break;
                case "search":
                    executeSearchAction(parameters, steps);
                    break;
                case "searchAndDownload":
                    executeSearchAndDownloadAction(parameters, steps);
                    break;
                case "serverStatus":
                    executeServerStatusAction(steps);
                    break;
                default:
                    steps.add(createStep("error", "未知操作", "不支持的操作: " + action, false));
            }
            
            boolean allSuccess = steps.stream().allMatch(step -> (Boolean) step.get("success"));
            response.put("success", allSuccess);
            response.put("message", allSuccess ? "操作完成" : "操作过程中出现错误");
            
        } catch (Exception e) {
            log.error("处理失败", e);
            response.put("success", false);
            response.put("message", "处理失败: " + e.getMessage());
            steps.add(createStep("error", "系统错误", e.getMessage(), false));
        }
        
        return response;
    }

    /**
     * 分析用户意图
     */
    private Map<String, Object> analyzeUserIntent(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return null;
        }
        
        Map<String, Object> plan = new HashMap<>();
        Map<String, Object> parameters = new HashMap<>();
        String lowerInput = userInput.toLowerCase();
        
        // SESSDATA设置
        if (lowerInput.contains("sessdata") || lowerInput.contains("设置")) {
            plan.put("action", "setSessdata");
            plan.put("message", "正在设置登录信息...");
            
            // 提取SESSDATA
            Pattern pattern = Pattern.compile("sessdata[=：\\s]*([a-zA-Z0-9%\\-_]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(userInput);
            if (matcher.find()) {
                parameters.put("sessdata", matcher.group(1));
            }
        }
        // 下载操作
        else if (lowerInput.contains("下载")) {
            plan.put("action", "searchAndDownload");
            plan.put("message", "正在为您搜索并下载视频...");
            
            String keyword = extractKeyword(userInput);
            if (keyword != null) {
                parameters.put("keyword", keyword);
            }
        }
        // 搜索操作
        else if (lowerInput.contains("搜索")) {
            plan.put("action", "search");
            plan.put("message", "正在为您搜索视频...");
            
            String keyword = extractKeyword(userInput);
            if (keyword != null) {
                parameters.put("keyword", keyword);
            }
        }
        // 状态查询
        else if (lowerInput.contains("状态") || lowerInput.contains("检查")) {
            plan.put("action", "serverStatus");
            plan.put("message", "正在检查服务器状态...");
        }
        // 默认：如果包含人名，当作搜索
        else {
            String keyword = extractKeyword(userInput);
            if (keyword != null) {
                plan.put("action", "search");
                plan.put("message", "正在为您搜索: " + keyword);
                parameters.put("keyword", keyword);
            } else {
                plan.put("action", "serverStatus");
                plan.put("message", "正在检查系统状态...");
            }
        }
        
        plan.put("parameters", parameters);
        return plan;
    }

    /**
     * 提取关键词
     */
    private String extractKeyword(String text) {
        // 已知的UP主名字
        String[] creators = {"程序员鱼皮", "鱼皮", "李鱼皮"};
        for (String creator : creators) {
            if (text.contains(creator)) {
                return creator;
            }
        }
        
        // 停用词
        String[] stopWords = {"设置", "搜索", "下载", "的", "视频", "所有", "全部", 
                              "我想", "我要", "帮我", "请", "可以"};
        
        // 分词处理
        String[] words = text.replaceAll("[，。！？；：、]", " ").split("\\s+");
        for (String word : words) {
            word = word.trim();
            if (word.length() < 2) continue;
            
            boolean isStopWord = false;
            for (String stopWord : stopWords) {
                if (word.equals(stopWord)) {
                    isStopWord = true;
                    break;
                }
            }
            
            if (!isStopWord) {
                return word;
            }
        }
        
        return null;
    }

    /**
     * 执行SESSDATA设置
     */
    private void executeSessdataAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String sessdata = (String) parameters.get("sessdata");
        if (sessdata == null) {
            steps.add(createStep("setSessdata", "设置失败", "未提供SESSDATA", false));
            return;
        }
        
        steps.add(createStep("setSessdata", "设置登录信息", "正在设置SESSDATA...", true));
        String result = bilibiliClientService.setSessdata(sessdata);
        log.info("SESSDATA设置结果: {}", result);
        steps.add(createStep("setSessdata", "设置结果", result, !result.contains("❌")));
    }

    /**
     * 执行搜索
     */
    private void executeSearchAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String keyword = (String) parameters.get("keyword");
        if (keyword == null) {
            steps.add(createStep("search", "搜索失败", "未提供搜索关键词", false));
            return;
        }
        
        steps.add(createStep("search", "搜索视频", "正在搜索: " + keyword, true));
        String result = bilibiliClientService.searchVideos(keyword);
        log.info("搜索结果: {}", result);
        steps.add(createStep("search", "搜索结果", result, !result.contains("❌")));
    }

    /**
     * 执行搜索并下载
     */
    private void executeSearchAndDownloadAction(Map<String, Object> parameters, List<Map<String, Object>> steps) {
        String keyword = (String) parameters.get("keyword");
        if (keyword == null) {
            steps.add(createStep("searchAndDownload", "操作失败", "未提供搜索关键词", false));
            return;
        }
        
        // 1. 先搜索
        steps.add(createStep("search", "搜索视频", "正在搜索: " + keyword, true));
        String searchResult = bilibiliClientService.searchVideos(keyword);
        log.info("搜索结果: {}", searchResult);
        
        if (searchResult.contains("❌")) {
            steps.add(createStep("search", "搜索失败", searchResult, false));
            return;
        }
        
        steps.add(createStep("search", "搜索成功", "找到相关视频", true));
        
        // 2. 提取BV号
        List<String> bvids = extractBVIds(searchResult);
        if (bvids.isEmpty()) {
            steps.add(createStep("extract", "提取失败", "未能从搜索结果中找到视频BV号", false));
            return;
        }
        
        steps.add(createStep("extract", "提取视频", "找到 " + bvids.size() + " 个视频", true));
        
        // 3. 下载视频
        int successCount = 0;
        for (String bvid : bvids) {
            steps.add(createStep("download", "下载视频", "正在下载: " + bvid, true));
            String downloadResult = bilibiliClientService.createDownloadTask(bvid);
            log.info("下载结果: {}", downloadResult);
            
            if (downloadResult.contains("❌")) {
                steps.add(createStep("download", "下载失败", downloadResult, false));
                break; // 遇到错误停止
            } else {
                steps.add(createStep("download", "下载成功", "已创建下载任务: " + bvid, true));
                successCount++;
            }
        }
        
        if (successCount > 0) {
            steps.add(createStep("summary", "下载总结", "成功创建 " + successCount + " 个下载任务", true));
        }
    }

    /**
     * 执行服务器状态查询
     */
    private void executeServerStatusAction(List<Map<String, Object>> steps) {
        steps.add(createStep("status", "检查状态", "正在检查服务器状态...", true));
        String result = bilibiliClientService.getServerStatus();
        log.info("服务器状态: {}", result);
        steps.add(createStep("status", "服务器状态", result, !result.contains("❌")));
    }

    /**
     * 从搜索结果中提取BV号
     */
    private List<String> extractBVIds(String searchResult) {
        List<String> bvids = new ArrayList<>();
        Pattern pattern = Pattern.compile("BV[0-9A-Za-z]{10}");
        Matcher matcher = pattern.matcher(searchResult);
        
        while (matcher.find()) {
            String bvid = matcher.group();
            if (!bvids.contains(bvid)) {
                bvids.add(bvid);
            }
        }
        
        return bvids;
    }

    /**
     * 创建步骤
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