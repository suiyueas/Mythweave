package com.novelcraft.web.service;

import com.novelcraft.web.entity.NovelOutline;
import com.novelcraft.web.mapper.NovelOutlineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutlineService {
    private final NovelOutlineMapper outlineMapper;

    public List<NovelOutline> getOutlinesByProjectId(Long projectId) {
        return outlineMapper.selectByProjectId(projectId);
    }

    @Transactional
    public int fixActDistribution(Long projectId) {
        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        if (outlines == null || outlines.isEmpty()) {
            return 0;
        }

        int total = outlines.size();
        int perAct = Math.max(1, (int) Math.ceil(total / 3.0));

        for (int i = 0; i < outlines.size(); i++) {
            NovelOutline outline = outlines.get(i);
            String newAct;
            if (i < perAct) {
                newAct = "first_act";
            } else if (i < perAct * 2) {
                newAct = "second_act";
            } else {
                newAct = "third_act";
            }

            if (!newAct.equals(outline.getAct())) {
                outline.setAct(newAct);
                outlineMapper.updateById(outline);
            }
        }

        return total;
    }
}