package org.jivesoftware.openfire.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InterceptorManager.class, JiveGlobals.class})
@SuppressStaticInitializationFor("org.jivesoftware.openfire.interceptor.InterceptorManager")
public class InterceptorManagerTest {
	
	@Mock
	private InterceptorPersistenceUtility mockPersistenceUtility;
	@Mock
	private Session mockClientSession;
	@Mock
	PacketInterceptor2 interceptor;
	@Mock
	ReentrantReadWriteLock readWriteLock;
	@Mock
	WriteLock writeLock;
	
	 
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		Whitebox.setInternalState(InterceptorManager.class, readWriteLock);
		doReturn(writeLock).when(readWriteLock).writeLock();
	}
	
	@Test
	public void whenManagerStartsThenInterceptorPropertiesAreLoaded() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		final Collection<PacketInterceptor2> interceptors = manager.getRequiredInterceptors();

		assertEquals(1, interceptors.size());
	}
	
	
	@Test
	public void whenRequiredInterceptorIsRemovedPermanentlyThenPersistenceUtilityIsCalledTheSecondTime() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
		
		manager.removeRequiredInterceptor("Two", true);
		verify(this.mockPersistenceUtility, times(2)).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
	}
	
	
	@Test
	public void whenRequiredInterceptorIsNOTRemovedPermanentlyThenPersistenceUtilityIsCalledOnlyOnce() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
		
		manager.removeRequiredInterceptor("Two", false);
		verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
	}
	
	
	@Test
	public void whenRequiredInterceptorsAreRegisteredThenNoPacketsAreBlocked() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		final Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.addRequiredInterceptor(interceptor, "One", new HashSet<EPacketType>(), new HashSet<EEventType>());
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	
	
	@Test
	public void whenNotAllRequiredInterceptorsAreRegisteredThenPacketsAreBlocked() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		final Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		}catch(final PacketRejectedException pre) {}			
	}	
	

	@Test
	public void whenInterceptorsAreRegisteredThenBlockingSetsArePopulatedCorrectyly() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		final Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Incoming));
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Outgoing));
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Processed));
		
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Message));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.IQ));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Presence));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Roster));
	}
	
	
	@Test
	public void whenBothInterceptorsAreRegisteredThenBlockingSetsArePopulatedCorrectyly() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Roster"} );
		
		final Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {"Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "Presence", "IQ"} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Incoming));
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Outgoing));
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Processed));
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.Unprocessed));
		
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Message));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.IQ));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Presence));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Roster));
	}
	
	
	@Test
	public void whenOneOfTheInterceptorsContainsAllThenBlockingSetsArePopulatedCorrectyly() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Roster"} );
		
		final Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {"All", "Processed", "Unprocessed"},
				 new String[]{"All", "Presence", "IQ"} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		assertEquals(1, manager.getEventTypesToBlock().size());
		assertEquals(1, manager.getPacketTypesToBlock().size());
		
		assertTrue(manager.getEventTypesToBlock().contains(EEventType.All));
		assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.All));
	}
	
	
	@Test
	public void whenConfiguredToBlockAllPacketsAndAllEventsThenAllPacketsAreRejected() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"All"}, new String[]{"All"} );
		
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}			
	}


	@Test
	public void whenNotConfiguredToBlockAnythingThenNoPacketsAreRejected() throws Exception {		
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{} );		 		 
		
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
				 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);		
		 
		manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	
	
	@Test
	public void whenIncomingPacketsArriveThenTheyWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming"}, new String[]{} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		 }catch(final PacketRejectedException pre) {}
		 
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		}catch(final PacketRejectedException pre) {}
		
		 manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		 manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	

	@Test
	public void whenIncomingAndOutgoinPacketsArriveThenTheyWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);

		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		} catch(final PacketRejectedException pre) {}			
	}
	

	@Test
	public void whenMessagesAreBlockedThenMessagePacketsWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{"Message"} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		}catch(final PacketRejectedException pre) {}
		
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}	
	

	@Test
	public void whenMessagesAndIQsAreBlockedThenPacketsWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{"Message", "IQ"} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected (Message)");
		}catch(final PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected (IQ)");
		}catch(final PacketRejectedException pre) {}
		
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	

	@Test
	public void whenIncomingMessagesAreBlockedThenPacketsWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming"}, new String[]{"Message"} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Message)");
		}catch(final PacketRejectedException pre) {}

		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}
	

	@Test
	public void whenIncomingAndOutgoingMessagesAreBlockedThenPacketsWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{"Message"} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
			fail("Expected to be rejected (Outgoing Message)");
		}catch(final PacketRejectedException pre) {}
		
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Incoming Message)");
		}catch(final PacketRejectedException pre) {}

		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}
	

	@Test
	public void whenIncomingAndOutgoingMessagesAndIQsAreBlockedThenPacketsWillBeRejected() throws Exception {
		final Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{"Message", "IQ"} );
		  		 
		doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		final InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
			fail("Expected to be rejected (Outgoing Message)");
		}catch(final PacketRejectedException pre) {}
		
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Incoming Message)");
		}catch(final PacketRejectedException pre) {}

		try {
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming
			fail("Expected to be rejected (Incoming IQ)");
		}catch(final PacketRejectedException pre) {}
		
		try {
		manager.invokeInterceptors(new IQ(), this.mockClientSession, false, false);//outgoing
			fail("Expected to be rejected (Outgoing IQ)");
		}catch(final PacketRejectedException pre) {}
		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}


	private Map<String, RequiredInterceptorDefinition> createDefinitions(final String name, final String[] eventTypes, final String[] packetTypes) {
		 final Map<String, RequiredInterceptorDefinition> definitionMap = new HashMap<String, RequiredInterceptorDefinition>();
		 
		 final Set<EEventType> eventSet = new HashSet<EEventType>();
		 for(final String eventType: eventTypes) {
			 final EEventType event = EEventType.valueOf(eventType);
			 if(event != null) {
				 eventSet.add(event);
			 }
		 }
		 
		 final Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 for(final String packetType : packetTypes) {
			 final EPacketType packet = EPacketType.valueOf(packetType);
			 if(packet != null) {
				 packetSet.add(packet);
			 }
		 }
		 
		 final RequiredInterceptorDefinition def = new RequiredInterceptorDefinition(eventSet, packetSet);
		 definitionMap.put(name, def);
		 
		 return definitionMap;
	}
}
