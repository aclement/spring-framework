/*
 * Copyright 2002-2014 the original author or authors.
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
 * Implements the equality operator.
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
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		return BooleanTypedValue.forValue(equalityCheck(state, left, right));
	}
	
	// This check is different to the one in the other numeric operators (OpLt/etc)
	// because we allow basic object comparison
	@Override
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
	
	@Override
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
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (targetType=='F') {
				mv.visitInsn(FCMPL);		
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (targetType=='J') {
				mv.visitInsn(LCMP);		
				mv.visitJumpInsn(IFNE, elseTarget);
			}
			else if (targetType=='I' || targetType=='Z') {
				mv.visitJumpInsn(IF_ICMPNE, elseTarget);		
			}
			else {
				throw new IllegalStateException("Unexpected descriptor "+leftDesc);
			}
		} else {
			getLeftOperand().generateCode(mv, codeflow);
			getRightOperand().generateCode(mv, codeflow);
			Label leftNotNull = new Label();
			mv.visitInsn(DUP_X1); // Dup right on the top of the stack
			mv.visitJumpInsn(IFNONNULL,leftNotNull);
				// Right is null!
				mv.visitInsn(SWAP);
				mv.visitInsn(POP); // remove it
				Label rightNotNull = new Label();
				mv.visitJumpInsn(IFNONNULL, rightNotNull);
					// Left is null too
					mv.visitInsn(ICONST_1);
				mv.visitJumpInsn(GOTO, endOfIf);
					mv.visitLabel(rightNotNull);
					mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO,endOfIf);
			
			
			mv.visitLabel(leftNotNull);
			mv.visitMethodInsn(INVOKEVIRTUAL,"java/lang/Object","equals","(Ljava/lang/Object;)Z",false);
			mv.visitLabel(endOfIf);
			codeflow.pushDescriptor("Z");
			return;
			// box?
			// box?
			// TODO [spelcompiler] calls equals but also allow for null
			// return left==null?right==null:left.equals(right);
//	        7: ifnonnull     22
//	        10: aload_2
//	        11: ifnonnull     18
//	        14: iconst_1
//	        15: goto          27
//	        18: iconst_0
//	        19: goto          27
//	        22: aload_1
//	        23: aload_2
//	        24: invokevirtual #9                  // Method java/lang/Object.equals:(Ljava/lang/Object;)Z
//	        27: istore_3
//	        28: iload_3
//			java/lang/String.equals:(Ljava/lang/Object;)Z
			
			
			// mv.visitJumpInsn(IF_ACMPNE, elseTarget);
		}
		mv.visitInsn(ICONST_1);
		mv.visitJumpInsn(GOTO,endOfIf);
		mv.visitLabel(elseTarget);
		mv.visitInsn(ICONST_0);
		mv.visitLabel(endOfIf);
		codeflow.pushDescriptor("Z");
	}

}
