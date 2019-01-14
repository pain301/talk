package com.pain.controller;

import com.pain.common.JsonData;
import com.pain.dto.DeptLevelDto;
import com.pain.service.SysDeptService;
import com.pain.service.SysTreeService;
import com.pain.vo.DeptVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by Administrator on 2018/6/11.
 */

@Controller
@RequestMapping("/sys/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysTreeService sysTreeService;

    @RequestMapping("/dept.page")
    public ModelAndView page() {
        return new ModelAndView("dept");
    }

    @RequestMapping("/save.json")
    @ResponseBody
    public JsonData saveDept(DeptVo deptVo) {
        sysDeptService.save(deptVo);
        return JsonData.success();
    }

    @RequestMapping("/edit.json")
    @ResponseBody
    public JsonData editDept(DeptVo deptVo) {
        sysDeptService.edit(deptVo);
        return JsonData.success();
    }

    @RequestMapping("/tree.json")
    @ResponseBody
    public JsonData tree() {
        List<DeptLevelDto> deptLevelDtoTree = sysTreeService.deptTree();
        return JsonData.success(deptLevelDtoTree);
    }

    @RequestMapping("/delete.json")
    @ResponseBody
    public JsonData delete(@RequestParam("id") int id) {
        sysDeptService.delete(id);
        return JsonData.success();
    }
}
