/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.util;

import java.util.List;
import java.util.Map;



/**
 * 
 * @author Andy Clement
 */
public class PerformanceTests {
	public static void main(String[] args) {
		new PerformanceTests().measure2();
	}

//	public void measure() {
//		PathMatcher pm = new PathMatcher();
//		String pattern = "/customer/{id}";
//		pm.addURITemplate(TestURITemplate.createFor(pattern));
//		
//		// warmup
//		for (int i=0;i<10000;i++) {
//			List<MatchResult> results = pm.findAllMatches("/customer/99");
//			String s = results.get(0).getValue("id");
//			if (!s.equals("99")) {
//				throw new IllegalStateException();
//			}
//		}
//	}
	static int REPEAT=1_000_000;
	
	public void measure1() {
		AntPathMatcher pm = new AntPathMatcher();
//		pm.addURITemplate(TestURITemplate.createFor("/customer/foo"));
		for (int i=0;i<10000;i++) {
			boolean b = pm.match("/customer/foo","/customer/foo");
			if (!b) throw new IllegalStateException();
			b = pm.match("/customer/foo","/foo/bar");
			if (b) throw new IllegalStateException();
			b = pm.match("/customer/foo","/foo");
			if (b) throw new IllegalStateException();
		}
		long stime = System.currentTimeMillis();
		for (int i=0;i<REPEAT;i++) {  
			pm.match("/customer/foo","/customer/foo");
			pm.match("/customer/foo","/foo/bar");
			pm.match("/customer/foo","/foo");
		}
		long etime = System.currentTimeMillis();
		System.out.println(REPEAT+" took "+(etime-stime)+"ms"); // 4412ms
	}
	
	public void measure2() {
		AntPathMatcher apm = new AntPathMatcher();
		String pattern = "/customer/{id}";
		// warmup
		System.out.println("10000 warmups");
		for (int i=0;i<REPEAT;i++) { 
//			boolean b = apm.match("/customer/info", "/customer/info");
//			if (!b) throw new IllegalStateException();
			Map<String,String> vars = apm.extractUriTemplateVariables(pattern, "/customer/99");
			String s = vars.get("id");
			if (!s.equals("99")) {
				throw new IllegalStateException();
			}
//			vars = apm.extractUriTemplateVariables("/customer/book/{isbn}", "/customer/book/376");
//			s = vars.get("isbn");
//			if (!s.equals("376")) {
//				throw new IllegalStateException();
//			}
		}
		
		long stime = System.currentTimeMillis();
		for (int i=0;i<1000000;i++) { 
//			boolean b = apm.match("/customer/info", "/customer/info");
			Map<String,String> vars = apm.extractUriTemplateVariables(pattern, "/customer/99");
			String s = vars.get("id");
//			vars = apm.extractUriTemplateVariables("/customer/book/{isbn}", "/customer/book/376");
//			s = vars.get("isbn");
		}
		long etime = System.currentTimeMillis();
		System.out.println("1 million took "+(etime-stime)+"ms"); // 4412ms
	}

	public void measure5() {
		PathMatcher pm = new AntPathMatcher();
//		pm.addURITemplate(TestURITemplate.createFor("/customer/{id}"));
//		pm.addURITemplate(TestURITemplate.createFor("/customer/book/{isbn}"));
//		pm.addURITemplate(TestURITemplate.createFor("/customer"));
//		pm.addURITemplate(TestURITemplate.createFor("/foo/{one}/*/{two}"));
//		pm.addURITemplate(TestURITemplate.createFor("/{one}/**/{two}"));
		
		// warmup
		for (int i=0;i<10000;i++) {
			Map<String,String> results = pm.extractUriTemplateVariables("/customer/{id}","/customer/99");
			String s = results.get("id");
			if (!s.equals("99")) {
				throw new IllegalStateException();
			} 
			results = pm.extractUriTemplateVariables("/customer/book/{isbn}","/customer/book/376");
			s = results.get("isbn");
			if (!s.equals("376")) {
				throw new IllegalStateException();
			} 
			results = pm.extractUriTemplateVariables("/customer","/customer");
//			if (results.size()!=1) throw new IllegalStateException();
			results = pm.extractUriTemplateVariables("/foo/{one}/*/{two}","/foo/aaa/something/bbb");
			s = results.get("one");
			if (!s.equals("aaa")) 
				throw new IllegalStateException();
			s = results.get("two");
			if (!s.equals("bbb")) 
				throw new IllegalStateException();
			results = pm.extractUriTemplateVariables("/{one}/**/{two}","/aaa/foo/bar/bbb");
			s = results.get("one");
			if (!s.equals("aaa")) 
				throw new IllegalStateException();
			s = results.get("two");
			if (!s.equals("bbb")) 
				throw new IllegalStateException();
		}

		long stime = System.currentTimeMillis();
		for (int i=0;i<1000000;i++) {  
			Map<String,String> results = pm.extractUriTemplateVariables("/customer/{id}","/customer/99");
			String s = results.get("id");
			results = pm.extractUriTemplateVariables("/customer/book/{isbn}","/customer/book/376");
			s = results.get("isbn");
			results = pm.extractUriTemplateVariables("/customer","/customer");
			results = pm.extractUriTemplateVariables("/foo/{one}/*/{two}","/foo/aaa/something/bbb");
			s = results.get("one");
			s = results.get("two");
			results = pm.extractUriTemplateVariables("/{one}/**/{two}","/aaa/foo/bar/bbb");
			s = results.get("one");
			s = results.get("two");
		}
		long etime = System.currentTimeMillis();
		System.out.println("1 million took "+(etime-stime)+"ms"); // 4412ms
	}
	

	public void measure5a() {
		PathMatcher pm = new AntPathMatcher();
//		pm.addURITemplate(TestURITemplate.createFor("/customer/{id}"));
//		pm.addURITemplate(TestURITemplate.createFor("/customer/book/{isbn}"));
//		pm.addURITemplate(TestURITemplate.createFor("/customer"));
//		pm.addURITemplate(TestURITemplate.createFor("/foo/{one}/*/{two}"));
//		pm.addURITemplate(TestURITemplate.createFor("/{one}/**/{two}"));
		
		// warmup
		for (int i=0;i<10000;i++) {
			Map<String,String> results = pm.extractUriTemplateVariables("/customer/{id}","/customer/99");
			String s = results.get("id");
			if (!s.equals("99")) {
				throw new IllegalStateException();
			} 
			results = pm.extractUriTemplateVariables("/customer/book/{isbn}","/customer/book/376");
			s = results.get("isbn");
			if (!s.equals("376")) {
				throw new IllegalStateException();
			} 
			results = pm.extractUriTemplateVariables("/customer","/customer");
//			if (results.size()!=1) throw new IllegalStateException();
			results = pm.extractUriTemplateVariables("/foo/{one}/*/{two}","/foo/aaa/something/bbb");
			s = results.get("one");
			if (!s.equals("aaa")) 
				throw new IllegalStateException();
			s = results.get("two");
			if (!s.equals("bbb")) 
				throw new IllegalStateException();
			results = pm.extractUriTemplateVariables("/{one}/**/{two}","/aaa/foo/bar/bbb");
			s = results.get("one");
			if (!s.equals("aaa")) 
				throw new IllegalStateException();
			s = results.get("two");
			if (!s.equals("bbb")) 
				throw new IllegalStateException();
		}

		long stime = System.currentTimeMillis();
		for (int i=0;i<1000000;i++) {  
			Map<String,String> results = pm.extractUriTemplateVariables("/customer/{id}","/customer/99");
			String s = results.get("id");
			results = pm.extractUriTemplateVariables("/customer/book/{isbn}","/customer/book/376");
			s = results.get("isbn");
			results = pm.extractUriTemplateVariables("/customer","/customer");
			results = pm.extractUriTemplateVariables("/foo/{one}/*/{two}","/foo/aaa/something/bbb");
			s = results.get("one");
			s = results.get("two");
			results = pm.extractUriTemplateVariables("/{one}/**/{two}","/aaa/foo/bar/bbb");
			s = results.get("one");
			s = results.get("two");
		}
		long etime = System.currentTimeMillis();
		System.out.println("1 million took "+(etime-stime)+"ms"); // 4412ms
	}
}
