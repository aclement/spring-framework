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
package org.springframework.web.util.patterns;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for URI template patterns. It breaks the path pattern into a number of
 * path elements in a linked list.
 * 
 * @author Andy Clement
 */
public class PathPatternParser {

	public final static char DEFAULT_SEPARATOR = '/';

	// The expected path separator to split path elements during parsing
	char separator = DEFAULT_SEPARATOR;

	// Is the parser producing case sensitive PathPattern matchers
	boolean caseSensitive = true;

	// The input data for parsing
	private char[] pathPatternData;

	// The length of the input data
	private int pathPatternLength;

	// Current parsing position
	int pos;

	// How many ? characters in a particular path element
	private int singleCharWildcardCount;

	// Is the path pattern using * characters in a particular path element
	private boolean wildcard = false;

	// Is the construct {*...} being used in a particular path element
	private boolean isCaptureTheRestVariable = false;

	// Has the parser entered a {...} variable capture block in a particular
	// path element
	private boolean insideVariableCapture = false;

	// How many variable captures are occuring in a particular path element
	private int variableCaptureCount = 0;

	// Start of the most recent path element in a particular path element
	int pathElementStart;

	// Start of the most recent variable capture in a particular path element
	int variableCaptureStart;

	// Variables captures in this path pattern
	List<String> capturedVariableNames;

	// The head of the path element chain currently being built
	PathElement headPE;

	// The most recently constructed path element in the chain
	PathElement currentPE;

	/**
	 * Default constructor, will use the default path separator to identify
	 * the elements of the path pattern.
	 */
	public PathPatternParser() {
	}

	/**
	 * Create a PatternParser that will use the specified separator instead of
	 * the default.
	 * 
	 * @param separator the path separator to look for when parsing.
	 */
	public PathPatternParser(char separator) {
		this.separator = separator;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Process the path pattern data, a character at a time, breaking it into
	 * path elements around separator boundaries and verifying the structure at each
	 * stage. Produces a PathPattern object that can be used for fast matching
	 * against paths.
	 * 
	 * @param pathPattern the input path pattern, e.g. /foo/{bar}
	 * @return a PathPattern for quickly matching paths against the specified path pattern
	 */
	public PathPattern parse(String pathPattern) {
		if (pathPattern == null) {
			pathPattern = "";
		}
		pathPatternData = pathPattern.toCharArray();
		pathPatternLength = pathPatternData.length;
		headPE = null;
		currentPE = null;
		capturedVariableNames = null;
		pathElementStart = -1;
		pos = 0;
		resetPathElementState();
		while (pos < pathPatternLength) {
			char ch = pathPatternData[pos];
			if (ch == separator) {
				if (pathElementStart != -1) {
					pushPathElement(createPathElement());
				}
				if (peekDoubleWildcard()) {
					// Warning message about /** no longer being treated as a multi section matcher
				}
				pushPathElement(new SeparatorPathElement(pos, separator));
			} else {
				if (pathElementStart == -1) {
					pathElementStart = pos;
				}
				if (ch == '?') {
					singleCharWildcardCount++;
				} else if (ch == '{') {
					if (insideVariableCapture) {
						throw new PatternParseException(pos, pathPatternData, PatternMessage.ILLEGAL_NESTED_CAPTURE);
					} else if (pos > 0 && pathPatternData[pos - 1] == '}') {
						throw new PatternParseException(pos, pathPatternData,
								PatternMessage.CANNOT_HAVE_ADJACENT_CAPTURES);
					}
					insideVariableCapture = true;
					variableCaptureStart = pos;
				} else if (ch == '}') {
					if (!insideVariableCapture) {
						throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_OPEN_CAPTURE);
					}
					insideVariableCapture = false;
					if (isCaptureTheRestVariable && (pos + 1) < pathPatternLength) {
						throw new PatternParseException(pos + 1, pathPatternData,
								PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
					}
					variableCaptureCount++;
				} else if (ch == ':') {
					if (insideVariableCapture) {
						skipCaptureRegex();
						insideVariableCapture = false;
						variableCaptureCount++;
					}
				} else if (ch == '*') {
					if (insideVariableCapture) {
						if (variableCaptureStart == pos - 1) {
							isCaptureTheRestVariable = true;
						}
					}
					wildcard = true;
				}
				// Check that the characters used for captured variable names are like java identifiers
				if (insideVariableCapture) {
					if ((variableCaptureStart + 1 + (isCaptureTheRestVariable ? 1 : 0)) == pos
							&& !Character.isJavaIdentifierStart(ch)) {
						throw new PatternParseException(pos, pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR,
								Character.toString(ch));

					} else if ((pos > (variableCaptureStart + 1 + (isCaptureTheRestVariable ? 1 : 0))
							&& !Character.isJavaIdentifierPart(ch))) {
						throw new PatternParseException(pos, pathPatternData,
								PatternMessage.ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR, Character.toString(ch));
					}
				}
			}
			pos++;
		}
		if (pathElementStart != -1) {
			pushPathElement(createPathElement());
		}
		return new PathPattern(pathPattern, headPE, separator, caseSensitive);
	}

	/**
	 * Just hit a ':' and want to jump over the regex specification for this
	 * variable. pos will be pointing at the ':', we want to skip until the }.
	 * Need to handle nesting of } in the regex and escaped variants of 
	 * brackets.
	 */
	private void skipCaptureRegex() {
		pos++;
		int regexStart = pos;
		int squareBracketDepth = 0; // how deep in [...]
		int curlyBracketDepth = 0; // how deep in further {...}
		while (pos < pathPatternLength) {
			char ch = pathPatternData[pos];
			if (ch == '[' && pathPatternData[pos - 1] != '\\') {
				squareBracketDepth++;
			} else if (ch == ']' && pathPatternData[pos - 1] != '\\') {
				squareBracketDepth--;
			}
			if (ch == '{' && pathPatternData[pos - 1] != '\\' && squareBracketDepth == 0) {
				curlyBracketDepth++;
			} else if (ch == '}' && pathPatternData[pos - 1] != '\\' && squareBracketDepth == 0) {
				if (ch == '}' && squareBracketDepth == 0 && curlyBracketDepth == 0) {
					if (regexStart == pos) {
						throw new PatternParseException(regexStart, pathPatternData,
								PatternMessage.MISSING_REGEX_CONSTRAINT);
					}
					return;
				}
				curlyBracketDepth--;
			}
			if (ch == separator && squareBracketDepth == 0) {
				throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
			}
			pos++;
		}
		throw new PatternParseException(pos - 1, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
	}

	/**
	 * After processing a separator, a quick peek whether it is followed by **
	 * (and only ** before the end of the pattern or the next separator)
	 */
	private boolean peekDoubleWildcard() {
		if ((pos + 2) >= pathPatternLength) {
			return false;
		}
		if (pathPatternData[pos + 1] != '*' || pathPatternData[pos + 2] != '*') {
			return false;
		}
		return (pos + 3 == pathPatternLength || pathPatternData[pos + 3] == separator);
	}

	/**
	 * @param newPathElement the new path element to add to the chain being built
	 */
	private void pushPathElement(PathElement newPathElement) {
		if (currentPE instanceof CaptureTheRestPathElement) {
			throw new PatternParseException(newPathElement.pos, pathPatternData,
					PatternMessage.NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST);
		}
		if (headPE == null) {
			headPE = newPathElement;
			currentPE = newPathElement;
		} else {
			currentPE.next = newPathElement;
			currentPE = currentPE.next;
		}
		resetPathElementState();
	}

	/**
	 * Used the knowledge built up whilst processing since the last path element to determine what kind of path
	 * element to create.
	 * @return the new path element
	 */
	private PathElement createPathElement() {
		if (insideVariableCapture) {
			throw new PatternParseException(pos, pathPatternData, PatternMessage.MISSING_CLOSE_CAPTURE);
		}
		char[] pathElementText = new char[pos - pathElementStart];
		System.arraycopy(pathPatternData, pathElementStart, pathElementText, 0, pos - pathElementStart);
		PathElement newPE = null;
		if (variableCaptureCount > 0) {
			if (variableCaptureCount == 1 && pathElementStart == variableCaptureStart && pathPatternData[pos - 1] == '}') {
				if (isCaptureTheRestVariable) {
					// It is {*....} 
					newPE = new CaptureTheRestPathElement(pathElementStart, pathElementText);
				} else {
					// It is a full capture of this element (possibly with constraint), for example: /foo/{abc}/
					newPE = new CaptureVariablePathElement(pathElementStart, pathElementText, caseSensitive);
					recordCapturedVariable(pathElementStart, ((CaptureVariablePathElement) newPE).getVariableName());
				}
			} else {
				if (isCaptureTheRestVariable) {
					throw new PatternParseException(pathElementStart, pathPatternData,
							PatternMessage.BADLY_FORMED_CAPTURE_THE_REST);
				}
				RegexPathElement newRegexSection = new RegexPathElement(pathElementStart, pathElementText, caseSensitive);
				for (String variableName : newRegexSection.getVariableNames()) {
					recordCapturedVariable(pathElementStart, variableName);
				}
				newPE = newRegexSection;
			}
		} else {
			if (wildcard) {
				if (pos - 1 == pathElementStart) {
					newPE = new WildcardPathElement(pathElementStart);
				} else {
					newPE = new RegexPathElement(pathElementStart, pathElementText, caseSensitive);
				}
			} else if (singleCharWildcardCount!=0) {
				newPE = new SingleCharWildcardedPathElement(pathElementStart, pathElementText, singleCharWildcardCount, caseSensitive);
			} else {
				newPE = new LiteralPathElement(pathElementStart, pathElementText, caseSensitive);
			}
		}
		return newPE;
	}

	/**
	 * Reset all the flags and position markers computed during path element processing.
	 */
	private void resetPathElementState() {
		pathElementStart = -1;
		singleCharWildcardCount = 0;
		insideVariableCapture = false;
		variableCaptureCount = 0;
		wildcard = false;
		isCaptureTheRestVariable = false;
		variableCaptureStart = -1;
	}

	/**
	 * Record a new captured variable. If it clashes with an existing one then report an error.
	 */
	private void recordCapturedVariable(int pos, String variableName) {
		if (capturedVariableNames == null) {
			capturedVariableNames = new ArrayList<>();
		}
		if (capturedVariableNames.contains(variableName)) {
			throw new PatternParseException(pos, this.pathPatternData, PatternMessage.ILLEGAL_DOUBLE_CAPTURE, variableName);
		}
		capturedVariableNames.add(variableName);
	}
}