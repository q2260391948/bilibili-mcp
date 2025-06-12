package com.demo.mcp.mcpserver.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 哔哩哔哩配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "bilibili")
public class BilibiliConfig {
    
    /**
     * API 接口配置
     */
    private Api api = new Api();
    
    /**
     * 下载配置
     */
    private Download download = new Download();
    
    /**
     * 请求头配置
     */
    private Headers headers = new Headers();
    
    @Data
    public static class Api {
        /**
         * 搜索接口
         */
        private String search = "https://api.bilibili.com/x/web-interface/search/all/v2";
        
        /**
         * 视频信息接口
         */
        private String videoInfo = "https://api.bilibili.com/x/web-interface/view";
        
        /**
         * 用户视频列表接口
         */
        private String userVideos = "https://api.bilibili.com/x/space/arc/search";
        
        /**
         * 用户信息接口
         */
        private String userInfo = "https://api.bilibili.com/x/web-interface/nav";
        
        /**
         * 播放地址接口
         */
        private String playUrl = "https://api.bilibili.com/x/player/playurl";
    }
    
    @Data
    public static class Download {
        /**
         * 下载基础目录（支持绝对路径和相对路径）
         */
        private String baseDir = "src/main/resources/downloads";
        
        /**
         * 临时目录（支持绝对路径和相对路径）
         */
        private String tempDir = "src/main/resources/temp";
        
        /**
         * 最大并发下载数
         */
        private Integer maxConcurrentDownloads = 3;
        
        /**
         * 请求超时时间（毫秒）
         */
        private Integer timeout = 30000;
        
        /**
         * 文件命名模板
         * 支持的占位符：{title}、{bvid}、{author}、{date}
         */
        private String fileNameTemplate = "{title}_{bvid}";
        
        /**
         * 是否为每个视频创建子目录
         */
        private Boolean createSubDir = true;
        
        /**
         * 是否自动清理临时文件
         */
        private Boolean autoCleanTemp = true;
        
        /**
         * 错误时是否保留临时文件（用于调试）
         */
        private Boolean keepTempOnError = false;
    }
    
    @Data
    public static class Headers {
        /**
         * User-Agent
         */
        private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
        
        /**
         * Referer
         */
        private String referer = "https://www.bilibili.com/";
    }
} 