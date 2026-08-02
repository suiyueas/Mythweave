package com.novelcraft.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcraft.web.client.DeepSeekClient;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InspirationService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    /**
     * AI 生成灵感
     * @param keywords 关键词
     * @return 生成的灵感列表
     */
    public List<InspirationItem> aiGenerate(String keywords) {
        try {
            String systemPrompt = "你是一位创意写作助手。根据用户提供的关键词，生成3条小说创作灵感，分别是对白灵感、场景描写和细节设定。" +
                    "以JSON数组格式返回，每条包含type和content字段。" +
                    "type的可选值：对白灵感、场景描写、细节设定。" +
                    "要求：内容新颖具体，具有画面感和文学性，每条30-100字。返回格式示例：" +
                    "[{\"type\":\"对白灵感\",\"content\":\"...\"},{\"type\":\"场景描写\",\"content\":\"...\"},{\"type\":\"细节设定\",\"content\":\"...\"}]";

            String userMessage = "关键词：" + keywords;
            String result = deepSeekClient.chat(systemPrompt, userMessage, 0.8, 1024);
            log.info("AI生成灵感原始返回: {}", result);

            // 尝试解析JSON
            String cleanJson = result.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.substring(3);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            cleanJson = cleanJson.trim();

            JsonNode root = objectMapper.readTree(cleanJson);
            if (root.isArray() && root.size() > 0) {
                List<InspirationItem> items = new ArrayList<>();
                for (JsonNode node : root) {
                    InspirationItem item = new InspirationItem();
                    item.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    item.setType(node.path("type").asText("detail"));
                    item.setContent(node.path("content").asText(""));
                    item.setSource("ai");
                    items.add(item);
                }
                if (!items.isEmpty()) {
                    return items;
                }
            }
        } catch (Exception e) {
            log.warn("AI生成灵感失败，降级使用模板数据: {}", e.getMessage());
        }

        // 降级方案：模板预设数据
        return generateFallback(keywords);
    }

    /**
     * 模板降级数据
     */
    private List<InspirationItem> generateFallback(String keywords) {
        String[] kw = keywords.split("\\s+");
        String kw0 = kw.length > 0 ? kw[0] : "故事";
        String kw1 = kw.length > 1 ? kw[1] : "冒险";
        String kw2 = kw.length > 2 ? kw[2] : "命运";

        String[][] templates = {
                {"对白灵感", String.format("「你以为%s只是%s？不，它背后隐藏着比%s更深的秘密。」他低声说道，眼中闪烁着危险的光芒。", kw0, kw1, kw2)},
                {"场景描写", String.format("%s的余晖洒在%s的废墟上，空气中弥漫着%s的气息。远处传来钟声，沉重而悠远，仿佛在宣告什么。", kw0, kw1, kw2)},
                {"细节设定", String.format("他的指尖划过%s的纹理，那上面刻着古老的符文——每一个符号都在诉说着与%s有关的%s预言。", kw0, kw1, kw2)}
        };

        // 随机偏移
        int offset = (int)(Math.random() * 3);
        String[][] selected = new String[3][2];
        for (int i = 0; i < 3; i++) {
            selected[i] = templates[(i + offset) % 3];
        }

        List<InspirationItem> items = new ArrayList<>();
        for (String[] t : selected) {
            InspirationItem item = new InspirationItem();
            item.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            item.setType(t[0]);
            item.setContent(t[1]);
            item.setSource("ai");
            items.add(item);
        }
        return items;
    }

    @Data
    public static class InspirationItem {
        private String id;
        private String type;
        private String content;
        private String source;
    }
}