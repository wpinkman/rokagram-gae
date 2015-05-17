package com.rokagram.entities;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Index
public class RegistryEntity implements Comparable<RegistryEntity> {
	@Id
	private String id;

	private Ref<RokuEntity> device;

	private String section;
	private String key;
	private String value;

	private long time;

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Ref<RokuEntity> getDevice() {
		return device;
	}

	public void setDevice(Ref<RokuEntity> device) {
		this.device = device;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "RegistryEntity [section=" + section + ", key=" + key + ", value=" + value + "]";
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public int compareTo(RegistryEntity o) {
		int ret = 0;
		if (o.getSection() != null && this.section != null) {
			ret = this.section.compareTo(o.getSection());
		}
		return ret;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

}
