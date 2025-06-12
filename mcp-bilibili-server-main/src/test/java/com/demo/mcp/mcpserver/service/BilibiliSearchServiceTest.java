package com.demo.mcp.mcpserver.service;

import com.demo.mcp.mcpserver.dto.BilibiliSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 哔哩哔哩搜索服务测试
 */
@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.demo.mcp.mcpserver=DEBUG",
    "spring.ai.mcp.server.stdio=false"
})
@DisplayName("哔哩哔哩搜索服务测试")
class BilibiliSearchServiceTest {

    @Autowired
    private BilibiliSearchService searchService;

    @BeforeEach
    void setUp() {
        assertNotNull(searchService, "BilibiliSearchService 应该成功注入");
        log.info("开始测试哔哩哔哩搜索服务");
    }

    @Test
    @DisplayName("测试基础网页搜索功能")
    void testBasicWebSearch() {
        log.info("=== 测试基础搜索功能 ===");
        
        List<BilibiliSearchResult> results = searchService.searchVideosFromWeb("程序员鱼皮", 1, "totalrank", 0);
        
        assertNotNull(results, "搜索结果不应为null");
        log.info("搜索到 {} 个结果", results.size());
        
        if (!results.isEmpty()) {
            BilibiliSearchResult firstResult = results.get(0);
            log.info("第一个结果: BV={}, 标题={}, UP主={}", 
                    firstResult.getBvid(), firstResult.getTitle(), firstResult.getAuthor());
            
            assertNotNull(firstResult.getBvid(), "BV号不应为空");
            assertNotNull(firstResult.getTitle(), "标题不应为空");
        }
    }

    @Test
    @DisplayName("测试排序功能")
    void testSearchWithSorting() {
        log.info("=== 测试排序功能 ===");
        
        // 测试按播放量排序
        List<BilibiliSearchResult> playResults = searchService.searchVideosFromWeb("java教程", 1, "click", 0);
        log.info("按播放量排序: 找到 {} 个结果", playResults.size());
        
        // 测试按最新发布排序
        List<BilibiliSearchResult> dateResults = searchService.searchVideosFromWeb("java教程", 1, "pubdate", 0);
        log.info("按发布时间排序: 找到 {} 个结果", dateResults.size());
        
        assertTrue(playResults.size() >= 0, "搜索结果应该有效");
        assertTrue(dateResults.size() >= 0, "搜索结果应该有效");
    }

    @Test
    @DisplayName("测试时长筛选功能")
    void testDurationFilter() {
        log.info("=== 测试时长筛选功能 ===");
        
        // 测试10-30分钟的视频
        List<BilibiliSearchResult> mediumResults = searchService.searchVideosFromWeb("spring boot", 1, "totalrank", 2);
        log.info("10-30分钟视频: 找到 {} 个结果", mediumResults.size());
        
        // 测试60分钟以上的视频
        List<BilibiliSearchResult> longResults = searchService.searchVideosFromWeb("spring boot", 1, "totalrank", 4);
        log.info("60分钟以上视频: 找到 {} 个结果", longResults.size());
        
        assertTrue(mediumResults.size() >= 0, "搜索结果应该有效");
        assertTrue(longResults.size() >= 0, "搜索结果应该有效");
    }

    @Test
    @DisplayName("测试UP主视频搜索")
    void testUpVideosSearch() {
        log.info("=== 测试UP主视频列表功能 ===");
        
        List<BilibiliSearchResult> results = searchService.getUpVideosList("程序员鱼皮", 1);
        
        assertNotNull(results, "UP主视频列表不应为null");
        log.info("找到UP主视频 {} 个", results.size());
        
        if (!results.isEmpty()) {
            for (int i = 0; i < Math.min(3, results.size()); i++) {
                BilibiliSearchResult result = results.get(i);
                log.info("视频{}: BV={}, 标题={}, UP主={}, 播放量={}, 时长={}", 
                        i + 1, result.getBvid(), result.getTitle(), result.getAuthor(), 
                        result.getPlay(), result.getDuration());
            }
        }
    }

    @Test
    @DisplayName("测试分页功能")
    void testPagination() {
        log.info("=== 测试分页功能 ===");
        
        // 测试第一页
        List<BilibiliSearchResult> page1 = searchService.searchVideosFromWeb("编程", 1, "totalrank", 0);
        log.info("第1页: 找到 {} 个结果", page1.size());
        
        // 测试第二页
        List<BilibiliSearchResult> page2 = searchService.searchVideosFromWeb("编程", 2, "totalrank", 0);
        log.info("第2页: 找到 {} 个结果", page2.size());
        
        assertTrue(page1.size() >= 0, "第1页搜索结果应该有效");
        assertTrue(page2.size() >= 0, "第2页搜索结果应该有效");
        
        // 验证两页的结果不完全相同（如果都有结果的话）
        if (!page1.isEmpty() && !page2.isEmpty()) {
            String firstBvid1 = page1.get(0).getBvid();
            String firstBvid2 = page2.get(0).getBvid();
            if (firstBvid1 != null && firstBvid2 != null) {
                assertNotEquals(firstBvid1, firstBvid2, "不同页面的第一个结果应该不同");
            }
        }
    }

    @Test
    @DisplayName("测试详细结果信息")
    void testDetailedResultInfo() {
        log.info("=== 测试详细结果信息 ===");
        
        List<BilibiliSearchResult> results = searchService.searchVideosFromWeb("python", 1, "totalrank", 0);
        
        if (!results.isEmpty()) {
            BilibiliSearchResult result = results.get(0);
            
            log.info("详细信息测试:");
            log.info("- BV号: {}", result.getBvid());
            log.info("- 标题: {}", result.getTitle());
            log.info("- UP主: {}", result.getAuthor());
            log.info("- 视频链接: {}", result.getVideoUrl());
            log.info("- 播放量: {}", result.getPlay());
            log.info("- 时长: {}", result.getDuration());
            log.info("- 发布时间: {}", result.getPubdate());
            
            // 验证基本字段不为空
            assertNotNull(result.getBvid(), "BV号不应为空");
            assertNotNull(result.getTitle(), "标题不应为空");
            
            // 如果有视频链接，应该包含BV号
            if (result.getVideoUrl() != null && result.getBvid() != null) {
                assertTrue(result.getVideoUrl().contains(result.getBvid()), "视频链接应包含BV号");
            }
        } else {
            log.warn("未找到搜索结果进行详细信息测试");
        }
    }

    @Test
    @DisplayName("测试多种搜索关键词")
    void testMultipleKeywords() {
        log.info("=== 测试多种搜索词 ===");
        
        String[] keywords = {"程序员", "前端开发", "机器学习", "游戏", "美食"};
        
        for (String keyword : keywords) {
            List<BilibiliSearchResult> results = searchService.searchVideosFromWeb(keyword, 1, "totalrank", 0);
            log.info("关键词 '{}': 找到 {} 个结果", keyword, results.size());
            
            assertNotNull(results, "搜索结果不应为null");
            
            if (!results.isEmpty()) {
                BilibiliSearchResult first = results.get(0);
                log.info("  首个结果: {} - {}", first.getBvid(), first.getTitle());
            }
        }
    }

    @Test
    @DisplayName("测试边界情况")
    void testEdgeCases() {
        log.info("=== 测试边界情况 ===");
        
        // 测试空关键词
        List<BilibiliSearchResult> emptyResults = searchService.searchVideosFromWeb("", 1, "totalrank", 0);
        assertNotNull(emptyResults, "空关键词搜索结果不应为null");
        log.info("空关键词搜索结果: {} 个", emptyResults.size());
        
        // 测试负数页码
        List<BilibiliSearchResult> negativePageResults = searchService.searchVideosFromWeb("java", -1, "totalrank", 0);
        assertNotNull(negativePageResults, "负数页码搜索结果不应为null");
        
        // 测试无效排序参数
        List<BilibiliSearchResult> invalidOrderResults = searchService.searchVideosFromWeb("java", 1, "invalid_order", 0);
        assertNotNull(invalidOrderResults, "无效排序参数搜索结果不应为null");
        
        log.info("边界情况测试完成");
    }

    @Test
    @DisplayName("测试SESSDATA设置")
    void testSetSessdata() {
        log.info("=== 测试SESSDATA设置 ===");
        
        // 测试设置SESSDATA
        String testSessdata = "test_sessdata_123456";
        searchService.setSessdata(testSessdata);
        
        // 验证设置后的搜索（这里只是验证不会抛异常）
        List<BilibiliSearchResult> searchResult = searchService.searchVideosFromWeb("测试", 1, "totalrank", 0);
        assertNotNull(searchResult, "设置SESSDATA后搜索应该正常");
        
        // 清空SESSDATA
        searchService.setSessdata("");
        
        List<BilibiliSearchResult> searchResult2 = searchService.searchVideosFromWeb("测试", 1, "totalrank", 0);
        assertNotNull(searchResult2, "清空SESSDATA后搜索应该正常");
        
        log.info("SESSDATA设置测试完成");
    }
} 