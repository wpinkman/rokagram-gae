package com.rokagram.backend.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.rokagram.backend.Utils;

public class CommonAttributesFilter implements Filter {

	@Override
	public void init(FilterConfig arg0) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			request.setAttribute("localhost", Utils.isDevServer());
			request.setAttribute("production", Utils.isProductionServer());
		}

		filterChain.doFilter(servletRequest, servletResponse);

	}

	@Override
	public void destroy() {

	}

}
