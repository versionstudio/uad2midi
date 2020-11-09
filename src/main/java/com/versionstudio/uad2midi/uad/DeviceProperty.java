/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceProperty {
	public String type;
	public Object value;

	public String getType() {
		return this.type;
	}

	public Object getValue() {
		return this.value;
	}
}