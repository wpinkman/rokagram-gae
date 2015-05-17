package com.rokagram.entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class UserEntity extends EntityBase {
	@Id
	private String id;

	private String access_token;
	@Index
	private String username;
	private String full_name;
	private String profile_picture;

	@Index
	private String code;

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFull_name() {
		return full_name;
	}

	public void setFull_name(String full_name) {
		this.full_name = full_name;
	}

	public String getProfile_picture() {
		return profile_picture;
	}

	public void setProfile_picture(String profile_picture) {
		this.profile_picture = profile_picture;
	}

	public void setId(String userid) {
		this.id = userid;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return "UserEntity [id=" + id + ", username=" + username + ", full_name=" + full_name + "]";
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
