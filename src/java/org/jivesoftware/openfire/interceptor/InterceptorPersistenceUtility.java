package org.jivesoftware.openfire.interceptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterceptorPersistenceUtility {

	private static final Logger Log = LoggerFactory.getLogger(InterceptorPersistenceUtility.class);
	private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	String REQUIRED_INTERCEPTORS_PROPERTY = "interceptors.required";
	String BLOCKING_INTERCEPTOR_PROPERTY = "interceptor.blocking";
	String BLOCKING_INTERCEPTOR_TYPE = BLOCKING_INTERCEPTOR_PROPERTY + "." + "type";
	String BLOCKING_INTERCEPTOR_EVENT = BLOCKING_INTERCEPTOR_PROPERTY + "." + "event";
	
	
	public void persistRequiredInterceptors(final Map<String, RequiredInterceptorDefinition> requiredInterceptors) {
		final WriteLock lock = readWriteLock.writeLock();
		lock.lock();
		try {
			final Map<String, String> properties = new HashMap<String, String>();
			String delim = "";
			final StringBuilder sb = new StringBuilder();
			
			for (final String interceptor : requiredInterceptors.keySet()) {
				sb.append(delim).append(interceptor);
				delim = ",";
	
				final RequiredInterceptorDefinition interceptorDefinition = requiredInterceptors.get(interceptor);
				
				final String joinedEventTypes = interceptorDefinition.getEventTypes().contains(EEventType.All) ? 
						EEventType.All.name() : join(interceptorDefinition.getEventTypes());
				 
				if(!joinedEventTypes.isEmpty()) {
					properties.put(BLOCKING_INTERCEPTOR_EVENT + "." + interceptor, joinedEventTypes);	
				}
				
				final String joinedPacketTypes = interceptorDefinition.getPacketTypes().contains(EPacketType.All) ? 
						EPacketType.All.toString() : join(interceptorDefinition.getPacketTypes());
						
				if(!joinedPacketTypes.isEmpty()) {
					properties.put(BLOCKING_INTERCEPTOR_TYPE + "." + interceptor, joinedPacketTypes);
				}
			}
	
			final String requiredInterceptorsValue = sb.toString().trim();
			if(!requiredInterceptorsValue.isEmpty() && !properties.isEmpty()) {
				properties.put(REQUIRED_INTERCEPTORS_PROPERTY, requiredInterceptorsValue);
				JiveGlobals.setProperties(properties);
				Log.debug("Setting required interceptors properties: {}", properties);
			} else {
				JiveGlobals.deleteProperty(REQUIRED_INTERCEPTORS_PROPERTY);
				JiveGlobals.deleteProperty(BLOCKING_INTERCEPTOR_TYPE);
				JiveGlobals.deleteProperty(BLOCKING_INTERCEPTOR_EVENT);
				Log.debug("Deleted required interceptor configs");
			}
		} finally {
			lock.unlock();
		}
	}

	private <T> String join(final Collection<T> items) {
		final StringBuilder sb = new StringBuilder();
		String delim = "";
		for(final T item : items) {
			sb.append(delim).append(item);
			delim = ",";
		}

		return sb.toString();
	}

	public Map<String, RequiredInterceptorDefinition> loadRequiredInterceptors() {
		final Map<String, RequiredInterceptorDefinition> requiredInterceptorsMap = new HashMap<String, RequiredInterceptorDefinition>();
		
		final ReadLock lock = readWriteLock.readLock();
		lock.lock();
		try {
			final String requiredInterceptors = JiveGlobals.getProperty(REQUIRED_INTERCEPTORS_PROPERTY);
			Log.debug("Loaded required interceptors: {}", requiredInterceptors);
			if(requiredInterceptors != null) {
				final String[] interceptorArray = requiredInterceptors.split(",");
				
				for (int i = 0; i < interceptorArray.length; i++) {
					final String interceptor = interceptorArray[i] != null ? interceptorArray[i].trim() : "";
					if(interceptor.isEmpty()) {
						continue;
					}
					
					final String blockingTypes = JiveGlobals.getProperty(BLOCKING_INTERCEPTOR_TYPE + "." + interceptor);
					final Set<EPacketType> packetTypes = new HashSet<EPacketType>();
					
					if (blockingTypes != null) {
						final String[] blockingTypesArray = blockingTypes.split(",");
						
						for (int j = 0; j < blockingTypesArray.length; j++) {
							final String type = blockingTypesArray[j] != null ? blockingTypesArray[j].trim() : "";
							if(type.isEmpty()) {
								continue;
							}
							
							final EPacketType packetType = convertToPacketType(type);
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
		
					final String blockingEvents = JiveGlobals.getProperty(BLOCKING_INTERCEPTOR_EVENT + "." + interceptor);
					
					final Set<EEventType> eventTypes = new HashSet<EEventType>();
					
					if (blockingEvents != null) {
						final String[] blockingEventsArray = blockingEvents.split(",");
						
						for (int j = 0; j < blockingEventsArray.length; j++) {
							final String event = blockingEventsArray[j] != null ? blockingEventsArray[j].trim() : "";
							if(event.isEmpty()) {
								continue;
							}
							
							final EEventType eventType = convertToEventType(event);
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
		
					Log.debug("Processed required interceptor config for {}: eventTypes={}, packetTypes={}", interceptor, eventTypes, packetTypes);
					requiredInterceptorsMap.put(interceptor, new RequiredInterceptorDefinition(eventTypes, packetTypes));
				}
			}
		} finally {
			lock.unlock();
		}
		return requiredInterceptorsMap;
	}
	
	private EEventType convertToEventType(final String eventType) {
		EEventType eEvent = null;
		try {
			eEvent = EEventType.valueOf(eventType);
		} catch(final IllegalArgumentException ia) {
			Log.warn("{} is not a valid EEventType", eventType);
		}
		
		return eEvent;
	}
	
	private EPacketType convertToPacketType(final String packet) {
		EPacketType ePacket = null;
		try {
			ePacket = EPacketType.valueOf(packet);
		} catch(final IllegalArgumentException ia) {
			Log.warn("{} is not a valid EPacketType", packet);
		}
		
		return ePacket;
	}	
}
