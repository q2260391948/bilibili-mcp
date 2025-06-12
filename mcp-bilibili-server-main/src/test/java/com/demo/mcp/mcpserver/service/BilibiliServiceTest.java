package com.demo.mcp.mcpserver.service;

import com.demo.mcp.mcpserver.config.BilibiliConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 哔哩哔哩服务单元测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.demo.mcp.mcpserver=DEBUG",
    "spring.ai.mcp.server.stdio=false"
})
@DisplayName("哔哩哔哩服务测试")
class BilibiliServiceTest {

    @Autowired
    private BilibiliService bilibiliService;

    @Autowired
    private BilibiliConfig bilibiliConfig;

    @BeforeEach
    void setUp() {
        // 确保每个测试开始前都是干净的状态
        assertNotNull(bilibiliService, "BilibiliService 应该成功注入");
        assertNotNull(bilibiliConfig, "BilibiliConfig 应该成功注入");
    }

    @Test
    @DisplayName("测试服务器状态功能")
    void testGetServerStatus() {
        // 测试获取服务器状态
        String result = bilibiliService.getServerStatus();
        
        assertNotNull(result, "服务器状态不应该为空");
        assertTrue(result.contains("✅"), "结果应该包含成功标识");
        assertTrue(result.contains("Java"), "结果应该包含Java信息");
        assertTrue(result.contains("内存"), "结果应该包含内存信息");
        
        System.out.println("服务器状态测试结果:");
        System.out.println(result);
    }

    @Test
    @DisplayName("测试 SESSDATA 设置功能")
    void testSetSessdata() {
        // 测试设置真实的 SESSDATA
        String realSessdataResult = bilibiliService.setSessdata("7a93a2bd%2C1764982481%2Cb6385%2A61CjArtVKTwbpCX2ezQBhpO-sJ0ERebnCs6cblaglvIqt8bBmvrU-hSegYW87RFTMK2aQSVjFPU2h6anpZMS1PaThiQ3B6M0FaZmhkXzJFY0Z6dVlQZDZ1SDgwVUI5TUphWWxIYm5DZDc3aDJ5T0xoMmpNSGdwUGFIbUdlXzI4dXpXNy1vb0t5WnB3IIEC");
        assertNotNull(realSessdataResult, "真实SESSDATA结果不应该为空");
        assertTrue(realSessdataResult.contains("✅") || realSessdataResult.contains("⚠️"), "应该成功设置或给出警告");
        
        // 测试设置空的 SESSDATA
        String emptyResult = bilibiliService.setSessdata("");
        assertNotNull(emptyResult, "空SESSDATA结果不应该为空");
        assertTrue(emptyResult.contains("✅"), "应该成功清空SESSDATA");
        assertTrue(emptyResult.contains("访客"), "应该提示访客模式");
        
        System.out.println("清空SESSDATA测试结果:");
        System.out.println(emptyResult);

        // 测试设置无效的 SESSDATA
        String invalidResult = bilibiliService.setSessdata("invalid_sessdata_test");
        assertNotNull(invalidResult, "无效SESSDATA结果不应该为空");
        assertTrue(invalidResult.contains("⚠️") || invalidResult.contains("❌"), 
                  "应该提示SESSDATA无效或保存");
        
        System.out.println("设置无效SESSDATA测试结果:");
        System.out.println(invalidResult);
    }

    @Test
    @DisplayName("测试 BV 号提取功能")
    void testBvidExtraction() {
        // 通过下载任务测试 BV 号提取（间接测试私有方法）
        
        // 测试完整的哔哩哔哩链接
        String fullUrlResult = bilibiliService.createDownloadTask("https://www.bilibili.com/video/BV1q6TxziECu/");
        assertNotNull(fullUrlResult, "完整链接处理结果不应该为空");
        
        // 测试直接的 BV 号
        String bvidResult = bilibiliService.createDownloadTask("BV1234567890");
        assertNotNull(bvidResult, "BV号处理结果不应该为空");
        
        // 测试无效的输入
        String invalidResult = bilibiliService.createDownloadTask("invalid_input");
        assertNotNull(invalidResult, "无效输入结果不应该为空");
        assertTrue(invalidResult.contains("❌"), "应该提示无效输入");
        
        System.out.println("BV号提取测试结果:");
        System.out.println("完整链接: " + fullUrlResult);
        System.out.println("直接BV号: " + bvidResult);
        System.out.println("无效输入: " + invalidResult);
    }


//    @Test
//    @DisplayName("测试获取视频信息功能")
//    void testGetVideoInfo() {
//        // 测试无效的视频 ID
//        String invalidResult = bilibiliService.getVideoInfo("invalid_video_id");
//        assertNotNull(invalidResult, "无效视频ID结果不应该为空");
//        assertTrue(invalidResult.contains("❌"), "应该提示无效的视频链接或BV号");
//
//        // 测试有效的 BV 号格式（但可能不存在）
//        String validFormatResult = bilibiliService.getVideoInfo("BV1234567890");
//        assertNotNull(validFormatResult, "有效格式结果不应该为空");
//
//        System.out.println("获取视频信息测试结果:");
//        System.out.println("无效ID: " + invalidResult);
//        System.out.println("有效格式: " + validFormatResult);
//    }

    @Test
    @DisplayName("测试下载任务管理功能")
    void testDownloadTaskManagement() {
        // 测试创建下载任务
        String createResult = bilibiliService.createDownloadTask("BV1234567890");
        assertNotNull(createResult, "创建任务结果不应该为空");
        
        if (createResult.contains("✅")) {
            // 如果任务创建成功，测试查询状态
            String statusResult = bilibiliService.getDownloadStatus(null);
            assertNotNull(statusResult, "查询状态结果不应该为空");
            
            System.out.println("下载任务管理测试结果:");
            System.out.println("创建任务: " + createResult);
            System.out.println("查询状态: " + statusResult);
        } else {
            System.out.println("下载任务创建失败（可能是网络问题）: " + createResult);
        }
        
        // 测试重复创建任务
        String duplicateResult = bilibiliService.createDownloadTask("BV1234567890");
        assertNotNull(duplicateResult, "重复创建任务结果不应该为空");
        
        System.out.println("重复创建任务: " + duplicateResult);
    }

    @Test
    @DisplayName("测试配置加载")
    void testConfigurationLoading() {
        // 验证配置是否正确加载
        assertNotNull(bilibiliConfig.getApi(), "API配置不应该为空");
        assertNotNull(bilibiliConfig.getApi().getSearch(), "搜索API不应该为空");
        assertNotNull(bilibiliConfig.getApi().getVideoInfo(), "视频信息API不应该为空");
        assertNotNull(bilibiliConfig.getApi().getUserInfo(), "用户信息API不应该为空");
        
        assertNotNull(bilibiliConfig.getDownload(), "下载配置不应该为空");
        assertNotNull(bilibiliConfig.getDownload().getBaseDir(), "下载目录不应该为空");
        assertTrue(bilibiliConfig.getDownload().getTimeout() > 0, "超时时间应该大于0");
        
        assertNotNull(bilibiliConfig.getHeaders(), "请求头配置不应该为空");
        assertNotNull(bilibiliConfig.getHeaders().getUserAgent(), "User-Agent不应该为空");
        assertNotNull(bilibiliConfig.getHeaders().getReferer(), "Referer不应该为空");
        
        System.out.println("配置加载测试通过:");
        System.out.println("搜索API: " + bilibiliConfig.getApi().getSearch());
        System.out.println("下载目录: " + bilibiliConfig.getDownload().getBaseDir());
        System.out.println("超时时间: " + bilibiliConfig.getDownload().getTimeout() + "ms");
    }

    @Test
    @DisplayName("综合功能测试")
    void testIntegratedWorkflow() {
        System.out.println("\n=== 综合功能测试开始 ===");
        
        // 1. 检查服务器状态
        String serverStatus = bilibiliService.getServerStatus();
        System.out.println("1. 服务器状态: " + (serverStatus.contains("✅") ? "正常" : "异常"));
        
        // 2. 设置访客模式
        String sessdataResult = bilibiliService.setSessdata("");
        System.out.println("2. 访客模式设置: " + (sessdataResult.contains("✅") ? "成功" : "失败"));
        
        // 3. 尝试搜索
        Object object = bilibiliService.searchVideos("编程教程");
        System.out.println(object.toString());
        
        // 4. 创建下载任务
        String taskResult = bilibiliService.createDownloadTask("BV1234567890");
        boolean taskSuccess = taskResult.contains("✅");
        System.out.println("4. 任务创建: " + (taskSuccess ? "成功" : "失败"));
        
        // 5. 查询任务状态
        String statusResult = bilibiliService.getDownloadStatus(null);
        System.out.println("5. 状态查询: " + (statusResult.contains("✅") ? "正常" : "无任务"));
        
        System.out.println("=== 综合功能测试完成 ===\n");
        
        // 至少基础功能应该正常
        assertTrue(serverStatus.contains("✅"), "服务器状态应该正常");
        assertTrue(sessdataResult.contains("✅"), "访客模式设置应该成功");
    }

    @Test
    void searchVideosFromWeb() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("keyword", "Java教程 尚硅谷");
        arguments.put("page", 1);
        arguments.put("pageSize", "");
//         arguments.put("duration", 0);
        Object object = bilibiliService.searchVideosFromWeb(arguments);
        System.out.println(object);

    }

    @Test
    void getVideo() {
        Object object = bilibiliService.searchVideos("Java教程 尚硅谷");
        System.out.println(object);

    }


}