package com.rokagram.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.rokagram.backend.TimeFormatUtils;

public class EntityBase {
	@Index
	@JsonIgnore
	private Date modified;

	@Index
	@JsonIgnore
	private Date added;

	@Ignore
	private boolean ignoreLifecycle = false;

	@Ignore
	@JsonIgnore
	private String editUrl;

	@OnSave
	void updateDate() {
		if (!ignoreLifecycle) {
			Date now = new Date();
			if (this.added == null) {
				this.setAdded(now);
			}
			this.setModified(now);
		}
	}

	@JsonIgnore
	public String getModifiedHuman() {
		return TimeFormatUtils.whenInPastHuman(this.modified);
	}

	@JsonIgnore
	public String getAddedHuman() {
		return TimeFormatUtils.whenInPastHuman(this.added);
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public Date getModified() {
		return modified;
	}

	public Date getAdded() {
		return added;
	}

	public void setAdded(Date added) {
		this.added = added;
	}

	public boolean isIgnoreLifecycle() {
		return ignoreLifecycle;
	}

	public void setIgnoreLifecycle(boolean ignoreLifecycle) {
		this.ignoreLifecycle = ignoreLifecycle;
	}

	public String getEditUrl() {
		return editUrl;
	}

	public void setEditUrl(String editUrl) {
		this.editUrl = editUrl;
	}

}
