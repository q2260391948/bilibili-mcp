package com.demo.mcp.mcpserver.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 下载任务信息
 */
@Data
public class DownloadTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * BV号
     */
    private String bvid;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 下载进度（0-100）
     */
    private Integer progress;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING("待开始"),
        DOWNLOADING("下载中"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败"),
        CANCELLED("已取消");
        
        private final String description;
        
        TaskStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 