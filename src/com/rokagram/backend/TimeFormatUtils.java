package com.rokagram.backend;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeFormatUtils {
	private static final long SEC = 1000;
	private static final long MIN = SEC * 60;
	private static final long HOUR = MIN * 60;
	private static final long DAY = HOUR * 24;
	private static final long WEEK = DAY * 7;
	private static final long YEAR = WEEK * 52;

	public static String whenInPastHuman(Date pastEvent) {
		String ret = "??";

		if (pastEvent != null) {
			Date now = new Date();

			long diffMs = (now.getTime() - pastEvent.getTime());

			long years = diffMs / YEAR;
			long weeks = diffMs / WEEK;
			long days = diffMs / DAY;
			long hours = diffMs / HOUR;
			long minutes = diffMs / MIN;
			long seconds = diffMs / SEC;

			if (years > 0) {
				ret = Long.toString(years) + "y";
			} else if (weeks > 0) {
				ret = Long.toString(weeks) + "w";
			} else if (days > 0) {
				ret = Long.toString(days) + "d";
			} else if (hours > 0) {
				ret = Long.toString(hours) + "h";
			} else if (minutes > 0) {
				ret = Long.toString(minutes) + "m";
			} else if (seconds > 0) {
				ret = Long.toString(seconds) + "s";
			}
		}
		return ret;
	}

	public static String getHuman(Date date) {
		String ret = null;
		if (date != null) {
			DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
			ret = df.format(date);
		}
		return ret;
	}

}
