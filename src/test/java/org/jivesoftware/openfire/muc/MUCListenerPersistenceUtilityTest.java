package org.jivesoftware.openfire.muc;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.openfire.muc.EMUCEventType;
import org.jivesoftware.openfire.muc.MUCListenerPersistenceUtility;
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
public class MUCListenerPersistenceUtilityTest {

	@Captor
	 ArgumentCaptor<Map<String, String>> captor;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(JiveGlobals.class);
	}
	
	//  loadRequiredListeners()
	 
	 @Test
	 public void When_No_Required_Listeners_Empty_Map_Is_Returned() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn(null);
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 
		 Assert.assertEquals(0, results.size()); 
	 }
	 
	 
	 @Test
	 public void When_Required_Listeners_Are_Specified_But_No_Values_Are_Persisted_Empty_Set_Is_Returned_For_Each_Listener() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn("One,Two,Three");
		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.One")).thenReturn(null);
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 Assert.assertEquals(3, results.size());
		 
		 for(Set<EMUCEventType> val : results.values()) {
			 Assert.assertEquals(0, val.size());
		 }
	 }
	 
	 
	 @Test
	 public void When_Required_Listeners_Are_Persisted_Values_Are_Returned_For_Each_Listener() {
		 PowerMockito.when(JiveGlobals.getProperty("muc.listeners.required")).thenReturn("One,Two,Three");
		 
		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.One")).thenReturn("BeforeCreated,Created,BeforeDestroyed,Destroyed");

		 PowerMockito.when(JiveGlobals.getProperty("muc.listener.blocking.event.Two")).thenReturn("Joined");
		 
		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 Map<String, Set<EMUCEventType>>results = util.loadRequiredListeners();
		 Assert.assertEquals(3, results.size());
		 
		 Set<EMUCEventType> setOne = results.get("One");
		 Assert.assertEquals(4, setOne.size());
		 
		 Assert.assertTrue(setOne.contains(EMUCEventType.BeforeCreated));
		 Assert.assertTrue(setOne.contains(EMUCEventType.Created));
		 Assert.assertTrue(setOne.contains(EMUCEventType.BeforeDestroyed));
		 Assert.assertTrue(setOne.contains(EMUCEventType.Destroyed));
		 
		 Set<EMUCEventType> setTwo = results.get("Two");
		 Assert.assertEquals(1, setTwo.size());		 
		 Assert.assertTrue(setTwo.contains(EMUCEventType.Joined));
		 
		 Set<EMUCEventType> setThree = results.get("Three");
		 Assert.assertEquals(0, setThree.size());
		 Assert.assertEquals(0, setThree.size());
	 }
 
	 //  persistRequiredListeners()
	 
	 @Test
	 public void When_No_Listeners_Are_Present_Properties_Are_Not_Persisted() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic(Mockito.never());
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());
	 }

	 
	 @Test
	 public void When_Required_Event_Types_Are_Not_Present_No_Listeners_Are_Persisted() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();
		 Set<EMUCEventType> emptyEventTypeSet = Collections.<EMUCEventType>emptySet();

		 requiredListeners.put("One", emptyEventTypeSet);
		 requiredListeners.put("Two", emptyEventTypeSet);

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic(Mockito.never());   
		 JiveGlobals.setProperties(Matchers.<Map<String, String>>any());		 
	 }
	

	 @Test
	 public void When_Requied_Listeners_Are_Persisted_Set_Properties_Is_Called() {
		 Map<String, Set<EMUCEventType>> requiredListeners = new HashMap<String, Set<EMUCEventType>>();
		 Set<EMUCEventType> eventSet1 = new HashSet<EMUCEventType>();
		 eventSet1.add(EMUCEventType.Created);
		 eventSet1.add(EMUCEventType.Destroyed);
		 eventSet1.add(EMUCEventType.Joined);
		 eventSet1.add(EMUCEventType.Left);
		 
		 Set<EMUCEventType> eventSet2 = new HashSet<EMUCEventType>();
		 eventSet2.add(EMUCEventType.BeforeCreated);
		 eventSet2.add(EMUCEventType.BeforeJoined);		 
		 
		 requiredListeners.put("One", eventSet1);
		 requiredListeners.put("Two", eventSet2);

		 MUCListenerPersistenceUtility util = new MUCListenerPersistenceUtility();
		 
		 util.persistRequiredListeners(requiredListeners);
		 PowerMockito.verifyStatic();
		 JiveGlobals.setProperties(captor.capture());
		 
		 Map<String, String> properties = captor.getValue();
		 		 
		 Assert.assertEquals(3, properties.size());
		 
		 String requiredListenerProperty = properties.get("muc.listeners.required");
		 Assert.assertTrue(requiredListenerProperty.indexOf("One") > -1);
		 Assert.assertTrue(requiredListenerProperty.indexOf("Two") > -1);
		 
		 String requiredEventTypes1 = properties.get("muc.listener.blocking.event.One");
		 
		 Assert.assertTrue( requiredEventTypes1.indexOf("Created") > -1);
		 Assert.assertTrue( requiredEventTypes1.indexOf("Destroyed") > -1);
		 Assert.assertTrue( requiredEventTypes1.indexOf("Joined") > -1);
		 Assert.assertTrue( requiredEventTypes1.indexOf("Left") > -1);
		 
		 String requiredEventTypes2 = properties.get("muc.listener.blocking.event.Two");
		 
		 Assert.assertTrue( requiredEventTypes2.indexOf("BeforeCreated") > -1);
		 Assert.assertTrue( requiredEventTypes2.indexOf("BeforeJoined") > -1);
	 }
}
