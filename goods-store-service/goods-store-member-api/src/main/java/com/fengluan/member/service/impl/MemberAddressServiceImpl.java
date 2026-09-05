package com.fengluan.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.member.entity.MemberAddressEntity;
import com.fengluan.member.entity.MemberEntity;
import com.fengluan.member.repository.MemberAddressMapper;
import com.fengluan.member.repository.MemberMapper;
import com.fengluan.member.service.MemberAddressService;
import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberAddressServiceImpl implements MemberAddressService {

    private final MemberAddressMapper addressMapper;
    private final MemberMapper memberMapper;

    @Override
    public List<MemberAddressVO> listAddress(Long memberId) {
        List<MemberAddressEntity> list = addressMapper.selectList(
                new LambdaQueryWrapper<MemberAddressEntity>()
                        .eq(MemberAddressEntity::getMemberAccount, resolveAccount(memberId))
                        .orderByDesc(MemberAddressEntity::getIsDefault)
                        .orderByDesc(MemberAddressEntity::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public MemberAddressVO createAddress(Long memberId, MemberAddressRequest request) {
        MemberAddressEntity entity = new MemberAddressEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setMemberAccount(resolveAccount(memberId));
        addressMapper.insert(entity);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefault(memberId, entity.getId());
        }
        return toVO(addressMapper.selectById(entity.getId()));
    }

    @Override
    public MemberAddressVO updateAddress(Long memberId, Long addrId, MemberAddressRequest request) {
        findOwned(memberId, addrId);
        MemberAddressEntity update = new MemberAddressEntity();
        update.setId(addrId);
        update.setReceiver(request.getReceiver());
        update.setPhone(request.getPhone());
        update.setAddrId(request.getAddrId());
        update.setAddrDetail(request.getAddrDetail());
        update.setIsDefault(request.getIsDefault());
        addressMapper.updateById(update);
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            setDefault(memberId, addrId);
        }
        return toVO(findOwned(memberId, addrId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long memberId, Long addrId) {
        // 越权校验 + 存在校验
        findOwned(memberId, addrId);
        addressMapper.deleteById(addrId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long memberId, Long addrId) {
        findOwned(memberId, addrId);
        setDefault(memberId, addrId);
    }

    @Override
    public MemberAddressVO getDefaultAddress(Long memberId) {
        MemberAddressEntity entity = addressMapper.selectList(
                        new LambdaQueryWrapper<MemberAddressEntity>()
                                .eq(MemberAddressEntity::getMemberAccount, resolveAccount(memberId))
                                .eq(MemberAddressEntity::getIsDefault, true)
                                .last("limit 1"))
                .stream().findFirst().orElse(null);
        return entity == null ? null : toVO(entity);
    }

    /**
     * 设置默认地址：先取消该会员所有默认，再置目标为默认（原子）
     */
    @Transactional(rollbackFor = Exception.class)
    protected void setDefault(Long memberId, Long addrId) {
        String account = resolveAccount(memberId);
        addressMapper.update(null, new LambdaUpdateWrapper<MemberAddressEntity>()
                .eq(MemberAddressEntity::getMemberAccount, account)
                .set(MemberAddressEntity::getIsDefault, false));
        addressMapper.update(null, new LambdaUpdateWrapper<MemberAddressEntity>()
                .eq(MemberAddressEntity::getId, addrId)
                .set(MemberAddressEntity::getIsDefault, true));
    }

    /**
     * 校验地址归属当前会员且存在，返回该地址
     */
    private MemberAddressEntity findOwned(Long memberId, Long addrId) {
        String account = resolveAccount(memberId);
        MemberAddressEntity entity = addressMapper.selectOne(
                new LambdaQueryWrapper<MemberAddressEntity>()
                        .eq(MemberAddressEntity::getId, addrId)
                        .eq(MemberAddressEntity::getMemberAccount, account));
        if (entity == null) {
            throw new BusinessException(ErrorCode.MEMBER_ADDRESS_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 会员 id -> member_account（member_address 表以 account 关联）
     */
    private String resolveAccount(Long memberId) {
        MemberEntity member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return member.getAccount();
    }

    private MemberAddressVO toVO(MemberAddressEntity entity) {
        MemberAddressVO vo = new MemberAddressVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}