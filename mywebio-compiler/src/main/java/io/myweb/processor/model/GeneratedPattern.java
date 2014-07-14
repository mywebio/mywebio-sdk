package io.myweb.processor.model;

import java.util.List;
import java.util.Map;

public class GeneratedPattern {

	private final String pattern;
	private final List<GroupMapping> groupMappings;

	public GeneratedPattern(String pattern, List<GroupMapping> groupMappings) {
		this.pattern = pattern;
		this.groupMappings = groupMappings;
	}

	public String getPattern() {
		return pattern;
	}

	public List<GroupMapping> getGroupMappings() {
		return groupMappings;
	}
}
