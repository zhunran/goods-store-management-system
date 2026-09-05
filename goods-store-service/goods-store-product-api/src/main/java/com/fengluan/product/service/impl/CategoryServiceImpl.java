package com.fengluan.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.product.entity.CategoryEntity;
import com.fengluan.product.repository.CategoryMapper;
import com.fengluan.product.service.CategoryService;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryTreeVO> getTree() {
        // 一次查全表，内存构建，避免 N+1
        List<CategoryEntity> entities = categoryMapper.selectList(
                new LambdaQueryWrapper<CategoryEntity>().orderByAsc(CategoryEntity::getSort));

        // parentId -> 子节点列表（顶层 parentId 视为 0）
        Map<Integer, List<CategoryTreeVO>> parentMap = new HashMap<>();
        for (CategoryEntity e : entities) {
            CategoryTreeVO vo = new CategoryTreeVO();
            BeanUtils.copyProperties(e, vo);
            int pid = e.getParentId() == null ? 0 : e.getParentId();
            parentMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(vo);
        }
        // 为每个节点挂载子级（id 转 Integer 匹配 parentId）
        for (List<CategoryTreeVO> siblings : parentMap.values()) {
            for (CategoryTreeVO vo : siblings) {
                vo.setChildren(parentMap.getOrDefault(vo.getId().intValue(), Collections.emptyList()));
            }
        }
        return parentMap.getOrDefault(0, Collections.emptyList());
    }
}