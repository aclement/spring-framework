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
import org.springframework.expression.spel.standard.Utils;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Implements the not-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpNE extends Operator {

	public OpNE(int pos, SpelNodeImpl... operands) {
		super("!=", pos, operands);
		this.exitTypeDescriptor = "Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {

		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();

		if (left instanceof Number && right instanceof Number) {
			Number op1 = (Number) left;
			Number op2 = (Number) right;

			if (op1 instanceof Double || op2 instanceof Double) {
				return BooleanTypedValue.forValue(op1.doubleValue() != op2.doubleValue());
			}

			if (op1 instanceof Float || op2 instanceof Float) {
				return BooleanTypedValue.forValue(op1.floatValue() != op2.floatValue());
			}

			if (op1 instanceof Long || op2 instanceof Long) {
				return BooleanTypedValue.forValue(op1.longValue() != op2.longValue());
			}

			return BooleanTypedValue.forValue(op1.intValue() != op2.intValue());
		}

		if (left != null && (left instanceof Comparable)) {
			return BooleanTypedValue.forValue(state.getTypeComparator().compare(left,
					right) != 0);
		}

		return BooleanTypedValue.forValue(left != right);
	}

	// This check is different to the one in the other numeric operators (OpLt/etc)
	// because we allow basic object comparison
	public boolean isCompilable() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right= getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}
		String leftdesc = left.getExitDescriptor();
		String rightdesc = right.getExitDescriptor();
		if ((CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(leftdesc) ||
				CodeFlow.isPrimitiveOrUnboxableSupportedNumber(rightdesc))) {
			if (!CodeFlow.boxingCompatible(leftdesc, rightdesc)) {
				return false;
			}
		}
		return true;
	}
	
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		String leftDesc = getLeftOperand().getExitDescriptor();
		String rightDesc = getRightOperand().getExitDescriptor();
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		boolean leftPrim = CodeFlow.isPrimitive(leftDesc);
		boolean rightPrim = CodeFlow.isPrimitive(rightDesc);

		if ((CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(leftDesc) || 
			 CodeFlow.isPrimitiveOrUnboxableSupportedNumberOrBoolean(rightDesc)) && 
			 CodeFlow.boxingCompatible(leftDesc,rightDesc)) {
			char targetType = CodeFlow.toPrimitiveTargetDesc(leftDesc);
			
			getLeftOperand().generateCode(mv, codeflow);
			if (!leftPrim) {
				Utils.insertUnboxInsns(mv, targetType, false);
			}
		
			getRightOperand().generateCode(mv, codeflow);
			if (!rightPrim) {
				Utils.insertUnboxInsns(mv, targetType, false);
			}
			// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
			if (targetType=='D') {
				mv.visitInsn(DCMPL);
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType=='F') {
				mv.visitInsn(FCMPL);		
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType=='J') {
				mv.visitInsn(LCMP);		
				mv.visitJumpInsn(IFEQ, elseTarget);
			}
			else if (targetType=='I' || targetType=='Z') {
				mv.visitJumpInsn(IF_ICMPEQ, elseTarget);		
			}
			else {
				throw new IllegalStateException("Unexpected descriptor "+leftDesc);
			}
		} else {
			getLeftOperand().generateCode(mv, codeflow);
			getRightOperand().generateCode(mv, codeflow);
			mv.visitJumpInsn(IF_ACMPEQ, elseTarget);
		}
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor("Z");
	}

}
