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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ast.SpelNodeImpl;

/**
 * 
 * @author Andy Clement
 */
public abstract class CompiledExpression extends SpelNodeImpl {


	protected static TypeDescriptor tdString = TypeDescriptor.valueOf(String.class);
	protected static TypeDescriptor tdInteger = TypeDescriptor.valueOf(Integer.class);
	protected static TypeDescriptor tdIntType = TypeDescriptor.valueOf(Integer.TYPE);
	protected static TypeDescriptor tdBooleanType = TypeDescriptor.valueOf(Boolean.TYPE);
	protected static TypeDescriptor tdCharacter = TypeDescriptor.valueOf(Character.class);
	protected static TypeDescriptor tdObject = TypeDescriptor.valueOf(Object.class);
	
	public CompiledExpression() {
		super(1, null);
	}
	
	public abstract Object getValue(Object target) throws EvaluationException;

	public abstract TypedValue getValueInternal(ExpressionState expressionState)
			throws EvaluationException ;
//	{
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}

	public String toStringAST() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

//	public abstract TypedValue getValueInternal(ExpressionState expressionState) throws EvaluationException;
	
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getValue(org.springframework.expression.spel.ExpressionState)
//	 */
//	@Override
//	public Object getValue(ExpressionState expressionState) throws EvaluationException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getTypedValue(org.springframework.expression.spel.ExpressionState)
//	 */
//	@Override
//	public TypedValue getTypedValue(ExpressionState expressionState)
//			throws EvaluationException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#isWritable(org.springframework.expression.spel.ExpressionState)
//	 */
//	@Override
//	public boolean isWritable(ExpressionState expressionState) throws EvaluationException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#setValue(org.springframework.expression.spel.ExpressionState, java.lang.Object)
//	 */
//	@Override
//	public void setValue(ExpressionState expressionState, Object newValue)
//			throws EvaluationException {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#toStringAST()
//	 */
//	@Override
//	public String toStringAST() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getChildCount()
//	 */
//	@Override
//	public int getChildCount() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getChild(int)
//	 */
//	@Override
//	public SpelNode getChild(int index) {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getObjectClass(java.lang.Object)
//	 */
//	@Override
//	public Class<?> getObjectClass(Object obj) {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getStartPosition()
//	 */
//	@Override
//	public int getStartPosition() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}
//
//	/* (non-Javadoc)
//	 * @see org.springframework.expression.spel.SpelNode#getEndPosition()
//	 */
//	@Override
//	public int getEndPosition() {
//		// TODO Auto-generated method stub
//		throw new UnsupportedOperationException("Auto-generated method stub");
//	}

}
