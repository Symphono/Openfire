package org.jivesoftware.openfire.interceptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterceptorPersistenceUtility {

	private static final Logger Log = LoggerFactory.getLogger(InterceptorPersistenceUtility.class);
	
	String REQUIRED_INTERCEPTORS_PROPERTY = "interceptors.required";
	String BLOCKING_INTERCEPTOR_PROPERTY = "interceptor.blocking";
	String BLOCKING_INTERCEPTOR_TYPE = BLOCKING_INTERCEPTOR_PROPERTY + "." + "type";
	String BLOCKING_INTERCEPTOR_EVENT = BLOCKING_INTERCEPTOR_PROPERTY + "." + "event";
	
	
	public synchronized void persistRequiredInterceptors(Map<String, RequiredInterceptorDefinition> requiredInterceptors) {
		Map<String, String> properties = new HashMap<String, String>();
		String delim = "";
		StringBuilder sb = new StringBuilder();
		
		for (String interceptor : requiredInterceptors.keySet()) {
			sb.append(delim).append(interceptor);
			delim = ",";

			RequiredInterceptorDefinition interceptorDefinition = requiredInterceptors.get(interceptor);
			
			String joinedEventTypes = interceptorDefinition.getEventTypes().contains(EEventType.All) ? 
					EEventType.All.toString() : join(interceptorDefinition.getEventTypes());
			 
			if(!joinedEventTypes.isEmpty()) {
				properties.put(BLOCKING_INTERCEPTOR_EVENT + "." + interceptor, joinedEventTypes);	
			}
			
			String joinedPacketTypes = interceptorDefinition.getPacketTypes().contains(EPacketType.All) ? 
					EPacketType.All.toString() : join(interceptorDefinition.getPacketTypes());
					
			if(!joinedPacketTypes.isEmpty()) {
				properties.put(BLOCKING_INTERCEPTOR_TYPE + "." + interceptor, joinedPacketTypes);
			}
		}

		String requiredInterceptorsValue = sb.toString().trim();
		if(!requiredInterceptorsValue.isEmpty() && !properties.isEmpty()) {
			properties.put(REQUIRED_INTERCEPTORS_PROPERTY, requiredInterceptorsValue);
			JiveGlobals.setProperties(properties);
		}
	}

	private <T> String join(Collection<T> items) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for(T item : items) {
			sb.append(delim).append(item);
			delim = ",";
		}

		return sb.toString();
	}

	public Map<String, RequiredInterceptorDefinition> loadRequiredInterceptors() {
		Map<String, RequiredInterceptorDefinition> requiredInterceptorsMap = new HashMap<String, RequiredInterceptorDefinition>();
		String requiredInterceptors = JiveGlobals.getProperty(REQUIRED_INTERCEPTORS_PROPERTY);
		
		if(requiredInterceptors != null) {
			String[] interceptorArray = requiredInterceptors.split(",");
			
			for (int i = 0; i < interceptorArray.length; i++) {
				String interceptor = interceptorArray[i] != null ? interceptorArray[i].trim() : "";
				if(interceptor.isEmpty()) {
					continue;
				}
				
				String blockingTypes = JiveGlobals.getProperty(BLOCKING_INTERCEPTOR_TYPE + "." + interceptor);
				Set<EPacketType> packetTypes = new HashSet<EPacketType>();
				
				if (blockingTypes != null) {
					String[] blockingTypesArray = blockingTypes.split(",");
					
					for (int j = 0; j < blockingTypesArray.length; j++) {
						String type = blockingTypesArray[j] != null ? blockingTypesArray[j].trim() : "";
						if(type.isEmpty()) {
							continue;
						}
						
						EPacketType packetType = convertToPacketType(type);
						if(packetType == null) {
							continue;
						}
						
						if (packetType == EPacketType.All) {
							packetTypes.clear();
							packetTypes.add(packetType);
							break;
						}
						packetTypes.add(packetType);
					}
				}
	
				String blockingEvents = JiveGlobals.getProperty(BLOCKING_INTERCEPTOR_EVENT + "." + interceptor);
				
				Set<EEventType> eventTypes = new HashSet<EEventType>();
				
				if (blockingEvents != null) {
					String[] blockingEventsArray = blockingEvents.split(",");
					
					for (int j = 0; j < blockingEventsArray.length; j++) {
						String event = blockingEventsArray[j] != null ? blockingEventsArray[j].trim() : "";
						if(event.isEmpty()) {
							continue;
						}
						
						EEventType eventType = convertToEventType(event);
						if(eventType == null) {
							continue;
						}
						if (eventType == EEventType.All) {
							eventTypes.clear();
							eventTypes.add(eventType);
							break;
						}
						eventTypes.add(eventType);
					}
				}
	
				requiredInterceptorsMap.put(interceptor, new RequiredInterceptorDefinition(eventTypes, packetTypes));
			}
		}
		return requiredInterceptorsMap;
	}
	
	private EEventType convertToEventType(String eventType) {
		EEventType eEvent = null;
		try {
			eEvent = EEventType.valueOf(eventType);
		} catch(IllegalArgumentException ia) {
			Log.warn("{} is not a valid EEventType", eventType);
		}
		
		return eEvent;
	}
	
	private EPacketType convertToPacketType(String packet) {
		EPacketType ePacket = null;
		try {
			ePacket = EPacketType.valueOf(packet);
		} catch(IllegalArgumentException ia) {
			Log.warn("{} is not a valid EPacketType", packet);
		}
		
		return ePacket;
	}	
}
