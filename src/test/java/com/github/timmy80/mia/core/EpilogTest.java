package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class EpilogTest {
	
	public static class TestTask extends Task {
		
		private static Logger logger = LogManager.getLogger(TestTask.class.getName());
		
		CompletableFuture<Void> epilog = new CompletableFuture<Void>();

		public TestTask(String name) throws IllegalArgumentException {
			super(name);
		}

		@Override
		public void eventStartTask() {
		}
		
		@Override
		protected void eventStopRequested() {
			// avoid the task from stopping using an epilog
			logger.always().log("{} {}", this, new LogFmt().append("event", "eventStopRequested handled. Avoiding stop with an epilog."));
			registerEpilog(epilog);
		}
		
		public void callMeToCompleteEpilog() {
			logger.always().log("{} {}", this, new LogFmt().append("event", "callMeToCompleteEpilog handled. Completing epilog."));
			epilog.complete(null);
		}
		
	}
	

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws InterruptedException {
		TestTask task = new TestTask("EPILOG-TASK");
		task.start();
		
		Thread.sleep(30); // let the task start
		
		task.stopTask(); // request the task to stop
		Thread.sleep(30); // ensure stop has been taken into account
		
		assertTrue(task.isAlive()); // check task isAlive waiting for epilog completion
		
		task.runLater(task::callMeToCompleteEpilog); // push a new job to complete the epilog

		Thread.sleep(30); // ensure job as been done
		
		assertFalse(task.isAlive()); // check task is stopped
	}

}
