package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.util.Timeout;

public class AyncTimerTest {
	
	static final long OFFSET = 50;
	
	static CompletableFuture<Long> stateFuture = new CompletableFuture<>();
	static CompletableFuture<Long> terminalFuture = new CompletableFuture<>();
	static CompletableFuture<Long> taskFuture = new CompletableFuture<>();
	static AyncTimerTestTask task = new AyncTimerTestTask();
	
	public static class AyncTimerTestTask extends Task {
		
		public static class AyncTimerTestTerminal extends Terminal<AyncTimerTestTask> {
			
			public class AyncTimerTestState extends TerminalState {

				long begin;
				
				@Override
				protected void eventEntry() {
					begin=System.currentTimeMillis();
					this.newTimeout(1000L, this::eventTimeout);
				}
				
				protected void eventTimeout(Timeout t) {
					stateFuture.complete(System.currentTimeMillis()-begin);
				}
				
			}
			
			long begin;

			public AyncTimerTestTerminal(AyncTimerTestTask task) {
				super(task);
				nextState(new AyncTimerTestState());
				
				begin=System.currentTimeMillis();
				this.newTimeout(1500L, this::eventTimeout);
			}
			
			protected void eventTimeout(Timeout t) {
				terminalFuture.complete(System.currentTimeMillis()-begin);
				terminate();
			}
			
		}

		long begin;
		
		public AyncTimerTestTask() throws IllegalArgumentException {
			super("timer-test-task");
		}

		@Override
		public void eventStartTask() {
			new AyncTimerTestTerminal(this);
			begin=System.currentTimeMillis();
			this.newTimeout(2000L, this::eventTimeout);
		}
		
		protected void eventTimeout(Timeout t) {
			taskFuture.complete(System.currentTimeMillis()-begin);
		}
		
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		task.start();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		task.stopTask();
	}

	@Test
	public void test() throws InterruptedException, ExecutionException, TimeoutException {
		long stateDuration = stateFuture.get(1000L+OFFSET, TimeUnit.MILLISECONDS);
		System.out.println(String.format("State duration %dms", stateDuration));
		assertFalse("State Duration too low.",   stateDuration < (1000L));
		assertFalse("State Duration too great.", stateDuration > (1000L+OFFSET));
		
		long teminalDuration = terminalFuture.get(1500L+OFFSET, TimeUnit.MILLISECONDS);
		System.out.println(String.format("Terminal duration %dms", teminalDuration));
		assertFalse("Terminal Duration too low.",   teminalDuration < (1500L));
		assertFalse("Terminal Duration too great.", teminalDuration > (1500L+OFFSET));
		
		long taskDuration = taskFuture.get(2000L+OFFSET, TimeUnit.MILLISECONDS);
		System.out.println(String.format("Task duration %dms", taskDuration));
		assertFalse("Task Duration too low.",   taskDuration < (2000L));
		assertFalse("Task Duration too great.", taskDuration > (2000L+OFFSET));
	}

}
