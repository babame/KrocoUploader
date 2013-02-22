package com.beam.krocouploader.chooser;

import java.util.Locale;

public class Option implements Comparable<Option> {
	private String name;
	private String data;
	private String path;

	public Option(String n, String d, String p) {
		name = n;
		data = d;
		path = p;
	}

	public String getName() {
		return name;
	}

	public String getData() {
		return data;
	}

	public String getPath() {
		return path;
	}

	@Override
	public int compareTo(Option o) {
		if (null != this.name)
			return this.name.toLowerCase(Locale.ENGLISH).compareTo(o.getName().toLowerCase(Locale.ENGLISH));
		else
			throw new IllegalArgumentException();
	}
}
