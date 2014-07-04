package io.myweb.processor.model;

public class GroupMapping {

	private final String name;
	private final int idx;

	public GroupMapping(String name, int idx) {
		this.name = name;
		this.idx = idx;
	}

	public int getIdx() {
		return idx;
	}

	public String getName() {
		return name;
	}
}
