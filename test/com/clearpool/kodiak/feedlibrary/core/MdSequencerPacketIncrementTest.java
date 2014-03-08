package com.clearpool.kodiak.feedlibrary.core;


import org.junit.Assert;
import org.junit.Test;

import com.clearpool.kodiak.feedlibrary.core.MdSequencer;

@SuppressWarnings("static-method")
public class MdSequencerPacketIncrementTest
{
	@Test
	public void testNoGaps()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 2 - [2,6]
		TestMdFeedPacket packetTwo = new TestMdFeedPacket(2, 5, true, false);
		sequencer.sequencePacket(selectionKeyB, packetTwo);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(3, sequencer.getNextSequenceNumber());
		
		sequencer.sequencePacket(selectionKeyA, packetTwo);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(3, sequencer.getNextSequenceNumber());
		
		//Packet 3 - [7,10]
		TestMdFeedPacket packetThree = new TestMdFeedPacket(3, 4, true, false);
		sequencer.sequencePacket(selectionKeyA, packetThree);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
		
		sequencer.sequencePacket(selectionKeyB, packetThree);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
	}

	@Test
	public void testGapOnARecoveredByB()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3 - A-[7,10]
		TestMdFeedPacket packetTwoA = new TestMdFeedPacket(3, 4, true, false);
		sequencer.sequencePacket(selectionKeyA, packetTwoA);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(1, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());

		//Packet 2 - B-[2,6]
		TestMdFeedPacket packetTwoB = new TestMdFeedPacket(2, 5, true, false);
		sequencer.sequencePacket(selectionKeyB, packetTwoB);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testGapOnBRecoveredByA()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3 - B-[7,10]
		TestMdFeedPacket packetTwoB = new TestMdFeedPacket(3, 4, true, false);
		sequencer.sequencePacket(selectionKeyB, packetTwoB);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(1, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());

		//Packet 2 - A-[2,6]
		TestMdFeedPacket packetTwoA = new TestMdFeedPacket(2, 5, true, false);
		sequencer.sequencePacket(selectionKeyA, packetTwoA);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testOutOfOrder()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3 - [7,10]
		TestMdFeedPacket packetThree = new TestMdFeedPacket(3, 4, true, false);
		sequencer.sequencePacket(selectionKeyA, packetThree);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(1, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		sequencer.sequencePacket(selectionKeyB, packetThree);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(2, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 2 - [2,6]
		TestMdFeedPacket packetTwo = new TestMdFeedPacket(2, 5, true, false);
		sequencer.sequencePacket(selectionKeyB, packetTwo);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
		
		sequencer.sequencePacket(selectionKeyA, packetTwo);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(4, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testOutOfOrderThreshold()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - A+B [1,2]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 2, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3-52 - A+B [4,102]
		for(int i=3; i<=52; i++)
		{
			TestMdFeedPacket packetA = new TestMdFeedPacket(i, 2, true, false);
			sequencer.sequencePacket(selectionKeyA, packetA);
			TestMdFeedPacket packetB = new TestMdFeedPacket(i, 2, true, false);
			sequencer.sequencePacket(selectionKeyB, packetB);
			Assert.assertEquals(0, sequencer.getDropCount());
			Assert.assertEquals((i-2)*2, sequencer.getQueueSize());
			Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		}

		//Packet 53 - A+B [103,104]
		TestMdFeedPacket packetA = new TestMdFeedPacket(53, 2, true, false);
		sequencer.sequencePacket(selectionKeyA, packetA);
		TestMdFeedPacket packetB = new TestMdFeedPacket(53, 2, true, false);
		sequencer.sequencePacket(selectionKeyB, packetB);
		Assert.assertEquals(1, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(54, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testLaggingWaitThreshold()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - A+B [1,2]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 2, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3-2001 - A [3,4004]
		for(int i=3; i<2002; i++)
		{
			TestMdFeedPacket packetA = new TestMdFeedPacket(i, 2, true, false);
			sequencer.sequencePacket(selectionKeyA, packetA);
			Assert.assertEquals(0, sequencer.getDropCount());
			Assert.assertEquals((i-2), sequencer.getQueueSize());
			Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		}

		//Packet 2002 - A [4005,4006]
		TestMdFeedPacket packetA = new TestMdFeedPacket(2002, 2, true, false);
		sequencer.sequencePacket(selectionKeyA, packetA);
		Assert.assertEquals(1, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2003, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testSequenceNumberResetInSequence()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		
		//Packet 2 - [2,6]
		TestMdFeedPacket packetTwo = new TestMdFeedPacket(2, 5, true, false);
		sequencer.sequencePacket(selectionKeyB, packetTwo);
		sequencer.sequencePacket(selectionKeyA, packetTwo);
		
		//Packet 3 - [7,7]
		TestMdFeedPacket packetThree = new TestMdFeedPacket(3, 1, false, true);
		sequencer.sequencePacket(selectionKeyA, packetThree);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(1, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testSequenceNumberResetOutOfSequence()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		
		//Packet 3 - [7,7]
		TestMdFeedPacket packetThree = new TestMdFeedPacket(3, 1, false, true);
		sequencer.sequencePacket(selectionKeyA, packetThree);
		Assert.assertEquals(1, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(1, sequencer.getNextSequenceNumber());
	}
	
	@Test
	public void testQueueGiveUpTime()
	{
		TestSequenceMessageReceivable callback = new TestSequenceMessageReceivable();
		MdSequencer sequencer = new MdSequencer(callback, "TEST", false);
		Object selectionKeyA = new Object();
		Object selectionKeyB = new Object();
		sequencer.setSelectionKeyA(selectionKeyA);
		sequencer.setSelectionKeyB(selectionKeyB);
		
		//Packet 1 - A+B [1,1]
		TestMdFeedPacket packetOne = new TestMdFeedPacket(1, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetOne);
		sequencer.sequencePacket(selectionKeyB, packetOne);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		//Packet 3 - A [3,3]
		TestMdFeedPacket packetTwo = new TestMdFeedPacket(3, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetTwo);
		Assert.assertEquals(0, sequencer.getDropCount());
		Assert.assertEquals(1, sequencer.getQueueSize());
		Assert.assertEquals(2, sequencer.getNextSequenceNumber());
		
		try{Thread.sleep(3100);} catch(Exception e){e.printStackTrace();}
		
		//Packet 3 - A [4,4]
		TestMdFeedPacket packetThree = new TestMdFeedPacket(4, 1, true, false);
		sequencer.sequencePacket(selectionKeyA, packetThree);
		Assert.assertEquals(1, sequencer.getDropCount());
		Assert.assertEquals(0, sequencer.getQueueSize());
		Assert.assertEquals(5, sequencer.getNextSequenceNumber());
	}
}