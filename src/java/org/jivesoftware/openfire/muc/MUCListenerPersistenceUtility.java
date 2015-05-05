package org.jivesoftware.openfire.muc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveGlobals;

public class MUCListenerPersistenceUtility {

	String REQUIRED_LISTENERS_PROPERTY = "muc.listeners.required";
	String BLOCKING_LISTENER_EVENT =  "muc.listener.blocking.event";

	public synchronized void persistRequiredListeners(Map<String, Set<EMUCEventType>> requiredListeners) {
		Map<String, String> properties = new HashMap<String, String>();
		String delim = "";
		StringBuilder sb = new StringBuilder();
		
		for (String listener : requiredListeners.keySet()) {
			sb.append(delim).append(listener);
			delim = ",";

			Set<EMUCEventType> eventTypeSet = requiredListeners.get(listener);
			
			String joinedEventTypes = join(eventTypeSet);
			 
			if(!joinedEventTypes.isEmpty()) {
				properties.put(BLOCKING_LISTENER_EVENT + "." + listener, joinedEventTypes);	
			}
		}

		String requiredListenerValue = sb.toString().trim();
		if(!requiredListenerValue.isEmpty() && !properties.isEmpty()) {
			properties.put(REQUIRED_LISTENERS_PROPERTY, requiredListenerValue);
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

	public Map<String, Set<EMUCEventType>> loadRequiredListeners() {
		Map<String, Set<EMUCEventType>> requiredMUCListeners = new HashMap<String, Set<EMUCEventType>>();
		String requiredListeners = JiveGlobals.getProperty(REQUIRED_LISTENERS_PROPERTY);
		
		if(requiredListeners != null) {
			String[] listenersArray = requiredListeners.split(",");
			
			for (int i = 0; i < listenersArray.length; i++) {
				String listener = listenersArray[i] != null ? listenersArray[i].trim() : "";
				if(listener.isEmpty()) {
					continue;
				}

				String blockingEvents = JiveGlobals.getProperty(BLOCKING_LISTENER_EVENT + "." + listener);
				
				Set<EMUCEventType> eventTypes = new HashSet<EMUCEventType>();
				
				if (blockingEvents != null) {
					String[] blockingEventsArray = blockingEvents.split(",");
					
					for (int j = 0; j < blockingEventsArray.length; j++) {
						String event = blockingEventsArray[j] != null ? blockingEventsArray[j].trim() : "";
						if(event.isEmpty()) {
							continue;
						}
						
						EMUCEventType eventType = EMUCEventType.fromString(event);
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
}
