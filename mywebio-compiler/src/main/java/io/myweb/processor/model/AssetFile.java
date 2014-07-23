package io.myweb.processor.model;

public class AssetFile {

	private final String name;
	private final long length;

	public AssetFile(String name, long length) {
		this.name = name;
		this.length = length;
	}

	public String getName() {
		return name;
	}

	public long getLength() {
		return length;
	}
}
