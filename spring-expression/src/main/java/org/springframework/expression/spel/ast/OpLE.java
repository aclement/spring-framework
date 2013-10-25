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
import org.springframework.util.Assert;

/**
 * Implements the less-than-or-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpLE extends Operator {

	public OpLE(int pos, SpelNodeImpl... operands) {
		super("<=", pos, operands);
		this.exitTypeDescriptor="Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state)
			throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return BooleanTypedValue.forValue(leftNumber.doubleValue() <= rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return BooleanTypedValue.forValue(leftNumber.floatValue() <= rightNumber.floatValue());
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return BooleanTypedValue.forValue(leftNumber.longValue() <= rightNumber.longValue());
			}
			else {
				return BooleanTypedValue.forValue(leftNumber.intValue() <= rightNumber.intValue());
			}
		}
		return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) <= 0);
	}
	

	public boolean isCompilable() {
		return isCompilableOperatorUsingNumerics();
	}
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		String leftDesc = getLeftOperand().getExitDescriptor();
		String rightDesc = getRightOperand().getExitDescriptor();

		boolean mayNeedToUnbox = SpelCompiler.isPrimitive(leftDesc) || SpelCompiler.isPrimitive(rightDesc);
		
		getLeftOperand().generateCode(mv, codeflow);
		if (mayNeedToUnbox && !SpelCompiler.isPrimitive(leftDesc)) {
			Utils.insertUnboxInsns(mv, rightDesc.charAt(0), false);
		}

		getRightOperand().generateCode(mv, codeflow);
		if (mayNeedToUnbox && !SpelCompiler.isPrimitive(rightDesc)) {
			Utils.insertUnboxInsns(mv, leftDesc.charAt(0), false);
		}
		// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		if (SpelCompiler.isDouble(leftDesc)) {
			mv.visitInsn(DCMPG);		
			mv.visitJumpInsn(IFGT, elseTarget);
		}
		else if (SpelCompiler.isFloat(leftDesc)) {
			mv.visitInsn(FCMPG);		
			mv.visitJumpInsn(IFGT, elseTarget);
		}
		else if (SpelCompiler.isLong(leftDesc)) {
			mv.visitInsn(LCMP);		
			mv.visitJumpInsn(IFGT, elseTarget);
		}
		else if (SpelCompiler.isInteger(leftDesc)) {
			mv.visitJumpInsn(IF_ICMPGT, elseTarget);		
		}
		else {
			throw new IllegalStateException("Unexpected descriptor "+leftDesc);
		}
		// Other numbers are not yet supported (isCompilable will not have returned true)
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor("Z");				
	}

}
