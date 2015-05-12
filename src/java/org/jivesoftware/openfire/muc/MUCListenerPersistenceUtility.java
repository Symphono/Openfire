package org.jivesoftware.openfire.muc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MUCListenerPersistenceUtility {
	
	private static final Logger Log = LoggerFactory.getLogger(MUCListenerPersistenceUtility.class);

	String REQUIRED_LISTENERS_PROPERTY = "muc.listeners.required";
	String BLOCKING_LISTENER_EVENT =  "muc.listener.blocking.event";

	public synchronized void persistRequiredListeners(final Map<String, Set<EMUCEventType>> requiredListeners) {
		final Map<String, String> properties = new HashMap<String, String>();
		String delim = "";
		final StringBuilder sb = new StringBuilder();
		
		for (final String listener : requiredListeners.keySet()) {
			sb.append(delim).append(listener);
			delim = ",";

			final Set<EMUCEventType> eventTypeSet = requiredListeners.get(listener);
			
			final String joinedEventTypes = join(eventTypeSet);
			 
			if(!joinedEventTypes.isEmpty()) {
				properties.put(BLOCKING_LISTENER_EVENT + "." + listener, joinedEventTypes);	
			}
		}

		final String requiredListenerValue = sb.toString().trim();
		if(!requiredListenerValue.isEmpty() && !properties.isEmpty()) {
			properties.put(REQUIRED_LISTENERS_PROPERTY, requiredListenerValue);
			JiveGlobals.setProperties(properties);
		} else {
			JiveGlobals.deleteProperty(REQUIRED_LISTENERS_PROPERTY);
			JiveGlobals.deleteProperty(BLOCKING_LISTENER_EVENT);
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

	public Map<String, Set<EMUCEventType>> loadRequiredListeners() {
		final Map<String, Set<EMUCEventType>> requiredMUCListeners = new HashMap<String, Set<EMUCEventType>>();
		final String requiredListeners = JiveGlobals.getProperty(REQUIRED_LISTENERS_PROPERTY);
		
		if(requiredListeners != null) {
			final String[] listenersArray = requiredListeners.split(",");
			
			for (int i = 0; i < listenersArray.length; i++) {
				final String listener = listenersArray[i] != null ? listenersArray[i].trim() : "";
				if(listener.isEmpty()) {
					continue;
				}

				final String blockingEvents = JiveGlobals.getProperty(BLOCKING_LISTENER_EVENT + "." + listener);
				
				final Set<EMUCEventType> eventTypes = new HashSet<EMUCEventType>();
				
				if (blockingEvents != null) {
					final String[] blockingEventsArray = blockingEvents.split(",");
					
					for (int j = 0; j < blockingEventsArray.length; j++) {
						final String event = blockingEventsArray[j] != null ? blockingEventsArray[j].trim() : "";
						if(event.isEmpty()) {
							continue;
						}
						
						final EMUCEventType eventType = convertToMUCEventType(event);
						if(eventType == null) {
							continue;
						}
						
						eventTypes.add(eventType);
					}
				}
	
				requiredMUCListeners.put(listener, eventTypes);
			}
		}
		return requiredMUCListeners;
	}
	
	private EMUCEventType convertToMUCEventType(final String eventType) {
		EMUCEventType eEvent = null;
		try {
			eEvent = EMUCEventType.valueOf(eventType);
		} catch(final IllegalArgumentException ia) {
			Log.warn("{} is not a valid EMUCEventType", eventType);
		}
		
		return eEvent;
	}
}
