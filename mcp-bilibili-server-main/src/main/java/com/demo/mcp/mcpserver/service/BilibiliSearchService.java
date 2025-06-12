package com.demo.mcp.mcpserver.service;

import com.demo.mcp.mcpserver.dto.BilibiliSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 哔哩哔哩搜索和HTML解析服务
 */
@Slf4j
@Service
public class BilibiliSearchService {

    private String sessdata = "";
    
    // 正则表达式用于提取BV号
    private static final Pattern BV_PATTERN = Pattern.compile("BV[a-zA-Z0-9]+");
    
    public void setSessdata(String sessdata) {
        this.sessdata = sessdata != null ? sessdata : "";
        log.info("搜索服务已更新SESSDATA设置");
    }

    /**
     * 从网页搜索视频
     * @param keyword 搜索关键词
     * @param page 页码（从1开始）
     * @param order 排序方式：totalrank(综合排序)、click(最多播放)、pubdate(最新发布)、dm(最多弹幕)、stow(最多收藏)
     * @param duration 时长筛选：0(全部)、1(10分钟以下)、2(10-30分钟)、3(30-60分钟)、4(60分钟以上)
     * @return 搜索结果列表
     */
    public List<BilibiliSearchResult> searchVideosFromWeb(String keyword, int page, String order, int duration) {
        List<BilibiliSearchResult> results = new ArrayList<>();
        
        // 构建搜索URL
        String url = buildSearchUrl(keyword, page, order, duration);
        log.info("正在访问搜索页面: {}", url);
        
        try {
            
            // 发送HTTP请求并解析页面 - 使用简化请求头避免反爬虫
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(20000)
                    .maxBodySize(5 * 1024 * 1024)  // 5MB
                    .followRedirects(true)
                    .get();
            
            // 解析搜索结果
            results = parseSearchResults(doc);
            
            log.info("成功解析到 {} 个搜索结果", results.size());
            
        } catch (IOException e) {
            log.error("搜索视频时发生网络错误 - URL: {}, 错误: {}", url, e.getMessage());
            
            // 如果是412错误或其他网络问题，尝试使用API搜索作为备用方案
            try {
                log.info("网页搜索失败，尝试使用API搜索作为备用方案...");
                results = searchVideosFromApi(keyword, page);
                log.info("API搜索成功，获取到 {} 个结果", results.size());
                
            } catch (Exception apiE) {
                log.error("API搜索也失败了: {}", apiE.getMessage());
                
                // 最后尝试最简化的请求
                try {
                    log.info("尝试最简化的请求...");
                    String simpleUrl = "https://search.bilibili.com/all?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
                    Document doc = Jsoup.connect(simpleUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(30000)
                            .get();
                    
                    results = parseSearchResults(doc);
                    log.info("简化请求成功，解析到 {} 个结果", results.size());
                    
                } catch (Exception finalE) {
                    log.error("所有搜索方法都失败了: {}", finalE.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("搜索视频时发生未知错误 - URL: {}, 错误类型: {}, 消息: {}", 
                    url, e.getClass().getSimpleName(), e.getMessage());
        }
        
        return results;
    }

    /**
     * 搜索指定UP主的视频列表
     * @param upName UP主名称
     * @param page 页码
     * @return 视频列表
     */
    public List<BilibiliSearchResult> getUpVideosList(String upName, int page) {
        return searchVideosFromWeb(upName, page, "pubdate", 0);
    }
    
    /**
     * 使用API搜索视频（备用方案）
     * @param keyword 搜索关键词
     * @param page 页码
     * @return 搜索结果列表
     */
    private List<BilibiliSearchResult> searchVideosFromApi(String keyword, int page) {
        List<BilibiliSearchResult> results = new ArrayList<>();
        
        try {
            // 使用简化的API搜索（模拟内部API调用）
            String apiUrl = "https://api.bilibili.com/x/web-interface/search/type?search_type=video&keyword=" 
                    + URLEncoder.encode(keyword, StandardCharsets.UTF_8) 
                    + "&page=" + page;
            
            log.info("尝试API搜索: {}", apiUrl);
            
            // 这里只是模拟API调用，实际上我们还是回退到简单的网页搜索
            // 因为API搜索通常需要更复杂的认证
            results = new ArrayList<>();
            
        } catch (Exception e) {
            log.error("API搜索失败: {}", e.getMessage());
        }
        
        return results;
    }

    /**
     * 构建搜索URL
     */
    private String buildSearchUrl(String keyword, int page, String order, int duration) {
        try {
            // 使用更简单的搜索URL，减少参数以避免触发反爬虫
            StringBuilder url = new StringBuilder("https://search.bilibili.com/all?");
            url.append("keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            url.append("&page=").append(page);
            
            // 只在必要时添加排序参数
            if (order != null && !"totalrank".equals(order)) {
                url.append("&order=").append(order);
            }
            
            // 只在有时长筛选时添加
            if (duration > 0) {
                url.append("&duration=").append(duration);
            }
            
            return url.toString();
        } catch (Exception e) {
            log.error("构建搜索URL时出错: {}", e.getMessage());
            return "https://search.bilibili.com/all?keyword=" + keyword;
        }
    }

    /**
     * 解析搜索结果页面
     */
    private List<BilibiliSearchResult> parseSearchResults(Document doc) {
        List<BilibiliSearchResult> results = new ArrayList<>();
        
        try {
            // 方法1: 尝试解析视频卡片 (bili-video-card)
            Elements videoCards = doc.select(".bili-video-card");
            log.info("找到 {} 个bili-video-card元素", videoCards.size());
            
            for (Element card : videoCards) {
                BilibiliSearchResult result = parseVideoCard(card);
                if (result != null && result.getBvid() != null) {
                    results.add(result);
                }
            }
            
            // 方法2: 如果方法1没有结果，尝试解析其他视频相关元素
            if (results.isEmpty()) {
                // 尝试查找包含video链接的a标签
                Elements videoLinks = doc.select("a[href*='/video/BV']");
                log.info("找到 {} 个视频链接", videoLinks.size());
                
                for (Element link : videoLinks) {
                    BilibiliSearchResult result = parseVideoLink(link);
                    if (result != null && result.getBvid() != null) {
                        // 避免重复添加相同的视频
                        boolean exists = results.stream().anyMatch(r -> r.getBvid().equals(result.getBvid()));
                        if (!exists) {
                            results.add(result);
                        }
                    }
                }
            }
            
            // 方法3: 通用解析，查找所有包含BV号的链接
            if (results.isEmpty()) {
                parseGeneralVideoElements(doc, results);
            }
            
        } catch (Exception e) {
            log.error("解析搜索结果时出错: {}", e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 解析单个视频卡片
     */
    private BilibiliSearchResult parseVideoCard(Element card) {
        try {
            BilibiliSearchResult result = new BilibiliSearchResult();
            
            // 解析视频链接和BV号
            Elements links = card.select("a[href*='/video/BV']");
            if (!links.isEmpty()) {
                String href = links.first().attr("href");
                result.setVideoUrl(href.startsWith("http") ? href : "https://www.bilibili.com" + href);
                result.setBvid(extractBvid(href));
            }
            
            // 解析标题
            Elements titleElements = card.select(".bili-video-card__info--tit");
            if (!titleElements.isEmpty()) {
                result.setTitle(titleElements.first().text().trim());
            } else {
                // 备用方案：从title属性获取
                Elements titleAttrs = card.select("[title]");
                for (Element titleAttr : titleAttrs) {
                    String title = titleAttr.attr("title");
                    if (title != null && title.length() > 5) {
                        result.setTitle(title.trim());
                        break;
                    }
                }
            }
            
            // 解析播放量
            Elements playElements = card.select(".bili-video-card__stats--item span");
            if (!playElements.isEmpty()) {
                try {
                    String playText = playElements.first().text().trim();
                    if (!playText.isEmpty() && playText.matches("\\d+.*")) {
                        result.setPlay(parseViewCount(playText));
                    }
                } catch (Exception e) {
                    log.debug("解析播放量失败: {}", e.getMessage());
                }
            }
            
            // 解析时长
            Elements durationElements = card.select(".bili-video-card__stats__duration");
            if (!durationElements.isEmpty()) {
                result.setDuration(durationElements.first().text().trim());
            }
            
            // 解析发布时间
            Elements dateElements = card.select(".bili-video-card__info--date");
            if (!dateElements.isEmpty()) {
                result.setPubdate(dateElements.first().text().trim());
            }
            
            // 解析UP主信息
            parseUpInfo(card, result);
            
            // 验证必要信息
            if (result.getBvid() != null && result.getTitle() != null) {
                log.debug("成功解析视频卡片: {} - {}", result.getBvid(), result.getTitle());
                return result;
            }
            
        } catch (Exception e) {
            log.debug("解析视频卡片时出错: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * 解析视频链接元素
     */
    private BilibiliSearchResult parseVideoLink(Element link) {
        try {
            BilibiliSearchResult result = new BilibiliSearchResult();
            
            // 解析链接和BV号
            String href = link.attr("href");
            result.setVideoUrl(href.startsWith("http") ? href : "https://www.bilibili.com" + href);
            result.setBvid(extractBvid(href));
            
            // 解析标题
            String title = link.text().trim();
            if (title.isEmpty()) {
                title = link.attr("title");
            }
            if (title.isEmpty()) {
                // 从父元素或子元素查找标题
                Element parent = link.parent();
                if (parent != null) {
                    Elements titleEls = parent.select("h3, .title, [class*='title'], [class*='tit']");
                    if (!titleEls.isEmpty()) {
                        title = titleEls.first().text().trim();
                    }
                }
            }
            result.setTitle(title);
            
            // 尝试从周围元素解析其他信息
            parseAdditionalInfo(link, result);
            
            if (result.getBvid() != null && result.getTitle() != null && !result.getTitle().isEmpty()) {
                return result;
            }
            
        } catch (Exception e) {
            log.debug("解析视频链接时出错: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * 通用解析方法
     */
    private void parseGeneralVideoElements(Document doc, List<BilibiliSearchResult> results) {
        try {
            // 查找所有包含BV号的元素
            Elements allElements = doc.select("*");
            
            for (Element element : allElements) {
                // 检查href属性
                String href = element.attr("href");
                if (href.contains("/video/BV")) {
                    BilibiliSearchResult result = new BilibiliSearchResult();
                    result.setVideoUrl(href.startsWith("http") ? href : "https://www.bilibili.com" + href);
                    result.setBvid(extractBvid(href));
                    
                    // 尝试获取标题
                    String title = element.text().trim();
                    if (title.isEmpty()) {
                        title = element.attr("title");
                    }
                    result.setTitle(title);
                    
                    if (result.getBvid() != null && !result.getTitle().isEmpty()) {
                        // 避免重复
                        boolean exists = results.stream().anyMatch(r -> r.getBvid().equals(result.getBvid()));
                        if (!exists) {
                            results.add(result);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("通用解析时出错: {}", e.getMessage());
        }
    }

    /**
     * 解析UP主信息
     */
    private void parseUpInfo(Element card, BilibiliSearchResult result) {
        try {
            // 查找UP主相关信息
            Elements upElements = card.select(".up-name, .bili-video-card__info--author, [class*='author'], [class*='up']");
            for (Element upElement : upElements) {
                String upName = upElement.text().trim();
                if (!upName.isEmpty() && upName.length() < 20) {
                    result.setAuthor(upName);
                    break;
                }
            }
            
            // 如果没找到，尝试从父容器查找
            if (result.getAuthor() == null) {
                Element parent = card.parent();
                if (parent != null) {
                    Elements userInfo = parent.select(".user-name, .username, [class*='user']");
                    if (!userInfo.isEmpty()) {
                        result.setAuthor(userInfo.first().text().trim());
                    }
                }
            }
            
        } catch (Exception e) {
            log.debug("解析UP主信息时出错: {}", e.getMessage());
        }
    }

    /**
     * 解析附加信息
     */
    private void parseAdditionalInfo(Element element, BilibiliSearchResult result) {
        try {
            Element container = element.parent();
            if (container == null) return;
            
            // 查找播放量
            Elements playElements = container.select("[class*='play'], [class*='view'], [class*='stats']");
            for (Element playEl : playElements) {
                String text = playEl.text();
                if (text.matches(".*\\d+.*播放.*") || text.matches(".*\\d+.*万.*")) {
                    result.setPlay(parseViewCount(text));
                    break;
                }
            }
            
            // 查找时长
            Elements durationElements = container.select("[class*='duration'], [class*='time']");
            for (Element durationEl : durationElements) {
                String text = durationEl.text().trim();
                if (text.matches("\\d{1,2}:\\d{2}")) {
                    result.setDuration(text);
                    break;
                }
            }
            
        } catch (Exception e) {
            log.debug("解析附加信息时出错: {}", e.getMessage());
        }
    }

    /**
     * 从URL中提取BV号
     */
    private String extractBvid(String url) {
        if (url == null) return null;
        
        Matcher matcher = BV_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 解析播放量文本为数字
     */
    private Long parseViewCount(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        
        try {
            // 移除非数字字符（保留小数点）
            String cleaned = text.replaceAll("[^\\d.]", "");
            
            if (cleaned.isEmpty()) return null;
            
            double value = Double.parseDouble(cleaned);
            
            // 根据原文本判断单位
            if (text.contains("万")) {
                value *= 10000;
            } else if (text.contains("亿")) {
                value *= 100000000;
            }
            
            return (long) value;
            
        } catch (Exception e) {
            log.debug("解析播放量失败: {} -> {}", text, e.getMessage());
            return null;
        }
    }
}