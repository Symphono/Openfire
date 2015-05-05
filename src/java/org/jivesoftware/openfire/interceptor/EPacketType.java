package org.jivesoftware.openfire.interceptor;

import java.util.HashMap;
import java.util.Map;

public enum EPacketType {
	All("All"),
	Presence("Presence"),
	IQ("IQ"),
	Message("Message"),
	Roster("Roster");
	
	private static final Map<String, EPacketType> packetMap = new HashMap<String, EPacketType>();
	private String packetTypeName;

    static {
        for (EPacketType event : EPacketType.values())
        	packetMap.put(event.getEventName(), event);
    }
	
    private EPacketType(String name) {
    	this.packetTypeName = name;
    }
	
    public String getEventName() {
    	return this.packetTypeName;
    }
	public static EPacketType fromString(String name) {
		return packetMap.get(name);
	}
}
