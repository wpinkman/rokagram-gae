package com.rokagram.backend.dto;

public class RegistryModification {

	private String action;
	private String section;
	private String key;
	private String value;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "RegistryModification [action=" + action + ", section=" + section + ", key=" + key + ", value=" + value
				+ "]";
	}

}
