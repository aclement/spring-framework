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

import org.springframework.expression.EvaluationException;

/**
 * Base superclass for compiled expressions. Each generated compiled expression class will
 * extend this class and implement one of the getValue() methods.
 * 
 * @author Andy Clement
 */
public abstract class CompiledExpression {

//	protected static TypeDescriptor tdString = TypeDescriptor.valueOf(String.class);
//	protected static TypeDescriptor tdInteger = TypeDescriptor.valueOf(Integer.class);
//	protected static TypeDescriptor tdIntType = TypeDescriptor.valueOf(Integer.TYPE);
//	protected static TypeDescriptor tdBooleanType = TypeDescriptor.valueOf(Boolean.TYPE);
//	protected static TypeDescriptor tdCharacter = TypeDescriptor.valueOf(Character.class);
//	protected static TypeDescriptor tdObject = TypeDescriptor.valueOf(Object.class);
	
	public CompiledExpression() {
	}
	
	public Object getValue(Object target) throws EvaluationException {
		throw new IllegalStateException("should never be invoked");
	}

//	public TypedValue getValueInternal(ExpressionState expressionState)
//			throws EvaluationException {
//		throw new IllegalStateException("should never be invoked");
//	}
	
//	public Object getValue(ExpressionState expressionState) throws EvaluationException {
////		throw new IllegalStateException("should never be invoked");
//	}

}
