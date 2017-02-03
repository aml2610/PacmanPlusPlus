package teamproject.networking.data;

import static org.junit.Assert.*;

import org.junit.Test;

public class PacketTest {
	@Test
	public void testDataReadWrite() {
		Packet p1 = new Packet("packetName");
		p1.setInteger("int1", 123);
		p1.setLong("longint", 40000000000l);
		p1.setDouble("pi", 3.14159);
		p1.setString("mystring", "weifj`/34$£5`$");
		String outputString = p1.toString();
		Packet p2 = Packet.fromString(outputString);
		assertEquals("packetName", p1.getPacketName());
		assertEquals(40000000000l, p2.getLong("longint"));
		assertEquals(123, p1.getInteger("int1"));
		assertEquals(3.14159, p1.getDouble("pi"), 0.00000001);
		assertEquals("weifj`/34$£5`$", p1.getString("mystring"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDataTypeCheck() {
		Packet p1 = new Packet("erer");
		p1.setInteger("stuff", 1234);
		p1.getDouble("stuff");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testArgumentExists() {
		Packet p1 = new Packet("rr");
		p1.getString("blah");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoDuplicateArguments() {
		Packet p1 = new Packet("rr");
		p1.setInteger("param", 123);
		p1.setFloat("param", 3.2222f);
	}
}
