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
 * A literal path element that does includes the single character wildcard '?' one
 * or more times (to basically many any character at that position).
 *
 * @author Andy Clement
 * @since 5.0
 */
class SingleCharWildcardedPathElement extends PathElement {

	private final char[] text;

	private final int len;

	private final int questionMarkCount;

	private final boolean caseSensitive;


	public SingleCharWildcardedPathElement(
			int pos, char[] literalText, int questionMarkCount, boolean caseSensitive, char separator) {

		super(pos, separator);
		this.len = literalText.length;
		this.questionMarkCount = questionMarkCount;
		this.caseSensitive = caseSensitive;
		if (caseSensitive) {
			this.text = literalText;
		}
		else {
			this.text = new char[literalText.length];
			for (int i = 0; i < len; i++) {
				this.text[i] = Character.toLowerCase(literalText[i]);
			}
		}
	}


	@Override
	public boolean matches(int segmentIndex, MatchingContext matchingContext) {
		if (segmentIndex >= matchingContext.pathSegmentCount) {
			return false; // there are no more path segments to match this PathElement
		}
		PathSegment pathSegment = matchingContext.pathSegments.get(segmentIndex);
		if (len != pathSegment.valueDecoded().length()) {
			return false; // the next path segment doesn't have enough data to match this PathElement
		}
		char[] data = pathSegment.valueDecoded().toCharArray();
		if (this.caseSensitive) {
			for (int i = 0; i <this.len; i++) {
				char t = this.text[i];
				if ((t != '?') && (data[i] != t)) {
					return false;
				}
			}
		}
		else {
			for (int i = 0; i < this.len; i++) {
				char t = this.text[i];
				if ((t != '?') && Character.toLowerCase(data[i]) != t) {
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
				return true;  // no more data but matches up to this point
			}
			return this.next.matches(segmentIndex + 1, matchingContext);
		}
	}

	@Override
	public int getWildcardCount() {
		return this.questionMarkCount;
	}

	@Override
	public int getNormalizedLength() {
		return len;
	}

	public String toString() {
		return "SingleCharWildcarded(" + String.valueOf(this.text) + ")";
	}

	@Override
	public char[] getText() {
		return this.text;
	}

}
