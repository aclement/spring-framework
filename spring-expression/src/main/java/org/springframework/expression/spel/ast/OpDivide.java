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
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.CodeFlow;

/**
 * Implements division operator.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class OpDivide extends Operator {

	public OpDivide(int pos, SpelNodeImpl... operands) {
		super("/", pos, operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object operandOne = getLeftOperand().getValueInternal(state).getValue();
		Object operandTwo = getRightOperand().getValueInternal(state).getValue();
		if (operandOne instanceof Number && operandTwo instanceof Number) {
			Number op1 = (Number) operandOne;
			Number op2 = (Number) operandTwo;
			if (op1 instanceof Double || op2 instanceof Double) {
				if (op1 instanceof Double && op1 instanceof Double) {
					// TODO asc need to do this change on opplus too?
					this.exitTypeDescriptor = "D";
				}
				return new TypedValue(op1.doubleValue() / op2.doubleValue());
			}
			else if (op1 instanceof Float || op2 instanceof Float) {
				if (op1 instanceof Float && op1 instanceof Float) {
					// TODO asc need to do this change on opplus too?
					this.exitTypeDescriptor = "F";
				}
				return new TypedValue(op1.floatValue() / op2.floatValue());
			}
			else if (op1 instanceof Long || op2 instanceof Long) {
				if (op1 instanceof Long && op1 instanceof Long) {
					// TODO asc need to do this change on opplus too?
					this.exitTypeDescriptor = "J";
				}
				return new TypedValue(op1.longValue() / op2.longValue());
			}
			else {
				this.exitTypeDescriptor = "I"; // TODO asc conditional on both being ints?
				// TODO what about non-int result of the division?
				return new TypedValue(op1.intValue() / op2.intValue());
			}
		}
		return state.operate(Operation.DIVIDE, operandOne, operandTwo);
	}
	
	@Override
	public boolean isCompilable() {
		// TODO asc check children are compilable
		return this.exitTypeDescriptor!=null;
	}
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		getLeftOperand().generateCode(mv, codeflow);
		getLeftOperand().getExitDescriptor();	
		if (this.children.length>1) {
			getRightOperand().generateCode(mv, codeflow);
			getRightOperand().getExitDescriptor();
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(IDIV);
					break;
				case 'J':
					mv.visitInsn(LDIV);
					break;
				case 'F': 
					mv.visitInsn(FDIV);
					break;
				case 'D':
					mv.visitInsn(DDIV);
					break;				
				default:
					throw new IllegalStateException();			
			}
		}
		codeflow.pushDescriptor(this.exitTypeDescriptor);
	}

}
