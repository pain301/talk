package com.pain.mapper;

import com.pain.model.SysAclModule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysAclModuleMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysAclModule record);

    int insertSelective(SysAclModule record);

    SysAclModule selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SysAclModule record);

    int updateByPrimaryKey(SysAclModule record);

    int countByNameAndParentId(@Param("parentId") Integer parentId,
                               @Param("name") String name,
                               @Param("id") Integer id);

    List<SysAclModule> getChildAclModuleListByLevel(String level);

    void batchUpdateLevel(List<SysAclModule> aclModuleList);

    List<SysAclModule> getAllAclModule();

    int countByParentId(Integer id);
}