package org.jivesoftware.openfire.muc;

import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

public interface MUCEventListener2 extends MUCEventListener {
    /**
     * Event triggered before a new room is created.
     *
     * @param roomJID JID of the room that was created.
     */
    boolean beforeRoomCreated(JID roomJID, JID userJID) throws MUCEventRejectedException;

    /**
     * Event triggered before a room is destroyed.
     *
     * @param roomJID JID of the room that was destroyed.
     */
    boolean beforeRoomDestroyed(JID roomJID) throws MUCEventRejectedException;

    /**
     * Event triggered before a new occupant joins a room.
     *
     * @param roomJID the JID of the room where the occupant has joined.
     * @param user the JID of the user joining the room.
     * @param nickname nickname of the user in the room.
     */
    boolean beforeOccupantJoined(JID roomJID, JID user, String nickname)  throws MUCEventRejectedException;

    /**
     * Event triggered before an occupant leaves a room.
     *
     * @param roomJID the JID of the room where the occupant has left.
     * @param user the JID of the user leaving the room.
     */
    boolean beforeOccupantLeft(JID roomJID, JID user) throws MUCEventRejectedException;

    /**
     * Event triggered before an occupant's nickname changes in a room.
     *
     * @param roomJID the JID of the room where the user changed his nickname.
     * @param user the JID of the user that changed his nickname.
     * @param oldNickname old nickname of the user in the room.
     * @param newNickname new nickname of the user in the room.
     */
    boolean beforeNicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) throws MUCEventRejectedException;

    /**
     * Event triggered before a message is sent to a room.
     *
     * @param roomJID the JID of the room that received the message.
     * @param user the JID of the user that sent the message.
     * @param nickname nickname used by the user when sending the message.
     * @param message the message sent by the room occupant.
     */
    boolean beforeMessageReceived(JID roomJID, JID user, String nickname, Message message) throws MUCEventRejectedException;

    /**
     * Event triggered before a room occupant's private message is sent to another room user
     *
     * @param toJID the JID of who the message is to.
     * @param fromJID the JID of who the message came from.
     * @param message the message sent to user.
     */
    boolean beforePrivateMessageRecieved(JID toJID, JID fromJID, Message message) throws MUCEventRejectedException;

    /**
     * Event triggered before the subject of a room is changed.
     *
     * @param roomJID the JID of the room that had its subject changed.
     * @param user the JID of the user that changed the subject.
     * @param newSubject new room subject.
     */
    boolean beforeRoomSubjectChanged(JID roomJID, JID user, String newSubject) throws MUCEventRejectedException;
	
}
