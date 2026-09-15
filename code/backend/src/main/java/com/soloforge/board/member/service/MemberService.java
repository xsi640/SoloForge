package com.soloforge.board.member.service;

import java.util.List;

import com.soloforge.board.member.dto.MemberCreateRequest;
import com.soloforge.board.member.dto.MemberStatusRequest;
import com.soloforge.board.member.dto.PasswordResetRequest;
import com.soloforge.board.member.vo.MemberVO;

public interface MemberService {

    List<MemberVO> list();

    MemberVO create(MemberCreateRequest request);

    MemberVO resetPassword(Long memberId, PasswordResetRequest request);

    MemberVO updateStatus(Long memberId, MemberStatusRequest request, Long currentUserId);
}