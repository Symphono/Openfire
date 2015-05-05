package org.jivesoftware.openfire.interceptor;

import java.util.Set;

public class RequiredInterceptorDefinition {
	private final Set<EEventType> eventTypes;
	private final Set<EPacketType> packetTypes;
	
	public RequiredInterceptorDefinition(Set<EEventType> eventTypes, Set<EPacketType> packetTypes) {
		this.eventTypes = eventTypes;
		this.packetTypes = packetTypes;
	}
	
	public Set<EEventType> getEventTypes() {
		return this.eventTypes;
	}
	
	public Set<EPacketType> getPacketTypes() {
		return this.packetTypes;
	}
}
