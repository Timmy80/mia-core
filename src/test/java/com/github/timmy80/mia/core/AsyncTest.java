package com.github.timmy80.mia.core;

import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncTest {
	
	public static class TestObject {
		
		public void eventWithNoArg() {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread! " + Thread.currentThread().getName());
		}
		
		public void eventWithOneArg(String arg0) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
		}
		
		public void eventWithTwoArg(String arg0, String arg1) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
		}
		
		public void eventWithThreeArg(String arg0, String arg1, String arg2) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
		}
		
		public void eventWithFourArg(String arg0, String arg1, String arg2, String arg4) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
		}

		
		public boolean methodWithNoArg() {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
			return true;
		}
		
		public String methodWithOneArg(String arg0) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
			return String.format("hello %s", arg0);
		}
		
		public String methodWithTwoArg(String arg0, String arg1) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
			return String.format("hello %s %s", arg0, arg1);
		}
		
		public String methodWithThreeArg(String arg0, String arg1, String arg2) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
			return String.format("hello %s %s %s", arg0, arg1, arg2);
		}
		
		public String methodWithFourArg(String arg0, String arg1, String arg2, String arg3) {
			if(thread != Thread.currentThread())
				throw new RuntimeException("Invalid caller thread!");
			
			return String.format("hello %s %s %s %s", arg0, arg1, arg2, arg3);
		}
		
	}
	
	static TestObject testObject;
	static Thread thread;
	static ExecutorService executor;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		 executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return (thread = new Thread(r, "Executor-test-thread"));
			}
		});
		testObject =  new TestObject();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		executor.shutdown();
	}

	@Test
	public void testRunLaterExecutorThrowingRunnable() throws InterruptedException, ExecutionException {
		Async.runLater(executor, testObject::eventWithNoArg).get();
	}

	@Test
	public void testRunLaterExecutorVoidFunction1OfT0T0() throws InterruptedException, ExecutionException {
		Async.runLater(executor, testObject::eventWithOneArg, "toto").get();
	}

	@Test
	public void testRunLaterExecutorVoidFunction2OfT0T1T0T1() throws InterruptedException, ExecutionException {
		Async.runLater(executor, testObject::eventWithTwoArg, "toto", "titi").get();
	}

	@Test
	public void testRunLaterExecutorVoidFunction3OfT0T1T2T0T1T2() throws InterruptedException, ExecutionException {
		Async.runLater(executor, testObject::eventWithThreeArg, "toto", "titi", "tata").get();
	}

	@Test
	public void testRunLaterExecutorVoidFunction4OfT0T1T2T3T0T1T2T3() throws InterruptedException, ExecutionException {
		Async.runLater(executor, testObject::eventWithFourArg, "toto", "titi", "tata", "tutu").get();
	}

	@Test
	public void testCallLaterExecutorCallableOfR() throws InterruptedException, ExecutionException {
		Async.callLater(executor, testObject::methodWithNoArg).get();
	}

	@Test
	public void testCallLaterExecutorFunction1OfRT0T0() throws InterruptedException, ExecutionException {
		Async.callLater(executor, testObject::methodWithOneArg, "toto").get();
	}

	@Test
	public void testCallLaterExecutorFunction2OfRT0T1T0T1() throws InterruptedException, ExecutionException {
		Async.callLater(executor, testObject::methodWithTwoArg, "toto", "titi").get();
	}

	@Test
	public void testCallLaterExecutorFunction3OfRT0T1T2T0T1T2() throws InterruptedException, ExecutionException {
		Async.callLater(executor, testObject::methodWithThreeArg, "toto", "titi", "tata").get();
	}

	@Test
	public void testCallLaterExecutorFunction4OfRT0T1T2T3T0T1T2T3() throws InterruptedException, ExecutionException {
		Async.callLater(executor, testObject::methodWithFourArg, "toto", "titi", "tata", "tutu").get();
	}
	
	@Test
	public void testJavaExecuteRunnable() {
		Async.execute(executor, new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	@Test(expected = TimeLimitExceededException.class)
	public void testJavaExecuteBeforeRunnableInterupted() throws Throwable {
		try {
			Async.callBefore(executor, TimeLimit.in(3, TimeUnit.SECONDS), new Callable<String>() {

				@Override
				public String call() throws Exception {
					Thread.sleep(3500);
					if(!Thread.interrupted())
						throw new IllegalStateException("Sould have been interrupted!");
					else
						return "fail me please";
				}
			}).get();
		} catch (ExecutionException e) {
			throw e.getCause();
		}
		fail("Should have ended in exception");
	}

}
