package teamproject.networking.data;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;

import teamproject.networking.StandardClientManager;

public class StandardClientManagerTest {
	@Test
	public void testTriggersTriggered() {
		ArrayList<Integer> received = new ArrayList<Integer>();
		
		StandardClientManager nm = new StandardClientManager(new MockSocket(0));
		
		nm.addTrigger(t -> {
			received.add(t.getInteger("value"));
		}, "data");
		
		nm.addTrigger(t -> {
			received.add(t.getInteger("value") + 1);
		}, "data-plus-1");
		
		Packet p1 = new Packet("data");
		p1.setInteger("value", 3);
		nm.receive(p1.toString().getBytes(StandardCharsets.UTF_8));
		
		Packet p2 = new Packet("data-plus-1");
		p2.setInteger("value", 5);
		nm.receive(p2.toString().getBytes(StandardCharsets.UTF_8));
		
		assertTrue(received.contains(3));
		assertTrue(received.contains(6));
		assertEquals(2, received.size());
	}
	
	@Test(expected=RuntimeException.class)
	public void testNonexistentTrigger() {
		StandardClientManager nm = new StandardClientManager(new MockSocket(0));
		Packet p1 = new Packet("data");
		nm.receive(p1.toString().getBytes(StandardCharsets.UTF_8));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testNoMultipleTriggers() {
		StandardClientManager nm = new StandardClientManager(new MockSocket(0));

		
		nm.addTrigger(t -> {
			System.out.println("d");
		}, "data");
		
		nm.addTrigger(t -> {
			System.out.println("e");
		}, "data");
	}
}
