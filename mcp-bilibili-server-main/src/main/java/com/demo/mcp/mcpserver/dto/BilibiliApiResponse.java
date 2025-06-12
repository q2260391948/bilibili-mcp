package com.demo.mcp.mcpserver.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 哔哩哔哩 API 通用响应格式
 */
@Data
public class BilibiliApiResponse<T> {
    
    /**
     * 响应码，0表示成功
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    @JsonProperty("ts")
    private Long timestamp;
    
    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
} 