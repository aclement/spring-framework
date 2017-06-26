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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A path element representing capturing a piece of the path as a variable. In the pattern
 * '/foo/{bar}/goo' the {bar} is represented as a {@link CaptureVariablePathElement}. There
 * must be at least one character to bind to the variable.
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureVariablePathElement extends PathElement {

	private final String variableName;

	private Pattern constraintPattern;

	/**
	 * @param pos the position in the pattern of this capture element
	 * @param captureDescriptor is of the form {AAAAA[:pattern]}
	 */
	CaptureVariablePathElement(int pos, char[] captureDescriptor, boolean caseSensitive, char separator) {
		super(pos, separator);
		int colon = -1;
		for (int i = 0; i < captureDescriptor.length; i++) {
			if (captureDescriptor[i] == ':') {
				colon = i;
				break;
			}
		}
		if (colon == -1) {
			// no constraint
			this.variableName = new String(captureDescriptor, 1, captureDescriptor.length - 2);
		}
		else {
			this.variableName = new String(captureDescriptor, 1, colon - 1);
			if (caseSensitive) {
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2));
			}
			else {
				this.constraintPattern = Pattern.compile(
						new String(captureDescriptor, colon + 1, captureDescriptor.length - colon - 2),
						Pattern.CASE_INSENSITIVE);
			}
		}
	}


	@Override
	public boolean matches(int segmentIndex, PathPattern.MatchingContext matchingContext) {
		String segment = matchingContext.getSegmentValue(segmentIndex);

		// There must be at least one character to capture:
		if (segment == null || segment.length() == 0) {
			return false;
		}

		if (this.constraintPattern != null) {
			// TODO possible optimization - only regex match if rest of pattern matches? Benefit likely to vary pattern to pattern
			Matcher matcher = constraintPattern.matcher(segment);
			if (matcher.groupCount() != 0) {
				throw new IllegalArgumentException(
						"No capture groups allowed in the constraint regex: " + this.constraintPattern.pattern());
			}
			if (!matcher.matches()) {
				return false;
			}
		}

		boolean match = false;
		if (isNoMorePattern()) {
			if (matchingContext.determineRemainingPath) {
				matchingContext.remainingPathIndex = segmentIndex + 1;
				match = true;
			}
			else {
				match = verifyEndOfPath(segmentIndex + 1, matchingContext);
			}
		}
		else {
			if (matchingContext.isMatchStartMatching && noMoreData(segmentIndex + 1, matchingContext)) {
				match = true;
			}
			else {
				match = this.next.matches(segmentIndex + 1, matchingContext);
			}
		}

		if (match && matchingContext.extractingVariables) {
			matchingContext.set(this.variableName, segment, matchingContext.pathSegments.get(segmentIndex).parameters());
		}
		return match;
	}

	public String getVariableName() {
		return this.variableName;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public int getWildcardCount() {
		return 0;
	}

	@Override
	public int getCaptureCount() {
		return 1;
	}

	@Override
	public int getScore() {
		return CAPTURE_VARIABLE_WEIGHT;
	}


	public String toString() {
		return "CaptureVariable({" + this.variableName +
				(this.constraintPattern != null ? ":" + this.constraintPattern.pattern() : "") + "})";
	}
	
	public char[] getText() {
		StringBuilder buf = new StringBuilder();
		buf.append("{").append(this.variableName);
		if (this.constraintPattern != null) {
			buf.append(":").append(this.constraintPattern.pattern());
		}
		buf.append("}");
		return buf.toString().toCharArray();
	}

}
