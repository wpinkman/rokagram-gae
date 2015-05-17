package com.rokagram.backend;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Alert implements Serializable {
	private static final long serialVersionUID = 1L;
	public static final String DANGER = "danger";
	public static final String SUCCESS = "success";
	public static final String INFO = "info";

	private static final String SESSION_KEY = "alert";

	public Alert(String strong, String message, String level) {
		super();
		this.strong = strong;
		this.message = message;
		this.level = level;
	}

	private String strong;
	private String message;
	private String level;

	public String getStrong() {
		return strong;
	}

	public void setStrong(String strong) {
		this.strong = strong;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public static void setSuccessAlert(HttpServletRequest req, String strong, String message) {
		Alert alert = new Alert(strong, message, SUCCESS);
		req.getSession().setAttribute(SESSION_KEY, alert);

	}

	public static void setErrorAlert(HttpServletRequest req, String strong, String message) {
		Alert alert = new Alert(strong, message, DANGER);
		req.getSession().setAttribute(SESSION_KEY, alert);
	}

	public static void checkAndInject(HttpServletRequest req) {
		HttpSession session = req.getSession(false);
		if (session != null) {
			Alert alert = (Alert) session.getAttribute(SESSION_KEY);
			if (alert != null) {
				req.setAttribute("alert", alert);
				session.removeAttribute(SESSION_KEY);
			}
		}

	}

}
