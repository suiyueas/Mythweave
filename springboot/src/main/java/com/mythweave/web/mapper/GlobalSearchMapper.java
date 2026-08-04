package com.mythweave.web.mapper;

import com.mythweave.web.model.SearchResultDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GlobalSearchMapper {
    List<SearchResultDTO> searchChapters(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchCharacters(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchWorldSettings(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchOutlines(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchPlotThreads(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchInspirations(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    List<SearchResultDTO> searchForeshadowings(@Param("projectId") Long projectId, @Param("keyword") String keyword);
}