package com.rokagram.backend.cron;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rokagram.backend.Utils;

@SuppressWarnings("serial")
public class WeekendHashtagProjectCron extends HttpServlet {
	// Nov 21, 2011 is the date WHP started supposedly
	private static final String X_APPENGINE_CRON = "X-Appengine-Cron";
	public static final Logger log = Logger.getLogger(WeekendHashtagProjectCron.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		boolean isCron = Boolean.parseBoolean(req.getHeader(X_APPENGINE_CRON));
		if (isCron || Utils.isDevServer()) {
			Date now = new Date();
			log.warning("It's CRON time" + now.toString());

		} else {
			log.warning(X_APPENGINE_CRON + "header missing or false");
		}
	}

}
