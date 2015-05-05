package org.jivesoftware.openfire.muc;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.muc.EMUCEventType;
import org.jivesoftware.openfire.muc.MUCEventDispatcher;
import org.jivesoftware.openfire.muc.MUCEventListener;
import org.jivesoftware.openfire.muc.MUCEventListener2;
import org.jivesoftware.openfire.muc.MUCListenerPersistenceUtility;
import org.jivesoftware.util.JiveGlobals;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;


@RunWith(PowerMockRunner.class)
@PrepareForTest({MUCEventDispatcher.class, JiveGlobals.class})
@SuppressStaticInitializationFor("org.jivesoftware.openfire.muc.MUCEventDispatcher")

public class MUCEventDispatcherTest {
	private MUCListenerPersistenceUtility persistenceUtility;
	private MUCEventListener2 mucEventListener;
	private MUCEventListener2 mucEventListener2;
	
	@Captor
	ArgumentCaptor<Map<String, Set<EMUCEventType>>> captor;

	@Before
	public void setUp() {
		persistenceUtility = Mockito.mock(MUCListenerPersistenceUtility.class);
		mucEventListener = Mockito.mock(MUCEventListener2.class);
		mucEventListener2 = Mockito.mock(MUCEventListener2.class);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "requiredListenersByName", new HashMap<String, MUCEventListener2>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "requiredListeners", new ConcurrentLinkedQueue<MUCEventListener2>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", new ConcurrentHashMap<String, Set<EMUCEventType>>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "persistenceUtility", persistenceUtility);
		Whitebox.setInternalState(MUCEventDispatcher.class, "listeners", new ConcurrentLinkedQueue<MUCEventListener>());
	}
	
	@Test
	public void When_First_Required_Listener_Is_Added_A_Call_Is_Made_To_Persist_It() {
		HashSet<EMUCEventType> events = new HashSet<EMUCEventType>();
		events.add(EMUCEventType.Created);
		events.add(EMUCEventType.Joined);
		
		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", events);
		
		Mockito.verify(persistenceUtility).persistRequiredListeners(captor.capture());
		 
		Map<String, Set<EMUCEventType>> listeners = captor.getValue();
		Set<EMUCEventType> retrievedEvents = listeners.get("One");
		Assert.assertEquals(1, listeners.size());
		
		Assert.assertTrue(retrievedEvents.contains(EMUCEventType.Created));
		Assert.assertTrue(retrievedEvents.contains(EMUCEventType.Joined));

		Assert.assertEquals(1, MUCEventDispatcher.getRequiredListeners().size());
		//nothing to block - all required listeners are present
		Assert.assertEquals(0, MUCEventDispatcher.getEventsToBlock().size());
		
	}
				
	@Test
	public void When_Another_Required_Listener_Is_Added_A_Call_Is_Made_To_Persist_All() {
		HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.Destroyed);
		originalEvents.add(EMUCEventType.SubjectChanged);
		
		Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.Created);
		newEvents.add(EMUCEventType.Joined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		Mockito.verify(persistenceUtility).persistRequiredListeners(captor.capture());
		 
		Map<String, Set<EMUCEventType>> listeners = captor.getValue();
		Set<EMUCEventType> originalRetrievedEvents = listeners.get("One");
		Assert.assertEquals(2, listeners.size());
		
		Set<EMUCEventType> retrievedEvents = listeners.get("Two");
		Assert.assertEquals(2, listeners.size());

		Assert.assertTrue(originalRetrievedEvents.contains(EMUCEventType.Destroyed));
		Assert.assertTrue(originalRetrievedEvents.contains(EMUCEventType.SubjectChanged));
		
		Assert.assertTrue(retrievedEvents.contains(EMUCEventType.Created));
		Assert.assertTrue(retrievedEvents.contains(EMUCEventType.Joined));
		
		Assert.assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));		
	}
	
	@Test
	public void When_All_Required_Listeners_Are_Present_No_Events_Are_Blocked() {
		HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.Destroyed);
		originalEvents.add(EMUCEventType.SubjectChanged);
		
		Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.Created);
		newEvents.add(EMUCEventType.Joined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		//At this point there are two listeners required (One and Two), but only one is present (Two)
		Assert.assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
		
		//After adding required listener One all required listeners are present. No events will be blocked
		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", originalEvents);
		Assert.assertEquals(2,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(0, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
	}	
	
	@Test
	public void After_Removing_Required_Listener_Its_Events_Are_Being_Blocked() {
		HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.Destroyed);
		originalEvents.add(EMUCEventType.SubjectChanged);
		
		Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.Created);
		newEvents.add(EMUCEventType.Joined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		Assert.assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
				
		//After removing required listener its events will be blocked
		MUCEventDispatcher.removeRequiredListener("Two", false);
		Assert.assertEquals(0,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(4, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
	}
	
	@Test
	public void After_Removing_Required_Listener_And_Marking_It_As_Not_Required_Its_Events_Are_Not_Blocked() {
		HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.Destroyed);
		originalEvents.add(EMUCEventType.SubjectChanged);
		
		Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.Created);
		newEvents.add(EMUCEventType.Joined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		Assert.assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
				
		//Removing a required event and marking it as NOT required anymore (true)
		MUCEventDispatcher.removeRequiredListener("Two", true);
		Assert.assertEquals(0,  MUCEventDispatcher.getRequiredListeners().size());
		
		Assert.assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Destroyed));
		Assert.assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.SubjectChanged));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Created));
		Assert.assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.Joined));
	}	
	
	@Test
	public void When_beforeMessageReceived_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		Mockito.verify(mucEventListener2).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}
	
	@Test
	public void When_beforeMessageReceived_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class))).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		Mockito.verify(mucEventListener2, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}	
	
	@Test
	public void When_beforeMessageReceived_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeMessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		Mockito.verify(mucEventListener2, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}

	@Test
	public void When_messageReceived_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.messageReceived(new JID("1"), new JID("2"), "nickname", new Message());

		Mockito.verify(mucEventListener).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		Mockito.verify(mucEventListener2).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}
	
	
	@Test
	public void When_messageReceived_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.MessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.messageReceived(new JID("1"), new JID("2"), "nickname", new Message());

		Mockito.verify(mucEventListener, never()).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		Mockito.verify(mucEventListener2, never()).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}	

	
	@Test
	public void When_beforeNicknameChanged_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		Mockito.verify(mucEventListener2).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}
	
	@Test
	public void When_beforeNicknameChanged_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString())).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		Mockito.verify(mucEventListener2, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}	
	
	@Test
	public void When_beforeNicknameChanged_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeNickChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		Mockito.verify(mucEventListener2, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}	

	
	@Test
	public void When_nicknameChanged_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.nicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");

		Mockito.verify(mucEventListener).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		Mockito.verify(mucEventListener2).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}
	
	
	@Test
	public void When_nicknameChanged_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.NickChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.nicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");

		Mockito.verify(mucEventListener, never()).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		Mockito.verify(mucEventListener2, never()).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}	
	

	@Test
	public void When_beforeOccupantJoined_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}
	
	@Test
	public void When_beforeOccupantJoined_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeOccupantJoined(any(JID.class), any(JID.class), anyString())).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}	
	
	@Test
	public void When_beforeOccupantJoined_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeJoined);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}
	

	@Test
	public void When_occupantJoined_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantJoined(new JID("1"), new JID("2"), "nickname");

		Mockito.verify(mucEventListener).occupantJoined(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2).occupantJoined(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void When_occupantJoined_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.Joined);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantJoined(new JID("1"), new JID("2"), "nickname");

		Mockito.verify(mucEventListener, never()).occupantJoined(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).occupantJoined(any(JID.class), any(JID.class), anyString());
	}		
	

	@Test
	public void When_beforeOccupantLeft_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeOccupantLeft(new JID("1"), new JID("2"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeOccupantLeft(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2).beforeOccupantLeft(any(JID.class), any(JID.class));
	}
	
	@Test
	public void When_beforeOccupantLeft_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeOccupantLeft(any(JID.class), any(JID.class))).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeOccupantLeft(new JID("1"), new JID("2"));
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeOccupantLeft(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeOccupantLeft(any(JID.class), any(JID.class));
	}	
	
	@Test
	public void When_beforeOccupantLeft_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeLeft);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeOccupantLeft(new JID("1"), new JID("2"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeOccupantLeft(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeOccupantLeft(any(JID.class), any(JID.class));
	}	

	
	@Test
	public void When_occupantLeft_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantLeft(new JID("1"), new JID("2"));

		Mockito.verify(mucEventListener).occupantLeft(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2).occupantLeft(any(JID.class), any(JID.class));
	}
	
	
	@Test
	public void When_occupantLeft_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.Left);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantLeft(new JID("1"), new JID("2"));

		Mockito.verify(mucEventListener, never()).occupantLeft(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2, never()).occupantLeft(any(JID.class), any(JID.class));
	}	
	

	@Test
	public void When_beforePrivateMessageRecieved_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		Mockito.verify(mucEventListener2).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}
	
	@Test
	public void When_beforePrivateMessageRecieved_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class))).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		Mockito.verify(mucEventListener2, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}	

	
	@Test
	public void When_beforePrivateMessageRecieved_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforePrivateMessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		Mockito.verify(mucEventListener2, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}	

	
	@Test
	public void When_privateMessageRecieved_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.privateMessageRecieved(new JID("1"), new JID("2"), new Message());

		Mockito.verify(mucEventListener).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		Mockito.verify(mucEventListener2).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}
	
	
	@Test
	public void When_privateMessageRecieved_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.PrivateMessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.privateMessageRecieved(new JID("1"), new JID("2"), new Message());

		Mockito.verify(mucEventListener, never()).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		Mockito.verify(mucEventListener2, never()).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}	
	
	
	@Test
	public void When_beforeRoomCreated_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeRoomCreated(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2).beforeRoomCreated(any(JID.class), any(JID.class));
	}
	
	@Test
	public void When_beforeRoomCreated_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeRoomCreated(any(JID.class), any(JID.class))).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeRoomCreated(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeRoomCreated(any(JID.class), any(JID.class));
	}	
	
	@Test
	public void When_beforeRoomCreated_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeCreated);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeRoomCreated(any(JID.class), any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeRoomCreated(any(JID.class), any(JID.class));
	}
	

	@Test
	public void When_roomCreated_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomCreated(new JID("1"));

		Mockito.verify(mucEventListener).roomCreated(any(JID.class));
		Mockito.verify(mucEventListener2).roomCreated(any(JID.class));
	}
	
	
	@Test
	public void When_roomCreated_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three", EMUCEventType.Created);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomCreated(new JID("1"));

		Mockito.verify(mucEventListener, never()).roomCreated(any(JID.class));
		Mockito.verify(mucEventListener2, never()).roomCreated(any(JID.class));
	}


	@Test
	public void When_beforeRoomDestroyed_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeRoomDestroyed(any(JID.class));
		Mockito.verify(mucEventListener2).beforeRoomDestroyed(any(JID.class));
	}
	
	@Test
	public void When_beforeRoomDestroyed_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeRoomDestroyed(any(JID.class))).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeRoomDestroyed(any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeRoomDestroyed(any(JID.class));
	}	
	
	@Test
	public void When_beforeRoomDestroyed_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeDestroyed);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeRoomDestroyed(any(JID.class));
		Mockito.verify(mucEventListener2, never()).beforeRoomDestroyed(any(JID.class));
	}	

	
	@Test
	public void When_roomDestroyed_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomDestroyed(new JID("1"));

		Mockito.verify(mucEventListener).roomDestroyed(any(JID.class));
		Mockito.verify(mucEventListener2).roomDestroyed(any(JID.class));
	}
	
	
	@Test
	public void When_roomDestroyed_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.Destroyed);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomDestroyed(new JID("1"));

		Mockito.verify(mucEventListener, never()).roomDestroyed(any(JID.class));
		Mockito.verify(mucEventListener2, never()).roomDestroyed(any(JID.class));
	}	
	
	
	@Test
	public void When_beforeRoomSubjectChanged_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	
	@Test
	public void When_beforeRoomSubjectChanged_Is_Called_And_One_Listener_Returns_True_Remaining_Listeners_Are_Not_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		Mockito.when(mucEventListener.beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString())).thenReturn(true);
		boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		Assert.assertTrue(returned);
		Mockito.verify(mucEventListener).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}	
	
	@Test
	public void When_beforeRoomSubjectChanged_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeSubjectChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		Assert.assertFalse(returned);
		Mockito.verify(mucEventListener, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	

	@Test
	public void When_roomSubjectChanged_Is_Called_And_Required_Listeners_Are_Present_All_Listeners_Are_Called() throws PacketRejectedException {
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomSubjectChanged(new JID("1"), new JID("2"), "newSubject");

		Mockito.verify(mucEventListener).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void When_roomSubjectChanged_Is_Called_And_Required_Listeners_Are_Not_Present_Calls_Are_Blocked() throws PacketRejectedException {
		//Three is a required listener that's not present
		Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.SubjectChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		Set<EMUCEventType> set1 = createEventSet(EMUCEventType.Destroyed);
		Set<EMUCEventType> set2 = createEventSet(EMUCEventType.Created);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomSubjectChanged(new JID("1"), new JID("2"), "newSubject");

		Mockito.verify(mucEventListener, never()).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
		Mockito.verify(mucEventListener2, never()).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}		
	

	private Set<EMUCEventType> createEventSet(EMUCEventType... events) {
		HashSet<EMUCEventType> eventSet = new HashSet<EMUCEventType>();
		Collections.addAll(eventSet, events);
		
		return eventSet;
	}
	
	private Map<String, Set<EMUCEventType>> createEventMap(String listenerName, EMUCEventType... events) {
		HashSet<EMUCEventType> eventSet = new HashSet<EMUCEventType>();
		Collections.addAll(eventSet, events);
		
		Map<String, Set<EMUCEventType>> listenerMap = new HashMap<String, Set<EMUCEventType>>();
		listenerMap.put(listenerName,  eventSet);
		
		return listenerMap;
	}
}
