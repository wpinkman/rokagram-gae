package com.rokagram.entities;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
@Cache
public class EventRollupEntity extends EntityBase {
	@Id
	private Long id;

	private Map<String, Long> eventCounts = new HashMap<String, Long>();

	public Map<String, Long> getEventCounts() {
		return eventCounts;
	}

}
