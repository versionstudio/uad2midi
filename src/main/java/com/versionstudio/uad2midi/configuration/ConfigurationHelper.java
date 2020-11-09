/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.configuration;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationHelper {
	private static final Logger logger = LogManager.getLogger(ConfigurationHelper.class);

	private final Properties properties;

	public ConfigurationHelper() {
		this.properties = new Properties();
	}

	/**
	 * Load configuration data from a resource bundle.
	 * @param baseName the base name of the resource bundle
	 */
	public void load(String baseName) {
		try {
			ResourceBundle rb = ResourceBundle.getBundle(baseName);
			for ( Enumeration<String> keys=rb.getKeys(); keys.hasMoreElements(); ) {
				String key = keys.nextElement();
				Object value = rb.getObject(key);
				this.properties.put(key,value);
			}
		} catch(MissingResourceException e) {
			logger.info("Could not find any configuration resource bundle with base name: {}",baseName);
		}
	}

	/**
	 * Get a boolean property value.
	 * @param key the property key to find
	 * @return the property value
	 */
	public boolean getBooleanValue(String key) {
		String value = getStringValue(key);
		return Boolean.parseBoolean(value);
	}

	/**
	 * Get a boolean property value.
	 * @param key the property key to find
	 * @param defaultValue the default value to return if no value was found
	 * @return the property value
	 */
	public boolean getBooleanValue(String key,boolean defaultValue) {
		if ( this.properties.containsKey(key) ) {
			return getBooleanValue(key);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get an int property value.
	 * @param key the property key to find
	 * @return the property value
	 */
	public int getIntValue(String key) {
		String value = getStringValue(key);
		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException e) {
			return -1;
		}
	}

	/**
	 * Get a int property value.
	 * @param key the property key to find
	 * @param defaultValue the default value to return if no value was found
	 * @return the property value
	 */
	public int getIntValue(String key,int defaultValue) {
		if ( this.properties.containsKey(key) ) {
			return getIntValue(key);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Get a string property value.
	 * @param key the property key to find
	 * @return the property value
	 */
	public String getStringValue(String key) {
		Object value = this.properties.getProperty(key);
		if ( value instanceof String ) {
			return (String)value;
		} else {
			return "";
		}
	}

	/**
	 * Get a string property value.
	 * @param key the property key to find
	 * @param defaultValue the default value to return if no value was found
	 * @return the property value
	 */
	public String getStringValue(String key,String defaultValue) {
		if ( this.properties.containsKey(key) ) {
			return getStringValue(key);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Gets the property keys that start with the given prefix.
	 * @param prefix the key prefix to look for.
	 * @return a list of all keys that started with the prefix
	 */
	public List<String> getKeysByPrefix(String prefix) {
		List<String> foundKeys = new ArrayList<>();
		Set<String> keys = this.properties.stringPropertyNames();
		for (String key : keys) {
			if ( key.startsWith(prefix) ) {
				foundKeys.add(key);
			}
		}
		return foundKeys;
	}

	/**
	 * Gets the property values for keys that start with the given prefix.
	 * @param prefix the key prefix to look for.
	 * @return a list of all keys that started with the prefix
	 */
	public List<String> getValuesByKeyPrefix(String prefix) {
		List<String> foundValues = new ArrayList<>();
		Set<String> keys = this.properties.stringPropertyNames();
		for (String key : keys) {
			if ( key.startsWith(prefix) ) {
				foundValues.add(this.properties.getProperty(key));
			}
		}
		return foundValues;
	}
}