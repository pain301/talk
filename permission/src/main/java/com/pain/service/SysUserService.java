package com.pain.service;

import com.google.common.base.Preconditions;
import com.pain.beans.PageQuery;
import com.pain.beans.PageResult;
import com.pain.common.RequestHolder;
import com.pain.exception.ParamException;
import com.pain.mapper.SysUserMapper;
import com.pain.model.SysUser;
import com.pain.util.BeanValidator;
import com.pain.util.IpUtil;
import com.pain.util.MD5Util;
import com.pain.util.PasswordUtil;
import com.pain.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2018/6/13.
 */

@Service
public class SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    public boolean checkEmailExist(String mail, Integer userId) {
        return sysUserMapper.countByMail(mail, userId) > 0;
    }

    public boolean checkTelephoneExist(String telephone, Integer userId) {
        return sysUserMapper.countByTelephone(telephone, userId) > 0;
    }

    /**
     *
     * @param keyword 可以为用户名，邮箱，电话号码
     * @return
     */
    public SysUser findByKeyword(String keyword) {
        return sysUserMapper.findByKeyword(keyword);
    }

    public void save(UserVo userVo) {
        BeanValidator.check(userVo);

        if(checkTelephoneExist(userVo.getTelephone(), userVo.getId())) {
            throw new ParamException("电话已被占用");
        }
        if(checkEmailExist(userVo.getMail(), userVo.getId())) {
            throw new ParamException("邮箱已被占用");
        }
        String password = PasswordUtil.randomPassword();
        String encryptedPassword = MD5Util.encrypt(password);

        SysUser user = new SysUser();
        user.setUsername(userVo.getUsername());
        user.setTelephone(userVo.getTelephone());
        user.setMail(userVo.getMail());
        user.setPassword(encryptedPassword);
        user.setDeptId(userVo.getDeptId());
        user.setStatus(userVo.getStatus());
        user.setRemark(userVo.getRemark());

        user.setOperator(RequestHolder.getCurrentUser().getUsername());
        user.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
//        user.setOperateTime(new Date());

        // TODO: sendEmail

        sysUserMapper.insertSelective(user);
//        sysLogService.saveUserLog(null, user);
    }

    public void edit(UserVo userVo) {
        BeanValidator.check(userVo);
        if(checkTelephoneExist(userVo.getTelephone(), userVo.getId())) {
            throw new ParamException("电话已被占用");
        }
        if(checkEmailExist(userVo.getMail(), userVo.getId())) {
            throw new ParamException("邮箱已被占用");
        }
        SysUser before = sysUserMapper.selectByPrimaryKey(userVo.getId());

        Preconditions.checkNotNull(before, "待更新的用户不存在");

        SysUser after = new SysUser();
        after.setId(userVo.getId());
        after.setUsername(userVo.getUsername());
        after.setTelephone(userVo.getTelephone());
        after.setMail(userVo.getMail());
        after.setTelephone(userVo.getTelephone());
        after.setDeptId(userVo.getDeptId());
        after.setStatus(userVo.getStatus());
        after.setRemark(userVo.getRemark());

        after.setOperator(RequestHolder.getCurrentUser().getUsername());
        after.setOperateIp(IpUtil.getRemoteIp(RequestHolder.getCurrentRequest()));
//        after.setOperateTime(new Date());
        sysUserMapper.updateByPrimaryKeySelective(after);
//        sysLogService.saveUserLog(before, after);
    }

    public PageResult<SysUser> getPageByDeptId(int deptId, PageQuery page) {
        BeanValidator.check(page);
        int count = sysUserMapper.countByDeptId(deptId);
        if (count > 0) {
            List<SysUser> list = sysUserMapper.getPageByDeptId(deptId, page);
            PageResult<SysUser> pageResult = new PageResult<SysUser>();
            pageResult.setTotal(count);
            pageResult.setData(list);
            return pageResult;
        }

        return new PageResult<>();
    }

    public List<SysUser> getAll() {
        return sysUserMapper.getAll();
    }
}
