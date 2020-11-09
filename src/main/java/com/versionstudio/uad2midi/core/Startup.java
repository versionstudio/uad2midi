/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Startup {
	private static final Logger logger = LogManager.getLogger(Application.class);
	
	public static void main(String[] args) {
		Application app = new Application();
		Runtime.getRuntime().addShutdownHook(new Shutdown(app));
		try {
			app.startup();
		} catch (ApplicationException e) {
			logger.error("Error while starting application. Exiting",e);
		}
	}
}