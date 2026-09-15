package com.soloforge.board.auth.service;

public interface AccountLookupService {

    boolean existsById(Long userId);

    String findDisplayName(Long userId);
}