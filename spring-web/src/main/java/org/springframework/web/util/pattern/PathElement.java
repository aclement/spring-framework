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
 * Common supertype for the Ast nodes created to represent a path pattern.
 *
 * @author Andy Clement
 * @since 5.0
 */
abstract class PathElement {

	// Score related
	protected static final int WILDCARD_WEIGHT = 100;

	protected static final int CAPTURE_VARIABLE_WEIGHT = 1;


	// Position in the pattern where this path element starts
	protected final int pos;

	// The separator used in this path pattern
	protected final char separator;

	// The next path element in the chain
	protected PathElement next;

	// The previous path element in the chain
	protected PathElement prev;

	protected PathPattern pp;

	protected PathPattern pathPattern;

	/**
	 * Create a new path element.
	 * @param pos the position where this path element starts in the pattern data
	 * @param separator the separator in use in the path pattern
	 */
	PathElement(int pos, char separator) {
		this.pos = pos;
		this.separator = separator;
	}


	/**
	 * Attempt to match this path element.
	 * @param segmentIndex the index of the path segment to match this PathElement against
	 * @param matchingContext encapsulates context for the match including the candidate
	 * @return {@code true} if it matches, otherwise {@code false}
	 */
	public abstract boolean matches(int segmentIndex, MatchingContext matchingContext);

	/**
	 * @return the length of the path element where captures are considered to be one character long.
	 */
	public abstract int getNormalizedLength();

	/**
	 * @return the textual representation of this element
	 */
	public abstract char[] getText();

	/**
	 * Return the number of variables captured by the path element.
	 */
	public int getCaptureCount() {
		return 0;
	}

	/**
	 * Return the number of wildcard elements (*, ?) in the path element.
	 */
	public int getWildcardCount() {
		return 0;
	}

	/**
	 * Return the score for this PathElement, combined score is used to compare parsed patterns.
	 */
	public int getScore() {
		return 0;
	}

	/**
	 * When the pattern has 'run out' this checks the remainder of the path segment container.
	 * <ul>
	 * <li>If there is more data, then this pattern does not match.
	 * <li>If the pattern ends with a separator then it may not match if the path doesn't
	 * have a trailing slash, depending on whether this is just a match start check
	 * <li>If the pattern has no trailing slash it is a match if the allow trailing slash
	 * option is on.
	 * </ul>
	 * @param segmentIndex the index of the element 
	 * @param matchingContext context in which match is occurring
	 * @return true if rest of the path is OK
	 */
	protected boolean verifyEndOfPath(int segmentIndex, MatchingContext matchingContext) {
		if (segmentIndex < matchingContext.pathSegmentCount) {
			// more data, can't be a match
			return false;
		}
		// What about separators?
		if (pathPattern.endsWithSep) {
			if (!matchingContext.candidate.hasTrailingSlash()) {
				if (matchingContext.isMatchStartMatching) {
					return true;
				}
				else {
					return false;
				}
			}
		}
		else if (matchingContext.candidate.hasTrailingSlash()) {
			return matchingContext.isAllowOptionalTrailingSlash();
		}
		return true;
	}

	/**
	 * @return true if the there are no more PathElements in the pattern
	 */
	protected final boolean isNoMorePattern() {
		return this.next == null;
	}

	/**
	 * @param index the index of the next segment to consider
	 * @param matchingContext the context in which the match is occurring
	 * @return true if there is more data
	 */
	protected final boolean noMoreData(int index, MatchingContext matchingContext) {
		return index >= matchingContext.pathSegmentCount;
	}

}
