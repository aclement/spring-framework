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

import org.springframework.http.server.reactive.PathSegment;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A literal path element. In the pattern '/foo/bar/goo' there are three
 * literal path elements 'foo', 'bar' and 'goo'.
 *
 * @author Andy Clement
 */
class LiteralPathElement extends PathElement {

	private char[] text;

	private int len;

	private boolean caseSensitive;

	public LiteralPathElement(int pos, char[] literalText, boolean caseSensitive, char separator) {
		super(pos, separator);
		this.len = literalText.length;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		}
		else {
			// Force all the text lower case to make matching faster
			this.text = new char[literalText.length];
			for (int i = 0; i < len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}

	@Override
	public boolean matches(int segmentIndex, MatchingContext matchingContext) {
		if (segmentIndex >= matchingContext.pathSegmentCount) {
			// no more path left to match this element
			return false;
		}
		PathSegment pathSegment = matchingContext.pathSegments.get(segmentIndex);
		if (len != pathSegment.valueDecoded().length()) {
			// Not enough data to match this path element
			return false;
		}

		// TODO is char comparison here faster than String operations on pathSegment.valueDecoded() ?
		char[] data = pathSegment.valueDecoded().toCharArray();
		if (this.caseSensitive) {
			for (int i = 0; i < len; i++) {
				if (data[i] != this.text[i]) {
					return false;
				}
			}
		}
		else {
			for (int i = 0; i < len; i++) {
				// TODO revisit performance if doing a lot of case insensitive matching
				if (Character.toLowerCase(data[i]) != this.text[i]) {
					return false;
				}
			}
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
				return true;  // no more data but everything matched so far
			}
			return this.next.matches(segmentIndex + 1, matchingContext);
		}
	}

	@Override
	public int getNormalizedLength() {
		return len;
	}

	public String toString() {
		return "Literal(" + String.valueOf(this.text) + ")";
	}

	@Override
	public char[] getText() {
		return this.text;
	}

}
