package com.soloforge.board.column.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soloforge.board.column.service.ColumnService;
import com.soloforge.board.column.vo.ColumnVO;
import com.soloforge.board.common.R;

@RestController
@RequestMapping("/api/board/columns")
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @GetMapping
    public R<List<ColumnVO>> listColumns() {
        return R.ok(columnService.listColumns());
    }
}
