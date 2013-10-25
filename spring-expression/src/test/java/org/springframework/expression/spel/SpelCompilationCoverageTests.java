/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import static org.junit.Assert.*;

/**
 * Checks the behaviour of the SpelCompiler. This should cover compilation all node types.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class SpelCompilationCoverageTests extends ExpressionTestCase {
	
	Expression expression;
	
	@Before
	public void setup() {
		SpelCompiler.reset();
	}
	
	// Nodes to test compilation of:
	
	// tested:
	// StringLiteral, BooleanLiteral, NullLiteral, RealLiteral, IntLiteral, FloatLiteral, LongLiteral
	// opOr, opAnd, opLt, opGe, opLe, opGt, opPlus, opMinus
	// OpEq, OpNe, OpMultiply, OpDivide
	// Ternary, Elvis
	// TypeReference
	// OperatorInstanceOf
	// OperatorNot
	// MethodReference
	
	// in progress:
	// Indexer
	// CompoundExpression
	// VariableReference
	
	// not tested enough or at all yet:
	
	// assign, beanreference, constructorref,
	// functionref, identifier?, inlinelist, 
	// opdec, opbetween,
	// opmatches, oppower, opinc,
	// opmodulus, projection,
	// propertyorfieldref, qualifiedid, selection, 

	
	@Test
	public void typeReference() throws Exception {
		expression = parse("T(String)");
		assertEquals(String.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(String.class,expression.getValue());
		 
		expression = parse("T(java.io.IOException)");
		assertEquals(IOException.class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException.class,expression.getValue());

		expression = parse("T(java.io.IOException[])");
		assertEquals(IOException[].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(IOException[].class,expression.getValue());

		expression = parse("T(int[][])");
		assertEquals(int[][].class,expression.getValue());
		assertCanCompile(expression);
		assertEquals(int[][].class,expression.getValue());

		expression = parse("T(int)");
		assertEquals(Integer.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Integer.TYPE,expression.getValue());

		expression = parse("T(byte)");
		assertEquals(Byte.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Byte.TYPE,expression.getValue());

		expression = parse("T(char)");
		assertEquals(Character.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Character.TYPE,expression.getValue());

		expression = parse("T(short)");
		assertEquals(Short.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Short.TYPE,expression.getValue());

		expression = parse("T(long)");
		assertEquals(Long.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Long.TYPE,expression.getValue());

		expression = parse("T(float)");
		assertEquals(Float.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Float.TYPE,expression.getValue());

		expression = parse("T(double)");
		assertEquals(Double.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Double.TYPE,expression.getValue());

		expression = parse("T(boolean)");
		assertEquals(Boolean.TYPE,expression.getValue());
		assertCanCompile(expression);
		assertEquals(Boolean.TYPE,expression.getValue());
		
		expression = parse("T(Missing)");
		assertGetValueFail(expression);
		assertCantCompile(expression);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void operatorInstanceOf() throws Exception {
		expression = parse("'xyz' instanceof T(String)");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		expression = parse("'xyz' instanceof T(Integer)");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		List<String> list = new ArrayList<String>();
		expression = parse("#root instanceof T(java.util.List)");
		assertEquals(true,expression.getValue(list));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(list));

		List<String>[] arrayOfLists = new List[]{new ArrayList<String>()};
		expression = parse("#root instanceof T(java.util.List[])");
		assertEquals(true,expression.getValue(arrayOfLists));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(arrayOfLists));
		
		int[] intArray = new int[]{1,2,3};
		expression = parse("#root instanceof T(int[])");
		assertEquals(true,expression.getValue(intArray));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(intArray));

		String root = null;
		expression = parse("#root instanceof T(Integer)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		// root still null
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(false,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(root));

		root = "howdy!";
		expression = parse("#root instanceof T(java.lang.Object)");
		assertEquals(true,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(root));
	}

	@Test
	public void stringLiteral() throws Exception {
		expression = parser.parseExpression("'abcde'");		
		assertEquals("abcde",expression.getValue(new TestClass1(),String.class));
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("abcde",resultC);
		assertEquals("abcde",expression.getValue(String.class));
		assertEquals("abcde",expression.getValue());
		assertEquals("abcde",expression.getValue(new StandardEvaluationContext()));
		expression = parser.parseExpression("\"abcde\"");
		assertCanCompile(expression);
		assertEquals("abcde",expression.getValue(String.class));
	}
	
	@Test
	public void nullLiteral() throws Exception {
		expression = parser.parseExpression("null");
		Object resultI = expression.getValue(new TestClass1(),Object.class);
		assertCanCompile(expression);
		Object resultC = expression.getValue(new TestClass1(),Object.class);
		assertEquals(null,resultI);
		assertEquals(null,resultC);
	}
	
	@Test
	public void realLiteral() throws Exception {
		expression = parser.parseExpression("3.4d");
		double resultI = expression.getValue(new TestClass1(),Double.TYPE);
		assertCanCompile(expression);
		double resultC = expression.getValue(new TestClass1(),Double.TYPE);
		assertEquals(3.4d,resultI,0.1d);
		assertEquals(3.4d,resultC,0.1d);

		assertEquals(3.4d,expression.getValue());
	}

	@Test
	public void intLiteral() throws Exception {
		expression = parser.parseExpression("42");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);

		expression = parser.parseExpression("T(Integer).valueOf(42)");
		expression.getValue(Integer.class);
		assertCanCompile(expression);
		assertEquals(new Integer(42),expression.getValue(null,Integer.class));
		
		// Code gen is different for -1 .. 6
		// Not an int literal but an opminus with one operand
//		expression = parser.parseExpression("-1");
//		assertCanCompile(expression);
//		assertEquals(-1,expression.getValue());
		expression = parser.parseExpression("0");
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());
		expression = parser.parseExpression("2");
		assertCanCompile(expression);
		assertEquals(2,expression.getValue());
		expression = parser.parseExpression("7");
		assertCanCompile(expression);
		assertEquals(7,expression.getValue());
	}
	
	@Test
	public void longLiteral() throws Exception {
		expression = parser.parseExpression("99L");
		long resultI = expression.getValue(new TestClass1(),Long.TYPE);
		assertCanCompile(expression);
		long resultC = expression.getValue(new TestClass1(),Long.TYPE);
		assertEquals(99L,resultI);
		assertEquals(99L,resultC);		
	}
		
	@Test
	public void booleanLiteral() throws Exception {
		expression = parser.parseExpression("true");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertTrue(SpelCompiler.compile(expression));
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertTrue(SpelCompiler.compile(expression));
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultC);
	}
	
	@Test
	public void floatLiteral() throws Exception {
		expression = parser.parseExpression("3.4f");
		float resultI = expression.getValue(new TestClass1(),Float.TYPE);
		assertCanCompile(expression);
		float resultC = expression.getValue(new TestClass1(),Float.TYPE);
		assertEquals(3.4f,resultI,0.1f);
		assertEquals(3.4f,resultC,0.1f);

		assertEquals(3.4f,expression.getValue());
	}
	
	@Test
	public void opOr() throws Exception {
		Expression expression = parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);
		
		expression = parser.parseExpression("false or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or false");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		TestClass4 tc = new TestClass4();
		expression = parser.parseExpression("getfalse() or gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression);
		resultC = expression.getValue(tc,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);

		// Can't compile this as we aren't going down the getfalse() branch in our evaluation
		expression = parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);
		
		expression = parser.parseExpression("getA() or getB()");
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = false;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertTrue(resultI);
		
		boolean b = false;
		expression = parse("#root or #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertFalse((Boolean)resultI2);
		assertFalse((Boolean)expression.getValue(b));
	}
	
	@Test
	public void opAnd() throws Exception {
		Expression expression = parser.parseExpression("false and false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("false and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);
		
		expression = parser.parseExpression("true and false");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);

		expression = parser.parseExpression("true and true");
		resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		TestClass4 tc = new TestClass4();

		// Can't compile this as we aren't going down the gettrue() branch in our evaluation
		expression = parser.parseExpression("getfalse() and gettrue()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression);
		
		expression = parser.parseExpression("getA() and getB()");
		tc.a = false;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCantCompile(expression); // Haven't yet been into second branch
		tc.a = true;
		tc.b = false;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertCanCompile(expression); // Now been down both
		assertFalse(resultI);
		tc.a = true;
		tc.b = true;
		resultI = expression.getValue(tc,Boolean.TYPE);
		assertTrue(resultI);
		
		boolean b = true;
		expression = parse("#root and #root");
		Object resultI2 = expression.getValue(b);
		assertCanCompile(expression);
		assertTrue((Boolean)resultI2);
		assertTrue((Boolean)expression.getValue(b));
	}
	
	@Test
	public void operatorNot() throws Exception {
		expression = parse("!true");
		assertEquals(false,expression.getValue());
		assertCanCompile(expression);
		assertEquals(false,expression.getValue());

		expression = parse("!false");
		assertEquals(true,expression.getValue());
		assertCanCompile(expression);
		assertEquals(true,expression.getValue());

		boolean b = true;
		expression = parse("!#root");
		assertEquals(false,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(false,expression.getValue(b));

		b = false;
		expression = parse("!#root");
		assertEquals(true,expression.getValue(b));
		assertCanCompile(expression);
		assertEquals(true,expression.getValue(b));
	}

	@Test
	public void ternary() throws Exception {
		Expression expression = parser.parseExpression("true?'a':'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		expression = parser.parseExpression("false?'a':'b'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("b",resultI);
		assertEquals("b",resultC);

		expression = parser.parseExpression("false?1:'b'");
		// All literals so we can do this straight away
		assertCanCompile(expression);
		assertEquals("b",expression.getValue());

		boolean root = true;
		expression = parser.parseExpression("(#root and true)?T(Integer).valueOf(1):T(Long).valueOf(3L)");
		assertEquals(1,expression.getValue(root));
		assertCantCompile(expression); // Have not gone down false branch
		root = false;
		assertEquals(3L,expression.getValue(root));
		assertCanCompile(expression);
		assertEquals(3L,expression.getValue(root));
		root = true;
		assertEquals(1,expression.getValue(root));		
	}
	
	@Test
	public void elvis() throws Exception {
		Expression expression = parser.parseExpression("'a'?:'b'");
		String resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		expression = parser.parseExpression("null?:'a'");
		resultI = expression.getValue(String.class);
		assertCanCompile(expression);
		resultC = expression.getValue(String.class);
		assertEquals("a",resultI);
		assertEquals("a",resultC);
		
		String s = "abc";
		expression = parser.parseExpression("#root?:'b'");
		assertCantCompile(expression);
		resultI = expression.getValue(s,String.class);
		assertEquals("abc",resultI);
		assertCanCompile(expression);
	}
	
	@Test
	public void variableReference_root() throws Exception {
		String s = "hello";
		Expression expression = parser.parseExpression("#root");
		String resultI = expression.getValue(s,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(s,String.class);
		assertEquals(s,resultI);
		assertEquals(s,resultC);		

		expression = parser.parseExpression("#root");
		int i = (Integer)expression.getValue(42);
		assertEquals(42,i);
		assertCanCompile(expression);
		i = (Integer)expression.getValue(42);
		assertEquals(42,i);
	}
	
	@Test
	public void opLt() throws Exception {
		expression = parse("3.0d < 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d < 1123.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 < 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2 < 4");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f < 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("1.0f < 5.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("30L < 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L < 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 < 3.0d");
		assertCantCompile(expression);
		
		expression = parse("T(Integer).valueOf(3) < 4");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}
	
	@Test
	public void opLe() throws Exception {
//		expression = parse("3.0d <= 4.0d");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		expression = parse("3446.0d <= 1123.0d");
//		assertCanCompile(expression);
//		assertFalse((Boolean)expression.getValue());
//		expression = parse("3446.0d <= 3446.0d");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//
//		expression = parse("3 <= 1");
//		assertCanCompile(expression);
//		assertFalse((Boolean)expression.getValue());
//		expression = parse("2 <= 4");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		expression = parse("3 <= 3");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		
//		expression = parse("3.0f <= 1.0f");
//		assertCanCompile(expression);
//		assertFalse((Boolean)expression.getValue());
//		expression = parse("1.0f <= 5.0f");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		expression = parse("2.0f <= 2.0f");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		
//		expression = parse("30L <= 30L");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		expression = parse("15L <= 20L");
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());
//		
//		// Differing types of number, not yet supported
//		expression = parse("1 <= 3.0d");
//		assertCantCompile(expression);
//
//		expression = parse("T(Integer).valueOf(3) <= 4");
//		assertTrue((Boolean)expression.getValue());
//		assertCanCompile(expression);
//		assertTrue((Boolean)expression.getValue());

		expression = parse("T(Integer).valueOf(3) <= T(Integer).valueOf(3)");
		assertTrue((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
	}
	
	@Test
	public void opEq() throws Exception {
		expression = parse("3.0d == 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d == 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 == 1");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 == 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f == 1.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("2.0f == 2.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("30L == 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L == 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 == 3.0d");
		assertCantCompile(expression);
		
		Double d = 3.0d;
		expression = parse("#root==3.0d");
		assertTrue((Boolean)expression.getValue(d));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(d));
		
		Integer i = 3;
		expression = parse("#root==3");
		assertTrue((Boolean)expression.getValue(i));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(i));

		Float f = 3.0f;
		expression = parse("#root==3.0f");
		assertTrue((Boolean)expression.getValue(f));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(f));
		
		long l = 300l;
		expression = parse("#root==300l");
		assertTrue((Boolean)expression.getValue(l));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(l));
		
		boolean b = true;
		expression = parse("#root==true");
		assertTrue((Boolean)expression.getValue(b));
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue(b));
	}
	
	@Test
	public void opGt() throws Exception {
		expression = parse("3.0d > 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d > 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 > 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 > 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("3.0f > 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f > 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("30L > 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L > 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 > 3.0d");
		assertCantCompile(expression);

		expression = parse("T(Integer).valueOf(3) > 4");
		assertFalse((Boolean)expression.getValue());
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
	}
	
	@Test
	public void opGe() throws Exception {
		expression = parse("3.0d >= 4.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3446.0d >= 1123.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d >= 3446.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());

		expression = parse("3 >= 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2 >= 4");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3 >= 3");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("3.0f >= 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("1.0f >= 5.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("3.0f >= 3.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		expression = parse("40L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("15L >= 20L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("30L >= 30L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 >= 3.0d");
		assertCantCompile(expression);
	}

	@Test
	public void opNe() throws Exception {
		expression = parse("3.0d != 4.0d");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3446.0d != 3446.0d");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());

		expression = parse("3 != 1");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("3 != 3");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("3.0f != 1.0f");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		expression = parse("2.0f != 2.0f");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		
		expression = parse("30L != 30L");
		assertCanCompile(expression);
		assertFalse((Boolean)expression.getValue());
		expression = parse("15L != 20L");
		assertCanCompile(expression);
		assertTrue((Boolean)expression.getValue());
		
		// Differing types of number, not yet supported
		expression = parse("1 != 3.0d");
		assertCantCompile(expression);
	}
	
	@Test
	public void opPlus() throws Exception {
		expression = parse("2+2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());
		
		expression = parse("2L+2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f+2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d+4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(7.0d,expression.getValue());
		
		expression = parse("+1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());		

		expression = parse("+1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());		

		expression = parse("+1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.5f,expression.getValue());		

		expression = parse("+2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.5d,expression.getValue());		
	}
	
	@Test
	public void opMultiply() throws Exception {
		expression = parse("2*2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4,expression.getValue());
		
		expression = parse("2L*2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4L,expression.getValue());

		expression = parse("2.0f*2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(4.0f,expression.getValue());

		expression = parse("3.0d*4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(12.0d,expression.getValue());
	}
	
	@Test
	public void opDivide() throws Exception {
		expression = parse("2/2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1,expression.getValue());
		
		expression = parse("2L/2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1L,expression.getValue());

		expression = parse("2.0f/2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(1.0f,expression.getValue());

		expression = parse("3.0d/4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0.75d,expression.getValue());
	}
	
	@Test
	public void opMinus() throws Exception {
		expression = parse("2-2");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(0,expression.getValue());
		
		expression = parse("4L-2L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2L,expression.getValue());

		expression = parse("4.0f-2.0f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(2.0f,expression.getValue());

		expression = parse("3.0d-4.0d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.0d,expression.getValue());
		
		expression = parse("-1");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1,expression.getValue());		

		expression = parse("-1L");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1L,expression.getValue());		

		expression = parse("-1.5f");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-1.5f,expression.getValue());		

		expression = parse("-2.5d");
		expression.getValue();
		assertCanCompile(expression);
		assertEquals(-2.5d,expression.getValue());		
	}
	
	@Test
	public void methodReference_simpleInstanceMethodNonLiteralArg() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}
	
	@Test
	public void methodReference_staticMethod() throws Exception {
		Expression expression = parser.parseExpression("T(Integer).valueOf(42)");
		int resultI = expression.getValue(new TestClass1(),Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue(new TestClass1(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);		
	}
	
	@Test
	public void methodReference_literalArguments_int() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TestClass1(),String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(new TestClass1(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}


	@Test
	public void methodReference_simpleInstanceMethodNoArg() throws Exception {
		Expression expression = parser.parseExpression("toString()");
		String resultI = expression.getValue(42,String.class);
		assertCanCompile(expression);
		String resultC = expression.getValue(42,String.class);
		assertEquals("42",resultI);
		assertEquals("42",resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodNoArgReturnPrimitive() throws Exception {
		expression = parser.parseExpression("intValue()");
		int resultI = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultI);
		assertCanCompile(expression);
		int resultC = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultC);
	}
	
	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive1() throws Exception {
		Expression expression = parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc",Integer.TYPE);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc",Integer.TYPE);
		assertEquals(1,resultI);
		assertEquals(1,resultC);
	}

	@Test
	public void methodReference_simpleInstanceMethodOneArgReturnPrimitive2() throws Exception {
		expression = parser.parseExpression("charAt(2)");
		int resultI = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultI);
		assertCanCompile(expression);
		int resultC = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultC);
	}

	@Test
	public void mixingItUp_indexerOpEqTernary() throws Exception {
		Map<String, String> m = new HashMap<String,String>();
		m.put("andy","778");

		expression = parse("['andy']==null?1:2");
		System.out.println(expression.getValue(m));
		assertCanCompile(expression);
		assertEquals(2,expression.getValue(m));
		m.remove("andy");
		assertEquals(1,expression.getValue(m));
	}
	
	@Test
	public void mixingItUp_propertyAccessIndexerOpLtTernaryRootNull() throws Exception {
		Payload payload = new Payload();
		Expression expression = parser.parseExpression("DR[0].three.four lt 0.1?#root:null");
		Object v = expression.getValue(payload);
		assertCanCompile(expression);
		Object vc = expression.getValue(payload);
		assertEquals(payload,v);
		assertEquals(payload,vc);
		payload.DR[0].three.four = 0.13d;
		vc = expression.getValue(payload);
		assertNull(vc);
	}
	
	// --- 
		
	public static class Payload {
		Two[] DR = new Two[]{new Two()};
		
		public Two[] getDR() {
			return DR;
		}
	}
	
	public static class Two {
		Three three = new Three();
		public Three getThree() {
			return three;
		}
	}
	
	public static class Three {
		double four = 0.04d;
		public double getFour() {
			return four;
		}
	}

	public static class TestClass1 {
		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";		
	}
	
	public static class TestClass4 {
		public boolean a,b;
		public boolean gettrue() { return true; }
		public boolean getfalse() { return false; }
		public boolean getA() { return a; }
		public boolean getB() { return b; }
	}
	
	private void assertCanCompile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}
	
	private void assertCantCompile(Expression expression) {
		assertFalse(SpelCompiler.compile(expression));
	}
	
	private Expression parse(String expression) {
		return parser.parseExpression(expression);
	}
	
	private void assertGetValueFail(Expression expression) {
		try {
			Object o = expression.getValue();
			fail("Calling getValue on the expression should have failed but returned "+o);
		} catch (Exception ex) {
			// success!
		}
	}
	
}
