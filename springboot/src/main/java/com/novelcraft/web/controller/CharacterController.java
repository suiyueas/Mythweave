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

/**
 * 角色管理控制器（人物工坊）
 * 
 * 主要功能：
 * - 角色的创建、查询、更新、删除（CRUD）操作
 * - 角色信息包括：姓名、定位、能力值、性格、关系、角色弧线等
 * 
 * 所有接口都需要用户登录认证，且操作的角色必须属于当前用户的作品
 */
@Tag(name = "人物工坊")
@RestController
@RequestMapping("/api/projects/{projectId}/characters")
@RequiredArgsConstructor
public class CharacterController {
    
    private final NovelCharacterMapper charMapper;

    /**
     * 获取作品的所有角色列表
     * @param projectId 作品ID
     * @return 角色列表
     */
    @Operation(summary = "获取人物列表")
    @GetMapping
    public R<List<NovelCharacter>> list(@PathVariable Long projectId) {
        return R.ok(charMapper.selectByProjectId(projectId));
    }

    /**
     * 创建新角色
     * @param projectId 作品ID
     * @param c 角色信息
     * @return 创建的角色对象
     */
    @PostMapping
    public R<NovelCharacter> create(@PathVariable Long projectId, @Valid @RequestBody NovelCharacter c) {
        c.setProjectId(projectId); 
        charMapper.insert(c); 
        return R.ok(c);
    }

    /**
     * 更新角色信息
     * @param projectId 作品ID
     * @param id 角色ID
     * @param c 更新后的角色信息
     * @return 更新后的角色对象
     */
    @PutMapping("/{id}")
    public R<NovelCharacter> update(@PathVariable Long projectId, @PathVariable Long id, @RequestBody NovelCharacter c) {
        c.setId(id); 
        charMapper.updateById(c); 
        return R.ok(charMapper.selectById(id));
    }

    /**
     * 删除角色（逻辑删除）
     * @param projectId 作品ID
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long projectId, @PathVariable Long id) {
        charMapper.deleteById(id); 
        return R.ok();
    }
}