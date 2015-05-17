package com.rokagram.backend.dto;

import com.rokagram.backend.Utils;

public class GoogleAnalyticsDto {

	private String tid = Utils.isDevServer() ? "UA-47056757-5" : "UA-47056757-2";
	private String sstid = "UA-47056757-6";
	private String cid;

	private boolean events = true;
	private boolean screens = true;
	private boolean social = true;
	private boolean timing = true;
	private boolean exceptions = true;

	public boolean isEvents() {
		return events;
	}

	public void setEvents(boolean events) {
		this.events = events;
	}

	public boolean isScreens() {
		return screens;
	}

	public void setScreens(boolean screens) {
		this.screens = screens;
	}

	public boolean isSocial() {
		return social;
	}

	public void setSocial(boolean social) {
		this.social = social;
	}

	public boolean isTiming() {
		return timing;
	}

	public void setTiming(boolean timing) {
		this.timing = timing;
	}

	public boolean isExceptions() {
		return exceptions;
	}

	public void setExceptions(boolean exceptions) {
		this.exceptions = exceptions;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getCid() {
		return cid;
	}

	public void setCid(String cid) {
		this.cid = cid;
	}

	public String getSstid() {
		return sstid;
	}

	public void setSstid(String sstid) {
		this.sstid = sstid;
	}

}
