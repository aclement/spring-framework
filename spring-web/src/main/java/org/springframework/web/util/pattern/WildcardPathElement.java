/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.util.pattern;

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A wildcard path element. In the pattern '/foo/&ast;/goo' the * is
 * represented by a WildcardPathElement. Within a path it matches at least 
 * one character but at the end of a path it can match zero characters.
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardPathElement extends PathElement {

	private final static char[] WILDCARD_CHARS = "*".toCharArray();

	public WildcardPathElement(int pos, char separator) {
		super(pos, separator);
	}

	/**
	 * WildcardPathElement will match an intermediate segment if it has at least one character
	 * or any trailing segment
	 */
	@Override
	public boolean matches(int segmentIndex, MatchingContext matchingContext) {
		CharSequence segmentValue = matchingContext.getSegmentValue(segmentIndex);
		boolean matched = false;
		if (isNoMorePattern()) {
			matched = pathPattern.endsWithSep?matchingContext.candidate.hasTrailingSlash():true;
			if (matched) {
				if (segmentValue == null) { // there is no more path data
					// Matches if the there was a trailing slash or path is simply '/'
					matched = matchingContext.candidate.hasTrailingSlash() ||
							  (segmentIndex == 0 && matchingContext.candidate.isAbsolute());
				}
				else {
					matched = (segmentValue.length() > 0); // this segment must have some data in it (more than zero chars)
					if (matched && ((segmentIndex + 1) == matchingContext.pathSegmentCount)) {
						matched = matched && (matchingContext.candidate.hasTrailingSlash()?(matchingContext.isAllowOptionalTrailingSlash()||pathPattern.endsWithSep):true);
					}
					else if (matched && ((segmentIndex + 1) < matchingContext.pathSegmentCount )) {
						if (matchingContext.determineRemainingPath) {
							matchingContext.remainingPathIndex = segmentIndex + 1;
						} 
						else {
							// there is more data, not a match
							matched = false;
						}
					}
				}
			}
		}
		else { 
			if ((segmentIndex >= matchingContext.pathSegmentCount)  // there is no data to match
					|| matchingContext.pathSegments.get(segmentIndex).valueDecoded().length() < 1) {
				return false;
			}

			if (matchingContext.isMatchStartMatching && noMoreData(segmentIndex + 1, matchingContext)) {
				return true; // no more data but matches up to this point
			}
			return this.next.matches(segmentIndex + 1, matchingContext);
		}
		return matched;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return WILDCARD_WEIGHT;
	}

	public String toString() {
		return "Wildcard(*)";
	}

	@Override
	public char[] getText() {
		return WILDCARD_CHARS;
	}

}
