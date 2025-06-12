package com.demo.mcp.client.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * MCP客户端自定义配置
 */
@Slf4j
@Component
public class McpClientCustomizer implements McpSyncClientCustomizer {

    @Override
    public void customize(String serverConfigurationName, McpClient.SyncSpec spec) {
        log.info("正在自定义MCP客户端配置: {}", serverConfigurationName);
        
        // 设置请求超时时间
        spec.requestTimeout(Duration.ofSeconds(30));
        
        // 添加工具变更监听器
        spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
            log.info("工具列表已更新，当前工具数量: {}", tools.size());
            for (McpSchema.Tool tool : tools) {
                log.info("可用工具: {} - {}", tool.name(), tool.description());
            }
        });
        
        // 添加资源变更监听器
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            log.info("资源列表已更新，当前资源数量: {}", resources.size());
            for (McpSchema.Resource resource : resources) {
                log.info("可用资源: {} - {}", resource.name(), resource.description());
            }
        });
        
        // 添加提示变更监听器
        spec.promptsChangeConsumer((List<McpSchema.Prompt> prompts) -> {
            log.info("提示列表已更新，当前提示数量: {}", prompts.size());
            for (McpSchema.Prompt prompt : prompts) {
                log.info("可用提示: {} - {}", prompt.name(), prompt.description());
            }
        });
        
        // 添加日志监听器
        spec.loggingConsumer((McpSchema.LoggingMessageNotification log) -> {
            this.log.info("MCP服务器日志 [{}] {}: {}", 
                log.level(), 
                log.logger() != null ? log.logger() : "Unknown", 
                log.data());
        });
        
        log.info("MCP客户端配置完成: {}", serverConfigurationName);
    }
} 