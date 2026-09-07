package com.fengluan.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fengluan.member.entity.RegionEntity;
import com.fengluan.member.repository.RegionMapper;
import com.fengluan.member.service.RegionService;
import com.fengluan.spi.member.vo.RegionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;

    @Override
    public List<RegionVO> children(Long parentId) {
        LambdaQueryWrapper<RegionEntity> wrapper = new LambdaQueryWrapper<>();
        if (parentId == null || parentId == 0) {
            // 数据库省级行 CRI_PARENT_ID 为 NULL，而非 0
            wrapper.isNull(RegionEntity::getParentId);
        } else {
            wrapper.eq(RegionEntity::getParentId, parentId);
        }
        wrapper.eq(RegionEntity::getDataState, 1)
                .orderByAsc(RegionEntity::getSort)
                .orderByAsc(RegionEntity::getId);
        return regionMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private RegionVO toVO(RegionEntity entity) {
        RegionVO vo = new RegionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}