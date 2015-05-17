package com.rokagram.backend.roku;

import com.rokagram.entities.UserEntity;

public class RegResponse {
	private String code;
	private boolean linked;
	private UserEntity user;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public boolean isLinked() {
		return linked;
	}

	public void setLinked(boolean linked) {
		this.linked = linked;
	}

	public void setUser(UserEntity user) {
		this.user = user;
	}

	public UserEntity getUser() {
		return user;
	}

}
