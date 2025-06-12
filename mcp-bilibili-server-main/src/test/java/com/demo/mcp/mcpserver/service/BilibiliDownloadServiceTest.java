package com.demo.mcp.mcpserver.service;

import com.demo.mcp.mcpserver.dto.DownloadTask;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 哔哩哔哩下载服务测试
 */
@Slf4j
@SpringBootTest
public class BilibiliDownloadServiceTest {

    @Autowired
    private BilibiliDownloadService bilibiliDownloadService;

    /**
     * 测试真实视频下载 - 使用一个短视频进行测试
     */
    @Test
    public void testRealVideoDownload() {
        try {
            log.info("=== 开始测试真实视频下载 ===");
            
            // 使用一个真实存在的哔哩哔哩视频
            String videoUrl = "https://www.bilibili.com/video/BV1q6TxziECu/";  // 这是一个经典短视频
            String sessdata = "";  // 可以设置真实的SESSDATA进行测试
            
            // 创建下载任务
            DownloadTask task = new DownloadTask();
            task.setTaskId("test_" + System.currentTimeMillis());
            task.setBvid("BV1q6TxziECu");
            task.setStatus(DownloadTask.TaskStatus.PENDING);
            task.setProgress(0);
            task.setCreateTime(LocalDateTime.now());
            
            log.info("创建下载任务: {} - {}", task.getTaskId(), videoUrl);
            
            // 启动下载
            CompletableFuture<Void> downloadFuture = bilibiliDownloadService.startRealDownload(task, videoUrl, sessdata);
            
            // 等待下载完成，最多等待5分钟
            downloadFuture.get(5, TimeUnit.MINUTES);
            
            // 验证结果
            log.info("下载任务状态: {}", task.getStatus());
            log.info("下载进度: {}%", task.getProgress());
            log.info("文件路径: {}", task.getFilePath());
            log.info("错误信息: {}", task.getErrorMessage());
            
            if (task.getStatus() == DownloadTask.TaskStatus.COMPLETED) {
                log.info("✅ 下载成功完成！");
                
                // 检查文件是否存在
                if (task.getFilePath() != null) {
                    File downloadedFile = new File(task.getFilePath());
                    if (downloadedFile.exists()) {
                        log.info("✅ 下载文件存在: {} (大小: {} bytes)", 
                                task.getFilePath(), downloadedFile.length());
                    } else {
                        log.error("❌ 下载文件不存在: {}", task.getFilePath());
                    }
                }
            } else {
                log.error("❌ 下载失败: {}", task.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("测试过程中发生异常", e);
        }
    }

    /**
     * 测试多个视频链接
     */
    @Test
    public void testMultipleVideoDownloads() {
        // 准备多个测试链接（确保这些是真实存在的视频）
        String[] testUrls = {
            "https://www.bilibili.com/video/BV1GJ411x7h7/",  // 经典测试视频
            "https://www.bilibili.com/video/BV1s54y1K7Dt/"   // 另一个短视频
        };
        
        for (String videoUrl : testUrls) {
            try {
                log.info("=== 测试视频: {} ===", videoUrl);
                
                // 提取BV号
                String bvid = videoUrl.replaceAll(".*/(BV[0-9A-Za-z]+).*", "$1");
                
                DownloadTask task = new DownloadTask();
                task.setTaskId("test_" + System.currentTimeMillis());
                task.setBvid(bvid);
                task.setStatus(DownloadTask.TaskStatus.PENDING);
                task.setProgress(0);
                task.setCreateTime(LocalDateTime.now());
                
                CompletableFuture<Void> downloadFuture = bilibiliDownloadService.startRealDownload(task, videoUrl, "");
                
                // 等待最多3分钟
                downloadFuture.get(3, TimeUnit.MINUTES);
                
                log.info("视频 {} 下载结果: {}", bvid, task.getStatus());
                
                // 短暂等待，避免请求过于频繁
                Thread.sleep(2000);
                
            } catch (Exception e) {
                log.error("下载视频 {} 时发生异常", videoUrl, e);
            }
        }
    }

    /**
     * 测试无效链接处理
     */
    @Test
    public void testInvalidVideoUrl() {
        try {
            log.info("=== 测试无效链接处理 ===");
            
            String invalidUrl = "https://www.bilibili.com/video/BV9999999999/";  // 无效的BV号
            
            DownloadTask task = new DownloadTask();
            task.setTaskId("test_invalid_" + System.currentTimeMillis());
            task.setBvid("BV9999999999");
            task.setStatus(DownloadTask.TaskStatus.PENDING);
            task.setProgress(0);
            task.setCreateTime(LocalDateTime.now());
            
            CompletableFuture<Void> downloadFuture = bilibiliDownloadService.startRealDownload(task, invalidUrl, "");
            
            // 等待完成
            downloadFuture.get(2, TimeUnit.MINUTES);
            
            // 应该失败
            if (task.getStatus() == DownloadTask.TaskStatus.FAILED) {
                log.info("✅ 正确处理了无效链接，任务状态: FAILED");
                log.info("错误信息: {}", task.getErrorMessage());
            } else {
                log.error("❌ 无效链接处理异常，任务状态: {}", task.getStatus());
            }
            
        } catch (Exception e) {
            log.error("测试无效链接时发生异常", e);
        }
    }

    /**
     * 测试网络连接和视频信息解析
     */
    @Test
    public void testVideoInfoParsing() {
        try {
            log.info("=== 测试视频信息解析 ===");
            
            String videoUrl = "https://www.bilibili.com/video/BV1GJ411x7h7/";
            
            DownloadTask task = new DownloadTask();
            task.setTaskId("test_info_" + System.currentTimeMillis());
            task.setBvid("BV1GJ411x7h7");
            task.setStatus(DownloadTask.TaskStatus.PENDING);
            task.setProgress(0);
            task.setCreateTime(LocalDateTime.now());
            
            // 只测试信息获取，不实际下载
            log.info("开始获取视频信息...");
            
            CompletableFuture<Void> downloadFuture = bilibiliDownloadService.startRealDownload(task, videoUrl, "");
            
            // 等待几秒钟，看看是否能成功获取视频信息
            Thread.sleep(10000);
            
            log.info("当前任务状态: {}", task.getStatus());
            log.info("当前进度: {}%", task.getProgress());
            log.info("视频标题: {}", task.getTitle());
            
            // 如果在下载过程中，说明信息获取成功
            if (task.getStatus() == DownloadTask.TaskStatus.DOWNLOADING && task.getProgress() > 0) {
                log.info("✅ 视频信息获取成功，开始下载过程");
            } else {
                log.warn("⚠️ 视频信息获取可能失败或网络延迟");
            }
            
        } catch (Exception e) {
            log.error("测试视频信息解析时发生异常", e);
        }
    }

    /**
     * 手动测试方法 - 用于IDE中直接运行
     */
    public static void main(String[] args) {
        log.info("=== 手动测试下载功能 ===");
        log.info("请在IDE中运行此测试，或使用 @Test 注解的方法");
        log.info("推荐测试视频: https://www.bilibili.com/video/BV1GJ411x7h7/");
        log.info("注意: 请确保网络连接正常，且可以访问哔哩哔哩");
    }
} 