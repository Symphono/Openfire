package org.jivesoftware.openfire.muc;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * Thrown by a MUCEventDispatcher when a packet is prevented from being processed.
 * This exception is normally caught and re-thrown as a {@link UnauthorizedException}, {@link NotAllowedException} or {@link ForbiddenException}
 *
 * @see MUCEventDispatcher
 */
public class MUCEventRejectedException extends Exception {
    private static final long serialVersionUID = 1L;

    private Throwable nestedThrowable = null;

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
}
