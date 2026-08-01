package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelCharacter;
import com.novelcraft.web.mapper.NovelCharacterMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "人物工坊")
@RestController
@RequestMapping("/api/projects/{projectId}/characters")
@RequiredArgsConstructor
public class CharacterController {
    private final NovelCharacterMapper charMapper;

    @Operation(summary = "获取人物列表")
    @GetMapping
    public R<List<NovelCharacter>> list(@PathVariable Long projectId) {
        return R.ok(charMapper.selectByProjectId(projectId));
    }
    @PostMapping
    public R<NovelCharacter> create(@PathVariable Long projectId, @Valid @RequestBody NovelCharacter c) {
        c.setProjectId(projectId); charMapper.insert(c); return R.ok(c);
    }
    @PutMapping("/{id}")
    public R<NovelCharacter> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody NovelCharacter c) {
        c.setId(id); charMapper.updateById(c); return R.ok(charMapper.selectById(id));
    }
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        charMapper.deleteById(id); return R.ok();
    }

}
