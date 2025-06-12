package com.demo.mcp.client.service;

import cn.hutool.json.JSONUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 哔哩哔哩客户端服务
 */
@Slf4j
@Service
public class BilibiliClientService {

    @Autowired(required = false)
    private List<McpSyncClient> mcpSyncClients;

    /**
     * 设置SESSDATA
     */
    public String setSessdata(String sessdata) {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 调用setSessdata工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("setSessdata", Map.of("sessdata", sessdata))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("设置SESSDATA失败", e);
            return "❌ 设置SESSDATA失败: " + e.getMessage();
        }
    }

    /**
     * 搜索视频
     */
    public String searchVideos(String keyword) {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 调用searchVideos工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("searchVideos", Map.of("keyword", keyword))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("搜索视频失败", e);
            return "❌ 搜索视频失败: " + e.getMessage();
        }
    }

    /**
     * 网页搜索视频（支持更多参数）
     */
    public String searchVideosFromWeb(String keyword, int page, String order, int duration) {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("keyword", keyword);
            arguments.put("page", page);
            arguments.put("order", order);
            arguments.put("duration", duration);
            
            // 调用searchVideosFromWeb工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("searchVideosFromWeb", Map.of("arguments", arguments))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("网页搜索视频失败", e);
            return "❌ 网页搜索视频失败: " + e.getMessage();
        }
    }

    /**
     * 创建下载任务
     */
    public String createDownloadTask(String videoUrlOrBvid) {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 调用createDownloadTask工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("createDownloadTask", Map.of("videoUrlOrBvid", videoUrlOrBvid))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("创建下载任务失败", e);
            return "❌ 创建下载任务失败: " + e.getMessage();
        }
    }

    /**
     * 查询下载状态
     */
    public String getDownloadStatus(String taskId) {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 调用getDownloadStatus工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("getDownloadStatus", Map.of("taskId", taskId))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("查询下载状态失败", e);
            return "❌ 查询下载状态失败: " + e.getMessage();
        }
    }

    /**
     * 获取服务器状态
     */
    public String getServerStatus() {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 调用getServerStatus工具
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest("getServerStatus", Map.of("random_string", "status"))
            );

            return parseToolResult(result);
        } catch (Exception e) {
            log.error("获取服务器状态失败", e);
            return "❌ 获取服务器状态失败: " + e.getMessage();
        }
    }

    /**
     * 获取可用工具列表
     */
    public String getAvailableTools() {
        try {
            if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
                return "❌ MCP客户端未连接";
            }

            McpSyncClient client = mcpSyncClients.get(0);
            
            // 获取工具列表
            McpSchema.ListToolsResult tools = client.listTools();

            StringBuilder sb = new StringBuilder();
            sb.append("🔧 可用工具列表:\n");
            
            for (McpSchema.Tool tool : tools.tools()) {
                sb.append(String.format("- %s: %s\n", tool.name(), tool.description()));
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("获取工具列表失败", e);
            return "❌ 获取工具列表失败: " + e.getMessage();
        }
    }

    /**
     * 解析工具调用结果
     */
    private String parseToolResult(McpSchema.CallToolResult result) {
        if (result == null) {
            return "❌ 工具调用结果为空";
        }

        if (result.isError()) {
            List<McpSchema.Content> errorContents = result.content();
            if (errorContents != null && !errorContents.isEmpty()) {
                StringBuilder errorMsg = new StringBuilder();
                for (McpSchema.Content content : errorContents) {
                    if (content instanceof McpSchema.TextContent) {
                        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
                        if (textContent.text() != null) {
                            errorMsg.append(textContent.text()).append(" ");
                        }
                    }
                }
                return "❌ 工具调用失败: " + errorMsg.toString().trim();
            } else {
                return "❌ 工具调用失败: 未知错误";
            }
        }

        // 获取结果内容
        List<McpSchema.Content> contents = result.content();
        if (contents == null || contents.isEmpty()) {
            return "❌ 工具调用结果内容为空";
        }

        StringBuilder sb = new StringBuilder();
        for (McpSchema.Content content : contents) {
            if (content instanceof McpSchema.TextContent) {
                McpSchema.TextContent textContent = (McpSchema.TextContent) content;
                if (textContent.text() != null && !textContent.text().trim().isEmpty()) {
                    sb.append(textContent.text()).append("\n");
                }
            }
        }

        String result_text = sb.toString().trim();
        return result_text.isEmpty() ? "✅ 操作完成" : result_text;
    }

    /**
     * 检查客户端连接状态
     */
    public String getConnectionStatus() {
        if (mcpSyncClients == null || mcpSyncClients.isEmpty()) {
            return "❌ MCP客户端未连接";
        }

        return String.format("✅ 已连接 %d 个MCP客户端", mcpSyncClients.size());
    }
} 