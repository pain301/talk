package com.pain.dto;

import com.google.common.collect.Lists;
import com.pain.model.SysAclModule;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * Created by Administrator on 2018/6/14.
 */
public class AclModuleLevelDto extends SysAclModule {
    private List<AclModuleLevelDto> aclModuleList = Lists.newArrayList();

    private List<AclDto> aclList = Lists.newArrayList();

    public static AclModuleLevelDto adapt(SysAclModule aclModule) {
        AclModuleLevelDto dto = new AclModuleLevelDto();
        BeanUtils.copyProperties(aclModule, dto);
        return dto;
    }

    public List<AclDto> getAclList() {
        return aclList;
    }

    public void setAclList(List<AclDto> aclList) {
        this.aclList = aclList;
    }

    public List<AclModuleLevelDto> getAclModuleList() {
        return aclModuleList;
    }

    public void setAclModuleList(List<AclModuleLevelDto> aclModuleList) {
        this.aclModuleList = aclModuleList;
    }
}
