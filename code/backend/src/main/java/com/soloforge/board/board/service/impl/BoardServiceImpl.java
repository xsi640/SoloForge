package com.soloforge.board.board.service.impl;

import org.springframework.stereotype.Service;

import com.soloforge.board.board.entity.Board;
import com.soloforge.board.board.mapper.BoardMapper;
import com.soloforge.board.board.service.BoardService;
import com.soloforge.board.board.vo.BoardVO;
import com.soloforge.board.common.BizException;
import com.soloforge.board.common.ErrorCode;

@Service
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;

    public BoardServiceImpl(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    @Override
    public BoardVO getBoard() {
        Board board = requireBoard();
        return new BoardVO(board.getId(), board.getName());
    }

    @Override
    public Long requireBoardId() {
        return requireBoard().getId();
    }

    private Board requireBoard() {
        Board board = boardMapper.findFirst();
        if (board == null) {
            throw new BizException(ErrorCode.SERVER_ERROR, "看板不存在");
        }
        return board;
    }
}
