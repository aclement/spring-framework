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

import org.springframework.asm.*;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author Andy Clement
 */
public class SpelCompiler implements Opcodes {
	
	// Global controls and information, useful for testing:
	
	// Global switch for turning compilation on/off (used in testing)
	public static boolean isCompilable = false;
	public static boolean dumpCompiledExpression = true;
	public static boolean verbose = true;
	public static int hitCountThreshold = 1;
	public static int compiledExpressionCount = 0;

	// TODO hold via weakreference - dont want to anchor another cl
	static ChildClassLoader ccl;
	
	// counter suffix for generated classes
	private static int suffixId;
	
	SpelCompiler() {
		// TODO asc compiler should give up trying after X attempts perhaps, so it doesn't perform possibly costly isCompilable checks over and over
	}
	
	public static CompiledExpression compile(SpelNodeImpl ast, ExpressionState expressionState) {
		// TODO asc synchronize this activity
		if (ccl == null) {
			// TODO asc use the one accessible through the evaluation context the user is passing in?
			ccl = new ChildClassLoader(ClassUtils.getDefaultClassLoader());
			suffixId = 1;
		}
		if (ast.isCompilable()) {
			Class<? extends CompiledExpression> clazz = createExpressionClass(ast,expressionState);
			try {
				CompiledExpression instance = clazz.newInstance();
				compiledExpressionCount++;
				return instance;
			}
			catch (InstantiationException ie) {
				ie.printStackTrace();
			} 
			catch ( IllegalAccessException iae) {
				iae.printStackTrace();
			}
		} else {
			if (!reported) {
				System.out.println("Unable to compile "+ast.toString());
				reported=true;
			}
		}
		return null;
	}
	
	static boolean reported = false;
	
	private synchronized static int getNextSuffix() {
		return suffixId++;
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl ast,
			ExpressionState expressionState) {
		
		String clazzName = "spel/Ex"+getNextSuffix();
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);//|ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_5,ACC_PUBLIC,clazzName,null,"org/springframework/expression/spel/CompiledExpression",null);

		// Create default ctor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression", "<init>", "()V");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		

//		public TypedValue getValueInternal(ExpressionState expressionState)
//				throws EvaluationException {
		// TODO asc move class names to field class literal refs
		mv = cw.visitMethod(ACC_PUBLIC, "getValueInternal", "(Lorg/springframework/expression/spel/ExpressionState;)Lorg/springframework/expression/TypedValue;", null, new String[]{"org/springframework/expression/EvaluationException"});
		mv.visitCode();
		
		// Cache the root context object
//		if ((ast.bits() & SpelNode.BITMASK_VARIABLES) != 0 {
//		mv.visitVarInsn(ALOAD,1);
//		mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/spel/ExpressionState", "getActiveContextObject", "()Lorg/springframework/expression/TypedValue;");
//		mv.visitMethodInsn(INVOKEVIRTUAL, "org/springframework/expression/TypedValue","getValue","()Ljava/lang/Object;");
//		mv.visitVarInsn(ASTORE,2);
//		}
		
//		mv.visitTypeInsn(CHECKCAST, "java/lang/String");
		
		CodeFlow codeflow = new CodeFlow();
		
		ast.generateCode(mv,codeflow);
		
		
		// Build result TypedValue
		pushCorrectStore(mv, codeflow.lastKnownType(), 3);
		mv.visitTypeInsn(NEW, "org/springframework/expression/TypedValue");
		mv.visitInsn(DUP);
		pushCorrectLoad(mv, codeflow.lastKnownType(), 3);
		boxIfNecessary(mv,codeflow.lastKnownType());
		// TODO for boolean faster way of sorting out typed value
		// TODO asc adjust tdString reference to use constants where possible otherwise call TypeDescriptor factory methods
		TypeDescriptor td = ast.getExitType();
		// TODO temporary whilst we flesh things out:
		if (td==null) {
			throw new IllegalStateException("This ast node has no exit type "+ast.getClass()+":"+ast.toString());
		}
		insertTypeDescriptorLoad(mv,td);
//		mv.visitFieldInsn(GETSTATIC,"org/springframework/expression/spel/CompiledExpression","tdString","Lorg/springframework/core/convert/TypeDescriptor;");
		mv.visitMethodInsn(INVOKESPECIAL,"org/springframework/expression/TypedValue","<init>","(Ljava/lang/Object;Lorg/springframework/core/convert/TypeDescriptor;)V");		
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0,0); // computed due to COMPUTE_MAXS
		mv.visitEnd();
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		if (dumpCompiledExpression) {
			Utils.dump(clazzName, data);
		}
		Class<? extends CompiledExpression> clazz = (Class<? extends CompiledExpression>) ccl.defineClass(clazzName.replaceAll("/","."),data);
		return clazz;
	}
	
	final static Map<String,String> tdMap;
	static {
		tdMap = new HashMap<String,String>();
		tdMap.put("java.lang.String","tdString");
		tdMap.put("java.lang.Integer","tdInteger");
		tdMap.put("java.lang.Character","tdCharacter");
		tdMap.put("int","tdIntType");
		tdMap.put("boolean","tdBooleanType");
		tdMap.put("java.lang.Object","tdObject");
	}

	private static void insertTypeDescriptorLoad(MethodVisitor mv, TypeDescriptor td) {
		String name = td.getType().getName();
		String tdconstant = tdMap.get(name);
		if (tdconstant!=null) {
			mv.visitFieldInsn(GETSTATIC,"org/springframework/expression/spel/CompiledExpression",tdconstant,"Lorg/springframework/core/convert/TypeDescriptor;");
		} else {
			// TODO asc support the general case!
			throw new IllegalStateException("nyi for "+name);
		}
	}

	public static void boxIfNecessary(MethodVisitor mv, Class<?> clazz) {
		if (!clazz.isPrimitive()) {
			return;
		}
		String name = clazz.getName();
		int len = name.length();
		switch (len) {
		case 3:
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
			break;
		case 4:
			if (name.equals("char")) {
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
				return;
			}
			throw new IllegalStateException("nyi for "+name);
		case 7:
			if (name.equals("boolean")) {
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
				return;
			}
			default:
				throw new IllegalStateException("nyi for "+name);
//		case 'F':
//			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
//			break;
//		case 'S':
//			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
//			break;
//		case 'J':
//			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
//			break;
//		case 'D':
//			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
//			break;
//		case 'B':
//			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
//			break;
//		case 'L':
//		case '[':
//			// no box needed
//			break;
//		default:
//			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
		}
	}
	
	
	private static void pushCorrectStore(MethodVisitor mv, Class<?> clazz, int local) {
		if (clazz.isPrimitive()) {
			String name = clazz.getName();
			int len = name.length();
			switch (len) {
				case 3:
					mv.visitVarInsn(ISTORE, local);
					break;
				case 7:
					mv.visitVarInsn(ISTORE,local);
					break;
				case 4:
					if (name.equals("char")) {
						mv.visitVarInsn(ISTORE, local);	
						return;
					}
					default: 
						throw new IllegalStateException("nyi for "+name);
			}
			
		} else {
			mv.visitVarInsn(ASTORE, local);
		}
	}

	private static void pushCorrectLoad(MethodVisitor mv, Class<?> clazz, int local) {
		if (clazz.isPrimitive()) {
			String name = clazz.getName();
			int len = name.length();
			switch (len) {
				case 3:
					mv.visitVarInsn(ILOAD, local);
					break;
				case 7:					
					mv.visitVarInsn(ILOAD, local);
					break;
				case 4:
					if (name.equals("char")) {
						mv.visitVarInsn(ILOAD, local);	
						return;
					}
					default: 
						throw new IllegalStateException("nyi for "+name);
			}
			
		} else {
			mv.visitVarInsn(ALOAD, local);
		}
	}


	static class Expr1 extends CompiledExpression {
		

		public TypedValue getValueInternal(ExpressionState state) {
			// Prep code:
			String target = (String) state.getActiveContextObject().getValue();
			// Invocation code:
			Object result = target.toUpperCase();
			// Result prep code:
			return new TypedValue(result, tdString);
		}
//				throws EvaluationException {
////		public String getValue(ExpressionState state) throws EvaluationException {
//			String value = (String) state.getRootContextObject().getValue();//getActiveContextObject().getValue();

		// this version is suitable if:
		// 1. very simple expression, nobody stacks up results (so changes the active context object)
		
		public Object getValue(Object target) throws EvaluationException {
			String value = (String) target;
			Object result = value.toUpperCase();
//			TypeDescriptor targetType = state.getActiveContextObject().getTypeDescriptor();
//			System.out.println("Expr1 computed "+value);
			return result;// new TypedValue(result,tdForResult);
		}
	}
	

	/**
	 * The ChildClassLoader will load the generated dispatchers and executors which change for each reload. Instances of this can be
	 * discarded which will cause 'old' dispatchers/executors to be candidates for GC too (avoiding memory leaks when lots of reloads
	 * occur).
	 */
	public static class ChildClassLoader extends URLClassLoader {

		private static URL[] NO_URLS = new URL[0];
		private int definedCount = 0;

		public ChildClassLoader(ClassLoader classloader) {
			super(NO_URLS, classloader);
		}

		public Class<?> defineClass(String name, byte[] bytes) {
			definedCount++;
			return super.defineClass(name, bytes, 0, bytes.length);
		}

		public int getDefinedCount() {
			return definedCount;
		}

	}

	public static String createDescriptor(Method method) {
		Class<?>[] params = method.getParameterTypes();
		StringBuilder s = new StringBuilder();
		s.append("(");
		for (int i = 0, max = params.length; i < max; i++) {
			appendDescriptor(params[i], s);
		}
		s.append(")");
		appendDescriptor(method.getReturnType(), s);
		return s.toString();
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

	public static void reset() {
		isCompilable=false;
		hitCountThreshold=100;
		compiledExpressionCount=0;
	}
	
	// For testing, forces a compile
	public static boolean compile(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			spelExpression.compiledAst = SpelCompiler.compile((SpelNodeImpl)spelExpression.getAST(), null);
			return spelExpression.compiledAst !=null;
		}
		return false;
	}
	
}
