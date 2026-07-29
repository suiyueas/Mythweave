package com.novelcraft.web.repository;

import com.novelcraft.web.entity.NovelAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<NovelAnalysis, Long> {

    List<NovelAnalysis> findByProjectIdOrderByCreateTimeDesc(Long projectId);

    List<NovelAnalysis> findByProjectIdAndChapterIdOrderByCreateTimeDesc(Long projectId, Long chapterId);

    void deleteByProjectId(Long projectId);
}