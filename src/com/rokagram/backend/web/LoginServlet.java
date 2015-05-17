package com.rokagram.backend.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokagram.backend.Constants;
import com.rokagram.backend.Instagram;

@SuppressWarnings("serial")
public class LoginServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		resp.sendRedirect(Instagram.getAuthorizationUrl(req, Constants.LOVE));
	}

}
