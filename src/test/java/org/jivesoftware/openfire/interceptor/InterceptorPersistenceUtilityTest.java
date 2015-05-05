package org.jivesoftware.openfire.interceptor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.openfire.interceptor.EEventType;
import org.jivesoftware.openfire.interceptor.EPacketType;
import org.jivesoftware.openfire.interceptor.InterceptorPersistenceUtility;
import org.jivesoftware.openfire.interceptor.RequiredInterceptorDefinition;
import org.jivesoftware.util.JiveGlobals;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
 

@RunWith(PowerMockRunner.class)
@PrepareForTest(JiveGlobals.class)
public class InterceptorPersistenceUtilityTest {
	
	@Captor
	 ArgumentCaptor<Map<String, String>> captor;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(JiveGlobals.class);
	}
	
	// 	loadRequiredInterceptors()
	 
	 @Test
	 public void When_No_Required_Interceptors_Empty_Map_Is_Returned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn(null);
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 
		 Assert.assertEquals(0, results.size()); 
	 }
	 
	 
	 @Test
	 public void When_Required_Interceptors_Are_Specified_But_No_Values_Are_Persisted_Empty_Definition_Is_Returned_For_Each_Interceptor() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 for(RequiredInterceptorDefinition val : results.values()) {
			 Assert.assertNotNull(val.getEventTypes());
			 Assert.assertNotNull(val.getPacketTypes());
			 Assert.assertEquals(0, val.getEventTypes().size());
			 Assert.assertEquals(0, val.getPacketTypes().size());
		 }
	 }
	 
	 
	 @Test
	 public void When_Required_Interceptors_Are_Persisted_Values_Are_Returned_For_Each_Interceptor() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed,Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 Assert.assertEquals(4, defOne.getEventTypes().size());
		 Assert.assertEquals(4, defOne.getPacketTypes().size());
		 
		 Assert.assertTrue(defOne.getEventTypes().contains(EEventType.Incoming));
		 Assert.assertTrue(defOne.getEventTypes().contains(EEventType.Outgoing));
		 Assert.assertTrue(defOne.getEventTypes().contains(EEventType.Processed));
		 Assert.assertTrue(defOne.getEventTypes().contains(EEventType.Unprocessed));
		 
		 Assert.assertTrue(defOne.getPacketTypes().contains(EPacketType.Presence));
		 Assert.assertTrue(defOne.getPacketTypes().contains(EPacketType.IQ));
		 Assert.assertTrue(defOne.getPacketTypes().contains(EPacketType.Message));
		 Assert.assertTrue(defOne.getPacketTypes().contains(EPacketType.Roster));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 Assert.assertEquals(1, defTwo.getEventTypes().size());
		 Assert.assertEquals(1, defTwo.getPacketTypes().size());
		 
		 Assert.assertTrue(defTwo.getEventTypes().contains(EEventType.Incoming));

		 Assert.assertTrue(defTwo.getPacketTypes().contains(EPacketType.IQ));
		 
		 RequiredInterceptorDefinition defThree = results.get("Three");
		 Assert.assertEquals(0, defThree.getEventTypes().size());
		 Assert.assertEquals(0, defThree.getPacketTypes().size());
	 }
	 
	 
	 @Test
	 public void When_Event_Contains_All_Only_One_Event_Type_Is_Returned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed, All, Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 Assert.assertEquals(1, defOne.getEventTypes().size());
		 Assert.assertEquals(4, defOne.getPacketTypes().size());
		 
		 Assert.assertTrue(defOne.getEventTypes().contains(EEventType.All));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 Assert.assertEquals(1, defTwo.getEventTypes().size());
		 Assert.assertEquals(1, defTwo.getPacketTypes().size());
	 }
	 
	 
	 @Test
	 public void When_Packet_Contains_All_Only_One_Packet_Type_Is_Returned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed, Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,All, Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 Assert.assertEquals(4, defOne.getEventTypes().size());
		 Assert.assertEquals(1, defOne.getPacketTypes().size());
		 
		 Assert.assertTrue(defOne.getPacketTypes().contains(EPacketType.All));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 Assert.assertEquals(1, defTwo.getEventTypes().size());
		 Assert.assertEquals(1, defTwo.getPacketTypes().size());		 
		 
	 }
	 
		
	 
	 //  persistRequiredInterceptors()
	 
	 @Test
	 public void When_No_Interceptors_Are_Present_Properties_Are_Not_Persisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic(Mockito.never());
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());
 
	 }
	 
	 
	 @Test
	 public void When_Required_Interceptor_Definitions_Are_Not_Present_No_Interceptors_Are_Persisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 
		 RequiredInterceptorDefinition def1 = new RequiredInterceptorDefinition(eventSet, packetSet);
		 requiredInterceptors.put("One", def1);
		 requiredInterceptors.put("Two", def1);

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic(Mockito.never());   
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());		 
	 }
	 
	 
	 @Test
	 public void When_Requied_Interceptor_Definitions_Are_Persisted_Set_Properties_Is_Called() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 eventSet.add(EEventType.Incoming);
		 eventSet.add(EEventType.Outgoing);
		 eventSet.add(EEventType.Processed);
		 eventSet.add(EEventType.Unprocessed);
		 
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 packetSet.add(EPacketType.IQ);
		 packetSet.add(EPacketType.Message);
		 packetSet.add(EPacketType.Presence);
		 packetSet.add(EPacketType.Roster);
		 
		 RequiredInterceptorDefinition def1 = new RequiredInterceptorDefinition(eventSet, packetSet);
		 requiredInterceptors.put("One", def1);
		 requiredInterceptors.put("Two", def1);

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic();
		 JiveGlobals.setProperties(captor.capture());
		 
		 Map<String, String> properties = captor.getValue();
		 		 
		 Assert.assertEquals(5, properties.size());
		 
		 String requiredInterceptorProperty = properties.get("interceptors.required");
		 Assert.assertTrue(requiredInterceptorProperty.indexOf("One") > -1);
		 Assert.assertTrue(requiredInterceptorProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes = properties.get("interceptor.blocking.event.One");
		 
		 Assert.assertTrue( requiredEventTypes.indexOf("Incoming") > -1);
		 Assert.assertTrue( requiredEventTypes.indexOf("Outgoing") > -1);
		 Assert.assertTrue( requiredEventTypes.indexOf("Processed") > -1);
		 Assert.assertTrue( requiredEventTypes.indexOf("Unprocessed") > -1);
		 
		 String requiredPacketTypes = properties.get("interceptor.blocking.type.One");
		 
		 Assert.assertTrue( requiredPacketTypes.indexOf("IQ") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Message") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Presence") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Roster") > -1);
	 }
	 
	 @Test
	 public void When_Required_Interceptor_Has_No_Events_Definitions_Properties_Are_Not_Persisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 packetSet.add(EPacketType.IQ);
		 packetSet.add(EPacketType.Message);
		 packetSet.add(EPacketType.Presence);
		 packetSet.add(EPacketType.Roster);
		 
		 RequiredInterceptorDefinition def1 = new RequiredInterceptorDefinition(eventSet, packetSet);
		 requiredInterceptors.put("One", def1);
		 requiredInterceptors.put("Two", def1);

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic();
		 JiveGlobals.setProperties(captor.capture());
		 
		 Map<String, String> properties = captor.getValue();
		 		 
		 Assert.assertEquals(3, properties.size());
		 
		 String requiredInterceptorProperty = properties.get("interceptors.required");
		 Assert.assertTrue(requiredInterceptorProperty.indexOf("One") > -1);
		 Assert.assertTrue(requiredInterceptorProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes = properties.get("interceptor.blocking.event.One");
		 
		 Assert.assertNull(requiredEventTypes);
		 
		 String requiredPacketTypes = properties.get("interceptor.blocking.type.One");
		 
		 Assert.assertTrue( requiredPacketTypes.indexOf("IQ") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Message") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Presence") > -1);
		 Assert.assertTrue( requiredPacketTypes.indexOf("Roster") > -1);
		 
	 }
	 
	 @Test
	 public void When_Events_Or_Packet_Types_Contain_All_Only_All_Is_Persisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 eventSet.add(EEventType.Incoming);
		 eventSet.add(EEventType.Outgoing);
		 eventSet.add(EEventType.Processed);
		 eventSet.add(EEventType.All);
		 eventSet.add(EEventType.Unprocessed);
		 
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 packetSet.add(EPacketType.IQ);
		 packetSet.add(EPacketType.Message);
		 packetSet.add(EPacketType.Presence);
		 packetSet.add(EPacketType.All);
		 packetSet.add(EPacketType.Roster);
		 
		 RequiredInterceptorDefinition def1 = new RequiredInterceptorDefinition(eventSet, packetSet);
		 requiredInterceptors.put("One", def1);

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic();
		 JiveGlobals.setProperties(captor.capture());
		 
		 Map<String, String> properties = captor.getValue();
		 		 
		 Assert.assertEquals(3, properties.size());
		 
		 Assert.assertEquals("One", properties.get("interceptors.required"));
		 
		 Assert.assertEquals("All", properties.get("interceptor.blocking.type.One"));
		 Assert.assertEquals("All", properties.get("interceptor.blocking.event.One"));
	 }
	 
}
