/*
 * Copyright (c) 2020 Erik Nilsson, software at versionstudio dot com
 * License: https://github.com/versionstudio/uad2midi/blob/main/LICENSE
 */
package com.versionstudio.uad2midi.uad;

import java.util.Map;

public class DeviceData {
    private Map<String,Object> children;
    private Map<String,DeviceProperty> properties;

    public Map<String,Object> getChildren() {
        return this.children;
    }

    public Map<String,DeviceProperty> getProperties() {
        return this.properties;
    }
}