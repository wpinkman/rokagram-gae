package com.rokagram.backend.dto;

public class LogTypeFilterDao {

	private String label;
	private String href;
	private boolean active;

	public LogTypeFilterDao(String label, String href, boolean active) {
		super();
		this.label = label;
		this.href = href;
		this.setActive(active);
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
