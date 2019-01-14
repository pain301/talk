package com.pain.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.pain.common.RequestHolder;
import com.pain.exception.ParamException;
import com.pain.mapper.SysRoleAclMapper;
import com.pain.mapper.SysRoleMapper;
import com.pain.mapper.SysRoleUserMapper;
import com.pain.mapper.SysUserMapper;
import com.pain.model.SysRole;
import com.pain.model.SysUser;
import com.pain.util.BeanValidator;
import com.pain.util.IpUtil;
import com.pain.vo.RoleVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Administrator on 2018/6/14.
 */

@Service
public class SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysRoleUserMapper sysRoleUserMapper;

    @Autowired
    private SysRoleAclMapper sysRoleAclMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    public void save(RoleVo roleVo) {
        BeanValidator.check(roleVo);

        if (checkExist(roleVo.getName(), roleVo.getId())) {
            throw new ParamException("角色名称已经存在");
        }

        SysRole sysRole = new SysRole();
        sysRole.setName(roleVo.getName());
        sysRole.setStatus(roleVo.getStatus());
        sysRole.setType(roleVo.getType());
        sysRole.setRemark(roleVo.getRemark());

        sysRole.setOperator(RequestHolder.getCurrentUser().getUsername());
        sysRole.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        sysRole.setOperateTime(new Date());

        sysRoleMapper.insertSelective(sysRole);
//        sysLogService.saveRoleLog(null, role);
    }

    private boolean checkExist(String name, Integer id) {
        return sysRoleMapper.countByName(name, id) > 0;
    }

    public void update(RoleVo roleVo) {
        BeanValidator.check(roleVo);

        if (checkExist(roleVo.getName(), roleVo.getId())) {
            throw new ParamException("角色名称已经存在");
        }

        SysRole before = sysRoleMapper.selectByPrimaryKey(roleVo.getId());
        Preconditions.checkNotNull(before, "待更新的角色不存在");

        SysRole after = new SysRole();
        after.setId(roleVo.getId());
        after.setName(roleVo.getName());
        after.setStatus(roleVo.getStatus());
        after.setType(roleVo.getType());
        after.setRemark(roleVo.getRemark());

        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        after.setOperateTime(new Date());
        sysRoleMapper.updateByPrimaryKeySelective(after);

//        sysLogService.saveRoleLog(before, after);
    }

    public List<SysRole> getAll() {
        return sysRoleMapper.getAll();
    }

    public List<SysRole> getRoleListByUserId(int userId) {
        List<Integer> roleIdList = sysRoleUserMapper.getRoleIdListByUserId(userId);
        if (CollectionUtils.isEmpty(roleIdList)) {
            return Lists.newArrayList();
        }

        return sysRoleMapper.getByIdList(roleIdList);
    }

    public List<SysRole> getRoleListByAclId(int aclId) {
        List<Integer> roleIdList = sysRoleAclMapper.getRoleIdListByAclId(aclId);
        if (CollectionUtils.isEmpty(roleIdList)) {
            return Lists.newArrayList();
        }
        return sysRoleMapper.getByIdList(roleIdList);
    }

    public List<SysUser> getUserListByRoleList(List<SysRole> roleList) {
        if (CollectionUtils.isEmpty(roleList)) {
            return Lists.newArrayList();
        }
        List<Integer> roleIdList = roleList.stream().map(role -> role.getId()).collect(Collectors.toList());
        List<Integer> userIdList = sysRoleUserMapper.getUserIdListByRoleIdList(roleIdList);
        if (CollectionUtils.isEmpty(userIdList)) {
            return Lists.newArrayList();
        }

        return sysUserMapper.getByIdList(userIdList);
    }
}
