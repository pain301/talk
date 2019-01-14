package com.pain.mapper;

import com.pain.model.SysRoleUser;

import java.util.List;

public interface SysRoleUserMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SysRoleUser record);

    int insertSelective(SysRoleUser record);

    SysRoleUser selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(SysRoleUser record);

    int updateByPrimaryKey(SysRoleUser record);

    List<Integer> getRoleIdListByUserId(Integer userId);

    List<Integer> getUserIdListByRoleId(int roleId);

    void deleteByRoleId(int roleId);

    void batchInsert(List<SysRoleUser> roleUserList);

    List<Integer> getUserIdListByRoleIdList(List<Integer> roleIdList);
}