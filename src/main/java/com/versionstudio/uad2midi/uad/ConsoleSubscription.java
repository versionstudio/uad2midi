/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

public class ConsoleSubscription {
	private String path;
	private String value;

	private String consoleCommand;

	private Integer midiCommand;
	private Integer midiChannel;
	private Integer midiData1;
	private Integer midiData2;

	public String getPath() {
		return this.path;
	}

	public String getValue() {
		return this.value;
	}

	public String getConsoleCommand() {
		return this.consoleCommand;
	}

	public Integer getMidiCommand() {
		return this.midiCommand;
	}

	public Integer getMidiChannel() {
		return this.midiChannel;
	}

	public Integer getMidiData1() {
		return this.midiData1;
	}

	public Integer getMidiData2() {
		return this.midiData2;
	}
}