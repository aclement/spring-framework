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

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.CodeFlow;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.Utils;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Implements equality operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpEQ extends Operator {

	public OpEQ(int pos, SpelNodeImpl... operands) {
		super("==", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state)
			throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (left instanceof Number && right instanceof Number) {
			Number op1 = (Number) left;
			Number op2 = (Number) right;
			if (op1 instanceof Double || op2 instanceof Double) {
				return BooleanTypedValue.forValue(op1.doubleValue() == op2.doubleValue());
			}
			else if (op1 instanceof Float || op2 instanceof Float) {
				return BooleanTypedValue.forValue(op1.floatValue() == op2.floatValue());
			}
			else if (op1 instanceof Long || op2 instanceof Long) {
				return BooleanTypedValue.forValue(op1.longValue() == op2.longValue());
			}
			else {
				return BooleanTypedValue.forValue(op1.intValue() == op2.intValue());
			}
		}
		if (left != null && (left instanceof Comparable)) {
			return BooleanTypedValue.forValue(state.getTypeComparator().compare(left,
					right) == 0);
		}
		else {
			return BooleanTypedValue.forValue(left == right);
		}
	}
	

	// This check is different to the one in the other numeric operators (OpLt/etc)
	// because we allow basic object comparison if 
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right= getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}
		// Supported operand types for equals (at the moment)
		String leftDesc = left.getExitDescriptor();
		String rightDesc= right.getExitDescriptor();
		if ((SpelCompiler.isPrimitiveOrUnboxableSupportedNumberOrBoolean(leftDesc) || SpelCompiler.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rightDesc)) && !SpelCompiler.boxingCompatible(leftDesc,rightDesc)) {
			return false;
		} 
		return true;
	}
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		String leftDesc = getLeftOperand().getExitDescriptor();
		String rightDesc = getRightOperand().getExitDescriptor();
		boolean unboxing = SpelCompiler.isPrimitive(leftDesc) || SpelCompiler.isPrimitive(rightDesc);
		
		getLeftOperand().generateCode(mv, codeflow);
		if (unboxing && !SpelCompiler.isPrimitive(leftDesc)) {
			Utils.insertUnboxInsns(mv, rightDesc.charAt(0), false);
		}
		getRightOperand().generateCode(mv, codeflow);
		if (unboxing && !SpelCompiler.isPrimitive(rightDesc)) {
			Utils.insertUnboxInsns(mv, leftDesc.charAt(0), false);
		}
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		if ((SpelCompiler.isPrimitiveOrUnboxableSupportedNumberOrBoolean(leftDesc) || SpelCompiler.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rightDesc)) && SpelCompiler.boxingCompatible(leftDesc,rightDesc)) {
			if (leftDesc.equals("D") || rightDesc.equals("D")) {
				mv.visitInsn(DCMPL);		
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (leftDesc.equals("F") || rightDesc.equals("F")) {
				mv.visitInsn(FCMPL);		
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (leftDesc.equals("J") || rightDesc.equals("J")) {
				mv.visitInsn(LCMP);		
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (leftDesc.equals("I") || rightDesc.equals("I") ||
					 leftDesc.equals("Z") || rightDesc.equals("Z")) {
				mv.visitJumpInsn(IF_ICMPNE,elseTarget);		
			}
		}
		else {
			mv.visitJumpInsn(IF_ACMPNE,elseTarget);		
		}
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor("Z");
	}

}
