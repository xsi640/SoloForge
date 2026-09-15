package com.soloforge.board.board.service;

import com.soloforge.board.board.vo.BoardVO;

public interface BoardService {

    BoardVO getBoard();

    Long requireBoardId();
}