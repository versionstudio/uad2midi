/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.core;

import com.versionstudio.uad2midi.configuration.ConfigurationHelper;
import com.versionstudio.uad2midi.midi.MidiException;
import com.versionstudio.uad2midi.midi.MidiSender;
import com.versionstudio.uad2midi.uad.ConsoleClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Application {
	private static final Logger logger = LogManager.getLogger(Application.class);
	
	private static final String CONFIGURATION_BUNDLE_KEY = "uad2midi";
	private static final String CONFIGURATION_MIDI_DEVICE_NAME = "uad2midi.midi.deviceName";
	private static final String CONFIGURATION_UAD_HOSTNAME = "uad2midi.uad.hostname";
	private static final String CONFIGURATION_UAD_PORT = "uad2midi.uad.port";
	private static final String CONFIGURATION_UAD_SUBSCRIPTION_PREFIX = "uad2midi.subscription.";

	private final ConfigurationHelper configuration;
	private final ConsoleClient consoleClient;
	private final MidiSender midiSender;

	public Application() {
		this.midiSender = new MidiSender();
		this.consoleClient = new ConsoleClient(this.midiSender);
		this.configuration = new ConfigurationHelper();
	}

	/**
	 * Startup and initialize the application.
	 * @throws ApplicationException if something prevented the application from starting up
	 */
	public void startup() throws ApplicationException {
		logger.info("Starting up uad2midi");

		this.configuration.load(CONFIGURATION_BUNDLE_KEY);

		try {
			this.midiSender.connect(
					this.configuration.getStringValue(CONFIGURATION_MIDI_DEVICE_NAME,"Bus 1"));
		} catch (MidiException e) {
			throw new ApplicationException("Could not initialize MIDI sender",e);
		}

		this.consoleClient.start(
				this.configuration.getStringValue(CONFIGURATION_UAD_HOSTNAME, "localhost"),
				this.configuration.getIntValue(CONFIGURATION_UAD_PORT,4710),
				this.configuration.getValuesByKeyPrefix(CONFIGURATION_UAD_SUBSCRIPTION_PREFIX));
	}
	
	/**
	 * Shutdown the application and take care of cleanup.
	 */
	public void shutdown() {
		logger.info("Shutting down uad2midi");
		this.consoleClient.stop();
		this.midiSender.disconnect();
	}
}