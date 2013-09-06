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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.CodeFlow;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Represents the boolean OR operation.
 *
 * @author Andy Clement
 * @author Mark Fisher
 * @author Oliver Becker
 * @since 3.0
 */
public class OpOr extends Operator {

	public OpOr(int pos, SpelNodeImpl... operands) {
		super("or", pos, operands);
		this.exitType = TypeDescriptor.valueOf(Boolean.TYPE);
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (getBooleanValue(state, getLeftOperand())) {
			// no need to evaluate right operand
			return BooleanTypedValue.TRUE;
		}
		return BooleanTypedValue.forValue(getBooleanValue(state, getRightOperand()));
	}

	private boolean getBooleanValue(ExpressionState state, SpelNodeImpl operand) {
		try {
			Boolean value = operand.getValue(state, Boolean.class);
			assertValueNotNull(value);
			return value;
		}
		catch (SpelEvaluationException ee) {
			ee.setPosition(operand.getStartPosition());
			throw ee;
		}
	}

	private void assertValueNotNull(Boolean value) {
		if (value == null) {
			throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, "null", "boolean");
		}
	}

	public boolean isCompilable() {
		return 
				AstUtils.isBooleanCompatible(getLeftOperand().getExitType()) &&
				AstUtils.isBooleanCompatible(getRightOperand().getExitType());
	}
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		// pseudo: if (leftOperandValue) { result=true; } else { result=rightOperandValue; }
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		getLeftOperand().generateCode(mv, codeflow);
		mv.visitJumpInsn(IFEQ, elseTarget);
		mv.visitLdcInsn(1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		getRightOperand().generateCode(mv, codeflow);
		mv.visitLabel(endOfIf);		
	}
	
}
