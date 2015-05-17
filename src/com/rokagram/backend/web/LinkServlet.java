package com.rokagram.backend.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.common.base.Strings;
import com.googlecode.objectify.Key;
import com.rokagram.backend.Alert;
import com.rokagram.backend.Constants;
import com.rokagram.backend.Instagram;
import com.rokagram.backend.Utils;
import com.rokagram.backend.dao.DAO;
import com.rokagram.backend.roku.Roku;
import com.rokagram.entities.UserEntity;

public class LinkServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String code = req.getParameter(Constants.CODE);

		String error = req.getParameter("error");
		String error_reason = req.getParameter("error_reason");
		String error_description = req.getParameter("error_description");
		UserEntity currentUser = null;
		if (Strings.isNullOrEmpty(code) && Strings.isNullOrEmpty(error)) {

			HttpSession session = req.getSession(false);
			if (session != null) {
				String sessionUserId = (String) session.getAttribute(Constants.USERID);
				if (sessionUserId != null) {
					currentUser = DAO.ofy().load().key(Key.create(UserEntity.class, sessionUserId)).now();
				}
				req.setAttribute(Constants.USER, currentUser);
			}

			Utils.resolveAndRender("link", req, resp);

		} else {

			HttpSession session = req.getSession();
			if (session.isNew()) {
				int maxInactiveInterval = session.getMaxInactiveInterval();
				Utils.warn("new session?, maxInactiveInterval:" + maxInactiveInterval);
			} else {

				String state = (String) session.getAttribute(Constants.STATE);
				String stateParam = (String) req.getParameter(Constants.STATE);
				if (state != null && org.apache.commons.lang.StringUtils.equals(state, stateParam)) {
					String redirect = "/";
					if (code != null) {
						// do something

						UserEntity user = Instagram.fetchAccessToken(req, code);

						if (user != null) {
							req.setAttribute(Constants.USER, user);
							// session.setAttribute(Constants.USERID,
							// user.getId());

							String rokuCode = (String) session.getAttribute(Constants.ROKUCODE);
							if (rokuCode != null) {
								boolean linked = Roku.createDeviceUserLinkage(req, rokuCode, user);
								if (linked) {
									redirect = "/love?from=getrokagram&to=" + user.getUsername();
								}

							}
						}

					} else {
						Utils.warn("code was null, error:" + error + ", error_reason:" + error_reason
								+ ", error_description:" + error_description);

						Alert.setErrorAlert(req, "Log in failed", error_description);

					}
					// req.getSession().invalidate();
					resp.sendRedirect(redirect);
				} else {
					Utils.warn("state was null");
					resp.sendRedirect("/");
				}

			}

		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String rokuCode = req.getParameter(Constants.ROKUCODE);
		if (rokuCode != null) {
			if (org.apache.commons.lang.StringUtils.isEmpty(rokuCode)) {
				Alert.setErrorAlert(req, "Error", "The code was empty.");
				resp.sendRedirect("/");
			} else {
				Roku.handleDeviceCodeForm(req, resp, rokuCode.toUpperCase());
			}
		} else {
			resp.sendRedirect("/");
		}
	}

}
