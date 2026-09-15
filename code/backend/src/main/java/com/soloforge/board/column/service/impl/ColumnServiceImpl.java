package com.soloforge.board.column.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.soloforge.board.board.service.BoardService;
import com.soloforge.board.column.entity.BoardColumn;
import com.soloforge.board.column.mapper.BoardColumnMapper;
import com.soloforge.board.column.service.ColumnService;
import com.soloforge.board.column.vo.ColumnVO;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.ErrorCode;
import com.soloforge.board.common.FieldErrorItem;

@Service
public class ColumnServiceImpl implements ColumnService {

    private final BoardColumnMapper boardColumnMapper;
    private final BoardService boardService;

    public ColumnServiceImpl(BoardColumnMapper boardColumnMapper, BoardService boardService) {
        this.boardColumnMapper = boardColumnMapper;
        this.boardService = boardService;
    }

    @Override
    public List<ColumnVO> listColumns() {
        Long boardId = boardService.requireBoardId();
        List<BoardColumn> columns = boardColumnMapper.findByBoardId(boardId);
        List<ColumnVO> result = new ArrayList<>(columns.size());
        for (BoardColumn column : columns) {
            result.add(new ColumnVO(column.getId(), column.getCode(), column.getName(), column.getSortOrder()));
        }
        return result;
    }

    @Override
    public BoardColumn requireColumn(Long columnId) {
        BoardColumn column = columnId == null ? null : boardColumnMapper.findById(columnId);
        if (column == null) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "所属列不存在",
                    new FieldErrorItem("columnId", "所属列不存在"));
        }
        return column;
    }
}
