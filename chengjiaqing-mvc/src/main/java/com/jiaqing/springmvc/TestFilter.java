package com.jiaqing.springmvc;

import javax.servlet.*;
import java.io.IOException;

/**
 * @date 2020/10/16 16:56
 *
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  
public class TestFilter implements Filter {
    public TestFilter() {
        System.out.println("TestFilter instance");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("testFilter");
        chain.doFilter(request,response);
    }

    @Override
    public void destroy() {

    }
}