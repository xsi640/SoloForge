package com.soloforge.board.board.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.soloforge.board.board.entity.Board;

@Mapper
public interface BoardMapper {

    Board findById(@Param("id") Long id);

    Board findFirst();
}