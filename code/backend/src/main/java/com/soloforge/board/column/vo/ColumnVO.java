package com.soloforge.board.column.vo;

public class ColumnVO {

    private Long id;
    private String code;
    private String name;
    private Integer sortOrder;

    public ColumnVO() {
    }

    public ColumnVO(Long id, String code, String name, Integer sortOrder) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}