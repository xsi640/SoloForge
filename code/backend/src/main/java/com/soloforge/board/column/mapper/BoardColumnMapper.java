package com.soloforge.board.column.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.soloforge.board.column.entity.BoardColumn;

@Mapper
public interface BoardColumnMapper {

    List<BoardColumn> findByBoardId(@Param("boardId") Long boardId);

    BoardColumn findById(@Param("id") Long id);
}