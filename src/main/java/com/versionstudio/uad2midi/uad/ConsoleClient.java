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

	private final List<ConsoleSubscription> subscriptions;
	private final MidiSender midiSender;
	private final ObjectMapper objectMapper;
	private Socket clientSocket;
	private Thread clientThread;

	private String uadHostname;
	private int uadPort;

	public ConsoleClient(MidiSender midiSender) {
		this.midiSender = midiSender;
		this.subscriptions = new ArrayList<>();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Start the console client which will continuously try to connect
	 * to the UAD console and begin polling messages
	 * @param uadHostname the hostname for connecting to the UAD console
	 * @param uadPort the port for connecting to the UAD console
	 * @param subscriptionConfigs the configurations in JSON format to be deserialized into ConsoleSubscription instances
	 */
	public void start(String uadHostname, int uadPort, List<String> subscriptionConfigs) {
		this.uadHostname = uadHostname;
		this.uadPort = uadPort;

		for ( String subscriptionConfig : subscriptionConfigs ) {
			try {
				this.subscriptions.add(this.objectMapper.readValue(
						subscriptionConfig,ConsoleSubscription.class));
			} catch (JsonProcessingException e) {
				logger.error("Failed to deserialize JSON into ConsoleSubscription: {}", subscriptionConfig);
			}
		}

		this.clientThread = new Thread() {
			@Override
			public void run() {
				while ( true ) {
					if ( connect() ) {
						subscribe();
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
	 * Initiate all UAD console subscriptions.
	 */
	private void subscribe() {
		List<String> requestedSubs = new ArrayList<>();
		for ( ConsoleSubscription sub : this.subscriptions ) {
			// don't subscribe multiple times to the same path
			if ( requestedSubs.contains(sub.getPath() ) ) {
				continue;
			}

			logger.info("Subscribing to UAD console message: {}", sub.getPath());
			sendSubscribe(sub.getPath());
			requestedSubs.add(sub.getPath());
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
				processDeviceData(path[1]);
			}

			// /devices/<id>/inputs
			else if (path.length==3 && path[2].equals("inputs") ) {
				processInputs(path[1],jsonMessage);
			}

			// /devices/<id>/<property>/value
			// /devices/<id>/inputs/<id>/<property>/value
			// /devices/<id>/inputs/<id>/preamps/0/<property>/value
			// /devices/<id>/inputs/<id>/sends/<id><property>/value
			// etc...
			else if ( path[path.length-1].equals("value")) {
				processValueChange(jsonMessage);
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
			logger.info("Requesting data for UAD device: {} (Enable debug logging to trace data)",key);
			sendGetDevice(key);
		}
	}

	/**
	 * Process '/devices/<id>/inputs' message and request more data for all inputs belonging to a device.
	 * @param deviceId the ID of the device owning the input
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processInputs(String deviceId, String jsonMessage) throws JsonProcessingException {
		ConsoleMessage msg = this.objectMapper.readValue(
				jsonMessage,ConsoleMessage.class);

		List<String> childKeys = new ArrayList<>(msg.getData().getChildren().keySet());
		logger.info("Requesting data for {} inputs on UAD device: {} (Enable debug logging to trace data)",childKeys.size(),deviceId);
		for (String key : childKeys) {
			sendGetInput(deviceId,key);
		}
	}

	/**
	 * Process '/devices/<id>' message and send a subscription request
	 * for all configured subscriptions.
	 * @param deviceId the ID of the current device
	 */
	private void processDeviceData(String deviceId) {
		sendGetInputs(deviceId);
	}

	/**
	 * Process any message from UAD that indicates a value change
	 * and trigger if we have a configured subscription that matches this change of value.
	 * @param jsonMessage the JSON message coming from UAD console
	 */
	private void processValueChange(String jsonMessage) throws JsonProcessingException {
		Map<String,Object> map = this.objectMapper.readValue(jsonMessage, Map.class);
		String path = map.get("path").toString();
		String data = map.get("data").toString();

		for ( ConsoleSubscription sub : this.subscriptions ) {
			// ensure we have a subscription that matches the exact path
			// and as well as the new data value
			if ( sub.getPath().equals(path)
					&& (sub.getData().equals(data) || sub.getData()==null) ) {

				// subscription is configured to send a MIDI message
				if ( sub.getMidiCommand()!=null ) {
					try {
						this.midiSender.sendMessage(sub.getMidiCommand(),sub.getMidiChannel(),
								sub.getMidiData1(),sub.getMidiData2());
					} catch (MidiException e) {
						logger.error("Error while sending MIDI message",e);
					}
				}

				// subscription is configured to send a response to the UAD console
				if ( sub.getResponse()!=null ) {
					this.sendMessage(sub.getResponse());
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
	 * Send message to UAD console for retrieving all inputs for a specific device.
	 * @param deviceId the ID of the device
	 */
	private void sendGetInputs(String deviceId) {
		sendMessage("get /devices/" + deviceId + "/inputs");
	}

	/**
	 * Send message to UAD console for retrieving more information about an input.
	 * @param deviceId the ID of the device
	 * @param inputId the ID of the input
	 */
	private void sendGetInput(String deviceId,String inputId) {
		sendMessage("get /devices/" + deviceId + "/inputs/" + inputId);
		sendMessage("get /devices/" + deviceId + "/inputs/" + inputId + "/sends");
	}

	/**
	 * Send message to UAD console for subscribing to changes in a specific property.
	 * @param path the message path to subscribe to
	 */
	private void sendSubscribe(String path) {
		sendMessage("subscribe " + path);
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