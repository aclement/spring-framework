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
 * An empty path element. Represents the empty element between adjacent separators in a
 * path. For example in '/foo//bar' there is a LiteralPathElement then an EmptyPathElement
 * then another LiteralPathElement (the leading separator is captured as a flag
 * on the PathPattern).
 *
 * @author Andy Clement
 * @since 5.0
 */
class EmptyPathElement extends PathElement {

	EmptyPathElement(int pos, char separator) {
		super(pos, separator);
	}

	@Override
	public boolean matches(int segmentIndex, MatchingContext matchingContext) {
		boolean matched = false;
		if (matchingContext.isSegmentEmpty(segmentIndex)) {
			matched = true;
		}
		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = segmentIndex + 1;
				return true;
			}
			else {
				return verifyEndOfPath(segmentIndex + 1, matchingContext);
			}
		}
		else {
			if (matchingContext.isMatchStartMatching && noMoreData(segmentIndex + 1, matchingContext)) {
				return true; // no more data but matches up to this point
			}
			matched = this.next.matches(segmentIndex + 1, matchingContext);
		}
		return matched;
	}

	@Override
	public int getNormalizedLength() {
		return 0;
	}

	public String toString() {
		return "Empty()";
	}

	@Override
	public char[] getText() {
		return "".toCharArray();
	}

}
