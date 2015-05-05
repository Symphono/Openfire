package org.jivesoftware.openfire.muc;

import java.util.HashMap;
import java.util.Map;

public enum EMUCEventType {
	BeforeCreated("BeforeCreated"),
	Created("Created"),
	BeforeDestroyed("BeforeDestroyed"), 
	Destroyed("Destroyed"),
	BeforeJoined("BeforeJoined"),
	Joined("Joined"),
	BeforeLeft("BeforeLeft"),
	Left("Left"),
	BeforeNickChanged("BeforeNickChanged"),
	NickChanged("NickChanged"),
	BeforeMessageReceived("BeforeMessageReceived"),
	MessageReceived("MessageReceived"),
	BeforePrivateMessageReceived("BeforePrivateMessageReceived"),
	PrivateMessageReceived("PrivateMessageReceived"),
	BeforeSubjectChanged("BeforeSubjectChanged"),
	SubjectChanged("SubjectChanged");
	
	private static final Map<String, EMUCEventType> eventMap = new HashMap<String, EMUCEventType>();
	private String eventName;

    static {
        for (EMUCEventType event : EMUCEventType.values())
            eventMap.put(event.getEventName(), event);
    }
	
    private EMUCEventType(String name) {
    	this.eventName = name;
    }
	
    public String getEventName() {
    	return this.eventName;
    }
	public static EMUCEventType fromString(String name) {
		return eventMap.get(name);
	}
}
