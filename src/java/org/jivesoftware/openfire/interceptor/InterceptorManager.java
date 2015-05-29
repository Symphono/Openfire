/**
 * $RCSfile$
 * $Revision: 3142 $
 * $Date: 2005-12-01 13:39:33 -0300 (Thu, 01 Dec 2005) $
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

package org.jivesoftware.openfire.interceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.xml.utils.NameSpace;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.jivesoftware.openfire.DuplicateRegistrationException;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError.Condition;


/**
 * An InterceptorManager manages the list of global interceptors and per-user
 * interceptors that are invoked before and after packets are read and sent.
 * If an interceptor is installed for a user then it will receive all packets
 * sent or received for <b>any</b> connection of that user.<p>
 *
 * PacketInterceptors that are invoked before the packet is sent or processed
 * (when read) may change the original packet or reject the packet by throwing
 * a {@link PacketRejectedException}. If the interceptor rejects a received packet
 * then the sender of the packet receive a
 * {@link org.xmpp.packet.PacketError.Condition#not_allowed not_allowed} error.
 *
 * @see PacketInterceptor
 * @author Gaston Dombiak
 */
public class InterceptorManager {

	private static final Logger Log = LoggerFactory.getLogger(InterceptorManager.class);
	
    private static InterceptorManager instance = new InterceptorManager();
    
    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    private XMPPServer server = XMPPServer.getInstance();
    private List<PacketInterceptor> globalInterceptors = new CopyOnWriteArrayList<PacketInterceptor>();
    
    private final Map<String, PacketInterceptor2> requiredInterceptorsByName = new ConcurrentHashMap<String, PacketInterceptor2>();
    private final Map<String, RequiredInterceptorDefinition> allRequiredInterceptors = new ConcurrentHashMap<String, RequiredInterceptorDefinition>();
    private final List<PacketInterceptor2> requiredInterceptors = new CopyOnWriteArrayList<PacketInterceptor2>();
        
    private Map<String, List<PacketInterceptor>> usersInterceptors = new ConcurrentHashMap<String, List<PacketInterceptor>>();
  
    private InterceptorPersistenceUtility persistenceUtility;
    
    private Set<EEventType> eventTypesToBlock;
    private Set<EPacketType> packetTypesToBlock;
    
    private InterceptorManager() {
    	persistenceUtility = new InterceptorPersistenceUtility();
    	this.allRequiredInterceptors.putAll(persistenceUtility.loadRequiredInterceptors());
    	determinePacketTypesToBlock();
    }
    

    /**
     * Returns a singleton instance of InterceptorManager.
     *
     * @return an instance of InterceptorManager.
     */
    public static InterceptorManager getInstance() {
        return instance;
    }

    /**
     * Returns an unmodifiable list of global packet interceptors. Global
     * interceptors are applied to all packets read and sent by the server.
     *
     * @return an unmodifiable list of the global packet interceptors.
     */
    public List<PacketInterceptor> getInterceptors() {
        return Collections.unmodifiableList(globalInterceptors);
    }

    /**
     * Registers a new required interceptor. If any of the required interceptors is not present, packets/events registered with this interceptor will be blocked from being processed.
     * 
     * @param interceptor - the required interceptor
     * @param name - interceptor's name
     * @param packetTypes - packet types that will be rejected if the required interceptor is not present
     * @param eventTypes - events to be rejected if the required interceptor is not present
     */    
    public void addRequiredInterceptor(PacketInterceptor2 interceptor, String name, Set<EPacketType> packetTypes, Set<EEventType> eventTypes)
    		throws DuplicateRegistrationException {
    	if(interceptor == null) {
    		throw new IllegalArgumentException("'interceptor' is a required argument");
    	}
    	if(name == null) {
    		throw new IllegalArgumentException("'name' is a required argument");
    	}
    	if(packetTypes == null) {
    		throw new IllegalArgumentException("'packetTypes' is a required argument");
    	}
    	if(eventTypes == null) {
    		throw new IllegalArgumentException("'eventTypes' is a required argument");
    	}    	
    	
    	RequiredInterceptorDefinition requiredInterceptorDefinition = new RequiredInterceptorDefinition(eventTypes, packetTypes);
    	Log.debug("Adding required interceptor: name={}, packetTypes={}, eventTypes={}", name, packetTypes, eventTypes);
    	
    	WriteLock lock = readWriteLock.writeLock();
    	lock.lock();
    	try {
	    	if(this.requiredInterceptorsByName.put(name, interceptor) != null) {
	    		throw new DuplicateRegistrationException("Required interceptor already registered with the name '" + name + "'");
	    	}
	    	this.allRequiredInterceptors.put(name, requiredInterceptorDefinition);
	    	determinePacketTypesToBlock();    	
	    	this.requiredInterceptors.add(interceptor);
     
    	} finally {
    		lock.unlock();
    	}

    	this.persistenceUtility.persistRequiredInterceptors(this.allRequiredInterceptors);
    }
    
    /**
     * Removes a required interceptor.
     * 
     * @param name - name of the interceptor to be removed
     * @param markAsNotRequired - boolean - if true, in addition to removing the interceptor, it will be permanently marked as not required.
     * 
     */
    public void removeRequiredInterceptor(String name, boolean markAsNotRequired) {
    	Log.debug("Removing required interceptor: name={}, markAsNotRequired={}", name, markAsNotRequired);
    	WriteLock lock = readWriteLock.writeLock();
    	lock.lock();
    	try {
	    	PacketInterceptor2 interceptor = this.requiredInterceptorsByName.remove(name);
	    	if(interceptor != null) {
	    		requiredInterceptors.remove(interceptor);
	    	}
	    	//it is not a required event listener anymore
	    	if(markAsNotRequired) {
	    		allRequiredInterceptors.remove(name);
	    	}    	
	    	determinePacketTypesToBlock();
    	} finally {
    		lock.unlock();
    	}
    	
    	if(markAsNotRequired) {
    		this.persistenceUtility.persistRequiredInterceptors(this.allRequiredInterceptors);
    	}
    }    
    
    /**
     * Figures out which events and packet types should be blocked based on the list of all required interceptors and interceptors
     * currently present.
     */
    private void determinePacketTypesToBlock() {
        Set<EEventType> eventTypesToBlock = new HashSet<EEventType>();
        Set<EPacketType> packetTypesToBlock = new HashSet<EPacketType>();        

    	for(Entry<String, RequiredInterceptorDefinition> definition : this.allRequiredInterceptors.entrySet()) {
    		if(!this.requiredInterceptorsByName.containsKey(definition.getKey())) {
	    		eventTypesToBlock.addAll(definition.getValue().getEventTypes());
	    		packetTypesToBlock.addAll(definition.getValue().getPacketTypes());
    		}
    	}
    	
    	if(eventTypesToBlock.contains(EEventType.All)) {
    		eventTypesToBlock.clear();
    		eventTypesToBlock.add(EEventType.All);
    	}
    	
    	if(packetTypesToBlock.contains(EPacketType.All)) {
    		packetTypesToBlock.clear();
    		packetTypesToBlock.add(EPacketType.All);
    	}
    	
    	this.eventTypesToBlock = eventTypesToBlock;
    	this.packetTypesToBlock = packetTypesToBlock;
    	
    	Log.trace("Packet types to block: eventTypesToBlock={}, packetTypesToBlock={}", eventTypesToBlock, packetTypesToBlock);
    }
 
    
    /**
     * Returns a collection of required interceptors
     * 
     * @return - an unmodifiable collection of required interceptors ({@link PacketInterceptor2})
     */
    public Collection<PacketInterceptor2> getRequiredInterceptors() {
    	return Collections.unmodifiableCollection(this.requiredInterceptors);
    }
    
    /**
     * Returns a set of {@link EEventType} event types that are being blocked because of missing required interceptors
     * 
     * @return - unmodifiable collection of {@link EEventType}
     */
    public Set<EEventType> getEventTypesToBlock() {
    	return Collections.unmodifiableSet(this.eventTypesToBlock);
    }
    
    /**
     * Returns a set of {@link EPacketType} packet types that are being blocked because of missing required interceptors
     * 
     * @return - umodifiable collection of {@link EPacketType} 
     */
    public Set<EPacketType> getPacketTypesToBlock() {
    	return Collections.unmodifiableSet(this.packetTypesToBlock);
    }
    
    /**
     * Inserts a new interceptor at the end of the list of currently configured
     * interceptors. This interceptor will be used for all the sent and received packets.
     *
     * @param interceptor the interceptor to add. 
     */
    public void addInterceptor(PacketInterceptor interceptor) {
        if (interceptor == null) {
            throw new NullPointerException("Parameter interceptor was null.");
        }
        // Remove the interceptor from the list since the position might have changed
        if (globalInterceptors.contains(interceptor)) {
            globalInterceptors.remove(interceptor);
        }
        globalInterceptors.add(interceptor);
    }

    /**
     * Inserts a new interceptor at specified index in the list of currently configured
     * interceptors. This interceptor will be used for all the sent and received packets.
     *
     * @param index the index in the list to insert the new interceptor at.
     * @param interceptor the interceptor to add.
     */
    public void addInterceptor(int index, PacketInterceptor interceptor) {
        if (index < 0 || (index > globalInterceptors.size())) {
            throw new IndexOutOfBoundsException("Index " + index + " invalid.");
        }
        if (interceptor == null) {
            throw new NullPointerException("Parameter interceptor was null.");
        }
        // Remove the interceptor from the list since the position might have changed
        if (globalInterceptors.contains(interceptor)) {
            int oldIndex = globalInterceptors.indexOf(interceptor);
            if (oldIndex < index) {
                index -= 1;
            }
            globalInterceptors.remove(interceptor);
        }

        globalInterceptors.add(index, interceptor);
    }

    /**
     * Removes the global interceptor from the list.
     *
     * @param interceptor the interceptor to remove.
     * @return true if the item was present in the list
     */
    public boolean removeInterceptor(PacketInterceptor interceptor) {
        return globalInterceptors.remove(interceptor);
    }

    /**
     * Returns an unmodifable list of packet interceptors that are related to the
     * specified username.
     *
     * @param username the name of the user.
     * @return an unmodifiable list of packet interceptors that are related to
     *      the specified username.
     */
    public List<PacketInterceptor> getUserInterceptors(String username) {
        List<PacketInterceptor> userInterceptors = usersInterceptors.get(username);
        if (userInterceptors == null) {
            return Collections.emptyList();
        }
        else {
            return Collections.unmodifiableList(userInterceptors);
        }
    }

    /**
     * Inserts a new interceptor at specified index in the list of currently configured
     * interceptors for a specific username. This interceptor will be used only when a packet
     * was sent or received by the specified username.
     *
     * @param username the name of the user.
     * @param index the index in the list to insert the new interceptor at.
     * @param interceptor the interceptor to add.
     */
    public void addUserInterceptor(String username, int index, PacketInterceptor interceptor) {
        List<PacketInterceptor> userInterceptors = usersInterceptors.get(username);
        if (userInterceptors == null) {
            userInterceptors = new CopyOnWriteArrayList<PacketInterceptor>();
            usersInterceptors.put(username, userInterceptors);
        }
        else {
            if (index < 0 || (index > userInterceptors.size())) {
                throw new IndexOutOfBoundsException("Index " + index + " invalid.");
            }
            if (interceptor == null) {
                throw new NullPointerException("Parameter interceptor was null.");
            }

            // Remove the interceptor from the list since the position might have changed
            if (userInterceptors.contains(interceptor)) {
                int oldIndex = userInterceptors.indexOf(interceptor);
                if (oldIndex < index) {
                    index -= 1;
                }
                userInterceptors.remove(interceptor);
            }
        }
        userInterceptors.add(index, interceptor);
    }

    /**
     * Removes the interceptor from the list of interceptors that are related to a specific
     * username.
     *
     * @param username the name of the user.
     * @param interceptor the interceptor to remove.
     * @return true if the item was present in the list
     */
    public boolean removeUserInterceptor(String username, PacketInterceptor interceptor) {
        boolean answer = false;
        List<PacketInterceptor> userInterceptors = usersInterceptors.get(username);
        if (userInterceptors != null) {
            answer = userInterceptors.remove(interceptor);
            // Remove the entry for this username if the list is now empty
            if (userInterceptors.isEmpty()) {
                usersInterceptors.remove(username);
            }
        }
        return answer;
    }

    /**
     * Invokes all currently-installed interceptors on the specified packet.
     * All required and global interceptors will be invoked as well as interceptors that
     * are related to the address of the session that received or is sending
     * the packet.<p>
     *
     * Interceptors are executed before and after processing an incoming packet
     * and sending a packet to a user. This means that interceptors are able to alter or
     * reject packets before they are processed further. If possible, interceptors
     * should perform their work in a short time so that overall performance is not
     * compromised.
     *
     * @param packet the packet that has been read or is about to be sent.
     * @param session the session that received the packet or that the packet
     *      will be sent to.
     * @param read true indicates that the packet was read. When false, the packet
     *      is being sent to a user.
     * @param processed true if the packet has already processed (incoming or outgoing).
     *      If the packet hasn't already been processed, this flag will be false.
     * @throws PacketRejectedException if the packet should be prevented from being processed.
     */
    public void invokeInterceptors(Packet packet, Session session, boolean read, boolean processed)
            throws PacketRejectedException
    {
    	checkForRequiredInterceptors(packet, read, processed);

    	if(!requiredInterceptors.isEmpty()) {
    		
            for (PacketInterceptor2 requiredInterceptor : requiredInterceptors) {
                try {
                    if(requiredInterceptor.interceptPacket2(packet, session, read, processed)) {
                    	return;
                    }
                }
                catch (PacketRejectedException e) {
                    if (processed) {
                        Log.error("Post interceptor cannot reject packet.", e);
                    }
                    else {
                        // Throw this exception since we don't really want to catch it
                        throw e;
                    }
                }
                catch (Throwable e) {
                    Log.error("Error in required interceptor: " + requiredInterceptor + " while intercepting: " + packet, e);
                }
            }    		
    	}

        // Invoke the global interceptors for this packet
        // Checking if collection is empty to prevent creating an iterator of
        // a CopyOnWriteArrayList that is an expensive operation
    	if (!globalInterceptors.isEmpty()) {
            for (PacketInterceptor interceptor : globalInterceptors) {
                try {
                    interceptor.interceptPacket(packet, session, read, processed);
                }
                catch (PacketRejectedException e) {
                    if (processed) {
                        Log.error("Post interceptor cannot reject packet.", e);
                    }
                    else {
                        // Throw this exception since we don't really want to catch it
                        throw e;
                    }
                }
                catch (Throwable e) {
                    Log.error("Error in interceptor: " + interceptor + " while intercepting: " + packet, e);
                }
            }
        }
        // Invoke the interceptors that are related to the address of the session
        if (usersInterceptors.isEmpty()) {
            // Do nothing
            return;
        }
        String username = session.getAddress().getNode();
        if (username != null && server.isLocal(session.getAddress())) {
            Collection<PacketInterceptor> userInterceptors = usersInterceptors.get(username);
            if (userInterceptors != null && !userInterceptors.isEmpty()) {
                for (PacketInterceptor interceptor : userInterceptors) {
                    try {
                        interceptor.interceptPacket(packet, session, read, processed);
                    }
                    catch (PacketRejectedException e) {
                        if (processed) {
                            Log.error("Post interceptor cannot reject packet.", e);
                        }
                        else {
                            // Throw this exception since we don't really want to catch it
                            throw e;
                        }
                    }
                    catch (Throwable e) {
                        Log.error("Error in interceptor: " + interceptor + " while intercepting: " + packet, e);
                    }
                }
            }
        }
    }
    
    /**
     * Checks if the {@link Packet} packet should be blocked because of required interceptors not present.
     * 
     * @param packet - packet that's being processed
     * @param read true indicates that the packet was read. When false, the packet
     *      is being sent to a user.
     * @param processed true if the packet has already processed (incoming or outgoing).
     *      If the packet hasn't already been processed, this flag will be false.
     * @throws PacketRejectedException - thrown when required interceptors are not present and a packet has to be blocked
     */
    private void checkForRequiredInterceptors(Packet packet, boolean read, boolean processed) throws PacketRejectedException {
    	boolean shouldBlock = false;
    	Log.trace("Checking for required interceptors: packet={}, read={}, processed={}, eventTypesToBlock={}, packetTypesToBlock={}", packet.getClass().getSimpleName(), read, processed, eventTypesToBlock, packetTypesToBlock);
    	if (this.eventTypesToBlock.isEmpty() && this.packetTypesToBlock.isEmpty()) {
    		return;
    	}
    	
    	if (!this.eventTypesToBlock.isEmpty() && !this.packetTypesToBlock.isEmpty()){
    		shouldBlock = shouldBlockEvent(processed, read) && shouldBlockPacket(packet);
    	}
    	else if(!this.eventTypesToBlock.isEmpty()) {
    		shouldBlock = shouldBlockEvent(processed, read);
    	}
    	else if(!this.packetTypesToBlock.isEmpty()) {
    		shouldBlock = shouldBlockPacket(packet);
    	}
    	
    	if(shouldBlock) {
    		Log.trace("Rejecting this packet");
    		PacketRejectedException exception = new PacketRejectedException();
    		//if it's just a chat state notification, then simply ignore it and don't send back an error message
    		if (!isChatStateMessage(packet)){
	    		exception.setErrorCondition(Condition.not_allowed);
	        	exception.setErrorText(packet.getClass().getSimpleName() + " rejected due to a required component that's not present. Contact your system administrator.");
	        	if (packet instanceof Message){
	        		exception.setRejectionMessage(exception.getErrorText());
	        	}
    		}
        	throw exception;
    	}
    }
        
    private boolean isChatStateMessage(Packet packet){
    	if (!(packet instanceof Message) || packet.getElement().nodeCount() > 1){
    		return false;
    	}
    	Object next = packet.getElement().elementIterator().next();
    	return next instanceof Element && ((Element)next).getNamespaceURI().equalsIgnoreCase("http://jabber.org/protocol/chatstates");
    }
    
    /**
     * Checks against a list of event types if, based on both flags, this event should be blocked
     * 
    * @param read true indicates that the packet was read. When false, the packet
     *      is being sent to a user.
     * @param processed true if the packet has already processed (incoming or outgoing).
     *      If the packet hasn't already been processed, this flag will be false.
     * 
     * @return - boolean - returns true if this event should be blocked
     */
    private boolean shouldBlockEvent(boolean processed, boolean read) {

    	boolean result = false;
    	Log.trace("Should the event be blocked? processed={}, read={}, eventTypesToBlock={}", processed, read, eventTypesToBlock);
    	
    	if(this.eventTypesToBlock.contains(EEventType.All)) {
    		return true;
    	}
    	
	   	 if(processed && this.eventTypesToBlock.contains(EEventType.Processed)) {
			 result = true;
		 }
		 else if (!processed && this.eventTypesToBlock.contains(EEventType.Unprocessed)) {
			 result = true;
		 }
		 else if (read && this.eventTypesToBlock.contains(EEventType.Incoming)) {
			 result = true;
		 }
		 else if (!read && this.eventTypesToBlock.contains(EEventType.Outgoing)) {
			 result = true;
		 }    	
    	
    	return result;
    }
    
    /**
     * Checks against a list of packets if this packet has to be blocked
     * 
     * @param packet - {@link Packet} - packet to check
     * 
     * @return - boolean- returns true if this packet should be blocked
     */
    private boolean shouldBlockPacket(Packet packet) {
  	
    	boolean result = false;
    	
    	if(this.packetTypesToBlock.contains(EPacketType.All)) {
    		return true;
    	}
    	
    	if(packet instanceof org.xmpp.packet.Roster && this.packetTypesToBlock.contains(EPacketType.Roster)) {
    		result = true;
    	}
    	else if (packet instanceof org.xmpp.packet.IQ && this.packetTypesToBlock.contains(EPacketType.IQ)) {
    		result = true;
    	}
    	else if (packet instanceof org.xmpp.packet.Presence && this.packetTypesToBlock.contains(EPacketType.Presence)) {
    		result = true;
    	}
    	else if (packet instanceof org.xmpp.packet.Message && this.packetTypesToBlock.contains(EPacketType.Message)) {
    		result = true;
    	}
    	
    	return result;
    }
}