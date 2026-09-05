package com.fengluan.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.member.entity.MemberEntity;
import com.fengluan.member.repository.MemberMapper;
import com.fengluan.member.service.MemberService;
import com.fengluan.member.util.DesensitizeUtil;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;

    @Override
    public MemberPageVO page(MemberQueryRequest query) {
        long num = query.getPageNum() == null ? 1 : query.getPageNum();
        long size = query.getPageSize() == null ? 10 : query.getPageSize();
        Page<MemberEntity> p = new Page<>(num, size);
        Page<MemberEntity> result = memberMapper.selectPage(p, new LambdaQueryWrapper<MemberEntity>()
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(MemberEntity::getAccount, query.getKeyword())
                        .or()
                        .like(MemberEntity::getName, query.getKeyword()))
                .orderByDesc(MemberEntity::getId));
        MemberPageVO vo = new MemberPageVO();
        vo.setTotal(result.getTotal());
        vo.setPageNum(num);
        vo.setPageSize(size);
        vo.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    @Override
    public MemberVO getProfile(Long id) {
        MemberEntity entity = memberMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return toVO(entity);
    }

    @Override
    public String getAccount(Long id) {
        MemberEntity entity = memberMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return entity.getAccount();
    }

    @Override
    public MemberVO updateProfile(Long id, MemberProfileUpdateRequest request) {
        MemberEntity existed = memberMapper.selectById(id);
        if (existed == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        MemberEntity update = new MemberEntity();
        update.setId(id);
        if (StringUtils.hasText(request.getName())) {
            update.setName(request.getName());
        }
        update.setSex(request.getSex());
        update.setBirthday(request.getBirthday());
        update.setPortrait(request.getPortrait());
        update.setEmail(request.getEmail());
        update.setPhone(request.getPhone());
        update.setUpdatedTime(LocalDateTime.now());
        memberMapper.updateById(update);
        return toVO(memberMapper.selectById(id));
    }

    private MemberVO toVO(MemberEntity entity) {
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(entity, vo);
        // 手机号脱敏返回
        vo.setPhone(DesensitizeUtil.maskPhone(entity.getPhone()));
        // 身份证仅返回脱敏值
        vo.setMaskedCardId(DesensitizeUtil.maskIdCard(entity.getCardId()));
        return vo;
    }
}