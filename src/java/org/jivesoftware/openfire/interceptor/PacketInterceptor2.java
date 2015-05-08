package org.jivesoftware.openfire.interceptor;

import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Packet;


/**
 * A required packet interceptor encapsulates an action that is invoked on a packet immediately
 * before or after it was received by a SocketReader and also when the packet is about to
 * be sent in SocketConnection. These types of actions fall into two broad categories:<ul>
 *      <li> Interceptors that reject the packet by throwing an exception (only when the packet
 *            has not been processed yet).
 *      <li> Interceptors that dynamically transform the packet content.
 * </ul>
 *
 * Any number of required interceptors can be added and removed at run-time.
 * Required interceptors are run first, followed by global and by any that are installed
 * for the username.<p>
 * 
 * If a required packet interceptor is not present, packets specific to that interceptor will be blocked
 * 	{@link PacketRejectedException} will be thrown.
 */

public interface PacketInterceptor2 extends PacketInterceptor{

    /**
     * Invokes the required interceptor on the specified packet. The interceptor can either modify
     * the packet, or throw a PacketRejectedException to block it from being sent or processed
     * (when read).<p>
     *
     * An exception can only be thrown when <tt>processed</tt> is false which means that the read
     * packet has not been processed yet or the packet was not sent yet. If the exception is thrown
     * with a "read" packet then the sender of the packet will receive an answer with an error. But
     * if the exception is thrown with a "sent" packet then nothing will happen.<p>
     *
     * Note that for each packet, every interceptor will be called twice: once before processing
     * is complete (<tt>processing==true</tt>) and once after processing is complete. Typically,
     * an interceptor will want to ignore one or the other case.
     *
     * @param packet the packet to take action on.
     * @param session the session that received or is sending the packet.
     * @param incoming flag that indicates if the packet was read by the server or sent from
     *      the server.
     * @param processed flag that indicates if the action (read/send) was performed. (PRE vs. POST).
     * 
     * @return - boolean - true if this interceptor wants to swallow a packet so other interceptors 
     * 						can't process it. True implies that the interceptor is responsible for
     * 						notifying the client about possible packet rejection.
     * @throws PacketRejectedException if the packet should be prevented from being processed.
     */
	 boolean interceptPacket2(Packet packet, Session session, boolean incoming, boolean processed)
	            throws PacketRejectedException;
}
