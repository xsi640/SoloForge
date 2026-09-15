package com.soloforge.board.auth.service.impl;

import org.springframework.stereotype.Service;

import com.soloforge.board.auth.entity.AppUser;
import com.soloforge.board.auth.mapper.AppUserMapper;
import com.soloforge.board.auth.service.AccountLookupService;

@Service
public class AccountLookupServiceImpl implements AccountLookupService {

    private final AppUserMapper appUserMapper;

    public AccountLookupServiceImpl(AppUserMapper appUserMapper) {
        this.appUserMapper = appUserMapper;
    }

    @Override
    public boolean existsById(Long userId) {
        return userId != null && appUserMapper.findById(userId) != null;
    }

    @Override
    public String findDisplayName(Long userId) {
        AppUser user = userId == null ? null : appUserMapper.findById(userId);
        return user == null ? null : user.getDisplayName();
    }
}
