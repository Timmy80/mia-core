package com.github.timmy80.mia.messaging;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.timmy80.mia.core.TimeLimit;
import com.github.timmy80.mia.core.LogFmt;

public class MessagingTest {
	
	public static class MessagingTestTask extends MessagingTask<String, String> {
		
		private static Logger logger = LogManager.getLogger(MessagingTestTask.class);

		public MessagingTestTask(String name, Messaging<String, String> messaging) throws IllegalArgumentException {
			super(name, messaging);
		}

		@Override
		public void eventStartTask() {
			logger.info("{}", new LogFmt().append("task", getName()).append("event", "hello world"));
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
	
	static MessagingTestTask theTask;
	static MessagingTestTask theSecondTask;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Messaging<String, String> messaging = new Messaging<>();
		theTask = new MessagingTestTask("the-task", messaging);
		theTask.start();
		theSecondTask = new MessagingTestTask("the-second-task", messaging);
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
