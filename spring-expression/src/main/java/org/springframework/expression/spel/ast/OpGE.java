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
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * Implements greater-than-or-equal operator.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OpGE extends Operator {

	public OpGE(int pos, SpelNodeImpl... operands) {
		super(">=", pos, operands);
		this.exitTypeDescriptor="Z";
	}


	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (left instanceof Number && right instanceof Number) {
			Number leftNumber = (Number) left;
			Number rightNumber = (Number) right;
			if (leftNumber instanceof Double || rightNumber instanceof Double) {
				return BooleanTypedValue.forValue(leftNumber.doubleValue() >= rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				return BooleanTypedValue.forValue(leftNumber.floatValue() >= rightNumber.floatValue());
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				return BooleanTypedValue.forValue(leftNumber.longValue() >= rightNumber.longValue());
			}
			else {
				return BooleanTypedValue.forValue(leftNumber.intValue() >= rightNumber.intValue());
			}
		}
		return BooleanTypedValue.forValue(state.getTypeComparator().compare(left, right) >= 0);
	}
	
	public boolean isCompilable() {
		String leftDesc = getLeftOperand().getExitDescriptor();
		String rightDesc = getRightOperand().getExitDescriptor();
		return SpelCompiler.isNumber(leftDesc) && SpelCompiler.isNumber(rightDesc) && leftDesc.equals(rightDesc);
	}
	
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		getLeftOperand().generateCode(mv, codeflow);
		String leftDesc = getLeftOperand().getExitDescriptor();
//		if (getLeftOperand().getExitDescriptor().equals("Ljava/lang/Double")) {
//			// unbox
//			Utils.insertUnboxInsns(mv, 'D', false);
//		}
		getRightOperand().generateCode(mv, codeflow);
		String rightDesc = getRightOperand().getExitDescriptor();
//		if (getLeftOperand().getExitDescriptor().equals("Ljava/lang/Double")) {
//			// unbox
//			Utils.insertUnboxInsns( mv, 'D', false);
//		}
		if (leftDesc.equals(rightDesc)) {
			if (leftDesc.equals("D")) {
				mv.visitInsn(DCMPL);		
				Label elseTarget = new Label();
				Label endOfIf = new Label();
				mv.visitJumpInsn(IFLT, elseTarget);
				mv.visitInsn(ICONST_1);
				mv.visitJumpInsn(GOTO,endOfIf);
				mv.visitLabel(elseTarget);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(endOfIf);
				codeflow.pushDescriptor("Z");				
			}
			else if (leftDesc.equals("F")) {
				mv.visitInsn(FCMPL);		
				Label elseTarget = new Label();
				Label endOfIf = new Label();
				mv.visitJumpInsn(IFLT, elseTarget);
				mv.visitInsn(ICONST_1);
				mv.visitJumpInsn(GOTO,endOfIf);
				mv.visitLabel(elseTarget);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(endOfIf);
				codeflow.pushDescriptor("Z");								
			}
			else if (leftDesc.equals("J")) {
				mv.visitInsn(LCMP);		
				Label elseTarget = new Label();
				Label endOfIf = new Label();
				mv.visitJumpInsn(IFLT, elseTarget);
				mv.visitInsn(ICONST_1);
				mv.visitJumpInsn(GOTO,endOfIf);
				mv.visitLabel(elseTarget);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(endOfIf);
				codeflow.pushDescriptor("Z");												
			}
			else if (leftDesc.equals("I")) {
				Label elseTarget = new Label();
				Label endOfIf = new Label();
				mv.visitJumpInsn(IF_ICMPLT,elseTarget);		
				mv.visitInsn(ICONST_1);
				mv.visitJumpInsn(GOTO,endOfIf);
				mv.visitLabel(elseTarget);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(endOfIf);
				codeflow.pushDescriptor("Z");												
			}
			else {
				throw new IllegalStateException("nyi "+leftDesc);
			}
		}
		else {
			throw new IllegalStateException("nyi "+leftDesc+" "+rightDesc);
		}
		
	}

}
