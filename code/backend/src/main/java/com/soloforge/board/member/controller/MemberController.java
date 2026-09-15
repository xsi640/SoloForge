package com.soloforge.board.member.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.common.BizException;
import com.soloforge.board.common.CurrentUser;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.R;
import com.soloforge.board.common.SessionUtil;
import com.soloforge.board.member.dto.MemberCreateRequest;
import com.soloforge.board.member.dto.MemberStatusRequest;
import com.soloforge.board.member.dto.PasswordResetRequest;
import com.soloforge.board.member.service.MemberService;
import com.soloforge.board.member.vo.MemberVO;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public R<List<MemberVO>> list() {
        return R.ok(memberService.list());
    }

    @PostMapping
    public R<MemberVO> create(@Valid @RequestBody MemberCreateRequest request) {
        return R.ok(memberService.create(request));
    }

    @PutMapping("/{id}/password")
    public R<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody PasswordResetRequest request) {
        memberService.resetPassword(id, request);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    public R<MemberVO> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody MemberStatusRequest request, HttpSession session) {
        CurrentUser currentUser = SessionUtil.get(session);
        if (currentUser == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return R.ok(memberService.updateStatus(id, request, currentUser.getUserId()));
    }
}
