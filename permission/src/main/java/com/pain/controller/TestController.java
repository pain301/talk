package com.pain.controller;

import com.pain.common.JsonData;
import com.pain.exception.PermissionException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by Administrator on 2018/6/10.
 */
@Controller
public class TestController {

    @GetMapping("/test.json")
    @ResponseBody
    public JsonData test() {
        throw new PermissionException("error permission");
//        return JsonData.success("hello world");
    }
}
