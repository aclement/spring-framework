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
//		Class<?> descriptor;
//		boolean stored;
//		State(Class<?> descriptor) {
//			this.descriptor=descriptor;
//			this.stored=false;
//		}
	}
	Stack<State> stack = new Stack<State>();
	
	List<Class<?>> types = new ArrayList<Class<?>>();

	int depth = 2;
//
//	public void pushDescriptor(Class<?> clazz) {
//		stack.push(new State(clazz));
//	}
//
//	public String peekDescriptor() {
//		if (stack.isEmpty()) {
//			return null;
//		}
//		return stack.peek().descriptor.getName().replace('.','/');
//	}
//
//	public String popDescriptor() {
//		if (stack.isEmpty()) {
//			return null;
//		}
//		return stack.pop().descriptor.getName().replace('.','/');
//	}
//	
//	public Class<?> peek() {
//		if (stack.isEmpty()) {
//			return null;
//		}
//		return stack.peek().descriptor;
//	}
//
//	public Class<?> pop() {
//		if (stack.isEmpty()) {
//			return null;
//		}
//		return stack.pop().descriptor;
//	}

	
	private int ROOT_VAR = 2;
	private boolean rootVarLoaded = false;
	private int nextFreeVar = 3;
	
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
		if (!rootVarLoaded) {
			mv.visitVarInsn(ALOAD,1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/spel/ExpressionState", "getRootContextObject", "()Lorg/springframework/expression/TypedValue;");
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/TypedValue","getValue","()Ljava/lang/Object;");
			mv.visitVarInsn(ASTORE,ROOT_VAR);
			rootVarLoaded = true;
		}
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

	public void incDepth() {
		depth++;
	}
	
	public void decDepth() {
		depth--;
	}

	public void pushType(Class<?> clazz) {
		types.add(clazz);
	}
	
	public Class<?> lastKnownType() {
		if (types.size()==0) {
			return null;
		}
		return types.get(types.size()-1);
	}

}
