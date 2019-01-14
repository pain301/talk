package com.pain.mapper;

import com.pain.model.SysRoleAcl;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SysRoleAclMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysRoleAcl record);

    int insertSelective(SysRoleAcl record);

    SysRoleAcl selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SysRoleAcl record);

    int updateByPrimaryKey(SysRoleAcl record);

    List<Integer> getAclIdListByRoleIdList(@Param("userRoleIdList") List<Integer> userRoleIdList);

    void deleteByRoleId(int roleId);

    void batchInsert(List<SysRoleAcl> roleAclList);

    List<Integer> getRoleIdListByAclId(int aclId);
}