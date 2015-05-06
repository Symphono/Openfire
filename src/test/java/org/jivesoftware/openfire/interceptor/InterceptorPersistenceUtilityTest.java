package org.jivesoftware.openfire.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveGlobals;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
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
	 public void whenNoRequiredInterceptorsAreConfiguredThenEmptyMapIsReturned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn(null);
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 
		 Assert.assertEquals(0, results.size()); 
	 }
	 
	 
	 @Test
	 public void whenRequiredInterceptorsAreSpecifiedButNoValuesArePersistedThenEmptyDefinitionIsReturnedForEachInterceptor() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 for(RequiredInterceptorDefinition val : results.values()) {
			 assertNotNull(val.getEventTypes());
			 assertNotNull(val.getPacketTypes());
			 assertEquals(0, val.getEventTypes().size());
			 assertEquals(0, val.getPacketTypes().size());
		 }
	 }
	 
	 
	 @Test
	 public void whenRequiredInterceptorsArePersistedThenValuesAreReturnedForEachInterceptor() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed,Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 Assert.assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 assertEquals(4, defOne.getEventTypes().size());
		 assertEquals(4, defOne.getPacketTypes().size());
		 
		 assertTrue(defOne.getEventTypes().contains(EEventType.Incoming));
		 assertTrue(defOne.getEventTypes().contains(EEventType.Outgoing));
		 assertTrue(defOne.getEventTypes().contains(EEventType.Processed));
		 assertTrue(defOne.getEventTypes().contains(EEventType.Unprocessed));
		 
		 assertTrue(defOne.getPacketTypes().contains(EPacketType.Presence));
		 assertTrue(defOne.getPacketTypes().contains(EPacketType.IQ));
		 assertTrue(defOne.getPacketTypes().contains(EPacketType.Message));
		 assertTrue(defOne.getPacketTypes().contains(EPacketType.Roster));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 assertEquals(1, defTwo.getEventTypes().size());
		 assertEquals(1, defTwo.getPacketTypes().size());
		 
		 assertTrue(defTwo.getEventTypes().contains(EEventType.Incoming));

		 assertTrue(defTwo.getPacketTypes().contains(EPacketType.IQ));
		 
		 RequiredInterceptorDefinition defThree = results.get("Three");
		 assertEquals(0, defThree.getEventTypes().size());
		 assertEquals(0, defThree.getPacketTypes().size());
	 }
	 
	 
	 @Test
	 public void whenEventContainsAllThenOnlyOneEventTypeIsReturned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed, All, Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 assertEquals(1, defOne.getEventTypes().size());
		 assertEquals(4, defOne.getPacketTypes().size());
		 
		 assertTrue(defOne.getEventTypes().contains(EEventType.All));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 assertEquals(1, defTwo.getEventTypes().size());
		 assertEquals(1, defTwo.getPacketTypes().size());
	 }
	 
	 
	 @Test
	 public void whenPacketContainsAllThenOnlyOnePacketTypeIsReturned() {
		 PowerMockito.when(JiveGlobals.getProperty("interceptors.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.One")).thenReturn("Incoming,Outgoing,Processed, Unprocessed");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.One")).thenReturn("Presence,IQ,All, Message,Roster");

		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.event.Two")).thenReturn("Incoming");
		 PowerMockito.when(JiveGlobals.getProperty("interceptor.blocking.type.Two")).thenReturn("IQ");
		 
		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 Map<String, RequiredInterceptorDefinition>results = util.loadRequiredInterceptors();
		 assertEquals(3, results.size());
		 
		 RequiredInterceptorDefinition defOne = results.get("One");
		 assertEquals(4, defOne.getEventTypes().size());
		 assertEquals(1, defOne.getPacketTypes().size());
		 
		 assertTrue(defOne.getPacketTypes().contains(EPacketType.All));
		 
		 RequiredInterceptorDefinition defTwo = results.get("Two");
		 assertEquals(1, defTwo.getEventTypes().size());
		 assertEquals(1, defTwo.getPacketTypes().size());		 
	 }
		
	 
	 //  persistRequiredInterceptors()
	 
	 @Test
	 public void whenNoInterceptorsArePresentThenPropertiesAreNotPersisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic(never());
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());
	 }
	 
	 
	 @Test
	 public void whenRequiredInterceptorDefinitionsAreNotPresentThenNoInterceptorsArePersisted() {
		 Map<String, RequiredInterceptorDefinition> requiredInterceptors = new HashMap<String, RequiredInterceptorDefinition>();
		 Set<EEventType> eventSet = new HashSet<EEventType>();
		 Set<EPacketType> packetSet = new HashSet<EPacketType>();
		 
		 RequiredInterceptorDefinition def1 = new RequiredInterceptorDefinition(eventSet, packetSet);
		 requiredInterceptors.put("One", def1);
		 requiredInterceptors.put("Two", def1);

		 InterceptorPersistenceUtility util = new InterceptorPersistenceUtility();
		 
		 util.persistRequiredInterceptors(requiredInterceptors);
		 PowerMockito.verifyStatic(never());   
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());		 
	 }
	 
	 
	 @Test
	 public void whenRequiedInterceptorDefinitionsArePersistedThenSetPropertiesIsCalled() {
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
		 		 
		 assertEquals(5, properties.size());
		 
		 String requiredInterceptorProperty = properties.get("interceptors.required");
		 assertTrue(requiredInterceptorProperty.indexOf("One") > -1);
		 assertTrue(requiredInterceptorProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes = properties.get("interceptor.blocking.event.One");
		 
		 assertTrue( requiredEventTypes.indexOf("Incoming") > -1);
		 assertTrue( requiredEventTypes.indexOf("Outgoing") > -1);
		 assertTrue( requiredEventTypes.indexOf("Processed") > -1);
		 assertTrue( requiredEventTypes.indexOf("Unprocessed") > -1);
		 
		 String requiredPacketTypes = properties.get("interceptor.blocking.type.One");
		 
		 assertTrue( requiredPacketTypes.indexOf("IQ") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Message") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Presence") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Roster") > -1);
	 }
	 
	 
	 @Test
	 public void whenRequiredInterceptorHasNoEventDefinitionsThenPropertiesAreNotPersisted() {
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
		 		 
		 assertEquals(3, properties.size());
		 
		 String requiredInterceptorProperty = properties.get("interceptors.required");
		 assertTrue(requiredInterceptorProperty.indexOf("One") > -1);
		 assertTrue(requiredInterceptorProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes = properties.get("interceptor.blocking.event.One");
		 
		 assertNull(requiredEventTypes);
		 
		 String requiredPacketTypes = properties.get("interceptor.blocking.type.One");
		 
		 assertTrue( requiredPacketTypes.indexOf("IQ") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Message") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Presence") > -1);
		 assertTrue( requiredPacketTypes.indexOf("Roster") > -1);
	 }
	 
	 
	 @Test
	 public void whenEventsOrPacketTypesContainAllThenOnlyAllIsPersisted() {
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
		 		 
		 assertEquals(3, properties.size());
		 
		 assertEquals("One", properties.get("interceptors.required"));
		 
		 assertEquals("All", properties.get("interceptor.blocking.type.One"));
		 assertEquals("All", properties.get("interceptor.blocking.event.One"));
	 }

}
