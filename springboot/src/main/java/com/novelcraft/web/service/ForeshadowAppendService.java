package com.novelcraft.web.service;

import com.novelcraft.web.client.DeepSeekClient;
import com.novelcraft.web.config.AiProperties;
import com.novelcraft.web.dto.AppendForeshadowRequest;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelForeshadowing;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelForeshadowingMapper;
import com.novelcraft.web.template.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForeshadowAppendService {

    private final DeepSeekClient deepSeekClient;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelChapterMapper chapterMapper;
    private final AiProperties aiProperties;

    public Map<String, Object> appendForeshadowing(AppendForeshadowRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        String originalContent = request.getOriginalContent();
        String insertPosition = request.getInsertPosition();
        Integer cursorPosition = request.getCursorPosition();

        String insertedContent;
        int insertIndex;

        if ("auto".equals(insertPosition)) {
            String autoPositionPrompt = String.format("""
                请分析以下小说章节，找出最适合插入伏笔「%s：%s」的位置。

                要求：
                1. 选择情节自然过渡、有合理铺垫的位置
                2. 避免在高潮或紧张场景中插入
                3. 考虑伏笔与已有内容的关联性

                【伏笔】
                标题：%s
                描述：%s

                【章节内容】
                %s

                【输出格式】
                仅输出一个整数，表示插入位置的字符偏移量，不要任何解释。
                """,
                    request.getForeshadowingTitle(),
                    request.getForeshadowingDescription(),
                    request.getForeshadowingTitle(),
                    request.getForeshadowingDescription(),
                    originalContent
            );

            try {
                String positionStr = deepSeekClient.chat(
                        "你是一位专业小说编辑，擅长分析情节和伏笔铺垫。",
                        autoPositionPrompt,
                        0.3,
                        50
                );
                insertIndex = Math.max(0, Math.min(parsePosition(positionStr), originalContent.length()));
            } catch (Exception e) {
                log.warn("AI智能定位失败，使用末尾插入: {}", e.getMessage());
                insertIndex = originalContent.length();
            }
        } else if ("cursor".equals(insertPosition) && cursorPosition != null) {
            insertIndex = Math.max(0, Math.min(cursorPosition, originalContent.length()));
        } else {
            insertIndex = originalContent.length();
        }

        String appendPrompt = PromptTemplates.APPEND_FORESHADOWING
                .replace("{foreshadowingTitle}", request.getForeshadowingTitle())
                .replace("{foreshadowingDescription}", request.getForeshadowingDescription())
                .replace("{originalContent}", originalContent);

        try {
            insertedContent = deepSeekClient.chat(
                    "你是一位专业小说作家，擅长在情节中自然地融入伏笔。",
                    appendPrompt,
                    0.7,
                    500
            );

            insertedContent = insertedContent.trim();
            if (insertedContent.startsWith("\"") && insertedContent.endsWith("\"")) {
                insertedContent = insertedContent.substring(1, insertedContent.length() - 1);
            }

            String fullContent = originalContent.substring(0, insertIndex)
                    + insertedContent
                    + originalContent.substring(insertIndex);

            result.put("insertedContent", insertedContent);
            result.put("insertPosition", insertIndex);
            result.put("fullContent", fullContent);
            result.put("tokenUsed", insertedContent.length() / 2);

            NovelForeshadowing foreshadowing = foreshadowingMapper.selectById(request.getForeshadowingId());
            if (foreshadowing != null && !"resolved".equals(foreshadowing.getStatus())) {
                foreshadowing.setStatus("resolved");
                foreshadowing.setResolvedChapterId(request.getProjectId());
                foreshadowingMapper.updateById(foreshadowing);
            }

            log.info("伏笔追加成功: foreshadowingId={}, insertedLen={}", request.getForeshadowingId(), insertedContent.length());

        } catch (IOException e) {
            log.error("伏笔追加失败: {}", e.getMessage(), e);
            throw new RuntimeException("伏笔追加失败: " + e.getMessage());
        }

        return result;
    }

    private int parsePosition(String positionStr) {
        try {
            String num = positionStr.replaceAll("[^0-9]", "");
            return num.isEmpty() ? -1 : Integer.parseInt(num);
        } catch (Exception e) {
            return -1;
        }
    }
}