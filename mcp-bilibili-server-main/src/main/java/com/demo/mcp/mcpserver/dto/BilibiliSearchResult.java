package com.demo.mcp.mcpserver.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 哔哩哔哩搜索结果
 */
@Data
public class BilibiliSearchResult {
    
    /**
     * BV号
     */
    private String bvid;
    
    /**
     * av号
     */
    private Long aid;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * UP主名称
     */
    private String author;
    
    /**
     * UP主ID
     */
    private Long mid;
    
    /**
     * 视频时长
     */
    private String duration;
    
    /**
     * 播放量
     */
    private Long play;
    
    /**
     * 视频封面
     */
    @JsonProperty("pic")
    private String cover;
    
    /**
     * 发布时间
     */
    private String pubdate;
    
    /**
     * 视频描述
     */
    private String description;
    
    /**
     * 标签
     */
    private String tag;
    
    /**
     * 视频URL
     */
    private String videoUrl;
    
    /**
     * 搜索结果列表（用于API响应）
     */
    private List<SearchResultItem> result;
    
    /**
     * 搜索结果项
     */
    @Data
    public static class SearchResultItem {
        
        /**
         * 结果类型
         */
        @JsonProperty("result_type")
        private String resultType;
        
        /**
         * 数据列表
         */
        private List<VideoSearchData> data;
    }
    
    /**
     * 视频搜索数据
     */
    @Data
    public static class VideoSearchData {
        
        /**
         * BV号
         */
        private String bvid;
        
        /**
         * av号
         */
        private Long aid;
        
        /**
         * 视频标题
         */
        private String title;
        
        /**
         * UP主名称
         */
        private String author;
        
        /**
         * UP主ID
         */
        private Long mid;
        
        /**
         * 视频时长
         */
        private String duration;
        
        /**
         * 播放量
         */
        private Long play;
        
        /**
         * 视频封面
         */
        @JsonProperty("pic")
        private String cover;
        
        /**
         * 发布时间
         */
        @JsonProperty("pubdate")
        private Long publishTime;
        
        /**
         * 视频描述
         */
        private String description;
        
        /**
         * 标签
         */
        private String tag;
    }
} 