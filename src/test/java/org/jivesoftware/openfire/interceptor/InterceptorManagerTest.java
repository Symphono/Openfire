package org.jivesoftware.openfire.interceptor;

import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.openfire.interceptor.EEventType;
import org.jivesoftware.openfire.interceptor.EPacketType;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.InterceptorPersistenceUtility;
import org.jivesoftware.openfire.interceptor.PacketInterceptor2;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.interceptor.RequiredInterceptorDefinition;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
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
	 
	private InterceptorPersistenceUtility mockPersistenceUtility;
	private Session mockClientSession;
	
	
	@Before
	public void setUp() {
		 this.mockPersistenceUtility = Mockito.mock(InterceptorPersistenceUtility.class);
		 this.mockClientSession = Mockito.mock(Session.class);
	}
	
	@Test
	public void When_ManagerStarts_Interceptor_Properties_Are_Loaded() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		PacketInterceptor2 interceptor = Mockito.mock(PacketInterceptor2.class);
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		Collection<PacketInterceptor2> interceptors = manager.getRequiredInterceptors();

		Assert.assertTrue(interceptors.size() == 1);
	}
	
	@Test
	public void When_Required_Interceptor_Is_Removed_Permanently_Persistence_Utility_Is_Called_The_Second_Time() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		PacketInterceptor2 interceptor = Mockito.mock(PacketInterceptor2.class);
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		Mockito.verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
		
		manager.removeRequiredInterceptor("Two", true);
		Mockito.verify(this.mockPersistenceUtility, org.mockito.Mockito.times(2)).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
	}
	
	@Test
	public void When_Required_Interceptor_Is_NOT_Removed_Permanently_Persistence_Utility_Is_Called_Only_Once() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] {"All", "Incoming", "Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
			  
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		PacketInterceptor2 interceptor = Mockito.mock(PacketInterceptor2.class);
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		Mockito.verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
		
		manager.removeRequiredInterceptor("Two", false);
		Mockito.verify(this.mockPersistenceUtility).persistRequiredInterceptors(Matchers.<Map<String, RequiredInterceptorDefinition>>any());
	}	
	
	@Test
	public void When_Required_Interceptors_Are_Registered_No_Packets_Are_Blocked() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		PacketInterceptor2 interceptor = Mockito.mock(PacketInterceptor2.class);
		manager.addRequiredInterceptor(interceptor, "One", new HashSet<EPacketType>(), new HashSet<EEventType>());
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	
	@Test
	public void When_Not_All_Required_Interceptors_Are_Registered_Packets_Are_Blocked() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		PacketInterceptor2 interceptor = Mockito.mock(PacketInterceptor2.class);
		manager.addRequiredInterceptor(interceptor, "Two", new HashSet<EPacketType>(), new HashSet<EEventType>());
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		}catch(PacketRejectedException pre) {}			
	}	
	

	@Test
	public void When_Interceptors_Are_Registered_Blocking_Sets_Are_Populated_Correctyly() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Presence", "Roster"} );
		
		Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {},
				 new String[]{} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Incoming));
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Outgoing));
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Processed));
		
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Message));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.IQ));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Presence));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Roster));
	}
	
	@Test
	public void When_Both_Interceptors_Are_Registered_Blocking_Sets_Are_Populated_Correctyly() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Roster"} );
		
		Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {"Outgoing", "Processed", "Unprocessed"},
				 new String[]{"Message", "Presence", "IQ"} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Incoming));
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Outgoing));
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Processed));
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.Unprocessed));
		
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Message));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.IQ));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Presence));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.Roster));
	}	
	
	@Test
	public void When_One_Of_The_Interceptors_Contains_All_Blocking_Sets_Are_Populated_Correctyly() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", 
				 new String[] { "Incoming", "Outgoing", "Processed"},
				 new String[]{"Message", "IQ", "Roster"} );
		
		Map<String, RequiredInterceptorDefinition> definitionMap2 = createDefinitions("Two", 
				 new String[] {"All", "Processed", "Unprocessed"},
				 new String[]{"All", "Presence", "IQ"} );
		
		definitionMap.putAll(definitionMap2);
		PowerMockito.suppress(PowerMockito.constructor(InterceptorManager.class));
		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);

		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		 
		Assert.assertTrue(manager.getEventTypesToBlock().size() == 1);
		Assert.assertTrue(manager.getPacketTypesToBlock().size() == 1);
		
		Assert.assertTrue(manager.getEventTypesToBlock().contains(EEventType.All));
		Assert.assertTrue(manager.getPacketTypesToBlock().contains(EPacketType.All));
	}
	
	
	@Test
	public void When_Configured_To_Block_All_Packets_All_Events_All_Packets_Are_Rejected() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"All"}, new String[]{"All"} );
		
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}			
	}


	@Test
	public void When_Not_Configured_To_Block_Anything_No_Packets_Are_Rejected() throws Exception {		
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{} );		 		 
		
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
				 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);		
		 
		manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}
	
	
	@Test
	public void When_Incoming_Packets_Arrive_They_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming"}, new String[]{} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		 }catch(PacketRejectedException pre) {}
		 
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		}catch(PacketRejectedException pre) {}
		
		 manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		 manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
		 
	}
	

	@Test
	public void When_Incoming_And_Outgoin_Packets_Arrive_They_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);

		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
			fail("Expected to be rejected");
		} catch(PacketRejectedException pre) {}			
	}
	

	@Test
	public void When_Messages_Are_Blocked_Message_Packets_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{"Message"} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected");
		}catch(PacketRejectedException pre) {}
		
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}	
	

	@Test
	public void When_Messages_And_IQs_Are_Blocked_Packets_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {}, new String[]{"Message", "IQ"} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);
			fail("Expected to be rejected (Message)");
		}catch(PacketRejectedException pre) {}
		
		try {
			manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);
			fail("Expected to be rejected (IQ)");
		}catch(PacketRejectedException pre) {}
		
		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);
	}	
	
	

	@Test
	public void When_Incoming_Messages_Are_Blocked_Packets_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming"}, new String[]{"Message"} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Message)");
		}catch(PacketRejectedException pre) {}

		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}
	

	@Test
	public void When_Incoming_And_Outgoing_Messages_Are_Blocked_Packets_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{"Message"} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
			fail("Expected to be rejected (Outgoing Message)");
		}catch(PacketRejectedException pre) {}
		
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Incoming Message)");
		}catch(PacketRejectedException pre) {}

		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}
	

	@Test
	public void When_Incoming_And_Outgoing_Messages_And_IQs_Are_Blocked_Packets_Will_Be_Rejected() throws Exception {
		Map<String, RequiredInterceptorDefinition> definitionMap = createDefinitions("One", new String[] {"Incoming", "Outgoing"}, new String[]{"Message", "IQ"} );
		  		 
		Mockito.doReturn(definitionMap).when(mockPersistenceUtility).loadRequiredInterceptors();
		 
		PowerMockito.whenNew(InterceptorPersistenceUtility.class).withAnyArguments().thenReturn(mockPersistenceUtility);
		 	
		InterceptorManager manager = Whitebox.invokeConstructor(InterceptorManager.class);
		
		try {
			manager.invokeInterceptors(new Message(), this.mockClientSession, false, true);//outgoing
			fail("Expected to be rejected (Outgoing Message)");
		}catch(PacketRejectedException pre) {}
		
		try {			
			manager.invokeInterceptors(new Message(), this.mockClientSession, true, true);//incoming
			fail("Expected to be rejected (Incoming Message)");
		}catch(PacketRejectedException pre) {}

		try {
		manager.invokeInterceptors(new IQ(), this.mockClientSession, true, false);//incoming
			fail("Expected to be rejected (Incoming IQ)");
		}catch(PacketRejectedException pre) {}
		
		try {
		manager.invokeInterceptors(new IQ(), this.mockClientSession, false, false);//outgoing
			fail("Expected to be rejected (Outgoing IQ)");
		}catch(PacketRejectedException pre) {}
		
 		manager.invokeInterceptors(new Presence(), this.mockClientSession, false, true);//outgoing
		manager.invokeInterceptors(new Roster(), this.mockClientSession, false, false);//outgoing
	}
	


	private Map<String, RequiredInterceptorDefinition> createDefinitions(String name, String[] eventTypes, String[] packetTypes) {
		 Map<String, RequiredInterceptorDefinition> definitionMap = new HashMap<String, RequiredInterceptorDefinition>();
		 
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 for(String eventType: eventTypes) {
			 EEventType event = EEventType.fromString(eventType);
			 if(event != null) {
				 eventSet.add(event);
			 }
		 }
		 
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 for(String packetType : packetTypes) {
			 EPacketType packet = EPacketType.fromString(packetType);
			 if(packet != null) {
				 packetSet.add(packet);
			 }
		 }
		 
		 RequiredInterceptorDefinition def = new RequiredInterceptorDefinition(eventSet, packetSet);
		 definitionMap.put(name, def);
		 
		 return definitionMap;
	}
}
