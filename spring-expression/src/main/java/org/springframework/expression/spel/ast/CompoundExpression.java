/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.CodeFlow;

/**
 * Represents a DOT separated expression sequence, such as 'property1.property2.methodOne()'
 *
 * @author Andy Clement
 * @since 3.0
 */
public class CompoundExpression extends SpelNodeImpl {

	public CompoundExpression(int pos,SpelNodeImpl... expressionComponents) {
		super(pos,expressionComponents);
		if (expressionComponents.length<2) {
			throw new IllegalStateException("Dont build compound expression less than one entry: "+expressionComponents.length);
		}
	}


	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		if (getChildCount() == 1) {
			return this.children[0].getValueRef(state);
		}
		TypedValue result = null;
		SpelNodeImpl nextNode = null;
		try {
			nextNode = this.children[0];
			result = nextNode.getValueInternal(state);
			int cc = getChildCount();
			for (int i = 1; i < cc - 1; i++) {
				try {
					state.pushActiveContextObject(result);
					nextNode = this.children[i];
					result = nextNode.getValueInternal(state);
				}
				finally {
					state.popActiveContextObject();
				}
			}
			try {
				state.pushActiveContextObject(result);
				nextNode = this.children[cc-1];
				ValueRef valuerefResult = nextNode.getValueRef(state);
				return valuerefResult;
			}
			finally {
				state.popActiveContextObject();
			}
		}
		catch (SpelEvaluationException ee) {
			// Correct the position for the error before re-throwing
			ee.setPosition(nextNode.getStartPosition());
			throw ee;
		}
	}

	/**
	 * Evaluates a compound expression. This involves evaluating each piece in turn and the return value from each piece
	 * is the active context object for the subsequent piece.
	 * @param state the state in which the expression is being evaluated
	 * @return the final value from the last piece of the compound expression
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		ValueRef ref = getValueRef(state);
		TypedValue result = ref.getValue();
		this.exitTypeDescriptor = this.children[this.children.length-1].getExitDescriptor();
//		if (this.exitTypeDescriptor ==  null) {
//			throw new IllegalStateException("Have not computed value exit descriptor for this node "+this.children[this.children.length-1]);
//		}
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws EvaluationException {
		getValueRef(state).setValue(value);
	}

	@Override
	public boolean isWritable(ExpressionState state) throws EvaluationException {
		return getValueRef(state).isWritable();
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(".");
			}
			sb.append(getChild(i).toStringAST());
		}
		return sb.toString();
	}
	
	@Override
	public boolean isCompilable() {
		for (SpelNodeImpl child: children) {
			if (!child.isCompilable()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void generateCode(MethodVisitor mv,CodeFlow codeflow) {		
		for (int i=0;i<children.length;i++) {
			SpelNodeImpl child = children[i];
			if (child instanceof TypeReference && 
				(i+1)<children.length && 
				children[i+1] instanceof MethodReference) {
				continue;
			}
			child.generateCode(mv, codeflow);
		}
//		if one is a type ref then a state method
//		for (SpelNodeImpl child: children) {
//			child.generateCode(mv, codeflow);
//		}
		codeflow.pushDescriptor(this.getExitDescriptor());
	}

}
