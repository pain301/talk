package com.pain.service;

import com.google.common.base.Preconditions;
import com.pain.common.RequestHolder;
import com.pain.exception.ParamException;
import com.pain.mapper.SysDeptMapper;
import com.pain.mapper.SysUserMapper;
import com.pain.model.SysDept;
import com.pain.util.BeanValidator;
import com.pain.util.IpUtil;
import com.pain.util.LevelUtil;
import com.pain.vo.DeptVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by Administrator on 2018/6/11.
 */

@Service
public class SysDeptService {

    @Autowired
    private SysDeptMapper sysDeptMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    private String getLevel(Integer deptId) {
        SysDept dept = sysDeptMapper.selectByPrimaryKey(deptId);
        if (dept == null) {
            return null;
        }
        return dept.getLevel();
    }

    private boolean checkExist(Integer parentId, String deptName, Integer deptId) {
        return sysDeptMapper.countByNameAndParentId(parentId, deptName, deptId) > 0;
    }

    @Transactional
    private void updateWithChild(SysDept before, SysDept after) {
        String newLevelPrefix = after.getLevel();
        String oldLevelPrefix = before.getLevel();

        // 部门的层级没有得到更新
        if (!after.getLevel().equals(before.getLevel())) {
            List<SysDept> deptList = sysDeptMapper.getChildDeptListByLevel(before.getLevel());
            if (CollectionUtils.isNotEmpty(deptList)) {
                for (SysDept dept : deptList) {
                    String level = dept.getLevel();
                    if (level.indexOf(oldLevelPrefix) == 0) {
                        level = newLevelPrefix + level.substring(oldLevelPrefix.length());
                        dept.setLevel(level);
                    }
                }
                sysDeptMapper.batchUpdateLevel(deptList);
            }
        }
        sysDeptMapper.updateByPrimaryKey(after);
    }

    public void save(DeptVo deptVo) {
        BeanValidator.check(deptVo);

        if(checkExist(deptVo.getParentId(), deptVo.getName(), deptVo.getId())) {
            throw new ParamException("同一层级下存在相同名称的部门");
        }

        SysDept sysDept = new SysDept();
        sysDept.setName(deptVo.getName());
        sysDept.setParentId(deptVo.getParentId());
        sysDept.setSeq(deptVo.getSeq());
        sysDept.setRemark(deptVo.getRemark());

        sysDept.setLevel(LevelUtil.calculateLevel(getLevel(deptVo.getParentId()), deptVo.getParentId()));
        sysDept.setOperator(RequestHolder.getCurrentUser().getUsername());
        sysDept.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
//        dept.setOperateTime(new Date());
        sysDeptMapper.insertSelective(sysDept);
//        sysLogService.saveDeptLog(null, dept);
    }

    public void edit(DeptVo deptVo) {
        BeanValidator.check(deptVo);

        if(checkExist(deptVo.getParentId(), deptVo.getName(), deptVo.getId())) {
            throw new ParamException("同一层级下存在相同名称的部门");
        }

        SysDept before = sysDeptMapper.selectByPrimaryKey(deptVo.getId());
        Preconditions.checkNotNull(before, "待更新的部门不存在");

        SysDept after = new SysDept();
        after.setName(deptVo.getName());
        after.setParentId(deptVo.getParentId());
        after.setSeq(deptVo.getSeq());
        after.setRemark(deptVo.getRemark());

        after.setLevel(LevelUtil.calculateLevel(getLevel(deptVo.getParentId()), deptVo.getParentId()));
        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
//        after.setOperateTime(new Date());
//        sysLogService.saveDeptLog(null, dept);

        updateWithChild(before, after);
    }

    public void delete(int deptId) {
        SysDept dept = sysDeptMapper.selectByPrimaryKey(deptId);
        Preconditions.checkNotNull(dept, "待删除的部门不存在，无法删除");

        if (sysDeptMapper.countByParentId(dept.getId()) > 0) {
            throw new ParamException("当前部门下面有子部门，无法删除");
        }

        if(sysUserMapper.countByDeptId(dept.getId()) > 0) {
            throw new ParamException("当前部门下面有用户，无法删除");
        }
        sysDeptMapper.deleteByPrimaryKey(deptId);
    }
}
