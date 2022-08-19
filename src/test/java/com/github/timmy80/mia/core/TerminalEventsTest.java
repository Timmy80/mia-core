package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class TerminalEventsTest {
	
	private static Logger logger = LogManager.getLogger(TerminalEventsTest.class.getName());
	
	public static class TestTask extends Task {

		public TestTask(String name) throws IllegalArgumentException {
			super(name);
		}

		@Override
		public void eventStartTask() {
			// TODO Auto-generated method stub
			
		}
		
		public TestTerminal createTestTerminal() {
			TestTerminal t = new TestTerminal(task);
			logger.always().log("{} {}", t, new LogFmt().append("event", "creating new test terminal"));
			return t;
		}
		
	}
	
	public static class TestTerminal extends Terminal<TestTask> {
		
		Terminal<TestTask> self = this;
		
		TestState1 s1 = new TestState1();
		TestState2 s2 = new TestState2();
		
		public class TestState1 extends TerminalState {
			
			public TestState1() {
				setTerminal(self);
			}

			@Override
			protected void eventEntry() {
				// Immediately change state
				nextState(s2);
			}
			
			public void iWillNeverBeCalled() {
				throw new NotImplementedException();
			}

		}
		
		public class TestState2 extends TerminalState {
			public TestState2() {
				setTerminal(self);
			}
			
			@Override
			protected void eventEntry() {
			}
			
			public void callMeOnce() {
				// nothing to be done
				logger.always().log("{} {}", self, new LogFmt()
						.append("event", "callMeOnce called"));
				terminate();
			}
			
		}

		public TestTerminal(TestTask task) {
			super(task);
		}
		
		public void iWillNeverBeCalled() {
			throw new NotImplementedException();
		}
		
	}
	
	public static TestTask task = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		task = new TestTask("TEST-TERM-EVTS");
		task.start();
		Thread.sleep(30);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		task.stopTask();
	}

	@Test
	public void test() throws InterruptedException, ExecutionException {
		Configurator.setLevel("com.github.timmy80.mia.core", Level.DEBUG);
		
		TestTerminal t = task.callLater(task::createTestTerminal).get();
		t.runLater(t::nextState, t.s1).get();
		
		assertThrows(InactiveStateException.class, () -> {
			try {
				t.s1.runLater(t.s1::iWillNeverBeCalled).get();
			} catch (InterruptedException e) {
				throw e;
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		});
		
		t.s2.runLater(t.s2::callMeOnce).get();
		
		assertThrows(InactiveStateException.class, () -> {
			try {
				t.s2.runLater(t.s2::callMeOnce).get();
			} catch (InterruptedException e) {
				throw e;
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		});
		
		assertThrows(TerminatedTerminalException.class, () -> {
			try {
				t.runLater(t::iWillNeverBeCalled).get();
			} catch (InterruptedException e) {
				throw e;
			} catch (ExecutionException e) {
				throw e.getCause();
			}
		});
	}

}
