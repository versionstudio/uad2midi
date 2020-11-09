/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

public class ConsoleTrigger {
	private String deviceId;
	private String property;
	private String value;

	private int midiCommand;
	private int midiChannel;
	private int midiData1;
	private int midiData2;

	public String getDeviceId() {
		return this.deviceId;
	}

	public String getProperty() {
		return this.property;
	}

	public String getValue() {
		return this.value;
	}

	public int getMidiCommand() {
		return this.midiCommand;
	}

	public int getMidiChannel() {
		return this.midiChannel;
	}

	public int getMidiData1() {
		return this.midiData1;
	}

	public int getMidiData2() {
		return this.midiData2;
	}
}