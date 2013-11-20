/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel;

/**
 * Captures the possible configuration settings for a compiler that can be
 * used when evaluating expressions.
 * 
 * @author Andy Clement
 */
public enum SpelCompilerMode {
	// The compiler is switched off, this is the default.
	off,
	
	// Expressions are compiled as soon as possible (usually after 1 interpreted run).
	// If a compiled expression fails it will throw an exception.
	immediate,
	
	// Expression evaluate switches between interpreted and compiled over time.
	// After a number of runs the expression gets compiled. If it later fails that
	// will be caught internally and we'll switch back to the interpreted version
	// and compile it again later.
	mixed 
}