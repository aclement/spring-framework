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

import java.util.List;

import org.springframework.http.server.reactive.PathSegment;
import org.springframework.http.server.reactive.PathSegmentContainer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A path element representing capturing the rest of a path. In the pattern
 * '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureTheRestPathElement}.
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureTheRestPathElement extends PathElement {

	public final static MultiValueMap<String,String> NO_PARAMETERS = new LinkedMultiValueMap<>();
	
	public final static char SEPARATOR = '/';
	
	private final String variableName;


	/**
	 * @param pos position of the path element within the path pattern text
	 * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
	 * @param separator the separator used in the path pattern
	 */
	CaptureTheRestPathElement(int pos, char[] captureDescriptor, char separator) {
		super(pos, separator);
		this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
	}

	@Override
	public boolean matches(int segmentIndex, MatchingContext matchingContext) {
		// No need to handle 'match start' checking as this captures everything
		// anyway and cannot be followed by anything else
		// assert next == null
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathSegmentCount;
		}
		if (matchingContext.extractingVariables) {
			// Collect the parameters from all the remaining segments
			MultiValueMap<String,String> parametersCollector = null;
			for (int i = segmentIndex; i < matchingContext.pathSegmentCount; i++) {
				PathSegment pathSegment = matchingContext.pathSegments.get(i);
				MultiValueMap<String, String> parameters = pathSegment.parameters();
				if (parameters != null && parameters.size()!=0) {
					if (parametersCollector == null) {
						parametersCollector = new LinkedMultiValueMap<>();
					}
					parametersCollector.addAll(parameters);
				}
			}
			matchingContext.set(variableName, pathToString(segmentIndex, matchingContext.candidate),
					parametersCollector == null?NO_PARAMETERS:parametersCollector);
		}
		return true;
	}
	
	private String pathToString(int fromSegment, PathSegmentContainer pathSegmentContainer) {
		StringBuilder buf = new StringBuilder();
		List<PathSegment> pathSegments = pathSegmentContainer.pathSegments();
		for (int i = fromSegment, max = pathSegments.size(); i < max; i++) {
			if (i == 0) {
				if (pathSegmentContainer.isAbsolute()) {
					buf.append(SEPARATOR);
				}
			}
			else {
				buf.append(SEPARATOR);
			}
			buf.append(pathSegments.get(i).valueDecoded());
		}
		if (pathSegmentContainer.hasTrailingSlash()) {
			buf.append(SEPARATOR);
		}
		return buf.toString();
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

	public String toString() {
		return "CaptureTheRest("+this.separator+"{*" + this.variableName + "})";
	}

	@Override
	public char[] getText() {
		StringBuilder buf = new StringBuilder();
		buf.append(separator).append("{*").append(this.variableName).append("}");
		return buf.toString().toCharArray();
	}

}
