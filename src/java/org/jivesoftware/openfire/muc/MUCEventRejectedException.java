package org.jivesoftware.openfire.muc;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Thrown by a MUCEventDispatcher when a packet is prevented from being processed. 
 *
 * @see MUCEventDispatcher
 */
public class MUCEventRejectedException extends Exception {
    private static final long serialVersionUID = 1L;

    private Throwable nestedThrowable = null;

    /**
     * Text to include in a message that will be sent to the sender of the packet that got
     * rejected. If no text is specified then no message will be sent to the user.
     */
    private String rejectionMessage;

    public MUCEventRejectedException() {
        super();
    }

    public MUCEventRejectedException(String msg) {
        super(msg);
    }

    public MUCEventRejectedException(Throwable nestedThrowable) {
        this.nestedThrowable = nestedThrowable;
    }

    public MUCEventRejectedException(String msg, Throwable nestedThrowable) {
        super(msg);
        this.nestedThrowable = nestedThrowable;
    }

    @Override
	public void printStackTrace() {
        super.printStackTrace();
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace();
        }
    }

    @Override
	public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(ps);
        }
    }

    @Override
	public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (nestedThrowable != null) {
            nestedThrowable.printStackTrace(pw);
        }
    }

    /**
     * Retuns the text to include in a message that will be sent to the sender of the packet
     * that got rejected or <tt>null</tt> if none was defined. If no text was specified then
     * no message will be sent to the sender of the rejected packet.
     *
     * @return the text to include in a message that will be sent to the sender of the packet
     *         that got rejected or <tt>null</tt> if none was defined.
     */
    public String getRejectionMessage() {
        return rejectionMessage;
    }

    /**
     * Sets the text to include in a message that will be sent to the sender of the packet
     * that got rejected or <tt>null</tt> if no message will be sent to the sender of the
     * rejected packet. Bt default, no message will be sent.
     *
     * @param rejectionMessage the text to include in the notification message for the rejection.
     */
    public void setRejectionMessage(String rejectionMessage) {
        this.rejectionMessage = rejectionMessage;
    }
}
