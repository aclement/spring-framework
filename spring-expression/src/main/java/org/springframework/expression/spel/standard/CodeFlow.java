/*
 * Copyright 2013 the original author or authors.
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
import java.util.Stack;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.util.Assert;

/**
 * Records intermediate compilation state as the bytecode is generated for a parsed expression.
 * The compilationScopes entries record what is currently on the bytecode stack as components of an expression
 * are evaluated.
 * 
 * @author Andy Clement
 */
public class CodeFlow implements Opcodes {

	private Stack<ArrayList<String>> compilationScopes;
	
	public CodeFlow() {
		compilationScopes = new Stack<ArrayList<String>>();
		compilationScopes.add(new ArrayList<String>());
	}
	
	public void loadContextObject(MethodVisitor mv, int var) {
		mv.visitVarInsn(ALOAD,var);
	}

	public void loadRootObject(MethodVisitor mv) {
		mv.visitVarInsn(ALOAD,1);		
	}
		
	// Currently the same as load root since #this isn't supported
	public void loadTarget(MethodVisitor mv) {
		loadRootObject(mv); 
	}

	public void pushDescriptor(String descriptor) {
		Assert.notNull(descriptor);
		compilationScopes.peek().add(descriptor);
	}
	
	public void enterCompilationScope() {
		compilationScopes.push(new ArrayList<String>());
	}
	
	public void exitCompilationScope() {
		compilationScopes.pop();
	}
	
	public String lastDescriptor() {
		if (compilationScopes.peek().size()==0) {
			return null;
		}
		return compilationScopes.peek().get(compilationScopes.peek().size()-1);
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
