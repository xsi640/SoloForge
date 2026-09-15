package com.soloforge.board.column.service;

import java.util.List;

import com.soloforge.board.column.entity.BoardColumn;
import com.soloforge.board.column.vo.ColumnVO;

public interface ColumnService {

    List<ColumnVO> listColumns();

    BoardColumn requireColumn(Long columnId);
}