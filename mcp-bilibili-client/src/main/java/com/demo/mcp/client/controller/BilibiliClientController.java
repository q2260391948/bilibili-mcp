package com.demo.mcp.client.controller;

import com.demo.mcp.client.service.BilibiliClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 哔哩哔哩MCP客户端控制器
 */
@RestController
@RequestMapping("/api/bilibili")
public class BilibiliClientController {

    @Autowired
    private BilibiliClientService bilibiliClientService;

    /**
     * 检查连接状态
     */
    @GetMapping("/status")
    public String getConnectionStatus() {
        return bilibiliClientService.getConnectionStatus();
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    public String getAvailableTools() {
        return bilibiliClientService.getAvailableTools();
    }

    /**
     * 获取服务器状态
     */
    @GetMapping("/server-status")
    public String getServerStatus() {
        return bilibiliClientService.getServerStatus();
    }

    /**
     * 设置SESSDATA
     */
    @PostMapping("/sessdata")
    public String setSessdata(@RequestParam String sessdata) {
        return bilibiliClientService.setSessdata(sessdata);
    }

    /**
     * 搜索视频
     */
    @GetMapping("/search")
    public String searchVideos(@RequestParam String keyword) {
        return bilibiliClientService.searchVideos(keyword);
    }

    /**
     * 网页搜索视频（高级搜索）
     */
    @GetMapping("/search-web")
    public String searchVideosFromWeb(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "totalrank") String order,
            @RequestParam(defaultValue = "0") int duration) {
        return bilibiliClientService.searchVideosFromWeb(keyword, page, order, duration);
    }

    /**
     * 创建下载任务
     */
    @PostMapping("/download")
    public String createDownloadTask(@RequestParam String videoUrlOrBvid) {
        return bilibiliClientService.createDownloadTask(videoUrlOrBvid);
    }

    /**
     * 查询下载状态
     */
    @GetMapping("/download/{taskId}")
    public String getDownloadStatus(@PathVariable String taskId) {
        return bilibiliClientService.getDownloadStatus(taskId);
    }
} 