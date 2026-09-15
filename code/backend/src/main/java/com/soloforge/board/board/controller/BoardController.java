package com.soloforge.board.board.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.board.service.BoardService;
import com.soloforge.board.board.vo.BoardVO;
import com.soloforge.board.common.R;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public R<BoardVO> getBoard() {
        return R.ok(boardService.getBoard());
    }
}
