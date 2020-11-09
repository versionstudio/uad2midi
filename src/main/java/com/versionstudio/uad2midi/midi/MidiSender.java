/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.midi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MidiSender {
	private static final Logger logger = LogManager.getLogger(MidiSender.class);

	private MidiDevice device;
	private Receiver receiver;

	/**
	 * Connect to the MIDI device used for sending MIDI messages.
	 * @param deviceName the name of the MIDI device to use for sending
	 * @throws MidiException if MIDI device could be initialized
	 */
	public void connect(String deviceName) throws MidiException {
		MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
		for (MidiDevice.Info deviceInfo : devices) {
			if ( !deviceInfo.getName().equals(deviceName)) {
				continue;
			}

			try {
				MidiDevice foundDevice = MidiSystem.getMidiDevice(deviceInfo);
				this.receiver = foundDevice.getReceiver();
				this.device = foundDevice;
				this.device.open();
				logger.info("Initialized MIDI device: {}",deviceName);
			} catch ( MidiUnavailableException e) {
				// simply ignore any exception and instead rely on the null check below
			}
		}
		if ( this.device==null ) {
			throw new MidiException("Failed to initialize MIDI device");
		}
	}

	/**
	 * Disconnect from the MIDI device.
	 */
	public void disconnect() {
		if ( this.device!= null ) {
			this.device.close();
		}
	}

	/**
	 * Send a MIDI message.
	 * @param command the MIDI command represented by the message. See javax.sound.midi.ShortMessage for values
	 * @param channel the channel associated with the message
	 * @param data1 the first data byte
	 * @param data2 the second data byte
	 * @throws MidiException if an error occurs when trying to send the MIDI message
	 */
	public void sendMessage(int command, int channel, int data1, int data2) throws MidiException {
		ShortMessage msg = new ShortMessage();
		try { msg.setMessage(command, channel, data1, data2); }
		catch (InvalidMidiDataException e) {
			throw new MidiException("Could not send MIDI message",e);
		}

		if ( logger.isDebugEnabled() ) {
			logger.debug("Sending MIDI message: Command({}), channel({}), data1({}), data2({})",
					msg.getCommand(),msg.getChannel(),msg.getData1(),msg.getData2());
		}

		this.receiver.send(msg,-1);
	}
}