package com.mythweave.web.service;

import com.mythweave.web.dto.OrchestratorResponse;
import com.mythweave.web.entity.NovelAnalysis;
import com.mythweave.web.mapper.AnalysisMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisMapper analysisMapper;

    @Transactional
    public NovelAnalysis saveAnalysis(Long projectId, Long chapterId, String chapterTitle,
                                      Integer chapterIndex, OrchestratorResponse response) {
        NovelAnalysis analysis = new NovelAnalysis();
        analysis.setProjectId(projectId);
        analysis.setChapterId(chapterId);
        analysis.setChapterTitle(chapterTitle);
        analysis.setChapterIndex(chapterIndex);
        analysis.setEditorResult(response.getEditorResult() != null ? response.getEditorResult().getContent() : null);
        analysis.setCharacterResult(response.getCharacterResult() != null ? response.getCharacterResult().getContent() : null);
        analysis.setStyleResult(response.getStyleResult() != null ? response.getStyleResult().getContent() : null);
        analysis.setReaderResult(response.getReaderResult() != null ? response.getReaderResult().getContent() : null);
        analysis.setSummary(response.getSummary());
        analysis.setTotalCostMs(response.getTotalCostMs());

        analysisMapper.insert(analysis);
        log.info("保存综合分析记录, id={}, projectId={}, chapter={}", analysis.getId(), projectId, chapterTitle);
        return analysis;
    }

    public List<NovelAnalysis> getAnalysisHistory(Long projectId) {
        return analysisMapper.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    public NovelAnalysis getAnalysisById(Long id) {
        return analysisMapper.selectById(id);
    }

    public List<NovelAnalysis> getChapterAnalysis(Long projectId, Long chapterId) {
        return analysisMapper.findByProjectIdAndChapterIdOrderByCreateTimeDesc(projectId, chapterId);
    }

    @Transactional
    public void deleteAnalysis(Long id) {
        analysisMapper.deleteById(id);
        log.info("删除综合分析记录, id={}", id);
    }

    @Transactional
    public void deleteProjectAnalysis(Long projectId) {
        analysisMapper.deleteByProjectId(projectId);
        log.info("删除项目所有分析记录, projectId={}", projectId);
    }
}