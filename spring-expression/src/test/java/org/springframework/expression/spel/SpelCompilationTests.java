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
	int count = 5000000; // Number of evaluations that are timed in one run
	int iterations = 10; // Number of times to repeat 'count' evaluations (for averaging)
	private final static boolean debugTests = false;
	
	Expression expression;
	
	@Before
	public void setup() {
		SpelCompiler.reset();
	}
	
	
	// payload.DR[0].DRFixedSection.duration lt 0.1
	
	public static class Payload {
		Two[] DR = new Two[]{new Two()};
		
		public Two[] getDR() {
			return DR;
		}
	}
	
	public static class Two {
		Three DRFixedSection = new Three();
		public Three getDRFixedSection() {
			return DRFixedSection;
		}
	}
	
	public static class Three {
		double duration = 0.4d;
		public double getDuration() {
			return duration;
		}
	}
	
	
	@Test
	public void complexExpressionPerformance() throws Exception {
		Payload payload = new Payload();
		Expression expression = parser.parseExpression("DR[0].DRFixedSection.duration lt 0.1");		
		boolean b = false;
		
		for (int i=0;i<1000000;i++) {
			b = expression.getValue(payload,Boolean.TYPE);			
		}
		long stime = System.currentTimeMillis();
		for (int i=0;i<8000000;i++) {
			b = expression.getValue(payload,Boolean.TYPE);			
		}
		long etime = System.currentTimeMillis();
		System.out.println("Time for interpreted "+(etime-stime)+"ms");
		compile(expression);
		boolean bc = false;
		stime = System.currentTimeMillis();
		for (int i=0;i<8000000;i++) {
			bc = expression.getValue(payload,Boolean.TYPE);			
		}
		etime = System.currentTimeMillis();
		System.out.println("Time for compiled "+(etime-stime)+"ms");
		assertFalse(b);
		assertEquals(b,bc);
		payload.DR[0].DRFixedSection.duration = 0.04d;
		bc = expression.getValue(payload,Boolean.TYPE);
		assertTrue(bc);
	}
	
	public static class HW {
			public String hello() {
				return "foobar";
			}
	}
	@Test
	public void compilingMethodReference() throws Exception {
		long interpretedTotal = 0, compiledTotal = 0;
		long stime,etime;
		String interpretedResult = null,compiledResult = null;

		HW testdata = new HW();
		Expression expression = parser.parseExpression("hello()");

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
		System.out.println(interpretedResult);
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
