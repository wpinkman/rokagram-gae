package com.rokagram.entities;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class ApiClientEntity extends EntityBase {

	@Id
	private String client_id;

	private int rateLimit;
	private int rateLimitEbb = 5000;

	public String getClient_id() {
		return client_id;
	}

	public void setClient_id(String client_id) {
		this.client_id = client_id;
	}

	public int getRateLimit() {
		return rateLimit;
	}

	public void setRateLimit(int rateLimit) {
		this.rateLimit = rateLimit;
	}

	public int getRateLimitEbb() {
		return rateLimitEbb;
	}

	public void setRateLimitEbb(int rateLimitEbb) {
		this.rateLimitEbb = rateLimitEbb;
	}

	@Override
	public String toString() {
		return "ApiClientEntity [client_id=" + client_id + ", rateLimit=" + rateLimit + ", rateLimitEbb="
				+ rateLimitEbb + "]";
	}

}
