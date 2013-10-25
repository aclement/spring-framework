/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.standard;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.util.Assert;


/**
 * 
 * @author Andy Clement
 */
public class CodeFlow implements Opcodes {

	static class State {
		int var;
		State(int i) {
			var = i;
		}
	}
	Stack<State> stack = new Stack<State>();
	
	List<String> types = new ArrayList<String>();

	int depth = 2;
	
	private int ROOT_VAR = 1;
	private boolean rootVarLoaded = false;
	private int nextFreeVar = 3;
	private boolean typeReferenceOnStack = false;
	
	public void loadActiveContextObject(MethodVisitor mv, boolean saveIt) {
		mv.visitVarInsn(ALOAD,1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/spel/ExpressionState", "getActiveContextObject", "()Lorg/springframework/expression/TypedValue;");
		mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/TypedValue","getValue","()Ljava/lang/Object;");
	}
	
	public int storeContextObject(MethodVisitor mv) {
		int var = nextFreeVar++;
		mv.visitVarInsn(ASTORE,var);
		mv.visitVarInsn(ALOAD,var);
		return var;
	}

	public void loadContextObject(MethodVisitor mv, int var) {
		mv.visitVarInsn(ALOAD,var);
	}

	public void loadRootObject(MethodVisitor mv) {
//		if (!rootVarLoaded) {
//			mv.visitVarInsn(ALOAD,1);
//			mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/spel/ExpressionState", "getRootContextObject", "()Lorg/springframework/expression/TypedValue;");
//			mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/TypedValue","getValue","()Ljava/lang/Object;");
//			mv.visitVarInsn(ASTORE,ROOT_VAR);
//			rootVarLoaded = true;
//		}
		mv.visitVarInsn(ALOAD,ROOT_VAR);		
		addStack(ROOT_VAR);
	}
	
	private void addStack(int i) {
		stack.push(new State(i));
	}
	private void popStack() {
		stack.pop();
	}
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	
	public void loadTarget(MethodVisitor mv) {
//		if (!rootVarLoaded) {
			loadRootObject(mv); 
//		} else {
//			State state = (this.stack.isEmpty()?null:this.stack.peek());
//			if (state == null) {
//			addStack(depth);
//			mv.visitVarInsn(ALOAD,depth);
			
//		}
	}

	public void storeTarget(MethodVisitor mv) {
		if (!rootVarLoaded) {
			loadRootObject(mv);
		}
		// TODO asc (un)boxing? or correct store/load?
		popStack();
		mv.visitVarInsn(ASTORE, depth);
		addStack(depth);
		mv.visitVarInsn(ALOAD, depth);
	}


	/**
	 * Descriptor of a type, almost like a bytecode one but slightly easier to chop up for asm usage.
	 * For a primitive type it is the single character. For a reference type it is the slashed name
	 * prefixed with an "L". Note there is no trailing ";". So for string the descriptor is "Ljava/lang/String".
	 *
	 * @param descriptor descriptor for the type on top of the stack in the bytecode
	 */
	public void pushDescriptor(String descriptor) {
		Assert.notNull(descriptor);
		types.add(descriptor);
	}
	
	public String lastDescriptor() {
		if (types.size()==0) {
			return null;
		}
		return types.get(types.size()-1);
	}

	Stack<String> requestedReturnDescriptors = new Stack<String>();
	
	public String popRequested() {
		return requestedReturnDescriptors.pop();
	}
	
	public void pushRequested(String requestedReturnDescriptor) {
		requestedReturnDescriptors.push(requestedReturnDescriptor);
	}

	public void clearDescriptor() {
		types.clear();
	}

	public boolean lastDescriptorIsPrimitive() {
		return lastDescriptor().length()==1;
	}
	
	public void insertUnboxIfNecessary(MethodVisitor mv, char desiredPrimitiveType) {
		String ld = lastDescriptor();
		switch (desiredPrimitiveType) {
			case 'Z':
				if (ld.equals("Ljava/lang/Boolean")) {
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
				} else if (!ld.equals("Z")) {
					throw new IllegalStateException("not unboxable to boolean:"+lastDescriptor());
				}
				break;
			default:
				throw new IllegalStateException("nyi "+desiredPrimitiveType);
		}
	}
	
	public static void insertBoxInsns(MethodVisitor mv, char ch) {
		switch (ch) {
		case 'I':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
			break;
		case 'F':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
			break;
		case 'S':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
			break;
		case 'Z':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
			break;
		case 'J':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
			break;
		case 'D':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
			break;
		case 'C':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
			break;
		case 'B':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
			break;
		case 'L':
		case '[':
			// no box needed
			break;
		default:
			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

}
