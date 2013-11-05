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
import org.springframework.expression.spel.standard.CodeFlow;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.Utils;

/**
 * Common supertype for operators that operate on either one or two operands. In the case
 * of multiply or divide there would be two operands, but for unary plus or minus, there
 * is only one.
 *
 * @author Andy Clement
 * @since 3.0
 */
public abstract class Operator extends SpelNodeImpl {

	String operatorName;


	public Operator(String payload,int pos,SpelNodeImpl... operands) {
		super(pos, operands);
		this.operatorName = payload;
	}


	public SpelNodeImpl getLeftOperand() {
		return this.children[0];
	}

	public SpelNodeImpl getRightOperand() {
		return this.children[1];
	}

	public final String getOperatorName() {
		return this.operatorName;
	}

	/**
	 * String format for all operators is the same '(' [operand] [operator] [operand] ')'
	 */
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(getChild(0).toStringAST());
		for (int i = 1; i < getChildCount(); i++) {
			sb.append(" ").append(getOperatorName()).append(" ");
			sb.append(getChild(i).toStringAST());
		}
		sb.append(")");
		return sb.toString();
	}

	protected boolean isCompilableOperatorUsingNumerics() {
		SpelNodeImpl left = getLeftOperand();
		SpelNodeImpl right= getRightOperand();
		if (!left.isCompilable() || !right.isCompilable()) {
			return false;
		}
		// Supported operand types for equals (at the moment)
		String leftDesc = left.getExitDescriptor();
		String rightDesc= right.getExitDescriptor();
		if (SpelCompiler.isPrimitiveOrUnboxableSupportedNumber(leftDesc) && SpelCompiler.isPrimitiveOrUnboxableSupportedNumber(rightDesc)) {
			if (SpelCompiler.boxingCompatible(leftDesc, rightDesc)) {
				return true;
			}
		}
		return false;
	}


	/** 
	 * Numeric comparison operators share very similar generated code, only differing in 
	 * two comparison instructions.
	 */
	protected void generateComparisonCode(MethodVisitor mv, CodeFlow codeflow, int compareInstruction1,
			int compareInstruction2) {
		String leftDesc = getLeftOperand().getExitDescriptor();
		String rightDesc = getRightOperand().getExitDescriptor();
		
		boolean unboxLeft = !SpelCompiler.isPrimitive(leftDesc);
		boolean unboxRight = !SpelCompiler.isPrimitive(rightDesc);
		char targetType = SpelCompiler.toPrimitiveTargetDesc(leftDesc);
		
		getLeftOperand().generateCode(mv, codeflow);
		if (unboxLeft) {
			Utils.insertUnboxInsns(mv, targetType, false);
		}
	
		getRightOperand().generateCode(mv, codeflow);
		if (unboxRight) {
			Utils.insertUnboxInsns(mv, targetType, false);
		}
		// assert: SpelCompiler.boxingCompatible(leftDesc, rightDesc)
		Label elseTarget = new Label();
		Label endOfIf = new Label();
		if (targetType=='D') {
			mv.visitInsn(DCMPG);
			mv.visitJumpInsn(compareInstruction1, elseTarget);
		}
		else if (targetType=='F') {
			mv.visitInsn(FCMPG);		
			mv.visitJumpInsn(compareInstruction1, elseTarget);
		}
		else if (targetType=='J') {
			mv.visitInsn(LCMP);		
			mv.visitJumpInsn(compareInstruction1, elseTarget);
		}
		else if (targetType=='I') {
			mv.visitJumpInsn(compareInstruction2, elseTarget);		
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
