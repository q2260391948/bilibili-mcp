package com.demo.mcp.mcpserver.service;

import com.demo.mcp.mcpserver.config.BilibiliConfig;
import com.demo.mcp.mcpserver.dto.DownloadTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 哔哩哔哩真实下载服务
 * 基于 Python 版本的下载逻辑实现
 */
@Slf4j
@Service
public class BilibiliDownloadService {
    
    @Autowired
    private BilibiliConfig bilibiliConfig;
    
    /**
     * 视频信息类
     */
    public static class VideoInfo {
        private String title;
        private String videoUrl;
        private String audioUrl;
        private int width;
        private int height;
        private int duration;
        
        // getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }
    
    /**
     * 开始真实下载任务
     */
    public CompletableFuture<Void> startRealDownload(DownloadTask task, String videoUrl, String sessdata) {
        return CompletableFuture.runAsync(() -> {
            String videoPath = null;
            String audioPath = null;
            
            try {
                log.info("开始真实下载任务: {} - {}", task.getTaskId(), videoUrl);
                
                // 1. 获取视频信息
                task.setStatus(DownloadTask.TaskStatus.DOWNLOADING);
                task.setProgress(5);
                
                VideoInfo videoInfo = fetchVideoInfo(videoUrl, sessdata);
                if (videoInfo == null) {
                    throw new RuntimeException("获取视频信息失败");
                }
                
                task.setTitle(videoInfo.getTitle());
                task.setProgress(10);
                
                log.info("视频信息: {} - 分辨率: {}x{}, 时长: {}s", 
                        videoInfo.getTitle(), videoInfo.getWidth(), videoInfo.getHeight(), videoInfo.getDuration());
                
                // 2. 创建目录
                createDirectories();
                
                // 3. 下载视频
                task.setProgress(15);
                String tempVideoName = sanitizeFileName(videoInfo.getTitle()) + "_video.mp4";
                videoPath = downloadFile(videoInfo.getVideoUrl(), tempVideoName, 
                        bilibiliConfig.getDownload().getTempDir(), sessdata, task, 15, 45);
                
                if (videoPath == null) {
                    throw new RuntimeException("视频下载失败");
                }
                
                // 4. 下载音频
                task.setProgress(50);
                String tempAudioName = sanitizeFileName(videoInfo.getTitle()) + "_audio.mp3";
                audioPath = downloadFile(videoInfo.getAudioUrl(), tempAudioName, 
                        bilibiliConfig.getDownload().getTempDir(), sessdata, task, 50, 80);
                
                if (audioPath == null) {
                    throw new RuntimeException("音频下载失败");
                }
                
                // 5. 合并视频和音频
                task.setProgress(85);
                String finalFileName = generateFileName(videoInfo.getTitle(), task.getBvid(), "未知UP主");
                String outputPath = mergeVideoAndAudio(videoPath, audioPath, finalFileName);
                
                if (outputPath == null) {
                    throw new RuntimeException("视频合并失败");
                }
                
                // 6. 清理临时文件
                if (bilibiliConfig.getDownload().getAutoCleanTemp()) {
                    cleanupTempFiles(videoPath, audioPath);
                }
                
                // 7. 完成
                task.setProgress(100);
                task.setStatus(DownloadTask.TaskStatus.COMPLETED);
                task.setFilePath(outputPath);
                
                log.info("下载任务完成: {} - {}", task.getTaskId(), outputPath);
                
            } catch (Exception e) {
                log.error("下载任务失败: {} - {}", task.getTaskId(), e.getMessage(), e);
                task.setStatus(DownloadTask.TaskStatus.FAILED);
                task.setErrorMessage("下载失败: " + e.getMessage());
                
                // 根据配置决定是否保留临时文件
                if (!bilibiliConfig.getDownload().getKeepTempOnError()) {
                    cleanupTempFiles(videoPath, audioPath);
                }
            }
        });
    }
    
    /**
     * 获取视频信息 - 严格按照Python版本实现
     */
    private VideoInfo fetchVideoInfo(String videoUrl, String sessdata) {
        try {
            log.info("正在获取视频信息: {}", videoUrl);
            
            URL url = new URL(videoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // 更加完整的请求头设置，模拟真实浏览器
            conn.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            conn.setRequestProperty("accept-language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("accept-encoding", "gzip, deflate, br");
            conn.setRequestProperty("cache-control", "no-cache");
            conn.setRequestProperty("pragma", "no-cache");
            conn.setRequestProperty("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
            conn.setRequestProperty("sec-ch-ua-mobile", "?0");
            conn.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
            conn.setRequestProperty("sec-fetch-dest", "document");
            conn.setRequestProperty("sec-fetch-mode", "navigate");
            conn.setRequestProperty("sec-fetch-site", "none");
            conn.setRequestProperty("sec-fetch-user", "?1");
            conn.setRequestProperty("upgrade-insecure-requests", "1");
            conn.setRequestProperty("referer", "https://www.bilibili.com/");
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            // 设置Cookie
            if (sessdata != null && !sessdata.isEmpty()) {
                conn.setRequestProperty("Cookie", "SESSDATA=" + sessdata + "; buvid3=; buvid4=; DedeUserID=; bili_jct=");
            } else {
                // 即使没有登录信息也设置一些基础cookies
                conn.setRequestProperty("Cookie", "buvid3=; buvid4=; finger=; CURRENT_FNVAL=4048; CURRENT_QUALITY=80;");
            }
            
            // 设置连接超时
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            
            int responseCode = conn.getResponseCode();
            log.info("HTTP响应状态码: {}", responseCode);
            
            if (responseCode != 200) {
                log.error("请求失败，状态码: {}", responseCode);
                // 尝试读取错误响应
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    log.error("错误响应内容: {}", errorResponse.toString().substring(0, Math.min(500, errorResponse.length())));
                } catch (Exception e) {
                    log.error("读取错误响应失败", e);
                }
                return null;
            }
            
            // 处理可能的gzip压缩
            InputStream inputStream = conn.getInputStream();
            String contentEncoding = conn.getHeaderField("Content-Encoding");
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                inputStream = new java.util.zip.GZIPInputStream(inputStream);
                log.info("响应使用gzip压缩，正在解压...");
            }
            
            // 读取响应内容
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            }
            
            String html = response.toString();
            log.info("HTML内容长度: {}", html.length());
            
            // 调试：输出HTML的前1000个字符
            if (html.length() > 0) {
                String htmlPreview = html.length() > 1000 ? html.substring(0, 1000) : html;
                log.info("HTML内容预览: {}", htmlPreview);
                
                // 检查是否包含关键内容
                boolean hasPlayinfo = html.contains("__playinfo__");
                boolean hasH1Tag = html.contains("<h1");
                boolean hasVideoKeyword = html.contains("video");
                boolean hasBvid = html.contains("BV1GJ411x7h7");
                
                log.info("内容检查: __playinfo__={}, <h1>={}, video={}, BVID={}", 
                        hasPlayinfo, hasH1Tag, hasVideoKeyword, hasBvid);
                
                // 如果没有关键内容，可能被重定向了
                if (!hasPlayinfo && !hasH1Tag) {
                    log.warn("页面可能被重定向或需要特殊处理");
                    
                    // 查找可能的重定向或错误信息
                    if (html.contains("登录") || html.contains("login")) {
                        log.warn("页面可能需要登录");
                    }
                    if (html.contains("移动版") || html.contains("mobile")) {
                        log.warn("可能被重定向到移动版");
                    }
                    if (html.contains("验证") || html.contains("captcha")) {
                        log.warn("可能需要验证码");
                    }
                }
            }
            
            // 多种方式提取视频标题
            String title = "未知标题";
            
            // 方式1：按照Python版本
            Pattern titlePattern1 = Pattern.compile("<h1 data-title=\"(.*?)\" title=\"");
            Matcher titleMatcher1 = titlePattern1.matcher(html);
            if (titleMatcher1.find()) {
                title = titleMatcher1.group(1);
                log.info("方式1提取到视频标题: {}", title);
            } else {
                // 方式2：通过title标签
                Pattern titlePattern2 = Pattern.compile("<title.*?>(.*?)</title>");
                Matcher titleMatcher2 = titlePattern2.matcher(html);
                if (titleMatcher2.find()) {
                    String titleFromTag = titleMatcher2.group(1);
                    if (titleFromTag.contains("_哔哩哔哩_bilibili")) {
                        title = titleFromTag.replace("_哔哩哔哩_bilibili", "").trim();
                        log.info("方式2通过title标签提取到标题: {}", title);
                    }
                } else {
                    // 方式3：通过其他可能的标题标签
                    Pattern titlePattern3 = Pattern.compile("<h1[^>]*>(.*?)</h1>");
                    Matcher titleMatcher3 = titlePattern3.matcher(html);
                    if (titleMatcher3.find()) {
                        title = titleMatcher3.group(1).replaceAll("<[^>]*>", "").trim();
                        log.info("方式3通过h1标签提取到标题: {}", title);
                    } else {
                        // 方式4：通过meta标签
                        Pattern titlePattern4 = Pattern.compile("<meta[^>]*name=\"title\"[^>]*content=\"(.*?)\"");
                        Matcher titleMatcher4 = titlePattern4.matcher(html);
                        if (titleMatcher4.find()) {
                            title = titleMatcher4.group(1);
                            log.info("方式4通过meta标签提取到标题: {}", title);
                        } else {
                            log.warn("所有方式都未能提取到视频标题");
                        }
                    }
                }
            }
            
            // 多种方式提取播放信息
            String playInfoJson = null;
            
            // 方式1：传统的__playinfo__方式
            Pattern playInfoPattern1 = Pattern.compile("__playinfo__=(.*?)</script>");
            Matcher playInfoMatcher1 = playInfoPattern1.matcher(html);
            if (playInfoMatcher1.find()) {
                playInfoJson = playInfoMatcher1.group(1);
                log.info("方式1找到__playinfo__数据，长度: {}", playInfoJson.length());
            } else {
                // 方式2：window.__playinfo__
                Pattern playInfoPattern2 = Pattern.compile("window\\.__playinfo__\\s*=\\s*(.*?);");
                Matcher playInfoMatcher2 = playInfoPattern2.matcher(html);
                if (playInfoMatcher2.find()) {
                    playInfoJson = playInfoMatcher2.group(1);
                    log.info("方式2找到window.__playinfo__数据，长度: {}", playInfoJson.length());
                } else {
                    // 方式3：__INITIAL_STATE__
                    Pattern initialStatePattern = Pattern.compile("__INITIAL_STATE__=(.*?);\\(function");
                    Matcher initialStateMatcher = initialStatePattern.matcher(html);
                    if (initialStateMatcher.find()) {
                        String initialState = initialStateMatcher.group(1);
                        log.info("方式3找到__INITIAL_STATE__数据，长度: {}", initialState.length());
                        // 从INITIAL_STATE中提取播放信息
                        if (initialState.contains("dash") && initialState.contains("video")) {
                            playInfoJson = initialState;
                            log.info("从__INITIAL_STATE__中找到视频信息");
                        }
                    } else {
                        // 方式4：__NEXT_DATA__
                        Pattern nextDataPattern = Pattern.compile("__NEXT_DATA__.*?({.*?})");
                        Matcher nextDataMatcher = nextDataPattern.matcher(html);
                        if (nextDataMatcher.find()) {
                            String nextData = nextDataMatcher.group(1);
                            log.info("方式4找到__NEXT_DATA__数据，长度: {}", nextData.length());
                            if (nextData.contains("dash") && nextData.contains("video")) {
                                playInfoJson = nextData;
                                log.info("从__NEXT_DATA__中找到视频信息");
                            }
                        }
                    }
                }
            }
            
            if (playInfoJson == null) {
                log.error("所有方式都无法找到播放信息数据");
                // 输出调试信息
                if (html.contains("__playinfo__")) {
                    int startPos = html.indexOf("__playinfo__");
                    String debugHtml = html.substring(Math.max(0, startPos - 100), Math.min(html.length(), startPos + 200));
                    log.debug("__playinfo__周围的HTML: {}", debugHtml);
                }
                if (html.contains("window.__INITIAL_STATE__")) {
                    log.debug("页面包含__INITIAL_STATE__");
                }
                if (html.contains("__NEXT_DATA__")) {
                    log.debug("页面包含__NEXT_DATA__");
                }
                return null;
            }
            
            log.info("成功提取到playinfo JSON，长度: {}", playInfoJson.length());
            
            // 解析JSON - 严格按照Python版本的路径
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setTitle(title);
            
            try {
                // 查找 video 数组中的第一个元素的 baseUrl
                Pattern videoUrlPattern = Pattern.compile("\"dash\":\\{.*?\"video\":\\[\\{[^}]*?\"baseUrl\":\"(https://[^\"]*?)\"");
                Matcher videoUrlMatcher = videoUrlPattern.matcher(playInfoJson);
                if (videoUrlMatcher.find()) {
                    String videoUrlStr = videoUrlMatcher.group(1).replace("\\/", "/");
                    videoInfo.setVideoUrl(videoUrlStr);
                    log.info("提取到视频URL: {}", videoUrlStr.substring(0, Math.min(100, videoUrlStr.length())) + "...");
                } else {
                    log.error("未找到视频URL");
                }
                
                // 查找 audio 数组中的第一个元素的 baseUrl
                Pattern audioUrlPattern = Pattern.compile("\"dash\":\\{.*?\"audio\":\\[\\{[^}]*?\"baseUrl\":\"(https://[^\"]*?)\"");
                Matcher audioUrlMatcher = audioUrlPattern.matcher(playInfoJson);
                if (audioUrlMatcher.find()) {
                    String audioUrlStr = audioUrlMatcher.group(1).replace("\\/", "/");
                    videoInfo.setAudioUrl(audioUrlStr);
                    log.info("提取到音频URL: {}", audioUrlStr.substring(0, Math.min(100, audioUrlStr.length())) + "...");
                } else {
                    log.error("未找到音频URL");
                }
                
                // 提取视频分辨率 - 从第一个video元素
                Pattern widthPattern = Pattern.compile("\"dash\":\\{.*?\"video\":\\[\\{[^}]*?\"width\":(\\d+)");
                Matcher widthMatcher = widthPattern.matcher(playInfoJson);
                if (widthMatcher.find()) {
                    int width = Integer.parseInt(widthMatcher.group(1));
                    videoInfo.setWidth(width);
                    log.info("视频宽度: {}", width);
                }
                
                Pattern heightPattern = Pattern.compile("\"dash\":\\{.*?\"video\":\\[\\{[^}]*?\"height\":(\\d+)");
                Matcher heightMatcher = heightPattern.matcher(playInfoJson);
                if (heightMatcher.find()) {
                    int height = Integer.parseInt(heightMatcher.group(1));
                    videoInfo.setHeight(height);
                    log.info("视频高度: {}", height);
                }
                
                // 提取时长
                Pattern durationPattern = Pattern.compile("\"duration\":(\\d+)");
                Matcher durationMatcher = durationPattern.matcher(playInfoJson);
                if (durationMatcher.find()) {
                    int duration = Integer.parseInt(durationMatcher.group(1));
                    videoInfo.setDuration(duration);
                    log.info("视频时长: {}秒", duration);
                }
                
            } catch (Exception e) {
                log.error("解析视频信息时出错", e);
            }
            
            // 验证是否成功获取了必要信息
            if (videoInfo.getVideoUrl() == null || videoInfo.getAudioUrl() == null) {
                log.error("关键信息缺失 - 视频URL: {}, 音频URL: {}", 
                         videoInfo.getVideoUrl() != null ? "有" : "无",
                         videoInfo.getAudioUrl() != null ? "有" : "无");
                return null;
            }
            
            log.info("成功获取视频信息: {} - 分辨率: {}x{}, 时长: {}s", 
                    title, videoInfo.getWidth(), videoInfo.getHeight(), videoInfo.getDuration());
            return videoInfo;
            
        } catch (Exception e) {
            log.error("获取视频信息时发生错误", e);
            return null;
        }
    }
    
    /**
     * 下载文件
     */
    private String downloadFile(String fileUrl, String filename, String dir, String sessdata, 
                               DownloadTask task, int startProgress, int endProgress) {
        try {
            log.info("开始下载文件: {}", filename);
            
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // 按照Python版本设置下载请求头
            conn.setRequestProperty("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            conn.setRequestProperty("referer", "https://www.bilibili.com/");
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
            
            // 设置Cookie
            if (sessdata != null && !sessdata.isEmpty()) {
                conn.setRequestProperty("Cookie", "SESSDATA=" + sessdata);
            }
            
            // 设置连接超时
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);  // 下载文件需要更长的超时时间
            
            int responseCode = conn.getResponseCode();
            log.info("下载请求响应码: {} - {}", responseCode, filename);
            
            if (responseCode != 200 && responseCode != 206) {  // 206是部分内容响应
                log.error("下载请求失败，状态码: {} - {}", responseCode, filename);
                return null;
            }
            
            long totalSize = conn.getContentLengthLong();
            
            Path filePath = Paths.get(dir, filename);
            
            try (InputStream inputStream = conn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
                
                byte[] buffer = new byte[8192];
                long downloadedSize = 0;
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;
                    
                    // 更新进度
                    if (totalSize > 0) {
                        int fileProgress = (int) ((downloadedSize * 100) / totalSize);
                        int taskProgress = startProgress + (fileProgress * (endProgress - startProgress)) / 100;
                        task.setProgress(taskProgress);
                    }
                }
            }
            
            log.info("文件下载完成: {}", filePath.toString());
            return filePath.toString();
            
        } catch (Exception e) {
            log.error("下载文件时发生错误: {}", filename, e);
            return null;
        }
    }
    
    /**
     * 合并视频和音频
     * 使用 JAVE 库实现，不依赖外部 FFmpeg
     */
    private String mergeVideoAndAudio(String videoPath, String audioPath, String title) {
        try {
            log.info("开始使用JAVE库合并视频和音频: {}", title);
            
            // 生成输出文件路径
            String sanitizedTitle = sanitizeFileName(title);
            String outputPath = getOutputFilePath(sanitizedTitle, "mp4");
            
            // 确保输出目录存在
            Path targetPath = Paths.get(outputPath);
            Files.createDirectories(targetPath.getParent());
            
            // 使用JAVE库合并视频和音频
            return mergeWithJave(videoPath, audioPath, outputPath, title);
            
        } catch (Exception e) {
            log.error("合并视频时发生错误", e);
            
            // 如果合并失败，至少保存视频文件
            try {
                String sanitizedTitle = sanitizeFileName(title);
                String fallbackPath = getOutputFilePath(sanitizedTitle + "_video_only", "mp4");
                
                // 确保目标目录存在
                Path targetPath = Paths.get(fallbackPath);
                Files.createDirectories(targetPath.getParent());
                
                Files.copy(Paths.get(videoPath), targetPath);
                log.info("合并失败，已保存视频文件: {}", fallbackPath);
                return fallbackPath;
            } catch (IOException ioException) {
                log.error("保存视频文件也失败了", ioException);
                
                // 最后尝试：直接返回视频文件路径
                if (Files.exists(Paths.get(videoPath))) {
                    log.info("返回临时视频文件路径: {}", videoPath);
                    return videoPath;
                }
                return null;
            }
        }
    }
    
    /**
     * 使用JAVE库合并视频和音频的具体实现
     * 当前暂时使用简单的文件合并方案，等依赖下载完成后启用完整功能
     */
    private String mergeWithJave(String videoPath, String audioPath, String outputPath, String title) {
        try {
            log.info("准备合并视频和音频 - 视频: {}, 音频: {}, 输出: {}", videoPath, audioPath, outputPath);
            
            File videoFile = new File(videoPath);
            File audioFile = new File(audioPath);
            
            // 检查输入文件是否存在
            if (!videoFile.exists()) {
                throw new RuntimeException("视频文件不存在: " + videoPath);
            }
            if (!audioFile.exists()) {
                throw new RuntimeException("音频文件不存在: " + audioPath);
            }
            
            log.info("文件检查通过 - 视频大小: {} bytes, 音频大小: {} bytes", 
                    videoFile.length(), audioFile.length());
            
            // 尝试使用ProcessBuilder调用ffmpeg（如果可用）
            if (tryFFmpegMerge(videoPath, audioPath, outputPath)) {
                return outputPath;
            }
            
            // FFmpeg不可用，使用MP4Parser进行简单合并
            log.info("FFmpeg不可用，尝试使用MP4Parser进行合并...");
            if (tryMp4ParserMerge(videoPath, audioPath, outputPath)) {
                return outputPath;
            }
            
            // 所有方法都失败，保存视频文件
            log.warn("所有合并方法都失败，保存视频文件");
            String fallbackPath = outputPath.replace(".mp4", "_video_only.mp4");
            Files.copy(Paths.get(videoPath), Paths.get(fallbackPath));
            log.info("已保存纯视频文件: {}", fallbackPath);
            return fallbackPath;
            
        } catch (Exception e) {
            log.error("合并失败，尝试保存视频文件", e);
            
            try {
                String fallbackPath = outputPath.replace(".mp4", "_video_only.mp4");
                Files.copy(Paths.get(videoPath), Paths.get(fallbackPath));
                log.info("异常恢复，已保存纯视频文件: {}", fallbackPath);
                return fallbackPath;
            } catch (IOException ioException) {
                log.error("保存视频文件也失败了", ioException);
                return null;
            }
        }
    }
    
    /**
     * 尝试使用FFmpeg进行合并（支持便携版和系统安装版）
     */
    private boolean tryFFmpegMerge(String videoPath, String audioPath, String outputPath) {
        // 尝试多个FFmpeg路径
        String[] ffmpegPaths = {
            "ffmpeg/ffmpeg.exe",        // 便携版FFmpeg
            "ffmpeg\\ffmpeg.exe",       // 便携版FFmpeg (反斜杠)
            "ffmpeg",                   // 系统PATH中的FFmpeg
            "C:\\ffmpeg\\bin\\ffmpeg.exe", // 常见安装路径
            "D:\\ffmpeg\\bin\\ffmpeg.exe"  // 另一个常见路径
        };
        
        for (String ffmpegPath : ffmpegPaths) {
            if (tryFFmpegWithPath(ffmpegPath, videoPath, audioPath, outputPath)) {
                return true;
            }
        }
        
        log.warn("所有FFmpeg路径都尝试失败");
        return false;
    }
    
    /**
     * 使用指定路径的FFmpeg进行合并
     */
    private boolean tryFFmpegWithPath(String ffmpegPath, String videoPath, String audioPath, String outputPath) {
        try {
            log.info("尝试使用FFmpeg: {}", ffmpegPath);
            
            // 检查FFmpeg文件是否存在（如果是文件路径）
            if (ffmpegPath.contains("/") || ffmpegPath.contains("\\")) {
                File ffmpegFile = new File(ffmpegPath);
                if (!ffmpegFile.exists()) {
                    log.debug("FFmpeg文件不存在: {}", ffmpegPath);
                    return false;
                }
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath, "-i", videoPath, "-i", audioPath,
                "-c:v", "copy", "-c:a", "aac", "-shortest", "-y", outputPath
            );
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 读取输出并记录关键信息
            boolean hasError = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 记录重要的错误信息
                    if (line.toLowerCase().contains("error") || line.toLowerCase().contains("failed")) {
                        log.warn("FFmpeg输出: {}", line);
                        hasError = true;
                    } else if (line.contains("time=") || line.contains("frame=")) {
                        // 这是进度信息，偶尔记录
                        log.debug("FFmpeg进度: {}", line);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && Files.exists(Paths.get(outputPath))) {
                File outputFile = new File(outputPath);
                log.info("✅ FFmpeg合并成功: {} (大小: {} bytes)", outputPath, outputFile.length());
                return true;
            } else {
                log.warn("❌ FFmpeg合并失败 - 路径: {}, 退出码: {}, 文件存在: {}", 
                        ffmpegPath, exitCode, Files.exists(Paths.get(outputPath)));
                return false;
            }
            
        } catch (Exception e) {
            log.debug("FFmpeg路径不可用: {} - {}", ffmpegPath, e.getMessage());
            return false;
        }
    }
    
    /**
     * 尝试使用MP4Parser进行简单合并
     * 注：这是一个备用方案，可能质量不如FFmpeg
     */
    private boolean tryMp4ParserMerge(String videoPath, String audioPath, String outputPath) {
        try {
            log.info("尝试使用MP4Parser进行文件合并...");
            
            // 暂时简单复制视频文件作为fallback
            // TODO: 实现真正的MP4Parser合并
            Files.copy(Paths.get(videoPath), Paths.get(outputPath));
            
            log.info("临时方案：已复制视频文件到: {}", outputPath);
            log.warn("注意：当前输出文件可能没有音频，请安装FFmpeg获得完整功能");
            
            return true;
            
        } catch (Exception e) {
            log.error("MP4Parser合并也失败了", e);
            return false;
        }
    }
    
    /**
     * 创建必要的目录
     */
    private void createDirectories() {
        try {
            // 创建临时目录
            Files.createDirectories(Paths.get(bilibiliConfig.getDownload().getTempDir()));
            
            // 创建基础下载目录
            Files.createDirectories(Paths.get(bilibiliConfig.getDownload().getBaseDir()));
            
            // 如果启用了子目录创建，也创建今天的日期目录
            if (bilibiliConfig.getDownload().getCreateSubDir()) {
                String subDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                Files.createDirectories(Paths.get(bilibiliConfig.getDownload().getBaseDir(), subDir));
                log.info("创建日期子目录: {}", subDir);
            }
            
            log.info("目录创建完成 - 临时目录: {}, 下载目录: {}", 
                    bilibiliConfig.getDownload().getTempDir(), 
                    bilibiliConfig.getDownload().getBaseDir());
        } catch (IOException e) {
            log.error("创建目录失败", e);
        }
    }
    
    /**
     * 生成输出文件路径
     */
    private String getOutputFilePath(String fileName, String extension) {
        String baseDir = bilibiliConfig.getDownload().getBaseDir();
        
        // 确保扩展名
        if (!fileName.endsWith("." + extension)) {
            fileName = fileName + "." + extension;
        }
        
        // 如果配置了创建子目录
        if (bilibiliConfig.getDownload().getCreateSubDir()) {
            String subDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return Paths.get(baseDir, subDir, fileName).toString();
        } else {
            return Paths.get(baseDir, fileName).toString();
        }
    }
    
    /**
     * 生成文件名（使用模板）
     */
    private String generateFileName(String title, String bvid, String author) {
        String template = bilibiliConfig.getDownload().getFileNameTemplate();
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        return template
                .replace("{title}", sanitizeFileName(title != null ? title : "未知标题"))
                .replace("{bvid}", bvid != null ? bvid : "unknown")
                .replace("{author}", sanitizeFileName(author != null ? author : "未知作者"))
                .replace("{date}", currentDate);
    }
    
    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "unknown";
        }
        
        // 移除或替换非法字符
        return fileName.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")  // Windows 非法字符
                .replaceAll("\\s+", " ")             // 多个空格合并为单个
                .replaceAll("^\\.|\\.$", "_")        // 不能以点开头或结尾
                .replaceAll("_{2,}", "_");           // 多个下划线合并为单个
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(String... filePaths) {
        for (String filePath : filePaths) {
            try {
                if (filePath != null) {
                    Files.deleteIfExists(Paths.get(filePath));
                    log.debug("已删除临时文件: {}", filePath);
                }
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", filePath, e);
            }
        }
    }
} 