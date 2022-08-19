package com.github.timmy80.mia.core;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogFmtTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		LogFmt fmt = new LogFmt()
				.append("`Something. very H0r1ble*€$-is@Th3%key`", "`an.awefull \"value\"`")
				.append("second key", "/a/path/of/some/sort")
				.append("anumber", 1);
		
		System.out.println(fmt);
		assertEquals("Something_very_H0r1bleisTh3key=\"`an.awefull \\\"value\\\"`\" second_key=\"/a/path/of/some/sort\" anumber=1", fmt.toString());
	}

}
