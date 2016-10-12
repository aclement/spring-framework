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

package org.springframework.util.patterns;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.util.patterns.PathPattern;
import org.springframework.util.patterns.PatternComparatorConsideringPath;
import org.springframework.util.patterns.PathPatternParser;

/**
 * Exercise matching of {@link PathPattern} objects.
 * 
 * @author Andy Clement
 */
public class PathPatternMatcherTests {

	@Test
	public void basicMatching() {
		checkMatches("/","/");
		checkNoMatch("/","/a");
		checkMatches("foo","foo");
		checkMatches("/foo","/foo");
		checkMatches("/foo/","/foo/");
		checkMatches("/foo/bar","/foo/bar");
		checkMatches("foo/bar","foo/bar");
		checkMatches("/foo/bar/","/foo/bar/");
		checkMatches("foo/bar/","foo/bar/");
		checkMatches("/foo/bar/woo","/foo/bar/woo");
		checkNoMatch("foo","foobar");
		checkMatches("/foo/bar","/foo/bar");
		checkNoMatch("/foo/bar","/foo/baz");
		checkMatches("/**/bar","/foo/bar");
		checkNoMatch("/**/bar","/foo/baz");
		checkMatches("/*/bar","/foo/bar");
		checkNoMatch("/*/bar","/foo/baz");
	}
	
	@Test
	public void questionMarks() {
		checkNoMatch("a","ab");
		checkMatches("/f?o/bar", "/foo/bar");
		checkNoMatch("/foo/b2r", "/foo/bar");
		checkNoMatch("?", "te");
		checkMatches("?","a");
		checkMatches("???","abc");
		checkNoMatch("tes?", "te");
		checkNoMatch("tes?", "tes");
		checkNoMatch("tes?", "testt");
		checkNoMatch("tes?", "tsst");
		checkMatches(".?.a", ".a.a");
	}
	
	@Test
	public void capturing() {
		checkCapture("{id}","99","id","99");
		checkCapture("/customer/{customerId}","/customer/78","customerId","78");
		checkCapture("/customer/{customerId}/banana","/customer/42/banana","customerId","42");
		checkCapture("{id}/{id2}","99/98","id","99","id2","98");
		checkCapture("/foo/{bar}/boo/{baz}","/foo/plum/boo/apple","bar","plum","baz","apple");
		checkCapture("/{bla}.*", "/testing.html","bla","testing");
	}
	
	@Test
	public void captureTheRest() {
		checkCapture("/customer/{*something}","/customer/99","something","99");
		checkCapture("/customer/{*something}","/customer/aa/bb/cc","something","aa/bb/cc");
		checkCapture("/customer/{*something}","/customer/","something",""); // TODO valid?
	}
	
	@Test
	public void captureMultiplePieces() {
//		checkCapture("/{one}/**/{two}","/a/b","one","a","two","b");
//		checkCapture("/{one}/**/{two}","/a/ccc/b","one","a","two","b");
//		checkCapture("/{one}/**/{two}","/a/ddd/eee/fff/b","one","a","two","b");
		checkCapture("/{one}/","//","one",""); // TODO valid?
	}

	@Test
	public void wildcards() {
		checkMatches("/f*/bar","/foo/bar");
		checkMatches("/*/bar","/foo/bar");
		checkMatches("/a*b*c*d/bar","/abcd/bar");
		checkMatches("*a*", "testa");
	}
	
	@Test
	public void constrainedMatches() {
		checkCapture("{foo:[0-9]*}","123","foo","123");
		checkNoMatch("{foo:[0-9]*}","abc");
//		checkCapture("/**/{foo:[0-9]*}/**","aaa/bbb/123/foo","foo","123"); // TODO no leading slash in pattern should this match?
		checkNoMatch("/{foo:[0-9]*}","abc");
		checkNoMatch("/{foo:[0-9]*}","abc");
//		checkCapture("/**/{foo:....}/**","/foo/barg/foo","foo","barg");
//		checkCapture("/**/{foo:....}/**","/foo/foo/barg/abc/def/ghi","foo","barg");
		checkNoMatch("{foo:....}","99");
		checkMatches("{foo:..}","99");
		// TODO escaped curly braces in expression
	}

	@Test
	public void oldAntPathMatcherTests() {
		// test exact matching
		checkMatches("test", "test");
		checkMatches("/test", "/test");
		checkMatches("http://example.org", "http://example.org");
		checkNoMatch("/test.jpg", "test.jpg");
		checkNoMatch("test", "/test");
		checkNoMatch("/test", "test");

		// test matching with ?'s
		checkMatches("t?st", "test");
		checkMatches("??st", "test");
		checkMatches("tes?", "test");
		checkMatches("te??", "test");
		checkMatches("?es?", "test");
		checkNoMatch("tes?", "tes");
		checkNoMatch("tes?", "testt");
		checkNoMatch("tes?", "tsst");

		// test matching with *'s
		checkMatches("*", "test");
		checkMatches("test*", "test");
		checkMatches("test*", "testTest");
		checkMatches("test/*", "test/Test");
		checkMatches("test/*", "test/t");
		checkMatches("test/*", "test/");
		checkMatches("*test*", "AnothertestTest");
		checkMatches("*test", "Anothertest");
		checkMatches("*.*", "test.");
		checkMatches("*.*", "test.test");
		checkMatches("*.*", "test.test.test");
		checkMatches("test*aaa", "testblaaaa");
		checkNoMatch("test*", "tst");
		checkNoMatch("test*", "tsttest");
		checkNoMatch("test*", "test/");
		checkNoMatch("test*", "test/t");
		checkNoMatch("test/*", "test");
		checkNoMatch("*test*", "tsttst");
		checkNoMatch("*test", "tsttst");
		checkNoMatch("*.*", "tsttst");
		checkNoMatch("test*aaa", "test");
		checkNoMatch("test*aaa", "testblaaab");

		// test matching with ?'s and /'s
		checkMatches("/?", "/a");
		checkMatches("/?/a", "/a/a");
		checkMatches("/a/?", "/a/b");
		checkMatches("/??/a", "/aa/a");
		checkMatches("/a/??", "/a/bb");
		checkMatches("/?", "/a");

		// test matching with **'s
//		checkMatches("/**/foo", "/foo");
//		checkMatches("/**", "/testing/testing");
//		checkMatches("/*/**", "/testing/testing");
//		checkMatches("/**/*", "/testing/testing");
//		checkMatches("/bla/**/bla", "/bla/testing/testing/bla");
//		checkMatches("/bla/**/bla", "/bla/testing/testing/bla/bla");
//		checkMatches("/**/test", "/bla/bla/test");
//		checkMatches("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla");
		checkMatches("/bla*bla/test", "/blaXXXbla/test");
		checkMatches("/*bla/test", "/XXXbla/test");
		checkNoMatch("/bla*bla/test", "/blaXXXbl/test");
		checkNoMatch("/*bla/test", "XXXblab/test");
		checkNoMatch("/*bla/test", "XXXbl/test");

		checkNoMatch("/????", "/bala/bla");
//		checkNoMatch("/**/*bla", "/bla/bla/bla/bbb");

//		checkMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/");
//		checkMatches("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing");
//		checkMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing");
//		checkMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg");

//		checkMatches("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/");
//		checkMatches("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing");
//		checkMatches("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing");
//		checkNoMatch("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing");

//		checkNoMatch("/x/x/**/bla", "/x/x/x/");

//		checkMatches("/foo/bar/**", "/foo/bar");

		checkMatches("", "");

		checkMatches("/{bla}.html", "/testing.html");

		checkCapture("/{bla}.*", "/testing.html","bla","testing");
	}

	@Test
	public void matchStart() {		
		// TODO findMatchSuccesses needs dealing with across all Segment nodes
		// TODO wildcard pattern matching needs adjustment to $ anchoring for match start (otherwise it won't work for
		// match start on partial input data)
//		checkStartMatches("/**/foo","bananas"); // TODO huh? is that right or *must* it start with /?
		checkStartMatches("test/{a}_{b}/foo","test/a_b");
//		checkStartMatches("test/**/foo","test/abc");
		checkStartMatches("test/?/abc","test/a");
		checkStartMatches("test/{*foobar}","test/");
		checkStartMatches("test/*/bar","test/a");
		checkStartMatches("test/{foo}/bar","test/abc");
		checkStartMatches("test//foo", "test//");
		checkStartMatches("test/foo", "test/");
		checkStartMatches("test/*", "test/");
		checkStartMatches("test", "test");
		checkStartNoMatch("test", "tes");
		checkStartMatches("test/", "test");
		
		// test exact matching
		checkStartMatches("test", "test");
		checkStartMatches("/test", "/test");
		checkStartNoMatch("/test.jpg", "test.jpg");
		checkStartNoMatch("test", "/test");
		checkStartNoMatch("/test", "test");

		// test matching with ?'s
		checkStartMatches("t?st", "test");
		checkStartMatches("??st", "test");
		checkStartMatches("tes?", "test");
		checkStartMatches("te??", "test");
		checkStartMatches("?es?", "test");
		checkStartNoMatch("tes?", "tes");
		checkStartNoMatch("tes?", "testt");
		checkStartNoMatch("tes?", "tsst");

		// test matching with *'s
		checkStartMatches("*", "test");
		checkStartMatches("test*", "test");
		checkStartMatches("test*", "testTest");
		checkStartMatches("test/*", "test/Test");
		checkStartMatches("test/*", "test/t");
		checkStartMatches("test/*", "test/");
		checkStartMatches("*test*", "AnothertestTest");
		checkStartMatches("*test", "Anothertest");
		checkStartMatches("*.*", "test.");
		checkStartMatches("*.*", "test.test");
		checkStartMatches("*.*", "test.test.test");
		checkStartMatches("test*aaa", "testblaaaa");
		checkStartNoMatch("test*", "tst");
		checkStartNoMatch("test*", "test/");
		checkStartNoMatch("test*", "tsttest");
		checkStartNoMatch("test*", "test/t");
		checkStartMatches("test/*", "test");
		checkStartMatches("test/t*.txt", "test");
		checkStartNoMatch("*test*", "tsttst"); 
		checkStartNoMatch("*test", "tsttst");
		checkStartNoMatch("*.*", "tsttst");
		checkStartNoMatch("test*aaa", "test");
		checkStartNoMatch("test*aaa", "testblaaab");

		// test matching with ?'s and /'s
		checkStartMatches("/?", "/a");
		checkStartMatches("/?/a", "/a/a");
		checkStartMatches("/a/?", "/a/b");
		checkStartMatches("/??/a", "/aa/a");
		checkStartMatches("/a/??", "/a/bb");
		checkStartMatches("/?", "/a");

		// test matching with **'s
//		checkStartMatches("/**", "/testing/testing");
//		checkStartMatches("/*/**", "/testing/testing");
//		checkStartMatches("/**/*", "/testing/testing");
//		checkStartMatches("test*/**", "test/");
//		checkStartMatches("test*/**", "test/t");
//		checkStartMatches("/bla/**/bla", "/bla/testing/testing/bla");
//		checkStartMatches("/bla/**/bla", "/bla/testing/testing/bla/bla");
//		checkStartMatches("/**/test", "/bla/bla/test");
//		checkStartMatches("/bla/**/**/bla", "/bla/bla/bla/bla/bla/bla");
		checkStartMatches("/bla*bla/test", "/blaXXXbla/test");
		checkStartMatches("/*bla/test", "/XXXbla/test");
		checkStartNoMatch("/bla*bla/test", "/blaXXXbl/test");
		checkStartNoMatch("/*bla/test", "XXXblab/test");
		checkStartNoMatch("/*bla/test", "XXXbl/test");

		checkStartNoMatch("/????", "/bala/bla");
//		checkStartMatches("/**/*bla", "/bla/bla/bla/bbb");

//		checkStartMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing/");
//		checkStartMatches("/*bla*/**/bla/*", "/XXXblaXXXX/testing/testing/bla/testing");
//		checkStartMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing");
//		checkStartMatches("/*bla*/**/bla/**", "/XXXblaXXXX/testing/testing/bla/testing/testing.jpg");

//		checkStartMatches("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing/");
//		checkStartMatches("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing");
//		checkStartMatches("*bla*/**/bla/**", "XXXblaXXXX/testing/testing/bla/testing/testing");
//		checkStartMatches("*bla*/**/bla/*", "XXXblaXXXX/testing/testing/bla/testing/testing");

//		checkStartMatches("/x/x/**/bla", "/x/x/x/");

		checkStartMatches("", "");
	}
	
	@Test
	public void caseSensitivity() {
		PathPatternParser pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		PathPattern p = pp.parse("abc");
		assertTrue(p.matches("AbC"));
		p = pp.parse("fOo");
		assertTrue(p.matches("FoO"));
		p = pp.parse("/fOo/bAr");
		assertTrue(p.matches("/FoO/BaR"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("abc");
		assertFalse(p.matches("AbC"));
		p = pp.parse("fOo");
		assertFalse(p.matches("FoO"));
		p = pp.parse("/fOo/bAr");
		assertFalse(p.matches("/FoO/BaR"));
		p = pp.parse("/fOO/bAr");
		assertTrue(p.matches("/fOO/bAr"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("{foo:[A-Z]*}");
		assertTrue(p.matches("abc"));
		assertTrue(p.matches("ABC"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("{foo:[A-Z]*}");
		assertFalse(p.matches("abc"));
		assertTrue(p.matches("ABC"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("ab?");
		assertTrue(p.matches("AbC"));
		p = pp.parse("fO?");
		assertTrue(p.matches("FoO"));
		p = pp.parse("/fO?/bA?");
		assertTrue(p.matches("/FoO/BaR"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("ab?");
		assertFalse(p.matches("AbC"));
		p = pp.parse("fO?");
		assertFalse(p.matches("FoO"));
		p = pp.parse("/fO?/bA?");
		assertFalse(p.matches("/FoO/BaR"));
		p = pp.parse("/fO?/bA?");
		assertTrue(p.matches("/fOO/bAr"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("{abc:[A-Z]*}_{def:[A-Z]*}");
		assertTrue(p.matches("abc_abc"));
		assertTrue(p.matches("ABC_aBc"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("{abc:[A-Z]*}_{def:[A-Z]*}");
		assertFalse(p.matches("abc_abc"));
		assertTrue(p.matches("ABC_ABC"));

		pp = new PathPatternParser();
		pp.setCaseSensitive(false);
		p = pp.parse("*?a?*");
		assertTrue(p.matches("bab"));
		assertTrue(p.matches("bAb"));
		
		pp = new PathPatternParser();
		pp.setCaseSensitive(true);
		p = pp.parse("*?A?*");
		assertFalse(p.matches("bab"));
		assertTrue(p.matches("bAb"));
	}
	
	
	@Test
	public void alternativeDelimiter() {
		try {
			separator = '.';
	
			// test exact matching
			checkMatches("test", "test");
			checkMatches(".test", ".test");
			checkNoMatch(".test/jpg", "test/jpg");
			checkNoMatch("test", ".test");
			checkNoMatch(".test", "test");
	
			// test matching with ?'s
			checkMatches("t?st", "test");
			checkMatches("??st", "test");
			checkMatches("tes?", "test");
			checkMatches("te??", "test");
			checkMatches("?es?", "test");
			checkNoMatch("tes?", "tes");
			checkNoMatch("tes?", "testt");
			checkNoMatch("tes?", "tsst");
	
			// test matching with *'s
			checkMatches("*", "test");
			checkMatches("test*", "test");
			checkMatches("test*", "testTest");
			checkMatches("*test*", "AnothertestTest");
			checkMatches("*test", "Anothertest");
			checkMatches("*/*", "test/");
			checkMatches("*/*", "test/test");
			checkMatches("*/*", "test/test/test");
			checkMatches("test*aaa", "testblaaaa");
			checkNoMatch("test*", "tst");
			checkNoMatch("test*", "tsttest");
			checkNoMatch("*test*", "tsttst");
			checkNoMatch("*test", "tsttst");
			checkNoMatch("*/*", "tsttst");
			checkNoMatch("test*aaa", "test");
			checkNoMatch("test*aaa", "testblaaab");
	
			// test matching with ?'s and .'s
			checkMatches(".?", ".a");
			checkMatches(".?.a", ".a.a");
			checkMatches(".a.?", ".a.b");
			checkMatches(".??.a", ".aa.a");
			checkMatches(".a.??", ".a.bb");
			checkMatches(".?", ".a");
	
			// test matching with **'s
//			checkMatches(".**", ".testing.testing");
//			checkMatches(".*.**", ".testing.testing");
//			checkMatches(".**.*", ".testing.testing");
//			checkMatches(".bla.**.bla", ".bla.testing.testing.bla");
//			checkMatches(".bla.**.bla", ".bla.testing.testing.bla.bla");
//			checkMatches(".**.test", ".bla.bla.test");
//			checkMatches(".bla.**.**.bla", ".bla.bla.bla.bla.bla.bla");
			checkMatches(".bla*bla.test", ".blaXXXbla.test");
			checkMatches(".*bla.test", ".XXXbla.test");
			checkNoMatch(".bla*bla.test", ".blaXXXbl.test");
			checkNoMatch(".*bla.test", "XXXblab.test");
			checkNoMatch(".*bla.test", "XXXbl.test");
		} finally {
			separator = PathPatternParser.DEFAULT_SEPARATOR;
		}
	}
	
	// TODO needs tests using the range of sections
	// TODO some of these probably fail the assertion that the pattern matches the pattern because of the removal of /**
	@Test
	public void extractPathWithinPattern() throws Exception {
		PathPatternParser pp = new PathPatternParser();
		PathPattern p = null;

		p = pp.parse("/welcome*/");
		assertEquals("welcome/",p.extractPathWithinPattern("/welcome/"));
//		assertEquals("welcome",pathMatcher.extractPathWithinPattern("/welcome*/", "/welcome/"));
//		assertEquals("welcome/",pathMatcher.extractPathWithinPattern("/welcome*/", "/welcome"));

		// What is the expected behaviour here? Is it an unwritten rule that trailing / are removed?
		p = pp.parse("/welcome*/");
		assertEquals("welcome/",p.extractPathWithinPattern("/welcome"));
		
		p = pp.parse("/docs/commit.html");
		assertEquals("",p.extractPathWithinPattern("/docs/commit.html"));

		p = pp.parse("/docs/*");
		assertEquals("cvs/commit", p.extractPathWithinPattern("/docs/cvs/commit"));

		p = pp.parse("/docs/cvs/*.html");
		assertEquals("commit.html",p.extractPathWithinPattern("/docs/cvs/commit.html"));
		
		p = pp.parse("/docs/**");
		assertEquals("cvs/commit",p.extractPathWithinPattern("/docs/cvs/commit"));
		
		p = pp.parse("/doo/{*foobar}");
		assertEquals("customer.html",p.extractPathWithinPattern("/doo/customer.html"));
		
		p = pp.parse("/doo/{*foobar}");
		assertEquals("daa/customer.html",p.extractPathWithinPattern("/doo/daa/customer.html"));

		p = pp.parse("/docs/**/*.html");
		assertEquals("cvs/commit.html",p.extractPathWithinPattern("/docs/cvs/commit.html"));
		
		p = pp.parse("/docs/**/*.html");
		assertEquals("commit.html",p.extractPathWithinPattern("/docs/commit.html"));
		
		p = pp.parse("/*.html");
		assertEquals("commit.html",p.extractPathWithinPattern("/commit.html"));
		
		p = pp.parse("/*.html");
		assertEquals("docs/commit.html",p.extractPathWithinPattern("/docs/commit.html"));
		
		p = pp.parse("*.html");
		assertEquals("/commit.html",p.extractPathWithinPattern("/commit.html"));
		
		p = pp.parse("*.html");
		assertEquals("/docs/commit.html",p.extractPathWithinPattern("/docs/commit.html"));
		
		p = pp.parse("**/*.*");
		assertEquals("/docs/commit.html",p.extractPathWithinPattern("/docs/commit.html"));

		p = pp.parse("*");
		assertEquals("/docs/commit.html",p.extractPathWithinPattern("/docs/commit.html"));

		p = pp.parse("**/commit.html");
		assertEquals("/docs/cvs/other/commit.html",p.extractPathWithinPattern("/docs/cvs/other/commit.html"));

		p = pp.parse("/docs/**/commit.html");
		assertEquals("cvs/other/commit.html",p.extractPathWithinPattern("/docs/cvs/other/commit.html"));

		p = pp.parse("/docs/**/**/**/**");
		assertEquals("cvs/other/commit.html",p.extractPathWithinPattern("/docs/cvs/other/commit.html"));

		p = pp.parse("/d?cs/*");
		assertEquals("docs/cvs/commit",p.extractPathWithinPattern("/docs/cvs/commit"));
		
		p = pp.parse("/docs/c?s/*.html");
		assertEquals("cvs/commit.html",p.extractPathWithinPattern("/docs/cvs/commit.html"));

		p = pp.parse("/d?cs/**");
		assertEquals("docs/cvs/commit",p.extractPathWithinPattern("/docs/cvs/commit"));

		p = pp.parse("/d?cs/**/*.html");
		assertEquals("docs/cvs/commit.html",p.extractPathWithinPattern("/docs/cvs/commit.html"));
		
		p = pp.parse("/a/b/c*d*/*.html");
		assertEquals("cod/foo.html",p.extractPathWithinPattern("/a/b/cod/foo.html"));
		
	}
	
	@Test
	public void extractUriTemplateVariables() throws Exception {
		PathPatternParser pp = new PathPatternParser();
		PathPattern p = null;
		Map<String,String> result;
		 
		p = pp.parse("/hotels/{hotel}");
		result = p.matchAndExtract("/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		// TODO check all state reset in PatternParser.parse()
		p = pp.parse("/h?tels/{hotel}");
		result = p.matchAndExtract("/hotels/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		p = pp.parse("/hotels/{hotel}/bookings/{booking}");
		result = p.matchAndExtract("/hotels/1/bookings/2");
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("hotel", "1");
		expected.put("booking", "2");
		assertEquals(expected, result);

		p = pp.parse("/**/hotels/**/{hotel}");
		result = p.matchAndExtract("/foo/hotels/bar/1");
		assertEquals(Collections.singletonMap("hotel", "1"), result);

		p = pp.parse("/{page}.html");
		result = p.matchAndExtract("/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		p = pp.parse("/{page}.*");
		result = p.matchAndExtract("/42.html");
		assertEquals(Collections.singletonMap("page", "42"), result);

		p = pp.parse("/A-{B}-C");
		result = p.matchAndExtract("/A-b-C");
		assertEquals(Collections.singletonMap("B", "b"), result);

		p = pp.parse("/{name}.{extension}");
		result = p.matchAndExtract("/test.html");
		expected = new LinkedHashMap<>();
		expected.put("name", "test");
		expected.put("extension", "html");
		assertEquals(expected, result);
	}

	 @Test
	 public void extractUriTemplateVariablesRegex() {
		 PathPatternParser pp = new PathPatternParser();
		 PathPattern p = null;
		 
		 p = pp.parse("{symbolicName:[\\w\\.]+}-{version:[\\w\\.]+}.jar");
		 Map<String,String> result = p.matchAndExtract("com.example-1.0.0.jar");
		 assertEquals("com.example", result.get("symbolicName"));
		 assertEquals("1.0.0", result.get("version"));

		 p = pp.parse("{symbolicName:[\\w\\.]+}-sources-{version:[\\w\\.]+}.jar");
		 result = p.matchAndExtract("com.example-sources-1.0.0.jar");
		 assertEquals("com.example", result.get("symbolicName"));
		 assertEquals("1.0.0", result.get("version"));
	 }
	
	 @Test
	 public void extractUriTemplateVarsRegexQualifiers() {
		 PathPatternParser pp = new PathPatternParser();
		 PathPattern p = null;

		 p = pp.parse("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.]+}.jar");
		 Map<String,String> result = p.matchAndExtract("com.example-sources-1.0.0.jar");
		 assertEquals("com.example", result.get("symbolicName"));
		 assertEquals("1.0.0", result.get("version"));
		 
//		 p = pp.parse("{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar");
//		 result = p.matchAndExtract("com.example-sources-1.0.0-20100220.jar");
//	
//	 result = pathMatcher.extractUriTemplateVariables(
//	 "{symbolicName:[\\w\\.]+}-sources-{version:[\\d\\.]+}-{year:\\d{4}}{month:\\d{2}}{day:\\d{2}}.jar",
//	 "com.example-sources-1.0.0-20100220.jar");
//	 assertEquals("com.example", result.get("symbolicName"));
//	 assertEquals("1.0.0", result.get("version"));
//	 assertEquals("2010", result.get("year"));
//	 assertEquals("02", result.get("month"));
//	 assertEquals("20", result.get("day"));
//	
		 p = pp.parse("{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar");
		 result = p.matchAndExtract("com.example-sources-1.0.0.{12}.jar");
//	 result = pathMatcher.extractUriTemplateVariables(
//	 "{symbolicName:[\\p{L}\\.]+}-sources-{version:[\\p{N}\\.\\{\\}]+}.jar",
//	 "com.example-sources-1.0.0.{12}.jar");
		 assertEquals("com.example", result.get("symbolicName"));
		 assertEquals("1.0.0.{12}", result.get("version"));
	 }
	
	 // TODO What was the spring bug here, why is this not supposed to work? is it because (..) is supposed to be a different group?
	 @Test
	 public void extractUriTemplateVarsRegexCapturingGroups() {
		 PathPatternParser pp = new PathPatternParser();
		 PathPattern p = pp.parse("/web/{id:foo(bar)?}");
		 Map<String,String> results = p.matchAndExtract("/web/foobar");
		 System.out.println(results);
//		 exception.expect(IllegalArgumentException.class);
//		 exception.expectMessage(containsString("The number of capturing groups in the pattern"));
//		 pathMatcher.extractUriTemplateVariables("/web/{id:foo(bar)?}",
//		 "/web/foobar");
	 }

	 static class TestPathCombiner {

		PathPatternParser pp = new PathPatternParser();
		
		public String combine(String string1, String string2) {
			PathPattern pattern1 = pp.parse(string1);
			return pattern1.combine(string2);
		}

	}

		@Rule
		public final ExpectedException exception = ExpectedException.none();
	 
	 @Test
	 public void combine() {
		 TestPathCombiner pathMatcher = new TestPathCombiner();
		 assertEquals("", pathMatcher.combine(null, null));
		 assertEquals("/hotels", pathMatcher.combine("/hotels", null));
		 assertEquals("/hotels", pathMatcher.combine(null, "/hotels"));
		 assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*","booking"));
		 assertEquals("/hotels/booking", pathMatcher.combine("/hotels/*",	 "/booking"));
		 assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**",	 "booking"));
		 assertEquals("/hotels/**/booking", pathMatcher.combine("/hotels/**",	 "/booking"));
		 assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "/booking"));
		 assertEquals("/hotels/booking", pathMatcher.combine("/hotels", "booking"));
		 assertEquals("/hotels/booking", pathMatcher.combine("/hotels/", "booking"));
		 assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels/*", "{hotel}"));
		 assertEquals("/hotels/**/{hotel}", pathMatcher.combine("/hotels/**",	 "{hotel}"));
		 assertEquals("/hotels/{hotel}", pathMatcher.combine("/hotels",	 "{hotel}"));
		 assertEquals("/hotels/{hotel}.*", pathMatcher.combine("/hotels",	 "{hotel}.*"));
		 assertEquals("/hotels/*/booking/{booking}",	 pathMatcher.combine("/hotels/*/booking", "{booking}"));
		 assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.html"));
		 assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel"));
		 assertEquals("/hotel.html", pathMatcher.combine("/*.html", "/hotel.*"));
		 // TODO surely this is bogus?
		 assertEquals("/d/e/f/hotel.html",pathMatcher.combine("/a/b/c/*.html", "/d/e/f/hotel.*"));
		 assertEquals("/*.html", pathMatcher.combine("/**", "/*.html"));
		 assertEquals("/*.html", pathMatcher.combine("/*", "/*.html"));
		 assertEquals("/*.html", pathMatcher.combine("/*.*", "/*.html"));
		 assertEquals("/{foo}/bar", pathMatcher.combine("/{foo}", "/bar")); // SPR-8858
		 assertEquals("/user/user", pathMatcher.combine("/user", "/user")); // SPR-7970
		 assertEquals("/{foo:.*[^0-9].*}/edit/", pathMatcher.combine("/{foo:.*[^0-9].*}", "/edit/")); // SPR-10062
		 assertEquals("/1.0/foo/test", pathMatcher.combine("/1.0", "/foo/test"));
		 // SPR-10554
		 assertEquals("/hotel", pathMatcher.combine("/", "/hotel")); // SPR-12975
		 assertEquals("/hotel/booking", pathMatcher.combine("/hotel/", "/booking")); // SPR-12975
	 }
	
	 @Test
	 public void combineWithTwoFileExtensionPatterns() {
		 TestPathCombiner pathMatcher = new TestPathCombiner();
		 exception.expect(IllegalArgumentException.class);
		 pathMatcher.combine("/*.html", "/*.txt");
	 }
	
	 
	 private PathPattern parse(String path) {
		 PathPatternParser pp = new PathPatternParser();
		 return pp.parse(path);
	 }
	 
	 @Test
	 public void patternComparator() {
		 Comparator<PathPattern> comparator = new PatternComparatorConsideringPath("/hotels/new"); 
	//	 pathMatcher.getPatternComparator("/hotels/new");
		
		 assertEquals(0, comparator.compare(null, null));
		 assertEquals(1, comparator.compare(null, parse("/hotels/new")));
		 assertEquals(-1, comparator.compare(parse("/hotels/new"), null));
	
		 assertEquals(0, comparator.compare(parse("/hotels/new"), parse("/hotels/new")));
	
		 assertEquals(-1, comparator.compare(parse("/hotels/new"), parse("/hotels/*")));
		 assertEquals(1, comparator.compare(parse("/hotels/*"), parse("/hotels/new")));
		 assertEquals(0, comparator.compare(parse("/hotels/*"), parse("/hotels/*")));
	
		 assertEquals(-1, comparator.compare(parse("/hotels/new"), parse("/hotels/{hotel}")));
		 assertEquals(1, comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/new")));
		 assertEquals(0, comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/{hotel}")));
		 assertEquals(-1, comparator.compare(parse("/hotels/{hotel}/booking"), parse("/hotels/{hotel}/bookings/{booking}")));
		 assertEquals(1, comparator.compare(parse("/hotels/{hotel}/bookings/{booking}"), parse("/hotels/{hotel}/booking")));
	
		 // SPR-10550 - TODO if ditching /** can ignore this
		 assertEquals(-1, comparator.compare(parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"), parse("/**")));
		 assertEquals(1, comparator.compare(parse("/**"), parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")));
		 assertEquals(0, comparator.compare(parse("/**"), parse("/**")));
		
		 assertEquals(-1, comparator.compare(parse("/hotels/{hotel}"),parse( "/hotels/*")));
		 assertEquals(1, comparator.compare(parse("/hotels/*"), parse("/hotels/{hotel}")));
		
		 assertEquals(-1, comparator.compare(parse("/hotels/*"),parse( "/hotels/*/**")));
		 assertEquals(1, comparator.compare(parse("/hotels/*/**"), parse("/hotels/*")));
		
		 assertEquals(-1, comparator.compare(parse("/hotels/new"), parse("/hotels/new.*")));
		 
		 // TODO ?? 2s
	//		 assertEquals(2, comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/{hotel}.*")));
		
		 // SPR-6741
		 assertEquals(-1, comparator.compare(parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}"), parse( "/hotels/**")));
		 assertEquals(1, comparator.compare(parse("/hotels/**"), parse("/hotels/{hotel}/bookings/{booking}/cutomers/{customer}")));
		 assertEquals(1, comparator.compare(parse("/hotels/foo/bar/**"), parse("/hotels/{hotel}")));
		 assertEquals(-1, comparator.compare(parse("/hotels/{hotel}"), parse("/hotels/foo/bar/**")));
		 // TODO ?? 2s
	//	 assertEquals(2, comparator.compare(parse("/hotels/**/bookings/**"), parse("/hotels/**")));
	//	 assertEquals(-2, comparator.compare(parse("/hotels/**"), parse("/hotels/**/bookings/**")));
		
		 // SPR-8683
		 assertEquals(1, comparator.compare(parse("/**"), parse("/hotels/{hotel}")));
		
		 // longer is better
		 assertEquals(1, comparator.compare(parse("/hotels"), parse("/hotels2")));
		
		 // SPR-13139
		 assertEquals(-1, comparator.compare(parse("*"),parse( "*/**")));
		 assertEquals(1, comparator.compare(parse("*/**"), parse("*")));
	 }

	 @Test
	 public void patternComparatorSort() {
		 Comparator<PathPattern> comparator = new PatternComparatorConsideringPath("/hotels/new");
		 List<PathPattern> paths = new ArrayList<>(3);
		 PathPatternParser pp = new PathPatternParser();
		 paths.add(null);
		 paths.add(pp.parse("/hotels/new"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertNull(paths.get(1));
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/new"));
		 paths.add(null);
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertNull(paths.get(1));
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/*"));
		 paths.add(pp.parse("/hotels/new"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertEquals("/hotels/*", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/new"));
		 paths.add(pp.parse("/hotels/*"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertEquals("/hotels/*", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/**"));
		 paths.add(pp.parse("/hotels/*"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/*", paths.get(0).getPatternString());
		 assertEquals("/hotels/**", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/*"));
		 paths.add(pp.parse("/hotels/**"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/*", paths.get(0).getPatternString());
		 assertEquals("/hotels/**", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/{hotel}"));
		 paths.add(pp.parse("/hotels/new"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertEquals("/hotels/{hotel}", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/new"));
		 paths.add(pp.parse("/hotels/{hotel}"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertEquals("/hotels/{hotel}", paths.get(1).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/*"));
		 paths.add(pp.parse("/hotels/{hotel}"));
		 paths.add(pp.parse("/hotels/new"));
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/new", paths.get(0).getPatternString());
		 assertEquals("/hotels/{hotel}", paths.get(1).getPatternString());
		 assertEquals("/hotels/*", paths.get(2).getPatternString());
		 paths.clear();
		
		 paths.add(pp.parse("/hotels/ne*"));
		 paths.add(pp.parse("/hotels/n*"));
		 Collections.shuffle(paths);
		 Collections.sort(paths, comparator);
		 assertEquals("/hotels/ne*", paths.get(0).getPatternString());
		 assertEquals("/hotels/n*", paths.get(1).getPatternString());
		 paths.clear();
		
		 // TODO why is .* a single wildcard and ** a double wildcard, what is just a * ?
//		 comparator = new PatternComparatorConsideringPath("/hotels/new.html");//.getPatternComparator("/hotels/new.html");
//		 paths.add(pp.parse("/hotels/new.*"));
//		 paths.add(pp.parse("/hotels/{hotel}"));
//		 Collections.shuffle(paths);
//		 Collections.sort(paths, comparator);
//		 assertEquals("/hotels/new.*", paths.get(0).toPatternString());
//		 assertEquals("/hotels/{hotel}", paths.get(1).toPatternString());
//		 paths.clear();
		
		 comparator =new PatternComparatorConsideringPath("/web/endUser/action/login.html");
//		 pathMatcher.getPatternComparator("/web/endUser/action/login.html");
		 paths.add(pp.parse("/**/login.*"));
		 paths.add(pp.parse("/**/endUser/action/login.*"));
		 Collections.sort(paths, comparator);
		 assertEquals("/**/endUser/action/login.*", paths.get(0).getPatternString());
		 assertEquals("/**/login.*", paths.get(1).getPatternString());
		 paths.clear();
	 }
//	//
//	// @Test // SPR-8687
//	// public void trimTokensOff() {
//	// pathMatcher.setTrimTokens(false);
//	//
//	// checkMatches("/group/{groupName}/members",
//	// "/group/sales/members"));
//	// checkMatches("/group/{groupName}/members", "/group/
//	// sales/members"));
//	// checkNoMatch("/group/{groupName}/members", "/Group/
//	// Sales/Members"));
//	// }
//	//
	 @Test // SPR-13286
	 public void caseInsensitive() {
		 PathPatternParser pp = new PathPatternParser();
		 pp.setCaseSensitive(false);
		 PathPattern p = pp.parse("/group/{groupName}/members");
		 assertTrue(p.matches("/group/sales/members"));
		 assertTrue(p.matches("/Group/Sales/Members"));
		 assertTrue(p.matches("/group/Sales/members"));	
	 }
//	//
//	// @Test
//	// public void defaultCacheSetting() {
//	// match();
//	// assertTrue(pathMatcher.stringMatcherCache.size() > 20);
//	//
//	// for (int i = 0; i < 65536; i++) {
//	// pathMatcher.match("test" + i, "test");
//	// }
//	// // Cache turned off because it went beyond the threshold
//	// assertTrue(pathMatcher.stringMatcherCache.isEmpty());
//	// }
//	//
//	// @Test
//	// public void cachePatternsSetToTrue() {
//	// pathMatcher.setCachePatterns(true);
//	// match();
//	// assertTrue(pathMatcher.stringMatcherCache.size() > 20);
//	//
//	// for (int i = 0; i < 65536; i++) {
//	// pathMatcher.match("test" + i, "test" + i);
//	// }
//	// // Cache keeps being alive due to the explicit cache setting
//	// assertTrue(pathMatcher.stringMatcherCache.size() > 65536);
//	// }
//	//
//	// @Test
//	// public void
//	// preventCreatingStringMatchersIfPathDoesNotStartsWithPatternPrefix() {
//	// pathMatcher.setCachePatterns(true);
//	// assertEquals(0, pathMatcher.stringMatcherCache.size());
//	//
//	// pathMatcher.match("test?", "test");
//	// assertEquals(1, pathMatcher.stringMatcherCache.size());
//	//
//	// pathMatcher.match("test?", "best");
//	// pathMatcher.match("test/*", "view/test.jpg");
//	// pathMatcher.match("test/**/test.jpg", "view/test.jpg");
//	// pathMatcher.match("test/{name}.jpg", "view/test.jpg");
//	// assertEquals(1, pathMatcher.stringMatcherCache.size());
//	// }
//	//
//	// @Test
//	// public void
//	// creatingStringMatchersIfPatternPrefixCannotDetermineIfPathMatch() {
//	// pathMatcher.setCachePatterns(true);
//	// assertEquals(0, pathMatcher.stringMatcherCache.size());
//	//
//	// pathMatcher.match("test", "testian");
//	// pathMatcher.match("test?", "testFf");
//	// pathMatcher.match("test/*", "test/dir/name.jpg");
//	// pathMatcher.match("test/{name}.jpg", "test/lorem.jpg");
//	// pathMatcher.match("bla/**/test.jpg", "bla/test.jpg");
//	// pathMatcher.match("**/{name}.jpg", "test/lorem.jpg");
//	// pathMatcher.match("/**/{name}.jpg", "/test/lorem.jpg");
//	// pathMatcher.match("/*/dir/{name}.jpg", "/*/dir/lorem.jpg");
//	//
//	// assertEquals(7, pathMatcher.stringMatcherCache.size());
//	// }
//	//
//	// @Test
//	// public void cachePatternsSetToFalse() {
//	// pathMatcher.setCachePatterns(false);
//	// match();
//	// assertTrue(pathMatcher.stringMatcherCache.isEmpty());
//	// }
//	//
//	// @Test
//	// public void extensionMappingWithDotPathSeparator() {
//	// pathMatcher.setPathSeparator(".");
//	// assertEquals("Extension mapping should be disabled with \".\" as path
//	// separator",
//	// "/*.html.hotel.*", pathMatcher.combine("/*.html", "hotel.*"));
//	// }
//	//
//	// }
//
//	// ---
//
//	private void assertContains(String uriTemplateToFind, String[] uriTemplatesToSearch) {
//		StringBuilder s = new StringBuilder();
//		for (String uriTemplate : uriTemplatesToSearch) {
//			s.append(uriTemplate).append("\n");
//			if (uriTemplate.equals(uriTemplateToFind)) {
//				return;
//			}
//		}
//		fail("Did not find expected URI template '" + uriTemplateToFind + "' in candidates:\n" + s.toString());
//	}
//
////	private void checkMatches(String pattern, String... inputs) {
////		Experiments matcher = new Experiments(pattern);
////		for (String input : inputs) {
////			assertTrue("Expected pattern '" + pattern + "' to match on '" + input + "'", matcher.match(input));
////		}
////	}
////
////	private void checkNoMatch(String pattern, String... inputs) {
////		Experiments matcher = new Experiments(pattern);
////		for (String input : inputs) {
////			assertFalse("Expected pattern '" + pattern + "' *not* to match on '" + input + "'", matcher.match(input));
////		}
////	}
//
//	private void addTemplate(PathMatcher pathMatcher, String templateString) {
//		pathMatcher.addURITemplate(new TestURITemplate(templateString));
//	}

//	private void assertMatchCount(int expectedMatchCount, List<MatchResult> matches) {
//		if (expectedMatchCount!=matches.size()) {
//			fail("Expected "+expectedMatchCount+" matches but found "+matches.size()+"\n"+matches);
//		}
//	}

	private char separator = PathPatternParser.DEFAULT_SEPARATOR;
	
	private void checkMatches(String uriTemplate, String path) {
		PathPatternParser parser = (separator==PathPatternParser.DEFAULT_SEPARATOR?new PathPatternParser():new PathPatternParser(separator));
		PathPattern p = parser.parse(uriTemplate);
		assertTrue(p.matches(path));
	}


	private void checkStartNoMatch(String uriTemplate, String path) {
		PathPatternParser p = new PathPatternParser();
		PathPattern pattern = p.parse(uriTemplate);
		printPattern(pattern);
		assertFalse(pattern.matchStart(path));
	}
	
	private void checkStartMatches(String uriTemplate, String path) {
		PathPatternParser p = new PathPatternParser();
		PathPattern pattern = p.parse(uriTemplate);
		printPattern(pattern);
		assertTrue(pattern.matchStart(path));
	}
	
	private void printPattern(PathPattern pattern) {
		PathElement s = pattern.getHeadSection();
		while (s!=null) {
			System.out.println(s);
			s=s.next;
		}
	}

	private void checkNoMatch(String uriTemplate, String path) {
		PathPatternParser p = new PathPatternParser();
		PathPattern pattern = p.parse(uriTemplate);
		assertFalse(pattern.matches(path));
	}

	private void checkCapture(String uriTemplate, String path, String... keyValues) {
		PathPatternParser parser = new PathPatternParser();
		PathPattern pattern = parser.parse(uriTemplate);
		Map<String,String> matchResults = pattern.matchAndExtract(path);
		Map<String,String> expectedKeyValues = new HashMap<>();
		if (keyValues!=null) {
			for (int i=0;i<keyValues.length;i+=2) {
				expectedKeyValues.put(keyValues[i], keyValues[i+1]);
			}
		}
		Map<String,String> capturedVariables = matchResults;
		for (Map.Entry<String,String> me: expectedKeyValues.entrySet()) {
			String value = capturedVariables.get(me.getKey());
			if (value == null) {
				fail("Did not find key '"+me.getKey()+"' in captured variables: "+capturedVariables);
			}
			if (!value.equals(me.getValue())) {
				fail("Expected value '"+me.getValue()+"' for key '"+me.getKey()+"' but was '"+value+"'");
			}
		}
	}

}
