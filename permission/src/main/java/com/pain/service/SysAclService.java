package com.pain.service;

import com.google.common.base.Preconditions;
import com.pain.beans.PageQuery;
import com.pain.beans.PageResult;
import com.pain.common.RequestHolder;
import com.pain.exception.ParamException;
import com.pain.mapper.SysAclMapper;
import com.pain.model.SysAcl;
import com.pain.util.BeanValidator;
import com.pain.util.IpUtil;
import com.pain.vo.AclVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2018/6/14.
 */

@Service
public class SysAclService {

    @Autowired
    private SysAclMapper sysAclMapper;

    public void save(AclVo aclVo) {
        BeanValidator.check(aclVo);

        if (checkExist(aclVo.getAclModuleId(), aclVo.getName(), aclVo.getId())) {
            throw new ParamException("当前权限模块下面存在相同名称的权限点");
        }

        SysAcl sysAcl = new SysAcl();
        sysAcl.setName(aclVo.getName());
        sysAcl.setAclModuleId(aclVo.getAclModuleId());
        sysAcl.setUrl(aclVo.getUrl());
        sysAcl.setType(aclVo.getType());
        sysAcl.setStatus(aclVo.getStatus());
        sysAcl.setSeq(aclVo.getSeq());
        sysAcl.setRemark(aclVo.getRemark());

        sysAcl.setCode(generateCode());
        sysAcl.setOperator(RequestHolder.getCurrentUser().getUsername());
        sysAcl.setOperateTime(new Date());
        sysAcl.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));

        sysAclMapper.insertSelective(sysAcl);
//        sysLogService.saveAclLog(null, acl);
    }

    private String generateCode() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return dateFormat.format(new Date()) + "_" + (int)(Math.random() * 100);
    }

    private boolean checkExist(Integer aclModuleId, String name, Integer id) {
        return sysAclMapper.countByNameAndAclModuleId(aclModuleId, name, id) > 0;
    }


    public void update(AclVo aclVo) {
        BeanValidator.check(aclVo);

        if (checkExist(aclVo.getAclModuleId(), aclVo.getName(), aclVo.getId())) {
            throw new ParamException("当前权限模块下面存在相同名称的权限点");
        }

        SysAcl before = sysAclMapper.selectByPrimaryKey(aclVo.getId());
        Preconditions.checkNotNull(before, "待更新的权限点不存在");

        SysAcl after = new SysAcl();
        after.setId(aclVo.getId());
        after.setName(aclVo.getName());
        after.setAclModuleId(aclVo.getAclModuleId());
        after.setUrl(aclVo.getUrl());
        after.setType(aclVo.getType());
        after.setStatus(aclVo.getStatus());
        after.setSeq(aclVo.getSeq());
        after.setRemark(aclVo.getRemark());

        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateTime(new Date());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));

        sysAclMapper.updateByPrimaryKeySelective(after);
//        sysLogService.saveAclLog(before, after);
    }

    public PageResult<SysAcl> getPageByAclModuleId(Integer aclModuleId, PageQuery pageQuery) {
        BeanValidator.check(pageQuery);
        int count = sysAclMapper.countByAclModuleId(aclModuleId);

        if (count > 0) {
            List<SysAcl> aclList = sysAclMapper.getPageByAclModuleId(aclModuleId, pageQuery);
            PageResult<SysAcl> result = new PageResult<>();
            result.setData(aclList);
            result.setTotal(count);
            return result;
        }

        return new PageResult<>();
    }
}
