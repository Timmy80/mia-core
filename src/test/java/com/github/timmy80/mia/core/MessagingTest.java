package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MessagingTest {
	
	public static class TheTask extends Task {

		public TheTask(String name) throws IllegalArgumentException {
			super(name);
		}

		@Override
		public void eventStartTask() {
			System.out.println("Hello world");
		}
	}
	
	public static class TheSubscriber implements Subscriber<String> {
		private final String name;
		private int receivedPublish = 0;
		
		public TheSubscriber(String name) {
			this.name = name;
		}
		
		public int getReceivedPublish() {
			return receivedPublish;
		}

		@Override
		public void eventReceivePublish(MessageCtx<?> message, String payload) {
			receivedPublish++;
			System.out.println(String.format("[%s] %s %s: %d", Thread.currentThread(), name, message.getTopic(), receivedPublish));
		}
	}
	
	static TheTask theTask;
	static TheTask theSecondTask;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		theTask = new TheTask("the-task");
		theTask.start();
		theSecondTask = new TheTask("the-second-task");
		theSecondTask.start();
		

		theTask.subscribe("test/nominal/string", new Subscriber<String>() {

			@Override
			public void eventReceivePublish(MessageCtx<?> message, String payload) {
				System.out.println("Publish received");
				System.out.println(payload);
				message.reply("bar");
			}
		});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		theTask.stopTask();
		theSecondTask.stopTask();
	}
	
	
	@Test
	public void testRegisterUnregister() {
		Subscriber<String> sub = new Subscriber<String>() {
			
			@Override
			public void eventReceivePublish(MessageCtx<?> message, String payload) {
				
			}
		};
		
		theTask.subscribe("registered", sub);
		theTask.publish("registered", "registered", TimeLimit.in(5000));
		theTask.unsubscribe("registered", sub);
		try {
			theTask.publish("registered", "registered", TimeLimit.in(5000));
			fail("last publish MUST throw InvalidTopicException");
		} catch(InvalidTopicException e) {
			// good
		}
	}

	@Test
	public void testPublish() throws InterruptedException, ExecutionException {
		theTask.publish("test/nominal/string", "foo", TimeLimit.in(5000), new ResponseHandler<String>() {
			
			@Override
			public void eventResponseReceived(MessageCtx<String> message, String payload) {
				System.out.println("Response received");
				System.out.println(payload);
			}
		}).join();
	}
	
	@Test
	public void testPush() throws InterruptedException, ExecutionException {
		theTask.publish("test/nominal/string", "foo", TimeLimit.in(5000)).join();
	}
	
	@Test(expected = ClassCastException.class)
	public void testPublishSubscriberCastException() throws Throwable {
		try {
			theTask.publish("test/nominal/string", 842L, TimeLimit.in(5000), new ResponseHandler<Long>() {
				
				@Override
				public void eventResponseReceived(MessageCtx<Long> message, Long payload) {
					System.out.println("Response received");
					System.out.println(payload);
				}
			}).join();
		} catch (ExecutionException e) {
			e.getCause().printStackTrace();
			throw e.getCause();
		}
	}

//	@Test(expected = ClassCastException.class)
//	public void testPublishResponseHandlerCastException() throws Throwable {
//		System.out.println(theTask.publish("test/nominal/string", "842", TimeLimit.in(5000), new ResponseHandler<Long>() {
//			
//			@Override
//			public void eventResponseReceived(MessageCtx<Long> message, Long payload) {
//				System.out.println("Response received");
//				System.out.println(payload);
//			}
//		}).get());
//	}
	
	@Test(expected = ClassCastException.class)
	public void testPublishResponseHandlerCastException() throws Throwable {
		try {	
			theTask.publish("test/nominal/string", "842", TimeLimit.in(5000), new ResponseHandler<Long>() {
				
				@Override
				public void eventResponseReceived(MessageCtx<Long> message, Long payload) {
					System.out.println("Response received");
					System.out.println(payload);
				}
			}).join();
		} catch (ExecutionException e) {
			e.getCause().printStackTrace();
			throw e.getCause();
		}
	}
	
	@Test
	public void testPublishDistribution() throws InterruptedException, ExecutionException {
		TheSubscriber sub1 = new TheSubscriber("Sub1");
		TheSubscriber sub2 = new TheSubscriber("Sub2");
		TheSubscriber sub3 = new TheSubscriber("Sub3");
		theTask.subscribe("#", sub1);
		theTask.subscribe("+/matching/topic", sub2);
		theSecondTask.subscribe("the/#", sub3);
		
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		theTask.publish("the/matching/topic", "push", TimeLimit.in(5000)).join();
		
		assertEquals(2, sub1.getReceivedPublish());
		assertEquals(2, sub2.getReceivedPublish());
		assertEquals(2, sub3.getReceivedPublish());
		
		theTask.unsubscribe("#", sub1);
		theTask.unsubscribe("+/matching/topic", sub2);
		theSecondTask.unsubscribe("#", sub1);
	}

}
