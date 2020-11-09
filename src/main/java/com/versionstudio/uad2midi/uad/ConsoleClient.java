/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.versionstudio.uad2midi.midi.MidiException;
import com.versionstudio.uad2midi.midi.MidiSender;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsoleClient {
	private static final Logger logger = LogManager.getLogger(ConsoleClient.class);

	private static final String ENCODING = "UTF-8";
	private static final String MESSAGE_SEPARATOR = "\u0000";

	private static final long RECONNECT_WAIT = 5000;

	private final List<ConsoleTrigger> triggers;
	private final MidiSender midiSender;
	private final ObjectMapper objectMapper;
	private Socket clientSocket;
	private Thread clientThread;

	private String uadHostname;
	private int uadPort;

	public ConsoleClient(MidiSender midiSender) {
		this.midiSender = midiSender;
		this.triggers = new ArrayList<>();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Start the console client which will continuously try to connect
	 * to the UAD console and begin polling messages
	 * @param uadHostname the hostname for connecting to the UAD console
	 * @param uadPort the port for connecting to the UAD console
	 * @param triggerConfigs the trigger configurations in JSON format to be deserialized into ConsoleTrigger instances
	 */
	public void start(String uadHostname, int uadPort, List<String> triggerConfigs) {
		this.uadHostname = uadHostname;
		this.uadPort = uadPort;

		for ( String triggerConfig : triggerConfigs ) {
			try {
				this.triggers.add(this.objectMapper.readValue(
						triggerConfig,ConsoleTrigger.class));
			} catch (JsonProcessingException e) {
				logger.error("Failed to deserialize JSON into ConsoleTrigger: {}", triggerConfig);
			}
		}

		this.clientThread = new Thread() {
			@Override
			public void run() {
				while ( true ) {
					if ( connect() ) {
						sendGetDevices();
						poll();
						disconnect();
					}
					try {
						Thread.sleep(RECONNECT_WAIT);
					} catch (InterruptedException e) {
						break;
					}
				}
			}
		};

		this.clientThread.start();
	}

	/**
	 * Connect to the UAD console.
	 * @return true if connection was successful
	 */
	private boolean connect() {
		try {
			InetAddress host = InetAddress.getByName(this.uadHostname);
			this.clientSocket = new Socket(host,this.uadPort);
			logger.info("Connected to UAD console at: {}", this.clientSocket.getRemoteSocketAddress());
			return true;
		} catch (IOException e) {
				logger.info("Could not connect to UAD console. Retrying in {} ms",RECONNECT_WAIT);
			if ( logger.isDebugEnabled() ) {
				logger.debug("Failed opening connection to UAD console",e);
			}
			return false;
		}
	}

	/**
	 * Disconnect from the UAD console.
	 */
	private void disconnect() {
		try {
			this.clientSocket.close();
		} catch (IOException e) {
			logger.error("Failed closing connection to the UAD console",e);
		}
	}

	/**
	 * Read incoming messages from the UAD console.
	 * Please note that Input Stream scanning used here is a blocking operation
	 * until thread is interrupted or UAD console connection is lost.
	 */
	private void poll() {
		try {
			Scanner scanner = new Scanner(this.clientSocket.getInputStream(),ENCODING);
			scanner.useDelimiter(MESSAGE_SEPARATOR);
			while (scanner.hasNext()) {
				String jsonMessage = scanner.next();
				processMessage(jsonMessage);
			}
		} catch (IOException e) {
			logger.error("Error while reading from UAD console",e);
		}
	}

	/**
	 * Process incoming JSON message.
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processMessage(String jsonMessage) {
		if ( logger.isDebugEnabled() ) {
			logger.debug("Message received from UAD console: {}", jsonMessage);
		}

		try {
			String[] path = processPath(jsonMessage);
			if ( !path[0].equals("devices")) {
				return;
			}

			// /devices
			if ( path.length==1 ) {
				processDevices(jsonMessage);
			}

			// /devices/<id>
			else if ( path.length==2 ) {
				processDeviceData(path[1],jsonMessage);
			}

			// msg: /devices/<id>/<property>/value
			else if ( path.length==4 && path[3].equals("value")) {
				processPropertyValueChange(path[1],path[2],jsonMessage);
			}
		} catch (JsonProcessingException e) {
			logger.error("Received invalid JSON response from UAD console",e);
		}
	}

	/**
	 * Process the path that's heading the JSON message.
	 * @param jsonMessage the JSON message coming from UAD console
	 * @return the path chopped up into an array of strings
	 */
	private String[] processPath(String jsonMessage) throws JsonProcessingException {
		BasicConsoleMessage msg = this.objectMapper.readValue(
				jsonMessage,BasicConsoleMessage.class);

		return msg.getPath().substring(1).split("/");
	}

	/**
	 * Process '/devices' message and request more data for all existing devices.
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processDevices(String jsonMessage) throws JsonProcessingException {
		ConsoleMessage msg = this.objectMapper.readValue(
				jsonMessage,ConsoleMessage.class);

		List<String> childKeys = new ArrayList<>(msg.getData().getChildren().keySet());
		for (String key : childKeys) {
			logger.info("Requesting data for UAD device {}",key);
			sendGetDevice(key);
		}
	}

	/**
	 * Process '/devices/<id>' message and send a subscription request
	 * for those UAD property value changes we have triggers for.
	 * @param deviceId the ID of the current device
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processDeviceData(String deviceId, String jsonMessage) throws JsonProcessingException {
		ConsoleMessage msg = this.objectMapper.readValue(
				jsonMessage,ConsoleMessage.class);

		// keep track of requested subscriptions to ensure we
		// only subscribe once per UAD property and device
		List<String> requestedSubs = new ArrayList<>();

		for ( ConsoleTrigger trigger : this.triggers ) {
			// check if we have a trigger for this UAD property
			if ( trigger.getDeviceId().equals(deviceId)
					&& msg.getData().getProperties().containsKey(trigger.getProperty()) ) {
				// do not subscribe to the same property more than once
				if ( requestedSubs.contains(trigger.getProperty()) ) {
					continue;
				}

				logger.info("Subscribing to UAD device {} and property: {}", deviceId, trigger.getProperty());
				sendSubscribe(deviceId,trigger.getProperty());
				requestedSubs.add(trigger.getProperty());
			}
		}
	}

	/**
	 * Process '/devices/<id>/<property>/value' message and trigger MIDI messages
	 * for those property value changes that matches those we've subscribed to.
	 * @param deviceId the ID of the current device
	 * @param property the name of the property that changed value
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processPropertyValueChange(String deviceId, String property, String jsonMessage) throws JsonProcessingException {
		Map<String,Object> map = this.objectMapper.readValue(jsonMessage, Map.class);
		String newValue = map.get("data").toString();

		for ( ConsoleTrigger trigger : this.triggers ) {
			// ensure that we have a trigger that matches the requirements
			if ( trigger.getDeviceId().equals(deviceId) && trigger.getProperty().equals(property)
					&& ( trigger.getValue().equals(newValue) || trigger.getValue()==null )) {
				try {
					this.midiSender.sendMessage(trigger.getMidiCommand(),trigger.getMidiChannel(),
							trigger.getMidiData1(),trigger.getMidiData2());
				} catch (MidiException e) {
					logger.error("Error while sending MIDI message",e);
				}
			}
		}
	}

	/**
	 * Send message to UAD console for retrieving all devices.
	 */
	private void sendGetDevices() {
		sendMessage("get /devices");
	}

	/**
	 * Send message to UAD console for retrieving details about a specific device.
	 * @param deviceId the ID of the device
	 */
	private void sendGetDevice(String deviceId) {
		sendMessage("get /devices/" + deviceId);
	}

	/**
	 * Send message to UAD console for subscribing to changes in a specific property.
	 * @param deviceId the ID of the device
	 * @param propertyName the name of the property to subscribe to
	 */
	private void sendSubscribe(String deviceId, String propertyName) {
		sendMessage("subscribe /devices/" + deviceId + "/" + propertyName);
	}

	/**
	 * Send a message to the UAD console.
	 * @param msg the message to send
	 */
	private void sendMessage(String msg) {
		if ( logger.isDebugEnabled() ) {
			logger.debug("Sending message to UAD console: {}", msg);
		}
		try {
			OutputStream os = clientSocket.getOutputStream();
			os.write((msg + MESSAGE_SEPARATOR).getBytes(ENCODING));
		} catch (IOException e) {
			logger.error("Error while sending message to UAD console",e);
		}
	}

	/**
	 * Stop and cleanup the listener.
	 * This signals the listener thread as interrupted.
	 */
	public void stop() {
		if ( this.clientThread!=null && this.clientThread.isAlive() ) {
			this.clientThread.interrupt();
		}
	}
}