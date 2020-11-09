/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.core;

class Shutdown extends Thread {
	private final Application application;
	
	public Shutdown(Application application) { this.application = application; }
	
    /**
     * {@inheritDoc}
     */	
	@Override
    public void run() {
		this.application.shutdown();
    }
}