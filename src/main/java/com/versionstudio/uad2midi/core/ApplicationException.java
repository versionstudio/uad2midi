/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.core;

public class ApplicationException extends Exception {
	private static final long serialVersionUID = 1L;

	public ApplicationException(String message,Throwable cause) {
		super(message,cause);
	}
}