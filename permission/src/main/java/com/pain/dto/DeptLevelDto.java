package com.pain.dto;

import com.google.common.collect.Lists;
import com.pain.model.SysDept;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * Created by Administrator on 2018/6/11.
 */
public class DeptLevelDto extends SysDept {
    private List<DeptLevelDto> dtoList = Lists.newArrayList();

    public static DeptLevelDto adapt(SysDept sysDept) {
        DeptLevelDto deptLevelDto = new DeptLevelDto();
        BeanUtils.copyProperties(sysDept, deptLevelDto);
        return deptLevelDto;
    }

    public List<DeptLevelDto> getDtoList() {
        return dtoList;
    }

    public void setDtoList(List<DeptLevelDto> dtoList) {
        this.dtoList = dtoList;
    }
}
