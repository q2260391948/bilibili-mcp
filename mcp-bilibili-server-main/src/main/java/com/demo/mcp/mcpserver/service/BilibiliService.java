package com.demo.mcp.mcpserver.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.demo.mcp.mcpserver.config.BilibiliConfig;
import com.demo.mcp.mcpserver.dto.BilibiliSearchResult;
import com.demo.mcp.mcpserver.dto.BilibiliVideoInfo;
import com.demo.mcp.mcpserver.dto.DownloadTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 哔哩哔哩服务
 */
@Slf4j
@Service
public class BilibiliService {

    @Autowired
    private BilibiliConfig bilibiliConfig;

    @Autowired
    private BilibiliSearchService bilibiliSearchService;
    
    @Autowired
    private BilibiliDownloadService bilibiliDownloadService;

    private String currentSessdata = "";
    private final Map<String, DownloadTask> downloadTasks = new ConcurrentHashMap<>();
    private static final Pattern BV_PATTERN = Pattern.compile("BV[0-9A-Za-z]+");

    @Tool(description = "设置哔哩哔哩登录状态，传入SESSDATA用于下载高清视频")
    public String setSessdata(String sessdata) {
        try {
            if (StrUtil.isBlank(sessdata)) {
                this.currentSessdata = "";
                return "✅ SESSDATA已清空，将以访客身份访问";
            }

            HttpRequest request = HttpRequest.get(bilibiliConfig.getApi().getUserInfo())
                    .header("User-Agent", bilibiliConfig.getHeaders().getUserAgent())
                    .header("Referer", bilibiliConfig.getHeaders().getReferer())
                    .cookie("SESSDATA=" + sessdata)
                    .timeout(bilibiliConfig.getDownload().getTimeout());

            HttpResponse response = request.execute();
            if (response.getStatus() == 200) {
                // 先解析为通用响应
                Map<String, Object> responseMap = JSONUtil.toBean(response.body(), Map.class);
                Integer code = (Integer) responseMap.get("code");
                
                if (code != null && code == 0) {
                    Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                    if (data != null) {
                        String username = (String) data.get("uname");
                        this.currentSessdata = sessdata;
                        // 同步设置到搜索服务
                        bilibiliSearchService.setSessdata(sessdata);
                        return String.format("✅ SESSDATA设置成功，已登录用户: %s", username != null ? username : "未知用户");
                    }
                }
            }
            
            this.currentSessdata = sessdata;
            // 同步设置到搜索服务
            bilibiliSearchService.setSessdata(sessdata);
            return "⚠️ SESSDATA可能无效，但已保存。请检查是否能正常使用";
            
        } catch (Exception e) {
            log.error("设置SESSDATA时出错", e);
            return String.format("❌ 设置SESSDATA失败: %s", e.getMessage());
        }
    }

    @Tool(description = "通过网页搜索哔哩哔哩视频，支持排序和筛选")
    public String searchVideosFromWeb(Map<String, Object> arguments) {
        try {
            String keyword = (String) arguments.getOrDefault("keyword", "");
            int page = ((Number) arguments.getOrDefault("page", 1)).intValue();
            String order = (String) arguments.getOrDefault("order", "totalrank");
            int duration = ((Number) arguments.getOrDefault("duration", 0)).intValue();
            
            if (keyword.trim().isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "搜索关键词不能为空",
                    "data", List.of()
                ).toString();
            }
            
            log.info("从网页搜索视频: keyword={}, page={}, order={}, duration={}", keyword, page, order, duration);
            
            List<BilibiliSearchResult> results = bilibiliSearchService.searchVideosFromWeb(keyword, page, order, duration);
            
            // 转换为用户友好的格式
            List<Map<String, Object>> formattedResults = new ArrayList<>();
            for (BilibiliSearchResult result : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("bvid", result.getBvid());
                item.put("title", result.getTitle() != null ? result.getTitle() : "未知标题");
                item.put("author", result.getAuthor() != null ? result.getAuthor() : "未知UP主");
                item.put("videoUrl", result.getVideoUrl() != null ? result.getVideoUrl() : "");
                item.put("duration", result.getDuration() != null ? result.getDuration() : "未知");
                item.put("play", result.getPlay() != null ? result.getPlay() : 0);
                item.put("pubdate", result.getPubdate() != null ? result.getPubdate() : "未知");
                formattedResults.add(item);
            }
            
            String message = String.format("✅ 搜索成功！找到 %d 个视频结果", formattedResults.size());
            if (formattedResults.isEmpty()) {
                message = "❌ 未找到相关视频，请尝试其他关键词";
            }
            
            return Map.of(
                "success", true,
                "message", message,
                "data", formattedResults,
                "keyword", keyword,
                "page", page,
                "order", order,
                "duration", duration
            ).toString();
            
        } catch (Exception e) {
            log.error("网页搜索视频失败", e);
            return Map.of(
                "success", false,
                "message", "搜索失败: " + e.getMessage(),
                "data", List.of()
            ).toString();
        }
    }

//    @Tool(description = "搜索指定UP主的视频列表")
//    public Object searchUpVideos(Map<String, Object> arguments) {
//        try {
//            String upName = (String) arguments.getOrDefault("upName", "");
//            int page = ((Number) arguments.getOrDefault("page", 1)).intValue();
//
//            if (upName.trim().isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "UP主名称不能为空",
//                    "data", List.of()
//                );
//            }
//
//            log.info("搜索UP主视频: upName={}, page={}", upName, page);
//
//            List<BilibiliSearchResult> results = bilibiliSearchService.getUpVideosList(upName, page);
//
//            // 过滤出真正属于该UP主的视频
//            List<Map<String, Object>> formattedResults = new ArrayList<>();
//            for (BilibiliSearchResult result : results) {
//                // 简单的名称匹配过滤
//                if (result.getAuthor() != null &&
//                    (result.getAuthor().contains(upName) || upName.contains(result.getAuthor()))) {
//                    Map<String, Object> item = new HashMap<>();
//                    item.put("bvid", result.getBvid());
//                    item.put("title", result.getTitle() != null ? result.getTitle() : "未知标题");
//                    item.put("author", result.getAuthor());
//                    item.put("videoUrl", result.getVideoUrl() != null ? result.getVideoUrl() : "");
//                    item.put("duration", result.getDuration() != null ? result.getDuration() : "未知");
//                    item.put("play", result.getPlay() != null ? result.getPlay() : 0);
//                    item.put("pubdate", result.getPubdate() != null ? result.getPubdate() : "未知");
//                    formattedResults.add(item);
//                }
//            }
//
//            String message = String.format("✅ 找到UP主 \"%s\" 的 %d 个视频", upName, formattedResults.size());
//            if (formattedResults.isEmpty()) {
//                message = String.format("❌ 未找到UP主 \"%s\" 的视频，请检查名称是否正确", upName);
//            }
//
//            return Map.of(
//                "success", true,
//                "message", message,
//                "data", formattedResults,
//                "upName", upName,
//                "page", page
//            );
//
//        } catch (Exception e) {
//            log.error("搜索UP主视频失败", e);
//            return Map.of(
//                "success", false,
//                "message", "搜索失败: " + e.getMessage(),
//                "data", List.of()
//            );
//        }
//    }

    @Tool(description = "根据关键词搜索哔哩哔哩视频，返回视频列表信息")
    public String searchVideos(String keyword) {
        try {
            int page = 1;
            String order ="totalrank";
            int duration = 0;

            if (keyword.trim().isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "搜索关键词不能为空",
                        "data", List.of()
                ).toString();
            }

            log.info("从网页搜索视频: keyword={}, page={}, order={}, duration={}", keyword, page, order, duration);

            List<BilibiliSearchResult> results = bilibiliSearchService.searchVideosFromWeb(keyword, page, order, duration);

            // 转换为用户友好的格式
            List<Map<String, Object>> formattedResults = new ArrayList<>();
            for (BilibiliSearchResult result : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("bvid", result.getBvid());
                item.put("title", result.getTitle() != null ? result.getTitle() : "未知标题");
                item.put("author", result.getAuthor() != null ? result.getAuthor() : "未知UP主");
                item.put("videoUrl", result.getVideoUrl() != null ? result.getVideoUrl() : "");
                item.put("duration", result.getDuration() != null ? result.getDuration() : "未知");
                item.put("play", result.getPlay() != null ? result.getPlay() : 0);
                item.put("pubdate", result.getPubdate() != null ? result.getPubdate() : "未知");
                formattedResults.add(item);
            }

            String message = String.format("✅ 搜索成功！找到 %d 个视频结果", formattedResults.size());
            if (formattedResults.isEmpty()) {
                message = "❌ 未找到相关视频，请尝试其他关键词";
            }

            return Map.of(
                    "success", true,
                    "message", message,
                    "data", formattedResults,
                    "keyword", keyword,
                    "page", page,
                    "order", order,
                    "duration", duration
            ).toString();

        } catch (Exception e) {
            log.error("网页搜索视频失败", e);
            return Map.of(
                    "success", false,
                    "message", "搜索失败: " + e.getMessage(),
                    "data", List.of()
            ).toString();
        }
    }

//    @Tool(description = "获取哔哩哔哩视频的详细信息，输入视频链接或BV号")
//    public String getVideoInfo(String videoUrlOrBvid) {
//        try {
//            String bvid = extractBvid(videoUrlOrBvid);
//            if (StrUtil.isBlank(bvid)) {
//                return "❌ 无效的视频链接或BV号";
//            }
//
//            // 使用正确的视频信息API
//            String videoInfoUrl = "https://api.bilibili.com/x/web-interface/view";
//
//            HttpRequest request = HttpRequest.get(videoInfoUrl)
//                    .header("User-Agent", bilibiliConfig.getHeaders().getUserAgent())
//                    .header("Referer", bilibiliConfig.getHeaders().getReferer())
//                    .header("Accept", "application/json, text/plain, */*")
//                    .form("bvid", bvid)
//                    .timeout(bilibiliConfig.getDownload().getTimeout());
//
//            if (StrUtil.isNotBlank(currentSessdata)) {
//                request.cookie("SESSDATA=" + currentSessdata);
//            }
//
//            HttpResponse response = request.execute();
//            log.info("视频信息API响应状态: {}", response.getStatus());
//            log.debug("视频信息API响应内容: {}", response.body());
//
//            if (response.getStatus() != 200) {
//                return String.format("❌ 获取视频信息失败，HTTP状态码: %d", response.getStatus());
//            }
//
//            // 解析响应
//            Map<String, Object> responseMap = JSONUtil.toBean(response.body(), Map.class);
//
//            Integer code = (Integer) responseMap.get("code");
//            String message = (String) responseMap.get("message");
//
//            if (code == null || code != 0) {
//                return String.format("❌ 获取视频信息失败: %s (code: %d)", message != null ? message : "未知错误", code);
//            }
//
//            // 解析视频信息
//            Map<String, Object> dataMap = (Map<String, Object>) responseMap.get("data");
//            if (dataMap == null) {
//                return "❌ 获取视频信息失败: 返回数据为空";
//            }
//
//            return formatVideoInfoFromMap(dataMap);
//
//        } catch (Exception e) {
//            log.error("获取视频信息时出错", e);
//            return String.format("❌ 获取视频信息失败: %s", e.getMessage());
//        }
//    }

    @Tool(description = "创建视频下载任务，输入视频链接或BV号开始下载")
    public String createDownloadTask(String videoUrlOrBvid) {
        try {
            // 提取BV号
            String bvid = extractBvid(videoUrlOrBvid);
            // 如果BV号为空，则返回无效的视频链接或BV号
            if (StrUtil.isBlank(bvid)) {
                return "❌ 无效的视频链接或BV号";
            }

            // 检查是否有重复任务
            for (DownloadTask task : downloadTasks.values()) {
                // 如果BV号相同且任务状态为待处理、下载中或处理中，则返回该视频已有下载任务正在进行中
                if (bvid.equals(task.getBvid()) &&
                    (task.getStatus() == DownloadTask.TaskStatus.PENDING || 
                     task.getStatus() == DownloadTask.TaskStatus.DOWNLOADING ||
                     task.getStatus() == DownloadTask.TaskStatus.PROCESSING)) {
                    return String.format("⚠️ 该视频已有下载任务正在进行中，任务ID: %s", task.getTaskId());
                }
            }

            // 如果输入的是视频链接，则直接使用；否则拼接成完整的视频链接
            String videoUrl = videoUrlOrBvid.startsWith("http") ? videoUrlOrBvid :
                             "https://www.bilibili.com/video/" + bvid;

            // 使用真实下载服务
            String taskId = createDownloadTaskInternal(videoUrl, bvid);

            // 返回任务信息
            return String.format("""
                    ✅ 真实下载任务创建成功！
                    
                    📋 任务信息:
                    - 任务ID: %s
                    - BV号: %s
                    - 视频链接: %s
                    - 下载目录: downloads/
                    - 登录状态: %s
                    
                    🚀 下载功能:
                    ✓ 真实获取视频信息和下载链接
                    ✓ 分别下载视频和音频文件
                    ✓ 使用 FFmpeg 自动合并
                    ✓ 实时进度显示
                    ✓ 自动清理临时文件
                    
                    💡 监控提示:
                    - 使用 getDownloadStatus("%s") 查看详细进度
                    - 使用 getAllDownloadTasks() 查看所有任务
                    - 下载完成后文件保存在 downloads/ 目录
                    
                    ⚠️ 注意: 需要系统安装 FFmpeg 用于视频合并
                    """, 
                    taskId, 
                    bvid, 
                    videoUrl,
                    StrUtil.isNotBlank(currentSessdata) ? "已登录(高清)" : "访客模式(普清)",
                    taskId);

        } catch (Exception e) {
            // 记录错误日志，并返回创建下载任务失败的信息
            log.error("创建下载任务时出错", e);
            return String.format("❌ 创建下载任务失败: %s", e.getMessage());
        }
    }

    @Tool(description = "查询下载任务的状态和进度")
    public String getDownloadStatus(String taskId) {
        try {
            if (StrUtil.isBlank(taskId)) {
                if (downloadTasks.isEmpty()) {
                    return "📋 当前没有下载任务";
                }

                StringBuilder sb = new StringBuilder("📋 所有下载任务状态:\n\n");
                for (DownloadTask task : downloadTasks.values()) {
                    sb.append(formatTaskStatus(task)).append("\n---\n");
                }
                return sb.toString();
            }

            DownloadTask task = downloadTasks.get(taskId);
            if (task == null) {
                return String.format("❌ 未找到任务ID为 %s 的下载任务", taskId);
            }

            return formatTaskStatus(task);

        } catch (Exception e) {
            log.error("获取下载状态时出错", e);
            return String.format("❌ 获取下载状态失败: %s", e.getMessage());
        }
    }

    @Tool(description = "获取哔哩哔哩下载服务器的状态信息")
    public String getServerStatus() {
        try {
            Map<DownloadTask.TaskStatus, Long> statusCount = new HashMap<>();
            downloadTasks.values().forEach(task -> 
                statusCount.merge(task.getStatus(), 1L, Long::sum));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            return String.format("""
                    🤖 哔哩哔哩视频下载 MCP 服务器状态
                    
                    📊 基本信息:
                    - 服务名称: bilibili-mcp-server
                    - 版本: 1.0.0
                    - 状态: 运行中 ✅
                    - 当前时间: %s
                    
                    🔐 登录状态:
                    - SESSDATA: %s
                    - 权限模式: %s
                    
                    📁 目录配置:
                    - 下载目录: %s
                    - 临时目录: %s
                    - 最大并发数: %d
                    
                    📋 任务统计:
                    - 总任务数: %d
                    - 待处理: %d
                    - 下载中: %d
                    - 已完成: %d
                    - 失败: %d
                    
                    🛠️ 可用工具:
                    - setSessdata: 设置登录状态
                    - searchVideos: 搜索视频
                    - getVideoInfo: 获取视频信息
                    - createDownloadTask: 创建下载任务
                    - getDownloadStatus: 查询任务状态
                    - getServerStatus: 服务器状态
                    
                    💡 使用提示:
                    1. 建议先设置SESSDATA以获得更好的体验
                    2. 搜索后可以获取具体的BV号进行下载
                    3. 实际下载功能需要额外的视频处理工具支持
                    """,
                    LocalDateTime.now().format(formatter),
                    StrUtil.isNotBlank(currentSessdata) ? "已设置" : "未设置",
                    StrUtil.isNotBlank(currentSessdata) ? "会员模式" : "访客模式",
                    bilibiliConfig.getDownload().getBaseDir(),
                    bilibiliConfig.getDownload().getTempDir(),
                    bilibiliConfig.getDownload().getMaxConcurrentDownloads(),
                    downloadTasks.size(),
                    statusCount.getOrDefault(DownloadTask.TaskStatus.PENDING, 0L),
                    statusCount.getOrDefault(DownloadTask.TaskStatus.DOWNLOADING, 0L),
                    statusCount.getOrDefault(DownloadTask.TaskStatus.COMPLETED, 0L),
                    statusCount.getOrDefault(DownloadTask.TaskStatus.FAILED, 0L));

        } catch (Exception e) {
            log.error("获取服务器状态时出错", e);
            return String.format("❌ 获取服务器状态失败: %s", e.getMessage());
        }
    }

    /**
     * 搜索并批量下载视频
     */
//    @Tool(description = "搜索视频并批量创建下载任务，支持排序和筛选")
//    public Object searchAndDownload(Map<String, Object> arguments) {
//        try {
//            String keyword = (String) arguments.getOrDefault("keyword", "");
//            int page = ((Number) arguments.getOrDefault("page", 1)).intValue();
//            String order = (String) arguments.getOrDefault("order", "totalrank");
//            int duration = ((Number) arguments.getOrDefault("duration", 0)).intValue();
//            int maxDownloads = ((Number) arguments.getOrDefault("maxDownloads", 5)).intValue(); // 最大下载数量
//
//            if (keyword.trim().isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "搜索关键词不能为空",
//                    "data", List.of()
//                );
//            }
//
//            log.info("搜索并下载视频: keyword={}, page={}, order={}, duration={}, maxDownloads={}",
//                    keyword, page, order, duration, maxDownloads);
//
//            // 1. 先搜索视频
//            List<BilibiliSearchResult> searchResults = bilibiliSearchService.searchVideosFromWeb(keyword, page, order, duration);
//
//            if (searchResults.isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "未找到相关视频",
//                    "data", List.of()
//                );
//            }
//
//            // 2. 限制下载数量
//            List<BilibiliSearchResult> toDownload = searchResults.stream()
//                    .limit(maxDownloads)
//                    .collect(Collectors.toList());
//
//            // 3. 批量创建下载任务
//            List<Map<String, Object>> downloadTasks = new ArrayList<>();
//
//            for (BilibiliSearchResult result : toDownload) {
//                try {
//                    String taskId = createDownloadTaskInternal(result.getVideoUrl(), result.getBvid());
//
//                    downloadTasks.add(Map.of(
//                        "taskId", taskId,
//                        "bvid", result.getBvid(),
//                        "title", result.getTitle(),
//                        "author", result.getAuthor() != null ? result.getAuthor() : "未知",
//                        "videoUrl", result.getVideoUrl(),
//                        "status", "已创建"
//                    ));
//
//                    log.info("创建下载任务: {} - {}", result.getBvid(), result.getTitle());
//
//                } catch (Exception e) {
//                    log.error("创建下载任务失败: {} - {}", result.getBvid(), e.getMessage());
//                    downloadTasks.add(Map.of(
//                        "taskId", "",
//                        "bvid", result.getBvid(),
//                        "title", result.getTitle(),
//                        "author", result.getAuthor() != null ? result.getAuthor() : "未知",
//                        "videoUrl", result.getVideoUrl(),
//                        "status", "创建失败: " + e.getMessage()
//                    ));
//                }
//            }
//
//            return Map.of(
//                "success", true,
//                "message", String.format("搜索到 %d 个视频，已创建 %d 个下载任务",
//                        searchResults.size(), downloadTasks.size()),
//                "data", Map.of(
//                    "keyword", keyword,
//                    "totalFound", searchResults.size(),
//                    "downloadTasks", downloadTasks
//                )
//            );
//
//        } catch (Exception e) {
//            log.error("搜索并下载失败", e);
//            return Map.of(
//                "success", false,
//                "message", "搜索并下载失败: " + e.getMessage(),
//                "data", List.of()
//            );
//        }
//    }

    /**
     * 根据UP主搜索并下载视频
     */
//    @Tool(description = "搜索指定UP主的视频并批量下载")
//    public Object searchUpAndDownload(Map<String, Object> arguments) {
//        try {
//            String upName = (String) arguments.getOrDefault("upName", "");
//            int page = ((Number) arguments.getOrDefault("page", 1)).intValue();
//            int maxDownloads = ((Number) arguments.getOrDefault("maxDownloads", 3)).intValue();
//
//            if (upName.trim().isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "UP主名称不能为空",
//                    "data", List.of()
//                );
//            }
//
//            log.info("搜索UP主并下载: upName={}, page={}, maxDownloads={}", upName, page, maxDownloads);
//
//            // 1. 搜索UP主视频
//            List<BilibiliSearchResult> searchResults = bilibiliSearchService.getUpVideosList(upName, page);
//
//            if (searchResults.isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "未找到该UP主的视频",
//                    "data", List.of()
//                );
//            }
//
//            // 2. 限制下载数量
//            List<BilibiliSearchResult> toDownload = searchResults.stream()
//                    .limit(maxDownloads)
//                    .collect(Collectors.toList());
//
//            // 3. 批量创建下载任务
//            List<Map<String, Object>> downloadTasks = new ArrayList<>();
//
//            for (BilibiliSearchResult result : toDownload) {
//                try {
//                    String taskId = createDownloadTaskInternal(result.getVideoUrl(), result.getBvid());
//
//                    downloadTasks.add(Map.of(
//                        "taskId", taskId,
//                        "bvid", result.getBvid(),
//                        "title", result.getTitle(),
//                        "author", result.getAuthor() != null ? result.getAuthor() : upName,
//                        "videoUrl", result.getVideoUrl(),
//                        "status", "已创建"
//                    ));
//
//                } catch (Exception e) {
//                    log.error("创建下载任务失败: {} - {}", result.getBvid(), e.getMessage());
//                    downloadTasks.add(Map.of(
//                        "taskId", "",
//                        "bvid", result.getBvid(),
//                        "title", result.getTitle(),
//                        "author", upName,
//                        "videoUrl", result.getVideoUrl(),
//                        "status", "创建失败: " + e.getMessage()
//                    ));
//                }
//            }
//
//            return Map.of(
//                "success", true,
//                "message", String.format("找到UP主 '%s' 的 %d 个视频，已创建 %d 个下载任务",
//                        upName, searchResults.size(), downloadTasks.size()),
//                "data", Map.of(
//                    "upName", upName,
//                    "totalFound", searchResults.size(),
//                    "downloadTasks", downloadTasks
//                )
//            );
//
//        } catch (Exception e) {
//            log.error("搜索UP主并下载失败", e);
//            return Map.of(
//                "success", false,
//                "message", "搜索UP主并下载失败: " + e.getMessage(),
//                "data", List.of()
//            );
//        }
//    }

    /**
     * 智能视频解析和下载
     */
//    @Tool(description = "智能解析视频链接信息并下载，支持BV号、链接、搜索关键词")
//    public Object smartDownload(Map<String, Object> arguments) {
//        try {
//            String input = (String) arguments.getOrDefault("input", "");
//
//            if (input.trim().isEmpty()) {
//                return Map.of(
//                    "success", false,
//                    "message", "输入不能为空",
//                    "data", Map.of()
//                );
//            }
//
//            log.info("智能下载: input={}", input);
//
//            // 1. 判断输入类型
//            String bvid = extractBvid(input);
//
//            if (bvid != null) {
//                // 直接下载BV号
//                log.info("检测到BV号，直接下载: {}", bvid);
//                String taskId = createDownloadTaskInternal("https://www.bilibili.com/video/" + bvid, bvid);
//
//                return Map.of(
//                    "success", true,
//                    "message", "已创建下载任务",
//                    "data", Map.of(
//                        "type", "direct",
//                        "bvid", bvid,
//                        "taskId", taskId
//                    )
//                );
//
//            } else if (input.contains("bilibili.com/video/")) {
//                // 是视频链接
//                log.info("检测到视频链接，直接下载: {}", input);
//                String extractedBvid = extractBvid(input);
//                String taskId = createDownloadTaskInternal(input, extractedBvid);
//
//                return Map.of(
//                    "success", true,
//                    "message", "已创建下载任务",
//                    "data", Map.of(
//                        "type", "link",
//                        "bvid", extractedBvid,
//                        "taskId", taskId,
//                        "videoUrl", input
//                    )
//                );
//
//            } else {
//                // 当作搜索关键词处理
//                log.info("当作搜索关键词处理: {}", input);
//
//                List<BilibiliSearchResult> searchResults = bilibiliSearchService.searchVideosFromWeb(input, 1, "totalrank", 0);
//
//                if (searchResults.isEmpty()) {
//                    return Map.of(
//                        "success", false,
//                        "message", "未找到相关视频",
//                        "data", Map.of("type", "search", "keyword", input)
//                    );
//                }
//
//                // 下载第一个搜索结果
//                BilibiliSearchResult firstResult = searchResults.get(0);
//                String taskId = createDownloadTaskInternal(firstResult.getVideoUrl(), firstResult.getBvid());
//
//                return Map.of(
//                    "success", true,
//                    "message", String.format("通过搜索找到视频并创建下载任务: %s", firstResult.getTitle()),
//                    "data", Map.of(
//                        "type", "search",
//                        "keyword", input,
//                        "bvid", firstResult.getBvid(),
//                        "title", firstResult.getTitle(),
//                        "author", firstResult.getAuthor(),
//                        "taskId", taskId,
//                        "totalFound", searchResults.size()
//                    )
//                );
//            }
//
//        } catch (Exception e) {
//            log.error("智能下载失败", e);
//            return Map.of(
//                "success", false,
//                "message", "智能下载失败: " + e.getMessage(),
//                "data", Map.of()
//            );
//        }
//    }

    /**
     * 获取所有下载任务状态
     */
    @Tool(description = "获取所有下载任务的状态列表")
    public Object getAllDownloadTasks() {
        try {
            List<Map<String, Object>> allTasks = new ArrayList<>();
            
            for (Map.Entry<String, DownloadTask> entry : downloadTasks.entrySet()) {
                DownloadTask task = entry.getValue();
                allTasks.add(Map.of(
                    "taskId", entry.getKey(),
                    "bvid", task.getBvid() != null ? task.getBvid() : "",
                    "title", task.getTitle() != null ? task.getTitle() : "",
                    "status", task.getStatus().toString(),
                    "progress", task.getProgress(),
                    "createTime", task.getCreateTime(),
                    "finishTime", task.getFinishTime(),
                    "filePath", task.getFilePath() != null ? task.getFilePath() : ""
                ));
            }
            
            // 按创建时间倒序排列
            allTasks.sort((a, b) -> 
                Long.compare((Long) b.get("createTime"), (Long) a.get("createTime")));
            
            return Map.of(
                "success", true,
                "message", String.format("当前共有 %d 个下载任务", allTasks.size()),
                "data", Map.of(
                    "totalTasks", allTasks.size(),
                    "tasks", allTasks
                )
            );
            
        } catch (Exception e) {
            log.error("获取下载任务列表失败", e);
            return Map.of(
                "success", false,
                "message", "获取下载任务列表失败: " + e.getMessage(),
                "data", Map.of()
            );
        }
    }

    /**
     * 内部方法：创建下载任务（真实下载）
     */
    private String createDownloadTaskInternal(String videoUrl, String bvid) {
        String taskId = "task_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
        
        DownloadTask task = new DownloadTask();
        task.setTaskId(taskId);
        task.setBvid(bvid);
        task.setStatus(DownloadTask.TaskStatus.PENDING);
        task.setProgress(0);
        task.setCreateTime(LocalDateTime.now());
        
        // 保存任务到内存
        this.downloadTasks.put(taskId, task);
        
        // 启动真实下载
        bilibiliDownloadService.startRealDownload(task, videoUrl, currentSessdata)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("下载任务异常: {} - {}", taskId, throwable.getMessage());
                    task.setStatus(DownloadTask.TaskStatus.FAILED);
                    task.setErrorMessage("下载异常: " + throwable.getMessage());
                } else {
                    task.setFinishTime(LocalDateTime.now());
                    log.info("下载任务处理完成: {}", taskId);
                }
            });
        
        log.info("创建真实下载任务: {} - {}", taskId, videoUrl);
        return taskId;
    }

    private String extractBvid(String input) {
        if (StrUtil.isBlank(input)) return null;
        Matcher matcher = BV_PATTERN.matcher(input);
        return matcher.find() ? matcher.group() : null;
    }

    private String formatSearchResultsFromMap(Map<String, Object> dataMap, String keyword) {
        try {
            StringBuilder result = new StringBuilder();
            result.append(String.format("🔍 搜索关键词: %s\n\n", keyword));

            // 获取result数组
            Object resultObj = dataMap.get("result");
            if (!(resultObj instanceof List)) {
                return String.format("🔍 搜索关键词: %s\n❌ 未找到相关视频", keyword);
            }

            List<Map<String, Object>> resultList = (List<Map<String, Object>>) resultObj;
            int videoCount = 0;

            for (Map<String, Object> item : resultList) {
                String resultType = (String) item.get("result_type");
                if ("video".equals(resultType)) {
                    Object dataObj = item.get("data");
                    if (dataObj instanceof List) {
                        List<Map<String, Object>> videos = (List<Map<String, Object>>) dataObj;
                        
                        for (Map<String, Object> video : videos) {
                            if (videoCount >= 10) break; // 限制显示数量

                            String title = (String) video.get("title");
                            String author = (String) video.get("author");
                            String bvid = (String) video.get("bvid");
                            String duration = (String) video.get("duration");
                            Object playObj = video.get("play");
                            Long play = playObj != null ? ((Number) playObj).longValue() : 0L;

                            result.append(String.format("""
                                    📺 视频 %d:
                                    标题: %s
                                    UP主: %s
                                    BV号: %s
                                    链接: https://www.bilibili.com/video/%s
                                    播放量: %s
                                    时长: %s
                                    
                                    """,
                                    videoCount + 1,
                                    title != null ? title : "未知",
                                    author != null ? author : "未知",
                                    bvid != null ? bvid : "未知",
                                    bvid != null ? bvid : "unknown",
                                    formatNumber(play),
                                    duration != null ? duration : "未知"
                            ));
                            videoCount++;
                        }
                    }
                    break; // 只处理第一个视频结果组
                }
            }

            if (videoCount == 0) {
                result.append("❌ 未找到相关视频");
            } else {
                result.append(String.format("✅ 共找到 %d 个视频结果", videoCount));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("格式化搜索结果时出错", e);
            return String.format("🔍 搜索关键词: %s\n❌ 格式化结果时出错: %s", keyword, e.getMessage());
        }
    }

    private String formatSearchResults(BilibiliSearchResult searchResult, String keyword) {
        if (searchResult == null || searchResult.getResult() == null) {
            return "❌ 搜索结果为空";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🔍 搜索关键词: %s\n\n", keyword));

        List<BilibiliSearchResult.VideoSearchData> videos = new ArrayList<>();
        
        for (BilibiliSearchResult.SearchResultItem item : searchResult.getResult()) {
            if ("video".equals(item.getResultType()) && item.getData() != null) {
                videos.addAll(item.getData());
                break;
            }
        }

        if (videos.isEmpty()) {
            return String.format("🔍 搜索关键词: %s\n❌ 未找到相关视频", keyword);
        }

        int count = Math.min(videos.size(), 10);
        for (int i = 0; i < count; i++) {
            BilibiliSearchResult.VideoSearchData video = videos.get(i);
            sb.append(String.format("""
                    📺 %d. %s
                    👤 UP主: %s
                    🆔 BV号: %s
                    🔗 链接: https://www.bilibili.com/video/%s
                    📊 播放量: %s | 时长: %s
                    
                    """, 
                    i + 1,
                    video.getTitle(),
                    video.getAuthor(),
                    video.getBvid(),
                    video.getBvid(),
                    formatNumber(video.getPlay()),
                    video.getDuration()));
        }

        sb.append(String.format("共找到 %d 个结果，显示前 %d 个", videos.size(), count));
        return sb.toString();
    }

    private String formatVideoInfoFromMap(Map<String, Object> dataMap) {
        try {
            String title = (String) dataMap.get("title");
            String bvid = (String) dataMap.get("bvid");
            String desc = (String) dataMap.get("desc");
            Object durationObj = dataMap.get("duration");
            Integer duration = durationObj != null ? ((Number) durationObj).intValue() : 0;
            Object pubtimeObj = dataMap.get("pubdate");
            Long pubtime = pubtimeObj != null ? ((Number) pubtimeObj).longValue() : 0L;

            // 获取owner信息
            Map<String, Object> ownerMap = (Map<String, Object>) dataMap.get("owner");
            String ownerName = "未知";
            String ownerMid = "未知";
            if (ownerMap != null) {
                ownerName = (String) ownerMap.get("name");
                Object midObj = ownerMap.get("mid");
                ownerMid = midObj != null ? midObj.toString() : "未知";
            }

            // 获取stat信息
            Map<String, Object> statMap = (Map<String, Object>) dataMap.get("stat");
            long view = 0, like = 0, coin = 0, favorite = 0, share = 0, reply = 0;
            if (statMap != null) {
                view = getNumberValue(statMap.get("view"));
                like = getNumberValue(statMap.get("like"));
                coin = getNumberValue(statMap.get("coin"));
                favorite = getNumberValue(statMap.get("favorite"));
                share = getNumberValue(statMap.get("share"));
                reply = getNumberValue(statMap.get("reply"));
            }

            return String.format("""
                    🎬 视频信息
                    
                    📺 标题: %s
                    🆔 BV号: %s
                    👤 UP主: %s (ID: %s)
                    ⏱️ 时长: %s
                    📅 发布时间: %s
                    📊 数据统计:
                      - 播放量: %s
                      - 点赞: %s
                      - 投币: %s
                      - 收藏: %s
                      - 分享: %s
                      - 评论: %s
                    
                    📝 简介: %s
                    🔗 链接: https://www.bilibili.com/video/%s
                    """,
                    title != null ? title : "未知",
                    bvid != null ? bvid : "未知",
                    ownerName,
                    ownerMid,
                    formatDuration(duration),
                    formatTimestamp(pubtime),
                    formatNumber(view),
                    formatNumber(like),
                    formatNumber(coin),
                    formatNumber(favorite),
                    formatNumber(share),
                    formatNumber(reply),
                    StrUtil.brief(desc != null ? desc : "无简介", 200),
                    bvid != null ? bvid : "unknown");
        } catch (Exception e) {
            log.error("格式化视频信息时出错", e);
            return String.format("❌ 格式化视频信息时出错: %s", e.getMessage());
        }
    }

    private long getNumberValue(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String formatVideoInfo(BilibiliVideoInfo videoInfo) {
        if (videoInfo == null) {
            return "❌ 视频信息为空";
        }

        return String.format("""
                🎬 视频信息
                
                📺 标题: %s
                🆔 BV号: %s
                👤 UP主: %s (ID: %s)
                ⏱️ 时长: %s
                📅 发布时间: %s
                📊 数据统计:
                  - 播放量: %s
                  - 点赞: %s
                  - 投币: %s
                  - 收藏: %s
                  - 分享: %s
                  - 评论: %s
                
                📝 简介: %s
                🔗 链接: https://www.bilibili.com/video/%s
                """,
                videoInfo.getTitle(),
                videoInfo.getBvid(),
                videoInfo.getOwner() != null ? videoInfo.getOwner().getName() : "未知",
                videoInfo.getOwner() != null ? videoInfo.getOwner().getMid() : "未知",
                formatDuration(videoInfo.getDuration()),
                formatTimestamp(videoInfo.getPublishTime()),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getView() : 0L),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getLike() : 0L),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getCoin() : 0L),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getFavorite() : 0L),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getShare() : 0L),
                formatNumber(videoInfo.getStat() != null ? videoInfo.getStat().getReply() : 0L),
                StrUtil.brief(videoInfo.getDescription(), 200),
                videoInfo.getBvid());
    }

    private String formatTaskStatus(DownloadTask task) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return String.format("""
                📋 任务ID: %s
                🎬 BV号: %s
                📺 标题: %s
                📊 状态: %s (%s)
                📈 进度: %d%%
                📅 创建时间: %s
                📅 完成时间: %s
                📁 文件路径: %s
                📦 文件大小: %s
                ❌ 错误信息: %s
                """,
                task.getTaskId(),
                task.getBvid(),
                task.getTitle() != null ? task.getTitle() : "未知",
                task.getStatus().getDescription(),
                task.getStatus().name(),
                task.getProgress() != null ? task.getProgress() : 0,
                task.getCreateTime().format(formatter),
                task.getFinishTime() != null ? task.getFinishTime().format(formatter) : "未完成",
                task.getFilePath() != null ? task.getFilePath() : "未设置",
                task.getFileSize() != null ? formatFileSize(task.getFileSize()) : "未知",
                task.getErrorMessage() != null ? task.getErrorMessage() : "无");
    }

    private String formatNumber(Long number) {
        if (number == null || number == 0) return "0";
        if (number >= 100000000) return String.format("%.1f亿", number / 100000000.0);
        else if (number >= 10000) return String.format("%.1f万", number / 10000.0);
        else return number.toString();
    }

    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) return "未知";
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    private String formatTimestamp(Long timestamp) {
        if (timestamp == null || timestamp <= 0) return "未知";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.ofEpochSecond(timestamp, 0, 
                java.time.ZoneOffset.ofHours(8)).format(formatter);
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
} 