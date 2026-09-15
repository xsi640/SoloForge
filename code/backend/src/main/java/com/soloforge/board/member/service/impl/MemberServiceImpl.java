package com.soloforge.board.member.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soloforge.board.auth.entity.AppUser;
import com.soloforge.board.auth.mapper.AppUserMapper;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.FieldErrorItem;
import com.soloforge.board.common.enums.UserStatus;
import com.soloforge.board.member.dto.MemberCreateRequest;
import com.soloforge.board.member.dto.MemberStatusRequest;
import com.soloforge.board.member.dto.PasswordResetRequest;
import com.soloforge.board.member.service.MemberService;
import com.soloforge.board.member.vo.MemberVO;

@Service
public class MemberServiceImpl implements MemberService {

    private static final String STATUS_FIELD = "status";

    private final AppUserMapper appUserMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(AppUserMapper appUserMapper, PasswordEncoder passwordEncoder) {
        this.appUserMapper = appUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<MemberVO> list() {
        List<MemberVO> members = new ArrayList<>();
        for (AppUser user : appUserMapper.findAll()) {
            members.add(toMemberVO(user));
        }
        return members;
    }

    @Override
    @Transactional
    public MemberVO create(MemberCreateRequest request) {
        if (appUserMapper.findByUsername(request.getUsername()) != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS, ErrorCode.USERNAME_EXISTS.getMessage(),
                    new FieldErrorItem("username", ErrorCode.USERNAME_EXISTS.getMessage()));
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setDisplayName(request.getDisplayName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAdmin(Boolean.TRUE.equals(request.getIsAdmin()));
        user.setStatus(resolveNewStatus(request.getStatus()));
        appUserMapper.insert(user);
        return toMemberVO(appUserMapper.findById(user.getId()));
    }

    @Override
    @Transactional
    public MemberVO resetPassword(Long memberId, PasswordResetRequest request) {
        AppUser user = requireMember(memberId);
        appUserMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(request.getPassword()));
        return toMemberVO(appUserMapper.findById(user.getId()));
    }

    @Override
    @Transactional
    public MemberVO updateStatus(Long memberId, MemberStatusRequest request, Long currentUserId) {
        AppUser user = requireMember(memberId);
        UserStatus targetStatus = parseStatus(request.getStatus());
        if (targetStatus == UserStatus.DISABLED && Objects.equals(user.getId(), currentUserId)) {
            throw new BizException(ErrorCode.OPERATION_NOT_ALLOWED, "不可停用当前登录账号");
        }
        appUserMapper.updateStatus(user.getId(), targetStatus.name());
        return toMemberVO(appUserMapper.findById(user.getId()));
    }

    private AppUser requireMember(Long memberId) {
        AppUser user = memberId == null ? null : appUserMapper.findById(memberId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "成员不存在");
        }
        return user;
    }

    private String resolveNewStatus(String status) {
        if (status == null || status.isBlank()) {
            return UserStatus.ENABLED.name();
        }
        return parseStatus(status).name();
    }

    private UserStatus parseStatus(String status) {
        for (UserStatus candidate : UserStatus.values()) {
            if (candidate.name().equals(status)) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.VALIDATION_FAILED, "账号状态不正确",
                new FieldErrorItem(STATUS_FIELD, "账号状态不正确"));
    }

    private MemberVO toMemberVO(AppUser user) {
        MemberVO member = new MemberVO();
        member.setId(user.getId());
        member.setUsername(user.getUsername());
        member.setDisplayName(user.getDisplayName());
        member.setIsAdmin(Boolean.TRUE.equals(user.getAdmin()));
        member.setStatus(user.getStatus());
        member.setCreatedAt(user.getCreatedAt());
        return member;
    }
}
