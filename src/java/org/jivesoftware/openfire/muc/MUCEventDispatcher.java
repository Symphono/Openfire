/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.openfire.muc;

import org.jivesoftware.openfire.DuplicateRegistrationException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Dispatches MUC events. The following events are supported:
 * <ul>
 * <li><b>occupantJoined</b> --&gt; Someone joined a room.</li>
 * <li><b>occupantLeft</b> --&gt; Someone left a room.</li>
 * <li><b>nicknameChanged</b> --&gt; A nickname was changed in a room.</li>
 * <li><b>messageReceived</b> --&gt; A message was received in a room.</li>
 * <li><b>roomCreated</b> --&gt; A room was created.</li>
 * <li><b>roomDestroyed</b> --&gt; A room was destroyed.</li>
 * </ul>
 * Use {@link #addListener(MUCEventListener)} and {@link #removeListener(MUCEventListener)}
 * to add or remove {@link MUCEventListener}.
 *
 * @author Daniel Henninger
 */
public class MUCEventDispatcher {
	
    private static Collection<MUCEventListener> listeners = new ConcurrentLinkedQueue<MUCEventListener>();
    
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    private static final MUCListenerPersistenceUtility persistenceUtility;
    private static Map<String, MUCEventListener2> requiredListenersByName = new ConcurrentHashMap<String, MUCEventListener2>();
    private static Collection<MUCEventListener2> requiredListeners = new ConcurrentLinkedQueue<MUCEventListener2>();
    private static Map<String, Set<EMUCEventType>> allRequiredListeners;
    private static Set<EMUCEventType> eventTypesToBlock;
  
    static {
    	persistenceUtility = new MUCListenerPersistenceUtility();
    	allRequiredListeners = persistenceUtility.loadRequiredListeners();
    	determineEventsToBlock();
    }

    public static void addRequiredListener(MUCEventListener2 listener, String name, Set<EMUCEventType> eventTypes) {
    	if(listener == null) {
    		throw new IllegalArgumentException("'listener' is a required argument");
    	}
    	if(name == null) {
    		throw new IllegalArgumentException("'name' is a required argument");
    	}
    	if(eventTypes == null) {
    		throw new IllegalArgumentException("'eventTypes' is a required argument");
    	}
    	
    	WriteLock lock = readWriteLock.writeLock();
    	lock.lock();
    	try {
	    	if(requiredListenersByName.put(name, listener) != null) {
	    		throw new DuplicateRegistrationException("Required listener already registered with the name '" + name + "'");
	    	}
	    	
	    	allRequiredListeners.put(name, eventTypes);
	    	determineEventsToBlock();
	    	requiredListeners.add(listener);
	    	addListener(listener);
    	} finally {
    		lock.unlock();
    	}
    	
    	persistenceUtility.persistRequiredListeners(allRequiredListeners);
    }

    public static void removeRequiredListener(String name, boolean markAsNotRequired) {
    	synchronized(MUCEventDispatcher.class) {
	    	MUCEventListener2 listener = requiredListenersByName.remove(name);
	    	if(listener != null) {
	    		requiredListeners.remove(listener);
	    		removeListener(listener);
	    	}
    	    	
	    	//it is not a required event listener anymore
	    	if(markAsNotRequired) {
	    		allRequiredListeners.remove(name);
	    	}    	
	    	determineEventsToBlock();
    	}
    	
    	if(markAsNotRequired) {    		
    		persistenceUtility.persistRequiredListeners(allRequiredListeners);
    	}
    }
    
    public static Collection<MUCEventListener2> getRequiredListeners() {
    	return Collections.unmodifiableCollection(requiredListeners);
    }
    
    public static Set<EMUCEventType> getEventsToBlock() {
    	return Collections.unmodifiableSet(eventTypesToBlock);
    }
    
    public static void addListener(MUCEventListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(MUCEventListener listener) {
        listeners.remove(listener);
    }
    
    public static boolean beforeOccupantJoined(JID roomJID, JID user, String nickname) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeJoined)) {
	        for (MUCEventListener2 listener : requiredListeners) {
	        	if(listener.beforeOccupantJoined(roomJID, user, nickname)) {
		        	result = true;
		        	break;
	        	}
	        } 
    	}
    	
    	return result;
    }

    public static void occupantJoined(JID roomJID, JID user, String nickname) {
        for (MUCEventListener listener : listeners) {
            listener.occupantJoined(roomJID, user, nickname);
        }
    }
    
    public static void occupantLeft(JID roomJID, JID user) {
        for (MUCEventListener listener : listeners) {
            listener.occupantLeft(roomJID, user);
        }
    }
    
    public static boolean beforeNicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeNickChanged)) {
	    	for (MUCEventListener2 listener : requiredListeners) {    		
	            if(listener.beforeNicknameChanged(roomJID, user, oldNickname, newNickname)) {
	            	result = true;
	            	break;
	            }
	    	}
    	}
    	
    	return result;
    }

    public static void nicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) {
        for (MUCEventListener listener : listeners) {
            listener.nicknameChanged(roomJID, user, oldNickname, newNickname);
        }
    }

    public static boolean beforeMessageReceived(JID roomJID, JID user, String nickname, Message message) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeMessageReceived)) {
	    	for (MUCEventListener2 listener : requiredListeners) {
	            if(listener.beforeMessageReceived(roomJID, user, nickname, message)) {
	            	result = true;
	            	break;
	            }
	        }
    	}
    	
    	return result;
    }
    
    public static void messageReceived(JID roomJID, JID user, String nickname, Message message) {
        for (MUCEventListener listener : listeners) {
            listener.messageReceived(roomJID, user, nickname, message);
        }
    }

    public static boolean beforePrivateMessageRecieved(JID toJID, JID fromJID, Message message) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforePrivateMessageReceived)) {
	        for (MUCEventListener2 listener : requiredListeners) {
	            if(listener.beforePrivateMessageRecieved(toJID, fromJID, message)) {
	            	result = true;
	            	break;
	            }
	        }
    	}
    	
    	return result;
    }
    
    public static void privateMessageRecieved(JID toJID, JID fromJID, Message message) {
        for (MUCEventListener listener : listeners) {
            listener.privateMessageRecieved(toJID, fromJID, message);
        }
    }

    public static boolean beforeRoomCreated(JID roomJID, JID userJID) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeCreated)) {
	        for (MUCEventListener2 listener : requiredListeners) {
	            if(listener.beforeRoomCreated(roomJID, userJID)) {
	            	result = true;
	            	break;
	            }
	        }
    	}
    	
    	return result;
    }
    
    public static void roomCreated(JID roomJID) {
        for (MUCEventListener listener : listeners) {
            listener.roomCreated(roomJID);
        }
    }

    public static boolean beforeRoomDestroyed(JID roomJID) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeDestroyed)) {
	        for (MUCEventListener2 listener : requiredListeners) {
	            if(listener.beforeRoomDestroyed(roomJID)) {
	            	result = true;
	            	break;
	            }
	        }
    	}
    	
    	return result;
    }
    
    public static void roomDestroyed(JID roomJID) {
    	for (MUCEventListener listener : listeners) {
            listener.roomDestroyed(roomJID);
        }
    }
    
    public static boolean beforeRoomSubjectChanged(JID roomJID, JID user, String newSubject) throws MUCEventRejectedException {
    	boolean result = false;
    	
    	if(isEventAllowed(EMUCEventType.BeforeSubjectChanged)) {
	        for (MUCEventListener2 listener : requiredListeners) {
	            if(listener.beforeRoomSubjectChanged(roomJID, user, newSubject)) {
	            	result = true;
	            	break;
	            }
	        }
    	}
    	
    	return result;
    }

    public static void roomSubjectChanged(JID roomJID, JID user, String newSubject) {
    	for (MUCEventListener listener : listeners) {
            listener.roomSubjectChanged(roomJID, user, newSubject);
        }
    }
    
    private static void determineEventsToBlock() {
        Set<EMUCEventType> toBlock = new HashSet<EMUCEventType>();    	

    	for(Entry<String, Set<EMUCEventType>> entry : allRequiredListeners.entrySet()) {
    		if(!requiredListenersByName.containsKey(entry.getKey())) {
    			//found a listener that's required but not present
    			toBlock.addAll(entry.getValue()); 
    		}
    	}
    	
    	eventTypesToBlock = new HashSet<EMUCEventType>(toBlock);
    }
    
    private static boolean isEventAllowed(EMUCEventType eventType) {
    	return !eventTypesToBlock.contains(eventType);
    }

}
