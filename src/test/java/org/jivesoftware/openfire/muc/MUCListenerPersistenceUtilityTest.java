package org.jivesoftware.openfire.muc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.util.JiveGlobals;
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
public class MUCListenerPersistenceUtilityTest {

	@Captor
	 ArgumentCaptor<Map<String, String>> captor;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(JiveGlobals.class);
	}
	
	//  loadRequiredListeners()
	 
	 @Test
	 public void whenThereAreNoRequiredListenersThenEmptyMapIsReturned() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn(null);
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 
		 assertEquals(0, results.size()); 
	 }
	 
	 
	 @Test
	 public void whenRequiredListenersAreSpecifiedButNoValuesArePersistedThenEmptySetIsReturnedForEachListener() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn("One,Two,Three");
		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.One")).thenReturn(null);
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 assertEquals(3, results.size());
		 
		 for(Set<EMUCEventType> val : results.values()) {
			 assertEquals(0, val.size());
		 }
	 }
	 
	 
	 @Test
	 public void whenRequiredListenersArePersistedThenValuesAreReturnedForEachListener() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.One")).thenReturn("BeforeCreated,BeforeJoined,BeforeDestroyed,BeforeSubjectChanged");

		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.Two")).thenReturn("BeforePrivateMessageReceived");
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 assertEquals(3, results.size());
		 
		 Set<EMUCEventType> setOne = results.get("One");
		 assertEquals(4, setOne.size());
		 
		 assertTrue(setOne.contains(EMUCEventType.BeforeCreated));
		 assertTrue(setOne.contains(EMUCEventType.BeforeJoined));
		 assertTrue(setOne.contains(EMUCEventType.BeforeDestroyed));
		 assertTrue(setOne.contains(EMUCEventType.BeforeSubjectChanged));
		 
		 Set<EMUCEventType> setTwo = results.get("Two");
		 assertEquals(1, setTwo.size());		 
		 assertTrue(setTwo.contains(EMUCEventType.BeforePrivateMessageReceived));
		 
		 Set<EMUCEventType> setThree = results.get("Three");
		 assertEquals(0, setThree.size());
		 assertEquals(0, setThree.size());
	 }
 
	 //  persistRequiredListeners()
	 
	 @Test
	 public void whenNoListenersArePresentThenPropertiesAreNotPersisted() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic(never());
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());
	 }

	 
	 @Test
	 public void whenRequiredEventTypesAreNotPresentThenNoListenersArePersisted() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();
		 Set<EMUCEventType> emptyEventTypeSet = Collections.<EMUCEventType>emptySet();

		 requiredListeners.put("One", emptyEventTypeSet);
		 requiredListeners.put("Two", emptyEventTypeSet);

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic(never());   
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());		 
	 }
	

	 @Test
	 public void whenRequiedListenersArePersistedThenSetPropertiesIsCalled() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();
		 Set<EMUCEventType> eventSet1 = new HashSet<EMUCEventType>();
		 eventSet1.add(EMUCEventType.BeforeCreated);
		 eventSet1.add(EMUCEventType.BeforeDestroyed);
		 eventSet1.add(EMUCEventType.BeforeJoined);
		 
		 Set<EMUCEventType> eventSet2 = new HashSet<EMUCEventType>();
		 eventSet2.add(EMUCEventType.BeforeNickChanged);
		 eventSet2.add(EMUCEventType.BeforeMessageReceived);		 
		 
		 requiredListeners.put("One", eventSet1);
		 requiredListeners.put("Two", eventSet2);

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic();
		 JiveGlobals.setProperties(captor.capture());
		 
		 Map<String, String> properties = captor.getValue();
		 		 
		 assertEquals(3, properties.size());
		 
		 String requiredListenerProperty = properties.get("muc.listeners.required");
		 assertTrue(requiredListenerProperty.indexOf("One") > -1);
		 assertTrue(requiredListenerProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes1 = properties.get("muc.listener.blocking.event.One");
		 
		 assertTrue( requiredEventTypes1.indexOf("BeforeCreated") > -1);
		 assertTrue( requiredEventTypes1.indexOf("BeforeDestroyed") > -1);
		 assertTrue( requiredEventTypes1.indexOf("BeforeJoined") > -1);
		 
		 String requiredEventTypes2 = properties.get("muc.listener.blocking.event.Two");
		 
		 assertTrue( requiredEventTypes2.indexOf("BeforeNickChanged") > -1);
		 assertTrue( requiredEventTypes2.indexOf("BeforeMessageReceived") > -1);
	 }
}
