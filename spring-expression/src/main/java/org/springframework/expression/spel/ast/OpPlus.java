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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.standard.CodeFlow;
import org.springframework.expression.spel.standard.Utils;
import org.springframework.util.Assert;

/**
 * The plus operator will:
 * <ul>
 * <li>add doubles (floats are represented as doubles)
 * <li>add longs
 * <li>add integers
 * <li>concatenate strings
 * </ul>
 * It can be used as a unary operator for numbers (double/long/int). The standard
 * promotions are performed when the operand types vary (double+int=double). For other
 * options it defers to the registered overloader.
 *
 * @author Andy Clement
 * @author Ivo Smid
 * @since 3.0
 */
public class OpPlus extends Operator {

	public OpPlus(int pos, SpelNodeImpl... operands) {
		super("+", pos, operands);
		Assert.notEmpty(operands);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();
		SpelNodeImpl rightOp = getRightOperand();

		if (rightOp == null) { // If only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				if (operandOne instanceof Double || operandOne instanceof Long) {
					this.exitTypeDescriptor = (operandOne instanceof Double)?"D":"J";
					return new TypedValue(operandOne);
				}
				if (operandOne instanceof Float) {
					this.exitTypeDescriptor = "F";
					return new TypedValue(((Number) operandOne).floatValue());
				}
				this.exitTypeDescriptor = "I";
				return new TypedValue(((Number) operandOne).intValue());
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		final TypedValue operandOneValue = leftOp.getValueInternal(state);
		final Object operandOne = operandOneValue.getValue();

		final TypedValue operandTwoValue = rightOp.getValueInternal(state);
		final Object operandTwo = operandTwoValue.getValue();

		if (operandOne instanceof Number && operandTwo instanceof Number) {
			Number op1 = (Number) operandOne;
			Number op2 = (Number) operandTwo;
			if (op1 instanceof Double || op2 instanceof Double) {
				if (op1 instanceof Double && op2 instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				return new TypedValue(op1.doubleValue() + op2.doubleValue());
			}
			if (op1 instanceof Float || op2 instanceof Float) {
				if (op1 instanceof Float && op2 instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				return new TypedValue(op1.floatValue() + op2.floatValue());
			}
			if (op1 instanceof Long || op2 instanceof Long) {
				if (op1 instanceof Long && op2 instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				return new TypedValue(op1.longValue() + op2.longValue());
			}
			// TODO what about overflow?
			this.exitTypeDescriptor = "I";
			return new TypedValue(op1.intValue() + op2.intValue());
		}

		if (operandOne instanceof String && operandTwo instanceof String) {
			return new TypedValue(new StringBuilder((String) operandOne).append(
					(String) operandTwo).toString());
		}

		if (operandOne instanceof String) {
			StringBuilder result = new StringBuilder((String) operandOne);
			result.append((operandTwo == null ? "null" : convertTypedValueToString(
					operandTwoValue, state)));
			return new TypedValue(result.toString());
		}

		if (operandTwo instanceof String) {
			StringBuilder result = new StringBuilder((operandOne == null ? "null"
					: convertTypedValueToString(operandOneValue, state)));
			result.append((String) operandTwo);
			return new TypedValue(result.toString());
		}

		return state.operate(Operation.ADD, operandOne, operandTwo);
	}

	@Override
	public String toStringAST() {
		if (this.children.length<2) {  // unary plus
			return new StringBuilder().append("+").append(getLeftOperand().toStringAST()).toString();
		}

		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			return null;
		}

		return this.children[1];
	}

	/**
	 * Convert operand value to string using registered converter or using
	 * {@code toString} method.
	 *
	 * @param value typed value to be converted
	 * @param state expression state
	 * @return {@code TypedValue} instance converted to {@code String}
	 */
	private static String convertTypedValueToString(TypedValue value, ExpressionState state) {
		final TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		final TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);

		if (typeConverter.canConvert(value.getTypeDescriptor(), typeDescriptor)) {
			final Object obj = typeConverter.convertValue(value.getValue(),
					value.getTypeDescriptor(), typeDescriptor);
			return String.valueOf(obj);
		}

		return String.valueOf(value.getValue());
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length>1) {
			 if (!getRightOperand().isCompilable()) {
				 return false;
			 }
		}
		return this.exitTypeDescriptor!=null;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow codeflow) {
		getLeftOperand().generateCode(mv, codeflow);
		String leftdesc = getLeftOperand().getExitDescriptor();
		if (!CodeFlow.isPrimitive(leftdesc)) {
			Utils.insertUnboxInsns(mv, this.exitTypeDescriptor.charAt(0), false);
		}
		if (this.children.length>1) {
			getRightOperand().generateCode(mv, codeflow);
			String rightdesc = getRightOperand().getExitDescriptor();
			if (!CodeFlow.isPrimitive(rightdesc)) {
				Utils.insertUnboxInsns(mv, this.exitTypeDescriptor.charAt(0), false);
			}
			switch (this.exitTypeDescriptor.charAt(0)) {
				case 'I':
					mv.visitInsn(IADD);
					break;
				case 'J':
					mv.visitInsn(LADD);
					break;
				case 'F': 
					mv.visitInsn(FADD);
					break;
				case 'D':
					mv.visitInsn(DADD);
					break;				
				default:
					throw new IllegalStateException("Unrecognized exit descriptor: '"+this.exitTypeDescriptor+"'");			
			}
		}
		codeflow.pushDescriptor(this.exitTypeDescriptor);
	}

}
