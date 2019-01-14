package com.pain.mapper;

import com.pain.beans.PageQuery;
import com.pain.model.SysAcl;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysAclMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysAcl record);

    int insertSelective(SysAcl record);

    SysAcl selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SysAcl record);

    int updateByPrimaryKey(SysAcl record);

    int countByNameAndAclModuleId(@Param("aclModuleId") Integer aclModuleId,
                                  @Param("name") String name,
                                  @Param("id") Integer id);

    int countByAclModuleId(@Param("aclModuleId") Integer aclModuleId);

    List<SysAcl> getPageByAclModuleId(@Param("aclModuleId") Integer aclModuleId,
                                      @Param("pageQuery") PageQuery pageQuery);

    List<SysAcl> getAll();

    List<SysAcl> getByIdList(@Param("userAclIdList") List<Integer> userAclIdList);

    List<SysAcl> getByUrl(String url);
}