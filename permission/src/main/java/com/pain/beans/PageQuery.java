package com.pain.beans;

import javax.validation.constraints.Min;

/**
 * Created by Administrator on 2018/6/14.
 */
public class PageQuery {

    @Min(value = 1, message = "当前页码不合法")
    private int pageNo = 1;

    @Min(value = 1, message = "每页展示数量不合法")
    private int pageSize = 10;

    private int offset;

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return (pageNo - 1) * pageSize;
    }
}
