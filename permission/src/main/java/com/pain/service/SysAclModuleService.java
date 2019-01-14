package com.pain.service;

import com.google.common.base.Preconditions;
import com.pain.common.RequestHolder;
import com.pain.exception.ParamException;
import com.pain.mapper.SysAclMapper;
import com.pain.mapper.SysAclModuleMapper;
import com.pain.model.SysAclModule;
import com.pain.util.BeanValidator;
import com.pain.util.IpUtil;
import com.pain.util.LevelUtil;
import com.pain.vo.AclModuleVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2018/6/14.
 */

@Service
public class SysAclModuleService {

    @Autowired
    private SysAclModuleMapper sysAclModuleMapper;

    @Autowired
    private SysAclMapper sysAclMapper;

    private boolean checkExist(Integer parentId, String name, Integer id) {
        return sysAclModuleMapper.countByNameAndParentId(parentId, name, id) > 0;
    }

    private String getLevel(Integer aclModuleId) {
        SysAclModule aclModule = sysAclModuleMapper.selectByPrimaryKey(aclModuleId);
        if (aclModule == null) {
            return null;
        }
        return aclModule.getLevel();
    }

    public void save(AclModuleVo aclModuleVo) {
        BeanValidator.check(aclModuleVo);
        if(checkExist(aclModuleVo.getParentId(), aclModuleVo.getName(), aclModuleVo.getId())) {
            throw new ParamException("同一层级下存在相同名称的权限模块");
        }

        SysAclModule sysAclModule = new SysAclModule();
        sysAclModule.setName(aclModuleVo.getName());
        sysAclModule.setParentId(aclModuleVo.getParentId());
        sysAclModule.setSeq(aclModuleVo.getSeq());
        sysAclModule.setStatus(aclModuleVo.getStatus());
        sysAclModule.setRemark(aclModuleVo.getRemark());
        sysAclModule.setLevel(LevelUtil.calculateLevel(getLevel(aclModuleVo.getParentId()), aclModuleVo.getParentId()));

        sysAclModule.setOperator(RequestHolder.getCurrentUser().getUsername());
        sysAclModule.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        sysAclModule.setOperateTime(new Date());

        sysAclModuleMapper.insertSelective(sysAclModule);
//        sysLogService.saveAclModuleLog(null, aclModule);
    }

    public void update(AclModuleVo aclModuleVo) {
        BeanValidator.check(aclModuleVo);
        if(checkExist(aclModuleVo.getParentId(), aclModuleVo.getName(), aclModuleVo.getId())) {
            throw new ParamException("同一层级下存在相同名称的权限模块");
        }

        SysAclModule before = sysAclModuleMapper.selectByPrimaryKey(aclModuleVo.getId());
        Preconditions.checkNotNull(before, "待更新的权限模块不存在");

        SysAclModule after = new SysAclModule();
        after.setName(aclModuleVo.getName());
        after.setParentId(aclModuleVo.getParentId());
        after.setSeq(aclModuleVo.getSeq());
        after.setStatus(aclModuleVo.getStatus());
        after.setRemark(aclModuleVo.getRemark());
        after.setLevel(LevelUtil.calculateLevel(getLevel(aclModuleVo.getParentId()), aclModuleVo.getParentId()));

        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
        after.setOperateTime(new Date());

        updateWithChild(before, after);
//        sysLogService.saveAclModuleLog(before, after);
    }

    @Transactional
    private void updateWithChild(SysAclModule before, SysAclModule after) {
        String newLevelPrefix = after.getLevel();
        String oldLevelPrefix = before.getLevel();

        if (!after.getLevel().equals(before.getLevel())) {
            List<SysAclModule> aclModuleList = sysAclModuleMapper.getChildAclModuleListByLevel(before.getLevel());
            if (CollectionUtils.isNotEmpty(aclModuleList)) {
                for (SysAclModule aclModule : aclModuleList) {
                    String level = aclModule.getLevel();
                    if (level.indexOf(oldLevelPrefix) == 0) {
                        level = newLevelPrefix + level.substring(oldLevelPrefix.length());
                        aclModule.setLevel(level);
                    }
                }
                sysAclModuleMapper.batchUpdateLevel(aclModuleList);
            }
        }
        sysAclModuleMapper.updateByPrimaryKeySelective(after);
    }

    public void delete(int aclModuleId) {
        SysAclModule aclModule = sysAclModuleMapper.selectByPrimaryKey(aclModuleId);
        Preconditions.checkNotNull(aclModule, "待删除的权限模块不存在，无法删除");

        if(sysAclModuleMapper.countByParentId(aclModule.getId()) > 0) {
            throw new ParamException("当前模块下面有子模块，无法删除");
        }
        if (sysAclMapper.countByAclModuleId(aclModule.getId()) > 0) {
            throw new ParamException("当前模块下面有权限点，无法删除");
        }
        sysAclModuleMapper.deleteByPrimaryKey(aclModuleId);
    }
}
