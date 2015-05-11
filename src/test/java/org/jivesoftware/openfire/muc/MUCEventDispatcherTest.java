package org.jivesoftware.openfire.muc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jivesoftware.util.JiveGlobals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
	@Mock
	private MUCListenerPersistenceUtility persistenceUtility;
	@Mock
	private MUCEventListener2 mucEventListener;
	@Mock
	private MUCEventListener2 mucEventListener2;
	@Mock
	ReentrantReadWriteLock readWriteLock;
	@Mock
	WriteLock writeLock;
	@Captor 
	ArgumentCaptor<Map<String, Set<EMUCEventType>>> captor;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "requiredListenersByName", new HashMap<String, MUCEventListener2>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "requiredListeners", new ConcurrentLinkedQueue<MUCEventListener2>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", new ConcurrentHashMap<String, Set<EMUCEventType>>());
		Whitebox.setInternalState(MUCEventDispatcher.class, "persistenceUtility", persistenceUtility);
		Whitebox.setInternalState(MUCEventDispatcher.class, "listeners", new ConcurrentLinkedQueue<MUCEventListener>());
		Whitebox.setInternalState(MUCEventDispatcher.class, readWriteLock);
		doReturn(writeLock).when(readWriteLock).writeLock();
	}
	
	
	@Test
	public void whenFirstRequiredListenerIsAddedThenACallIsMadeToPersistIt() {
		final HashSet<EMUCEventType> events = new HashSet<EMUCEventType>();
		events.add(EMUCEventType.BeforeCreated);
		events.add(EMUCEventType.BeforeJoined);
		
		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", events);
		
		verify(persistenceUtility).persistRequiredListeners(captor.capture());
		 
		final Map<String, Set<EMUCEventType>> listeners = captor.getValue();
		final Set<EMUCEventType> retrievedEvents = listeners.get("One");
		assertEquals(1, listeners.size());
		
		assertTrue(retrievedEvents.contains(EMUCEventType.BeforeCreated));
		assertTrue(retrievedEvents.contains(EMUCEventType.BeforeJoined));

		assertEquals(1, MUCEventDispatcher.getRequiredListeners().size());
		//nothing to block - all required listeners are present
		assertEquals(0, MUCEventDispatcher.getEventsToBlock().size());
	}
	
				
	@Test
	public void whenAnotherRequiredListenerIsAddedThenACallIsMadeToPersistAll() {
		final HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.BeforeDestroyed);
		originalEvents.add(EMUCEventType.BeforeSubjectChanged);
		
		final Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		final HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.BeforeCreated);
		newEvents.add(EMUCEventType.BeforeJoined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		verify(persistenceUtility).persistRequiredListeners(captor.capture());
		 
		final Map<String, Set<EMUCEventType>> listeners = captor.getValue();
		final Set<EMUCEventType> originalRetrievedEvents = listeners.get("One");
		assertEquals(2, listeners.size());
		
		final Set<EMUCEventType> retrievedEvents = listeners.get("Two");
		assertEquals(2, listeners.size());

		assertTrue(originalRetrievedEvents.contains(EMUCEventType.BeforeDestroyed));
		assertTrue(originalRetrievedEvents.contains(EMUCEventType.BeforeSubjectChanged));
		
		assertTrue(retrievedEvents.contains(EMUCEventType.BeforeCreated));
		assertTrue(retrievedEvents.contains(EMUCEventType.BeforeJoined));
		
		assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));		
	}
	
	
	@Test
	public void whenAllRequiredListenersArePresentThenNoEventsAreBlocked() {
		final HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.BeforeDestroyed);
		originalEvents.add(EMUCEventType.BeforeSubjectChanged);
		
		final Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		final HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.BeforeCreated);
		newEvents.add(EMUCEventType.BeforeJoined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		//At this point there are two listeners required (One and Two), but only one is present (Two)
		assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
		
		//After adding required listener One, all required listeners are present. No events will be blocked
		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", originalEvents);
		assertEquals(2,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(0, MUCEventDispatcher.getEventsToBlock().size());		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
	}
	
	
	@Test
	public void whenRequiredListenerIsRemovedThenItsEventsAreBeingBlocked() {
		final HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.BeforeDestroyed);
		originalEvents.add(EMUCEventType.BeforeSubjectChanged);
		
		final Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		final HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.BeforeCreated);
		newEvents.add(EMUCEventType.BeforeJoined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
				
		//After removing required listener its events will be blocked
		MUCEventDispatcher.removeRequiredListener("Two", false);
		assertEquals(0,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(4, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
	}

	
	@Test
	public void whenRequiredListenerIsRemovedAndMarkedAsNotRequiredThenItsEventsAreNotBlocked() {
		final HashSet<EMUCEventType> originalEvents = new HashSet<EMUCEventType>();
		originalEvents.add(EMUCEventType.BeforeDestroyed);
		originalEvents.add(EMUCEventType.BeforeSubjectChanged);
		
		final Map<String, Set<EMUCEventType>> originalMap = new HashMap<String, Set<EMUCEventType>>();
		originalMap.put("One",  originalEvents);
		
		final HashSet<EMUCEventType> newEvents = new HashSet<EMUCEventType>();
		newEvents.add(EMUCEventType.BeforeCreated);
		newEvents.add(EMUCEventType.BeforeJoined);
		
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		MUCEventDispatcher.addRequiredListener(mucEventListener, "Two", newEvents);
		
		assertEquals(1,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
				
		//Removing a required event and marking it as NOT required anymore (true)
		MUCEventDispatcher.removeRequiredListener("Two", true);
		assertEquals(0,  MUCEventDispatcher.getRequiredListeners().size());
		
		assertEquals(2, MUCEventDispatcher.getEventsToBlock().size());		
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeDestroyed));
		assertTrue(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeSubjectChanged));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeCreated));
		assertFalse(MUCEventDispatcher.getEventsToBlock().contains(EMUCEventType.BeforeJoined));
	}
	
	
	@Test
	public void whenBeforeMessageReceivedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		assertFalse(returned);
		verify(mucEventListener).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		verify(mucEventListener2).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}
	
	
	@Test
	public void whenBeforeMessageReceivedIsCalledAndOneListenerReturnsTrueTehnRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class))).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		assertTrue(returned);
		verify(mucEventListener).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		verify(mucEventListener2, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}	
	
	
	@Test
	public void whenBeforeMessageReceivedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeMessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeMessageReceived(new JID("1"), new JID("2"), "nickname", new Message());
		assertFalse(returned);
		verify(mucEventListener, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		verify(mucEventListener2, never()).beforeMessageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}

	
	@Test
	public void whenMessageReceivedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.messageReceived(new JID("1"), new JID("2"), "nickname", new Message());

		verify(mucEventListener).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
		verify(mucEventListener2).messageReceived(any(JID.class), any(JID.class), anyString(), any(Message.class));
	}
	
		
	@Test
	public void whenBeforeNicknameChangedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		assertFalse(returned);
		verify(mucEventListener).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		verify(mucEventListener2).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}
	
	
	@Test
	public void whenBeforeNicknameChangedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString())).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		assertTrue(returned);
		verify(mucEventListener).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		verify(mucEventListener2, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}	
	
	
	@Test
	public void whenBeforeNicknameChangedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeNickChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeNicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");
		assertFalse(returned);
		verify(mucEventListener, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		verify(mucEventListener2, never()).beforeNicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}	

	
	@Test
	public void whenNicknameChangedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.nicknameChanged(new JID("1"), new JID("2"), "nickname", "newNickname");

		verify(mucEventListener).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
		verify(mucEventListener2).nicknameChanged(any(JID.class), any(JID.class), anyString(), anyString());
	}
	

	@Test
	public void whenBeforeOccupantJoinedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		assertFalse(returned);
		verify(mucEventListener).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void whenBeforeOccupantJoinedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeOccupantJoined(any(JID.class), any(JID.class), anyString())).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		assertTrue(returned);
		verify(mucEventListener).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void whenBeforeOccupantJoinedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeJoined);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeOccupantJoined(new JID("1"), new JID("2"), "nickname");
		assertFalse(returned);
		verify(mucEventListener, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2, never()).beforeOccupantJoined(any(JID.class), any(JID.class), anyString());
	}
	

	@Test
	public void whenOccupantJoinedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantJoined(new JID("1"), new JID("2"), "nickname");

		verify(mucEventListener).occupantJoined(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2).occupantJoined(any(JID.class), any(JID.class), anyString());
	}

	
	@Test
	public void whenOccupantLeftIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.occupantLeft(new JID("1"), new JID("2"));

		verify(mucEventListener).occupantLeft(any(JID.class), any(JID.class));
		verify(mucEventListener2).occupantLeft(any(JID.class), any(JID.class));
	}


	@Test
	public void whenBeforePrivateMessageRecievedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		assertFalse(returned);
		verify(mucEventListener).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		verify(mucEventListener2).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}
	
	
	@Test
	public void whenBeforePrivateMessageRecievedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class))).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		assertTrue(returned);
		verify(mucEventListener).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		verify(mucEventListener2, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}	

	
	@Test
	public void whenBeforePrivateMessageRecievedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforePrivateMessageReceived);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforePrivateMessageRecieved(new JID("1"), new JID("2"), new Message());
		assertFalse(returned);
		verify(mucEventListener, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		verify(mucEventListener2, never()).beforePrivateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}	

	
	@Test
	public void whenPrivateMessageRecievedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.privateMessageRecieved(new JID("1"), new JID("2"), new Message());

		verify(mucEventListener).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
		verify(mucEventListener2).privateMessageRecieved(any(JID.class), any(JID.class), any(Message.class));
	}

	
	@Test
	public void whenBeforeRoomCreatedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		assertFalse(returned);
		verify(mucEventListener).beforeRoomCreated(any(JID.class), any(JID.class));
		verify(mucEventListener2).beforeRoomCreated(any(JID.class), any(JID.class));
	}
	
	
	@Test
	public void whenBeforeRoomCreatedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeRoomCreated(any(JID.class), any(JID.class))).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		assertTrue(returned);
		verify(mucEventListener).beforeRoomCreated(any(JID.class), any(JID.class));
		verify(mucEventListener2, never()).beforeRoomCreated(any(JID.class), any(JID.class));
	}
	
	
	@Test
	public void whenBeforeRoomCreatedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeCreated);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomCreated(new JID("1"), new JID("2"));
		assertFalse(returned);
		verify(mucEventListener, never()).beforeRoomCreated(any(JID.class), any(JID.class));
		verify(mucEventListener2, never()).beforeRoomCreated(any(JID.class), any(JID.class));
	}
	

	@Test
	public void whenRoomCreatedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomCreated(new JID("1"));

		verify(mucEventListener).roomCreated(any(JID.class));
		verify(mucEventListener2).roomCreated(any(JID.class));
	}
	

	@Test
	public void whenBeforeRoomDestroyedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		assertFalse(returned);
		verify(mucEventListener).beforeRoomDestroyed(any(JID.class));
		verify(mucEventListener2).beforeRoomDestroyed(any(JID.class));
	}
	
	
	@Test
	public void whenBeforeRoomDestroyedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeRoomDestroyed(any(JID.class))).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		assertTrue(returned);
		verify(mucEventListener).beforeRoomDestroyed(any(JID.class));
		verify(mucEventListener2, never()).beforeRoomDestroyed(any(JID.class));
	}
	
	
	@Test
	public void whenBeforeRoomDestroyedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeDestroyed);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomDestroyed(new JID("1"));
		assertFalse(returned);
		verify(mucEventListener, never()).beforeRoomDestroyed(any(JID.class));
		verify(mucEventListener2, never()).beforeRoomDestroyed(any(JID.class));
	}	

	
	@Test
	public void whenRoomDestroyedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomDestroyed(new JID("1"));

		verify(mucEventListener).roomDestroyed(any(JID.class));
		verify(mucEventListener2).roomDestroyed(any(JID.class));
	}

	
	@Test
	public void whenBeforeRoomSubjectChangedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		assertFalse(returned);
		verify(mucEventListener).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void whenBeforeRoomSubjectChangedIsCalledAndOneListenerReturnsTrueThenRemainingListenersAreNotCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		when(mucEventListener.beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString())).thenReturn(true);
		final boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		assertTrue(returned);
		verify(mucEventListener).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	
	
	@Test
	public void whenBeforeRoomSubjectChangedIsCalledAndRequiredListenersAreNotPresentThenCallsAreBlocked() throws MUCEventRejectedException {
		//Three is a required listener that's not present
		final Map<String, Set<EMUCEventType>> originalMap = createEventMap("Three",  EMUCEventType.BeforeSubjectChanged);
		Whitebox.setInternalState(MUCEventDispatcher.class, "allRequiredListeners", originalMap);
		
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		final boolean returned = MUCEventDispatcher.beforeRoomSubjectChanged(new JID("1"), new JID("2"), "newSubject");
		assertFalse(returned);
		verify(mucEventListener, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2, never()).beforeRoomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	

	@Test
	public void whenRoomSubjectChangedIsCalledAndRequiredListenersArePresentThenAllListenersAreCalled() throws MUCEventRejectedException {
		final Set<EMUCEventType> set1 = createEventSet(EMUCEventType.BeforeDestroyed);
		final Set<EMUCEventType> set2 = createEventSet(EMUCEventType.BeforeCreated);

		MUCEventDispatcher.addRequiredListener(mucEventListener, "One", set1);
		MUCEventDispatcher.addRequiredListener(mucEventListener2, "Two", set2);
		
		MUCEventDispatcher.roomSubjectChanged(new JID("1"), new JID("2"), "newSubject");

		verify(mucEventListener).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
		verify(mucEventListener2).roomSubjectChanged(any(JID.class), any(JID.class), anyString());
	}
	

	private Set<EMUCEventType> createEventSet(final EMUCEventType... events) {
		final HashSet<EMUCEventType> eventSet = new HashSet<EMUCEventType>();
		Collections.addAll(eventSet, events);
		
		return eventSet;
	}
	
	
	private Map<String, Set<EMUCEventType>> createEventMap(final String listenerName, final EMUCEventType... events) {
		final HashSet<EMUCEventType> eventSet = new HashSet<EMUCEventType>();
		Collections.addAll(eventSet, events);
		
		final Map<String, Set<EMUCEventType>> listenerMap = new HashMap<String, Set<EMUCEventType>>();
		listenerMap.put(listenerName,  eventSet);
		
		return listenerMap;
	}
}
