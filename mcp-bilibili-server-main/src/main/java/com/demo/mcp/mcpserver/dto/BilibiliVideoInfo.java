package com.demo.mcp.mcpserver.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 哔哩哔哩视频信息
 */
@Data
public class BilibiliVideoInfo {
    
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
     * 视频描述
     */
    @JsonProperty("desc")
    private String description;
    
    /**
     * 视频时长（秒）
     */
    private Integer duration;
    
    /**
     * UP主信息
     */
    private Owner owner;
    
    /**
     * 统计信息
     */
    private Stat stat;
    
    /**
     * 视频封面
     */
    @JsonProperty("pic")
    private String cover;
    
    /**
     * 发布时间戳
     */
    @JsonProperty("pubdate")
    private Long publishTime;
    
    /**
     * UP主信息
     */
    @Data
    public static class Owner {
        /**
         * UP主ID
         */
        private Long mid;
        
        /**
         * UP主昵称
         */
        private String name;
        
        /**
         * UP主头像
         */
        private String face;
    }
    
    /**
     * 统计信息
     */
    @Data
    public static class Stat {
        /**
         * 播放量
         */
        private Long view;
        
        /**
         * 弹幕数
         */
        private Long danmaku;
        
        /**
         * 点赞数
         */
        private Long like;
        
        /**
         * 投币数
         */
        private Long coin;
        
        /**
         * 收藏数
         */
        private Long favorite;
        
        /**
         * 分享数
         */
        private Long share;
        
        /**
         * 评论数
         */
        private Long reply;
    }
} 