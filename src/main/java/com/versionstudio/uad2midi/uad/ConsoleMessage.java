/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

public class ConsoleMessage {
	private DeviceData data;
	private String path;

	public DeviceData getData() {
		return this.data;
	}

	public String getPath() {
		return this.path;
	}
}