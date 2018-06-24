package com.yanchyshyn.smartmethodstest;

import com.yanchyshyn.smartmethods.SmartMethod;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

public class SmartMethodsUnitTest2 {

	public enum TestEnum {
		Value1,
		Value2
	}


	private static final char CHAR_VALUE = 'a';
	private static final int INT_VALUE = 10;
	private static final int LONG_VALUE = 20;
	private static final float FLOAT_VALUE = 3.14f;
	private static final double DOUBLE_VALUE = 12345.67;
	private static final int[] INT_ARR_VALUE = new int[] {1, 2, 3};
	private static final String STR_VALUE = "This is a test";
	private static final Object OBJ_VALUE = new Object();
	private static final TestEnum ENUM_VALUE = TestEnum.Value1;


	// region SmartMethod fields
	private final SmartMethodsUnitTest2_TestMethod testMethod = new SmartMethodsUnitTest2_TestMethod(this::testMethod);
	// endregion

	private boolean fired = false;

	@Test
	public void doTest() {
		testMethod.setCharParam(CHAR_VALUE);
		testMethod.setIntParam(INT_VALUE);
		testMethod.setLongParam(LONG_VALUE);
		testMethod.setFloatParam(FLOAT_VALUE);
		testMethod.setDoubleParam(DOUBLE_VALUE);
		testMethod.setIntArrParam(INT_ARR_VALUE);
		testMethod.setStrParam(STR_VALUE);
		testMethod.setObjParam(OBJ_VALUE);
		testMethod.setEnumParam(ENUM_VALUE);
		assertTrue("Method should be fired", fired);
	}

	@SmartMethod
	private void testMethod(char charParam, int intParam, long longParam, float floatParam, double doubleParam, int[] intArrParam, String strParam, Object objParam, TestEnum enumParam) {
		assertEquals(charParam, CHAR_VALUE);
		assertEquals(intParam, INT_VALUE);
		assertEquals(longParam, LONG_VALUE);
		assertEquals(floatParam, FLOAT_VALUE);
		assertEquals(doubleParam, DOUBLE_VALUE);
		assertEquals(intArrParam, INT_ARR_VALUE);
		assertEquals(strParam, STR_VALUE);
		assertEquals(objParam, OBJ_VALUE);
		assertEquals(enumParam, ENUM_VALUE);
		fired = true;
	}
}
