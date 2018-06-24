package com.yanchyshyn.smartmethodstest;

import com.yanchyshyn.smartmethods.SmartMethod;
import junit.framework.TestCase;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class SmartMethodsUnitTest {

	private static final int TEST_VALUE1 = 1;
	private static final int TEST_VALUE2 = 2;

	// region SmartMethod fields
	private final SmartMethodsUnitTest_TestMethod testMethod = new SmartMethodsUnitTest_TestMethod(this::testMethod);
	// endregion

	private boolean fired = false;

	@Test
	public void doTest() {
		// check default values
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertFalse("isParam1Set should return false", testMethod.isParam1Set());
		assertFalse("isParam2Set should return false", testMethod.isParam2Set());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), 0);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), 0);

		// check setParam1 method
		testMethod.setParam1(TEST_VALUE1);
		// post-conditions: param1 value should be set, param1Set should be changed to true, method should NOT be fired
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), TEST_VALUE1);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), 0);
		assertTrue("isParam1Set should return true", testMethod.isParam1Set());
		assertFalse("isParam2Set should return false", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check setParam2 method
		testMethod.setParam2(TEST_VALUE2);
		// post-conditions: param2 value should be set, param2Set should be changed to true, method should be fired
		assertTrue("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), TEST_VALUE1);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), TEST_VALUE2);
		assertTrue("isParam1Set should return true", testMethod.isParam1Set());
		assertTrue("isParam2Set should return true", testMethod.isParam2Set());
		assertTrue("Method should be fired", fired);


		// check clearParam1
		fired = false;
		testMethod.clearParam1();
		// post-conditions: param1 value should be reset, param1Set should be changed to false, method should NOT be fired
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), 0);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), TEST_VALUE2);
		assertFalse("isParam1Set should return false", testMethod.isParam1Set());
		assertTrue("isParam2Set should return true", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check fire
		testMethod.fire();
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), 0);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), TEST_VALUE2);
		assertFalse("isParam1Set should return false", testMethod.isParam1Set());
		assertTrue("isParam2Set should return true", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check clearParam2
		testMethod.clearParam2();
		// post-conditions: param2 value should be reset, param2Set should be changed to false, method should NOT be fired
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), 0);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), 0);
		assertFalse("isParam1Set should return false", testMethod.isParam1Set());
		assertFalse("isParam2Set should return false", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check assignParam1
		testMethod.assignParam1(TEST_VALUE1);
		// post-conditions: param2 value should be set, param2Set should be changed to true, method should NOT be fired
		assertFalse("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), TEST_VALUE1);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), 0);
		assertTrue("isParam1Set should return false", testMethod.isParam1Set());
		assertFalse("isParam2Set should return false", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check assignParam2
		testMethod.assignParam2(TEST_VALUE2);
		// post-conditions: param2 value should be set, param2Set should be changed to true, method should NOT be fired
		assertTrue("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), TEST_VALUE1);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), TEST_VALUE2);
		assertTrue("isParam1Set should return false", testMethod.isParam1Set());
		assertTrue("isParam2Set should return false", testMethod.isParam2Set());
		assertFalse("Method should not be fired", fired);

		// check fire
		testMethod.fire();
		assertTrue("SmartMethod areParametersSet returned wrong value", testMethod.areParametersSet());
		assertEquals("SmartMethod getParam1 returned wrong value", testMethod.getParam1(), TEST_VALUE1);
		assertEquals("SmartMethod getParam2 returned wrong value", testMethod.getParam2(), TEST_VALUE2);
		assertTrue("isParam1Set should return false", testMethod.isParam1Set());
		assertTrue("isParam2Set should return false", testMethod.isParam2Set());
		assertTrue("Method should be fired", fired);
	}

	@SmartMethod
	private void testMethod(int param1, int param2) {
		TestCase.assertEquals(param1, TEST_VALUE1);
		TestCase.assertEquals(param2, TEST_VALUE2);
		fired = true;
	}
}
