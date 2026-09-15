package com.soloforge.board.auth.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.soloforge.board.auth.entity.AppUser;

@Mapper
public interface AppUserMapper {

    AppUser findById(@Param("id") Long id);

    AppUser findByUsername(@Param("username") String username);

    List<AppUser> findAll();

    long countAll();

    int insert(AppUser user);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}