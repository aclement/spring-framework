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

import org.junit.Before;
import org.junit.Test;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.junit.Assert.*;

/**
 * Checks the behaviour of the SpelCompiler.
 *
 * @author Andy Clement
 * @since 4.0
 */
public class SpelCompilationTests extends ExpressionTestCase {

	// For tests that compare interpreted vs compiled performance. These
	// can feasibly fail if a machine is very busy.
	boolean runComparisonTests = true;
	int count = 500000; // Number of evaluations that are timed in one run
	int iterations = 7; // Number of times to repeat 'count' evaluations (for averaging)
	private final static boolean debugTests = false;
	
	Expression expression;
	
	@Before
	public void setup() {
		SpelCompiler.reset();
	}
	
	// Nodes to test compilation of:
	
	// tested:
	// stringlit, booleanlit, nulllit, opOr, opAnd
	
	// not tested enough or at all yet:
	
	// assign, beanreference, compoundexpr, constructorref, elvis,
	// floatlit, functionref, identifier?, indexer, inlinelist, intlit,
	// longlit, methodref, opand, opdec, opdivide, opeq, opbetween,
	// opinstanceof, opmatches, opnot, oppower, opge, opgt, opinc, ople, oplt,
	// opminut, opmodulus, opmultiply, opne, opor, opplus, projection,
	// propertyorfieldref, qualifiedid, reallit, selection, 
	// ternary, typeref, valueref?, varref
	
	@Test
	public void methodReferenceVariants_simpleInstanceMethodNonLiteralArg() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(index1,index2)");
		String resultI = expression.getValue(new TC3(),String.class);
		compile(expression);
		String resultC = expression.getValue(new TC3(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}
	public static class TC3 {
		public int index1 = 1;
		public int index2 = 3;
		public String word = "abcd";		
	}
	
	@Test
	public void literalArguments_int() throws Exception {
		Expression expression = parser.parseExpression("'abcd'.substring(1,3)");
		String resultI = expression.getValue(new TC3(),String.class);
		compile(expression);
		String resultC = expression.getValue(new TC3(),String.class);
		assertEquals("bc",resultI);
		assertEquals("bc",resultC);
	}

	@Test
	public void node_nullLiteral() throws Exception {
		expression = parser.parseExpression("null");
		Object resultI = expression.getValue(new TC3(),Object.class);
		compile(expression);
		Object resultC = expression.getValue(new TC3(),Object.class);
		assertEquals(null,resultI);
		assertEquals(null,resultC);
	}
	
	@Test
	public void node_stringLiteral() throws Exception {
		expression = parser.parseExpression("'abcde'");
		// TODO asc test other variants of getValue() like the one with no required type
		String resultI = expression.getValue(new TC3(),String.class);
		compile(expression);
		String resultC = expression.getValue(new TC3(),String.class);
		assertEquals("abcde",resultI);
		assertEquals("abcde",resultC);
	}
	
	@Test
	public void node_intLiteral() throws Exception {
		expression = parser.parseExpression("42");
		int resultI = expression.getValue(new TC3(),Integer.TYPE);
		compile(expression);
		int resultC = expression.getValue(new TC3(),Integer.TYPE);
		assertEquals(42,resultI);
		assertEquals(42,resultC);
	}
	
	@Test
	public void node_opMinusSingleOperand() throws Exception {
		expression = parser.parseExpression("-1");
		int resultI = expression.getValue(new TC3(),Integer.TYPE);
		compile(expression);
		int resultC = expression.getValue(new TC3(),Integer.TYPE);
		assertEquals(-1,resultI);
		assertEquals(-1,resultC);
	}
	
	@Test
	public void node_booleanLiteral() throws Exception {
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
	public void node_opOr() throws Exception {
		Expression expression = parser.parseExpression("false or false");
		boolean resultI = expression.getValue(1,Boolean.TYPE);
		SpelCompiler.compile(expression);
		boolean resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(false,resultI);
		assertEquals(false,resultC);
		
		expression = parser.parseExpression("false or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or false");
		resultI = expression.getValue(1,Boolean.TYPE);
		compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		expression = parser.parseExpression("true or true");
		resultI = expression.getValue(1,Boolean.TYPE);
		compile(expression);
		resultC = expression.getValue(1,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
		
		// TODO asc test here is failing because when run we are not exercising the second method, so not computing the accessor for it...
		TestClass4 tc = new TestClass4();
		expression = parser.parseExpression("gettrue() or getfalse()");
		resultI = expression.getValue(tc,Boolean.TYPE);
		compile(expression);
		resultC = expression.getValue(tc,Boolean.TYPE);
		assertEquals(true,resultI);
		assertEquals(true,resultC);
	}
	static class TestClass4 {
		public boolean gettrue() { return true; }
		public boolean getfalse() { return false; }
	}

	@Test
	public void node_opAnd() throws Exception {
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
	}
	
	@Test
	public void methodReferenceVariants_simpleInstanceMethodNoArg() throws Exception {
		Expression expression = parser.parseExpression("toString()");
		String resultI = expression.getValue(42,String.class);
		compile(expression);
		String resultC = expression.getValue(42,String.class);
		assertEquals("42",resultI);
		assertEquals("42",resultC);
	}

	@Test
	public void methodReferenceVariants_simpleInstanceMethodNoArgReturnPrimitive() throws Exception {
		expression = parser.parseExpression("intValue()");
		int resultI = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultI);
		compile(expression);
		int resultC = expression.getValue(new Integer(42),Integer.TYPE);
		assertEquals(42,resultC);
	}
	
	@Test
	public void methodReferenceVariants_simpleInstanceMethodOneArgReturnPrimitive1() throws Exception {
		Expression expression = parser.parseExpression("indexOf('b')");
		int resultI = expression.getValue("abc",Integer.TYPE);
		compile(expression);
		int resultC = expression.getValue("abc",Integer.TYPE);
		assertEquals(1,resultI);
		assertEquals(1,resultC);
	}

	@Test
	public void methodReferenceVariants_simpleInstanceMethodOneArgReturnPrimitive2() throws Exception {
		expression = parser.parseExpression("charAt(2)");
		int resultI = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultI);
		compile(expression);
		int resultC = expression.getValue("abc",Character.TYPE);
		assertEquals('c',resultC);
	}

	// TODO asc the compiler is kicking in for submodes if the top node isn't compilable, is that good and f'kin cool or wrong?

	// -- performance comparisons: interpreted vs compiled
	
	@Test
	public void compilingMethodReference() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0;
		long stime,etime;
		String interpretedResult = null,compiledResult = null;

		String testdata = "Hello World";
		Expression expression = parser.parseExpression("toLowerCase()");

		// warmup
		for (int i=0;i<count;i++) {
			interpretedResult = expression.getValue(testdata,String.class);
		}
		
		// Interpreter loop
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			interpretedTotal+=(etime-stime);
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
		}
		
		compile(expression);

		for (int i=0;i<count;i++) {
			interpretedResult = expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			compiledTotal+=(etime-stime);
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
		}
		assertEquals(interpretedResult,compiledResult);
		reportPerformance("method reference", interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}
	}
	
	


	public static class TestClass2 { 
		public String name = "Santa";
		private String name2 = "foobar";
		public String getName2() {
			return name2;
		}
		public Foo foo = new Foo();
		public static class Foo {
			public Bar bar = new Bar();
			Bar b = new Bar();
			public Bar getBaz() {
				return b;
			}
			public Bar bay() {
				return b;
			}
		}
		public static class Bar {
			public String boo = "oranges";
		}
	}
	
	@Test
	public void compilingPropertyReferenceField() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;
		
		TestClass2 testdata = new TestClass2();		
		Expression expression = parser.parseExpression("name");
		// warmup
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			interpretedTotal+=(etime-stime);
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
		}

		compile(expression);
		
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			compiledTotal+=(etime-stime);
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
		}
		assertEquals(interpretedResult,compiledResult);
		reportPerformance("property reference (field)",interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}		
	}
	
	@Test
	public void compilingPropertyReferenceNestedField() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;
		
		TestClass2 testdata = new TestClass2();
		
		Expression expression = parser.parseExpression("foo.bar.boo");
		// warmup
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			interpretedTotal+=(etime-stime);
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
		}

		compile(expression);

		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			compiledTotal+=(etime-stime);
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
		}
		assertEquals(interpretedResult,compiledResult);
		reportPerformance("property reference (nested field)",interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}		
	}
	
	@Test
	public void compilingPropertyReferenceNestedMixedFieldGetter() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;
		
		TestClass2 testdata = new TestClass2();		
		Expression expression = parser.parseExpression("foo.baz.boo");
		// warmup
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			interpretedTotal+=(etime-stime);
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
		}
		
		compile(expression);

		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			compiledTotal+=(etime-stime);
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
		}
		assertEquals(interpretedResult,compiledResult);
		reportPerformance("nested property reference (mixed field/getter)",interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}		
	}
	
	@Test
	public void compilingNestedMixedFieldPropertyReferenceMethodReference() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;
		
		TestClass2 testdata = new TestClass2();		
		Expression expression = parser.parseExpression("foo.bay().boo");
		// warmup
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			interpretedTotal+=(etime-stime);
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
		}

		compile(expression);

		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			compiledTotal+=(etime-stime);
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
		}
		assertEquals(interpretedResult,compiledResult);
		reportPerformance("nested reference (mixed field/method)",interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}		
	}
	
	@Test
	public void compilingPropertyReferenceGetter() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0, stime, etime;
		String interpretedResult = null, compiledResult = null;

		TestClass2 testdata = new TestClass2();
		Expression expression = parser.parseExpression("name2");
		// warmup
		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		
		
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();		
			for (int i=0;i<count;i++) {
				interpretedResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			log("Elapsed time for method invocation (interpreter) "+(etime-stime)+"ms");
			interpretedTotal+=(etime-stime);
		}
		
		compile(expression);

		for (int i=0;i<count;i++) {
			expression.getValue(testdata,String.class);
		}
		for (int iter=0;iter<iterations;iter++) {
			stime = System.currentTimeMillis();
			for (int i=0;i<count;i++) {
				compiledResult = expression.getValue(testdata,String.class);
			}
			etime = System.currentTimeMillis();
			log("Elapsed time for method invocation (compiled) "+(etime-stime)+"ms");
			compiledTotal+=(etime-stime);
		}
		assertEquals(interpretedResult,compiledResult);
		
		reportPerformance("property reference (getter)", interpretedTotal, compiledTotal);
		if (compiledTotal>=interpretedTotal) {
			fail("Compiled version is slower than interpreted!");
		}		
	}
	
	// --- 

	private void reportPerformance(String title, long interpretedTotal, long compiledTotal) {
		double averageInterpreted = interpretedTotal/(iterations);
		double averageCompiled = compiledTotal/(iterations);
		
		System.out.println(title+": average per "+count+" interpreted iterations: "+averageInterpreted+"ms");
		System.out.println(title+": average per "+count+" compiled iterations: "+averageCompiled+"ms");
		
		double ratio = (averageCompiled/averageInterpreted)*100.0d;
		System.out.println(title+": compiled version takes "+((int)ratio)+"% of the interpreted time");
	}

	private void log(String message) {
		if (debugTests) {
			System.out.println(message);
		}
	}

	private void compile(Expression expression) {
		assertTrue(SpelCompiler.compile(expression));
	}
}
