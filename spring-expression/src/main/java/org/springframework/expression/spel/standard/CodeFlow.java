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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Stack;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.util.Assert;

/**
 * Records intermediate compilation state as the bytecode is generated for a parsed expression.
 * The compilationScopes entries record what is currently on the top of the bytecode stack as 
 * components of an expression are evaluated.
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
	
	public void unboxBooleanIfNecessary(MethodVisitor mv) {
		String ld = lastDescriptor();
		if (ld.equals("Ljava/lang/Boolean")) {
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
		}
	}
	
	public static void appendDescriptor(Class<?> p, StringBuilder s) {
		if (p.isArray()) {
			while (p.isArray()) {
				s.append("[");
				p = p.getComponentType();
			}
		}
		if (p.isPrimitive()) {
			if (p == Void.TYPE) {
				s.append('V');
			} else if (p == Integer.TYPE) {
				s.append('I');
			} else if (p == Boolean.TYPE) {
				s.append('Z');
			} else if (p == Character.TYPE) {
				s.append('C');
			} else if (p == Long.TYPE) {
				s.append('J');
			} else if (p == Double.TYPE) {
				s.append('D');
			} else if (p == Float.TYPE) {
				s.append('F');
			} else if (p == Byte.TYPE) {
				s.append('B');
			} else if (p == Short.TYPE) {
				s.append('S');
			}
		} else {
			s.append("L");
			s.append(p.getName().replace('.', '/'));
			s.append(";");
		}
	}

	public static String createDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuilder s = new StringBuilder();
		s.append("(");
		for (int i = 0, max = params.length; i < max; i++) {
			CodeFlow.appendDescriptor(params[i], s);
		}
		s.append(")");
		CodeFlow.appendDescriptor(method.getReturnType(), s);
		return s.toString();
	}

	public static String createDescriptor(Constructor<?> ctor) {
		Class<?>[] params = ctor.getParameterTypes();
		StringBuilder s = new StringBuilder();
		s.append("(");
		for (int i = 0, max = params.length; i < max; i++) {
			CodeFlow.appendDescriptor(params[i], s);
		}
		s.append(")V");
		return s.toString();
	}

	public static String getDescriptor(Class<?> p) {
		StringBuilder s= new StringBuilder();
		if (p.isArray()) {
			while (p.isArray()) {
				s.append("[");
				p = p.getComponentType();
			}
		}
		if (p.isPrimitive()) {
			if (p == Void.TYPE) {
				s.append('V');
			} else if (p == Integer.TYPE) {
				s.append('I');
			} else if (p == Boolean.TYPE) {
				s.append('Z');
			} else if (p == Character.TYPE) {
				s.append('C');
			} else if (p == Long.TYPE) {
				s.append('J');
			} else if (p == Double.TYPE) {
				s.append('D');
			} else if (p == Float.TYPE) {
				s.append('F');
			} else if (p == Byte.TYPE) {
				s.append('B');
			} else if (p == Short.TYPE) {
				s.append('S');
			}
		} else {
			s.append("L");
			s.append(p.getName().replace('.', '/'));
			s.append(";");
		}
		return s.toString();
	}

	public static String toDescriptorFromObject(Object value) {
		if (value == null) {
			return "Ljava/lang/Object";
		} else {
			return toDescriptor(value.getClass());
		}
	}

	public static String toDescriptor(Class<?> type) {
		String name = type.getName();
		if (type.isPrimitive() ) {
			switch (name.length()) {
				case 3:
					return "I";
				case 4:
					if (name.equals("long")) {
						return "J";
					}
					else if (name.equals("char")) {
						return "C";
					}
					else if (name.equals("byte")) {
						return "B";
					}
					else if (name.equals("void")) {
						return "V";
					}
					break;
				case 5:
					if (name.equals("float")) {
						return "F";
					}
					else if (name.equals("short")) {
						return "S";
					}
					break;
				case 6:
					if (name.equals("double")) {
						return "D";
					}
					break;
				case 7:
					if (name.equals("boolean")) {
						return "Z";
					}
					break;
				default:
					throw new IllegalStateException("nyi "+name);
			}
		} else {
			if (name.charAt(0)!='[') {
				return new StringBuilder("L").append(type.getName().replace('.', '/')).toString();
			} else {
				if (name.endsWith(";")) {
					return name.substring(0,name.length()-1).replace('.','/');					
				} else {
					return name; // primitive
				}
			}
		}
		throw new IllegalStateException("nyi "+name);
	}

	public static boolean isBooleanCompatible(String descriptor) {
		return descriptor!=null && ( descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean"));
	}

	public static boolean isPrimitive(String descriptor) {
		return descriptor!=null && descriptor.length()==1;
	}

	public static boolean isPrimitiveArray(String descriptor) {
		boolean primitive = true;
		for (int i=0,max=descriptor.length();i<max;i++) {
			char ch = descriptor.charAt(i);
			if (ch=='[') {
				continue;
			} 
			primitive = (ch!='L');
			break;
		}
		return primitive;
	}

	public static void insertUnboxIfNecessary(MethodVisitor mv, CodeFlow codeflow,char desiredPrimitiveType) {
		String ld = codeflow.lastDescriptor();
		switch (desiredPrimitiveType) {
			case 'Z':
				if (ld.equals("Ljava/lang/Boolean")) {
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z",false);
				} else if (!ld.equals("Z")) {
					throw new IllegalStateException("not unboxable to boolean:"+codeflow.lastDescriptor());
				}
				break;
			default:
				throw new IllegalStateException("nyi "+desiredPrimitiveType);
		}
	}

	/**
	 * return true if you can get (via boxing) from one descriptor to the other. Assumes
	 * at least one of is a boxable type.
	 */
	public static boolean boxingCompatible(String desc1, String desc2) {
		if (desc1.equals(desc2)) {
			return true;
		}
		if (desc1.length()==1) {
			if (desc1.equals("D")) {
				return desc2.equals("Ljava/lang/Double");
			}
			else if (desc1.equals("F")) {
				return desc2.equals("Ljava/lang/Float");
			}
			else if (desc1.equals("J")) {
				return desc2.equals("Ljava/lang/Long");
			}
			else if (desc1.equals("I")) {
				return desc2.equals("Ljava/lang/Integer");
			}
			else if (desc1.equals("Z")) {
				return desc2.equals("Ljava/lang/Boolean");
			}
		}
		else if (desc2.length()==1) {
			if (desc2.equals("D")) {
				return desc1.equals("Ljava/lang/Double");
			}
			else if (desc2.equals("F")) {
				return desc1.equals("Ljava/lang/Float");
			}
			else if (desc2.equals("J")) {
				return desc1.equals("Ljava/lang/Long");
			}
			else if (desc2.equals("I")) {
				return desc1.equals("Ljava/lang/Integer");
			}			
			else if (desc2.equals("Z")) {
				return desc1.equals("Ljava/lang/Boolean");
			}
		}
		return false;
	}

	public static boolean isPrimitiveOrUnboxableSupportedNumberOrBoolean(String descriptor) {
		if (descriptor==null) {
			return false;
		}
		if (descriptor.length()==1) {
			return "DFJZI".indexOf(descriptor.charAt(0))!=-1;
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			if (descriptor.equals("Ljava/lang/Double") || descriptor.equals("Ljava/lang/Integer") || 
				descriptor.equals("Ljava/lang/Float") || descriptor.equals("Ljava/lang/Long") ||
				descriptor.equals("Ljava/lang/Boolean")) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPrimitiveOrUnboxableSupportedNumber(String descriptor) {
		if (descriptor==null) {
			return false;
		}
		if (descriptor.length()==1) {
			return "DFJI".indexOf(descriptor.charAt(0))!=-1;
		}
		if (descriptor.startsWith("Ljava/lang/")) {
			if (descriptor.equals("Ljava/lang/Double") || descriptor.equals("Ljava/lang/Integer") || 
				descriptor.equals("Ljava/lang/Float") || descriptor.equals("Ljava/lang/Long")) {
				return true;
			}
		}
		return false;
	}

	public static char toPrimitiveTargetDesc(String descriptor) {
		if (descriptor.length()==1) {
			return descriptor.charAt(0);
		}
		if (descriptor.equals("Ljava/lang/Double")) {
			return 'D';
		} else if (descriptor.equals("Ljava/lang/Integer")) {
			return 'I';
		} else if (descriptor.equals("Ljava/lang/Float")) {
			return 'F';
		} else if (descriptor.equals("Ljava/lang/Long")) {
			return 'J';
		} else if (descriptor.equals("Ljava/lang/Boolean")) {
			return 'Z';
		} else {
			throw new IllegalStateException("No primitive for '"+descriptor+"'");
		}
	}

	public static void insertCheckCast(MethodVisitor mv, String exitTypeDescriptor) {
		if (exitTypeDescriptor.length()!=1) {
			if (exitTypeDescriptor.charAt(0)=='[') {
				if (CodeFlow.isPrimitiveArray(exitTypeDescriptor)) {
					mv.visitTypeInsn(CHECKCAST, exitTypeDescriptor);					
				}
				else {
					mv.visitTypeInsn(CHECKCAST, exitTypeDescriptor+";");
				}
			} else {
				// This is chopping off the 'L' to leave us with "java/lang/String"
				mv.visitTypeInsn(CHECKCAST, exitTypeDescriptor.substring(1));
			}
		}
	}

	public static void boxIfNecessary(MethodVisitor mv, String descriptor) {
		if (descriptor.length()!=1) {
			return;
		}
		char ch = descriptor.charAt(0);
		switch (ch) {
		case 'I':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			break;
		case 'C':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
			break;
		case 'J':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
			break;
		case 'Z':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
			break;
		case 'F':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
			break;
		case 'S':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
			break;
		case 'D':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
			break;
		case 'B':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
			break;
		case 'V':
		case '[':
			// does not need boxing
			break;
		default:
			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	public static void insertBoxInsns(MethodVisitor mv, char ch) {
		switch (ch) {
		case 'I':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
			break;
		case 'F':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
			break;
		case 'S':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
			break;
		case 'Z':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
			break;
		case 'J':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
			break;
		case 'D':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
			break;
		case 'C':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
			break;
		case 'B':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
			break;
		case 'L':
		case '[':
			// no box needed
			break;
		default:
			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	public static String[] toParamDescriptors(Method m) {
		Class<?>[] parameterTypes = m.getParameterTypes();
		int parameterCount = m.getParameterCount();
		String[] parameters = new String[parameterCount];
		for (int p=0;p<parameterCount;p++) {
			parameters[p] = CodeFlow.toDescriptor(parameterTypes[p]);
		}
		return parameters;
	}
	
	public static String[] toParamDescriptors(Constructor<?> c) {
		Class<?>[] parameterTypes = c.getParameterTypes();
		int parameterCount = c.getParameterCount();
		String[] parameters = new String[parameterCount];
		for (int p=0;p<parameterCount;p++) {
			parameters[p] = CodeFlow.toDescriptor(parameterTypes[p]);
		}
		return parameters;
	}

}
