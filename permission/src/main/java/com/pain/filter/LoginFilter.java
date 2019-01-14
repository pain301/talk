package com.pain.filter;

import com.pain.common.RequestHolder;
import com.pain.model.SysUser;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Administrator on 2018/6/14.
 */
public class LoginFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        SysUser sysUser = (SysUser)req.getSession().getAttribute("user");

        if (sysUser == null) {
//            String path = "signin.jsp";

            String path = "/signin.jsp";
            resp.sendRedirect(path);
            return;
        }

        RequestHolder.add(sysUser);
        RequestHolder.add(req);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
