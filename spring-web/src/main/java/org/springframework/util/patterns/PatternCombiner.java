///*
// * Copyright 2016 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.springframework.util.patterns;
//
///**
// * Combine patterns according to some basic rules.
// * 
// * @author Andy Clement
// */
//public class PatternCombiner {
//
//	/**
//	 * Combine two patterns into a new pattern.
//	 * <p>This implementation simply concatenates the two patterns, unless
//	 * the first pattern contains a file extension match (e.g., {@code *.html}).
//	 * In that case, the second pattern will be merged into the first. Otherwise,
//	 * an {@code IllegalArgumentException} will be thrown.
//	 * <h3>Examples</h3>
//	 * <table border="1">
//	 * <tr><th>Pattern 1</th><th>Pattern 2</th><th>Result</th></tr>
//	 * <tr><td>{@code null}</td><td>{@code null}</td><td>&nbsp;</td></tr>
//	 * <tr><td>/hotels</td><td>{@code null}</td><td>/hotels</td></tr>
//	 * <tr><td>{@code null}</td><td>/hotels</td><td>/hotels</td></tr>
//	 * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr>
//	 * <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
//	 * <tr><td>/hotels/*</td><td>/bookings</td><td>/hotels/bookings</td></tr>
//	 * <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
//	 * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
//	 * <tr><td>/hotels/*</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
//	 * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
//	 * <tr><td>/*.html</td><td>/hotels.html</td><td>/hotels.html</td></tr>
//	 * <tr><td>/*.html</td><td>/hotels</td><td>/hotels.html</td></tr>
//	 * <tr><td>/*.html</td><td>/*.txt</td><td>{@code IllegalArgumentException}</td></tr>
//	 * </table>
//	 * @param pattern1 the first pattern
//	 * @param pattern2 the second pattern
//	 * @return the combination of the two patterns
//	 * @throws IllegalArgumentException if the two patterns cannot be combined
//	 */
//	public String combine(PathPattern pattern1, PathPattern pattern2) {
//		if (pattern1.getSeparator() != pattern2.getSeparator()) {
//			throw new IllegalArgumentException("Patterns using different separators cannot be combined");
//		}
//		String pattern1string = pattern1.getPatternString();
//		String pattern2string = pattern2.getPatternString();
//		return combine(pattern1string, pattern2string);
//	}
//		
//	public String combine(String pattern1string, String pattern2string) {
//		// If one of them is empty the result is the other. If both empty the result is ""
//		if (pattern1string.length()==0) {
//			if (pattern2string.length()==0) {
//				return "";
//			} else {
//				return pattern2string;
//			}
//		} else if (pattern2string.length()==0) {
//			return pattern1string;
//		}
//		
//		boolean pattern1containsUriVar = pattern1string.indexOf('{')!=-1;//getCapturedVariableCount()>0;
//		// TODO case sensitivity of the patterns, at all relevant?
//		if (!pattern1string.equals(pattern2string) && !pattern1containsUriVar && pattern1string.matches(pattern2.getPatternString())) {
//			return pattern2.getPatternString();
//		}
////		boolean pattern1ContainsUriVar = (pattern1.indexOf('{') != -1);
////		if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
////			// /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
////			// However /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
////			return pattern2;
////		}
////
//		// /hotels/* + /booking -> /hotels/booking
//		// /hotels/* + booking -> /hotels/booking
//		if (pattern1.endsWithSeparatorWildcard()) {
//			return concat(Character.toString(pattern1.getSeparator()),pattern1string.substring(0,pattern1string.length()-2),pattern2string);
//		}
////		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
////			return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
////		}
////
//		// no need to support this
////		// /hotels/** + /booking -> /hotels/**/booking
////		// /hotels/** + booking -> /hotels/**/booking
////		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
////			return concat(pattern1, pattern2);
////		}
////
//		
//		int starDotPos1 = pattern1string.indexOf("*."); // TODO what is this asking?
//		if (pattern1containsUriVar || starDotPos1 == -1 || pattern1.getSeparator()=='.') {
//			// simply concatenate the two patterns
//			return concat(Character.toString(pattern1.getSeparator()),pattern1string, pattern2string);			
//		}
//		
////		int starDotPos1 = pattern1.indexOf("*.");
////		if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
////			// simply concatenate the two patterns
////			return concat(pattern1, pattern2);
////		}
//		
//		
//		String ext1 = pattern1string.substring(starDotPos1+1); // looking for the first extension
//		int dotPos2 = pattern2string.indexOf('.');
//		String file2 = (dotPos2==-1?pattern2string:pattern2string.substring(0,dotPos2));// TODO What about multiple dots?
//		String ext2 = (dotPos2 == -1?"":pattern2string.substring(dotPos2));
//		boolean ext1All = (ext1.equals(".*") || ext1.equals(""));
//		boolean ext2All = (ext2.equals(".*") || ext2.equals(""));
//		if (!ext1All && !ext2All) {
//			throw new IllegalArgumentException("Cannot combine patterns: " + pattern1string + " vs " + pattern2string);
//		}
//		String ext = (ext1All ?ext2:ext1);
//		return file2+ext;
//		
////		String ext1 = pattern1.substring(starDotPos1 + 1);
////		int dotPos2 = pattern2.indexOf('.');
////		String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
////		String ext2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
////		boolean ext1All = (ext1.equals(".*") || ext1.equals(""));
////		boolean ext2All = (ext2.equals(".*") || ext2.equals(""));
////		if (!ext1All && !ext2All) {
////			throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
////		}
////		String ext = (ext1All ? ext2 : ext1);
////		return file2 + ext;
////	}
//// }
//	}
//
//	private String concat(String separator, String path1, String path2) {
//		boolean path1EndsWithSeparator = path1.endsWith(separator);
//		boolean path2StartsWithSeparator = path2.startsWith(separator);
//
//		if (path1EndsWithSeparator && path2StartsWithSeparator) {
//			return path1 + path2.substring(1);
//		}
//		else if (path1EndsWithSeparator || path2StartsWithSeparator) {
//			return path1 + path2;
//		}
//		else {
//			return path1 + separator + path2;
//		}
//	}
//
//}
