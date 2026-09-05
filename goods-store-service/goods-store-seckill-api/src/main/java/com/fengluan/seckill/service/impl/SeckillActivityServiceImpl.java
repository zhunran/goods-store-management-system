package com.fengluan.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.seckill.entity.SeckillEntity;
import com.fengluan.seckill.repository.SeckillMapper;
import com.fengluan.seckill.service.SeckillActivityService;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillMapper, SeckillEntity>
        implements SeckillActivityService {

    private final SeckillMapper seckillMapper;

    @Override
    public SeckillVO create(SeckillActivityRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isBefore(now)) {
            throw new BusinessException("结束时间必须晚于开始时间且晚于当前", ErrorCode.BAD_REQUEST.getCode());
        }
        SeckillEntity e = new SeckillEntity();
        e.setName(request.getName());
        e.setEnabled(request.getEnabled());
        e.setStartTime(request.getStartTime());
        e.setEndTime(request.getEndTime());
        e.setDescription(request.getDescription());
        e.setCreatedTime(now);
        e.setUpdatedTime(now);
        seckillMapper.insert(e);
        return toVO(e);
    }

    @Override
    public SeckillVO update(Long id, SeckillActivityRequest request) {
        SeckillEntity exist = requireById(id);
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间", ErrorCode.BAD_REQUEST.getCode());
        }
        exist.setName(request.getName());
        exist.setEnabled(request.getEnabled());
        exist.setStartTime(request.getStartTime());
        exist.setEndTime(request.getEndTime());
        exist.setDescription(request.getDescription());
        exist.setUpdatedTime(LocalDateTime.now());
        seckillMapper.updateById(exist);
        return toVO(exist);
    }

    @Override
    public void delete(Long id) {
        requireById(id);
        seckillMapper.deleteById(id);
    }

    @Override
    public SeckillVO detail(Long id) { return toVO(requireById(id)); }

    @Override
    public PageVO<SeckillVO> page(Long pageNum, Long pageSize, String name) {
        Page<SeckillEntity> p = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<SeckillEntity> w = new LambdaQueryWrapper<SeckillEntity>()
                .like(name != null && !name.isBlank(), SeckillEntity::getName, name)
                .orderByDesc(SeckillEntity::getId);
        Page<SeckillEntity> r = seckillMapper.selectPage(p, w);
        PageVO<SeckillVO> vo = new PageVO<>();
        vo.setTotal(r.getTotal());
        vo.setPageNum(p.getCurrent());
        vo.setPageSize(p.getSize());
        vo.setRecords(r.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    @Override
    public SeckillEntity getById(Long id) { return requireById(id); }

    private SeckillEntity requireById(Long id) {
        SeckillEntity e = seckillMapper.selectById(id);
        if (e == null) throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        return e;
    }

    private SeckillVO toVO(SeckillEntity e) {
        SeckillVO v = new SeckillVO();
        v.setId(e.getId());
        v.setName(e.getName());
        v.setEnabled(e.getEnabled());
        v.setStartTime(e.getStartTime());
        v.setEndTime(e.getEndTime());
        v.setDescription(e.getDescription());
        v.setCreatedTime(e.getCreatedTime());
        v.setUpdatedTime(e.getUpdatedTime());
        return v;
    }
}