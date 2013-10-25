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
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.util.ClassUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/*
 * Compiler follow on work items:
 * 
 * - OpMinus with a single literal operand could be treated as a negative literal. Will save a
 *   pointless loading of 0 and then a subtract instruction in code gen.
 * - A TypeReference followed by (what ends up as) a static method invocation can really skip
 *   code gen for the TypeReference since once that is used to locate the method it is not
 *   used again.
 * - The opEq implementation is quite basic. It will compare numbers of the same type (allowing
 *   them to be their boxed or unboxed variants) or compare object references. It does not
 *   compile expressions where numbers are of different types and nothing when objects implement
 *   Comparable.  
 * - The only kind of compilable VariableReference is #root.
 * 
 */

/**
 * A SpelCompiler can generate a class that implements an expression. When an expression is
 * evaluated, the generated class will evaluate much faster than using the normal interpreted
 * route. The SpelCompiler is not complete but covers many of the common cases
 * which can really benefit from the accelerated compiled evaluation. The framework is
 * extensible to cover more cases in the future. For absolute maximum speed there is no
 * checking. Once an expression is evaluated once in interpreted mode it is suitable for
 * compilation and that compiled version will base assumptions on what happened during the
 * interpreted run (for example, if a particular type of data came from a map or a particular
 * method was determined to be the getter for property access). This means if, on subsequent
 * invocations of the compiled form, if any of that inferred information is no longer correct,
 * the compiled form of the expression may fail.
 * 
 * @author Andy Clement
 */
public class SpelCompiler implements Opcodes {
	
	public static int DEFAULT_HIT_COUNT_THRESHOLD = 100;
	
	public static boolean compilerActive = false;
	public static boolean dumpCompiledExpression = true;
	public static boolean verbose = true;
	public static int hitCountThreshold = DEFAULT_HIT_COUNT_THRESHOLD;
		
	private static Map<ClassLoader,SpelCompiler> compilers = Collections.synchronizedMap(new WeakHashMap<ClassLoader,SpelCompiler>());
	
	private WeakReference<ChildClassLoader> ccl;

	// counter suffix for generated classes within this SpelCompiler instance
	private int suffixId;
	
	public static SpelCompiler getCompiler() {
		ClassLoader classloader = ClassUtils.getDefaultClassLoader();
		synchronized (compilers) {
			SpelCompiler compiler = compilers.get(classloader);
			if (compiler == null) {
				compiler = new SpelCompiler(classloader);
				compilers.put(classloader,compiler);
			}
			return compiler;
		}
	}
	
	static {
		try {
			String compilationFlag = System.getProperty("spel.compiler","false").toLowerCase();
			if (compilationFlag.equals("true")) {
				compilerActive = true;
				System.out.println("SpelCompiler: switched ON");
			}
			String threshold = System.getProperty("spel.compiler.hitcount");
			if (threshold!=null) {
				DEFAULT_HIT_COUNT_THRESHOLD = Integer.parseInt(threshold);
				hitCountThreshold = DEFAULT_HIT_COUNT_THRESHOLD;
				System.out.println("SpelCompiler: threshold for compilation = "+DEFAULT_HIT_COUNT_THRESHOLD);
			}
			String verbose = System.getProperty("spel.compiler.verbose");
			if (verbose!=null) {
				SpelCompiler.verbose = verbose.equalsIgnoreCase("true");
			}
		} catch (Exception e) {
			// security exception...
		}
	}
	
	private SpelCompiler(ClassLoader classloader) {
		this.ccl = new WeakReference<ChildClassLoader>(new ChildClassLoader(classloader));
		this.suffixId = 1;
	}
	
	
	public CompiledExpression compile(SpelNodeImpl ast, ExpressionState expressionState) {
		if (ast.isCompilable()) {
			if (verbose) {
				System.out.println("SpEL: compiling "+ast.toStringAST());
			}
			Class<? extends CompiledExpression> clazz = createExpressionClass(ast,expressionState);
			try {
				CompiledExpression instance = clazz.newInstance();
				return instance;
			}
			catch (InstantiationException ie) {
				ie.printStackTrace();
			} 
			catch ( IllegalAccessException iae) {
				iae.printStackTrace();
			}
		} else {
			if (verbose) {
				System.out.println("SpEL: unable to compile "+ast.toString());
			}
		}
		return null;
	}
	
	private synchronized int getNextSuffix() {
		return suffixId++;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl ast,
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
		

		// TODO asc move class names to field class literal refs
//		mv = cw.visitMethod(ACC_PUBLIC, "getValueInternal", "(Lorg/springframework/expression/spel/ExpressionState;)Lorg/springframework/expression/TypedValue;", null, new String[]{"org/springframework/expression/EvaluationException"});
		mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"org/springframework/expression/EvaluationException"});
		mv.visitCode();
			
		CodeFlow codeflow = new CodeFlow();
		ast.generateCode(mv,codeflow);
		boxIfNecessary(mv,codeflow.lastDescriptor());

//		// Build result TypedValue
//		pushCorrectStore(mv, codeflow.lastKnownType(), 3);
//		mv.visitTypeInsn(NEW, "org/springframework/expression/TypedValue");
//		mv.visitInsn(DUP);
//		pushCorrectLoad(mv, codeflow.lastKnownType(), 3);
//		boxIfNecessary(mv,codeflow.lastKnownType());
//		// TODO for boolean faster way of sorting out typed value
//		// TODO asc adjust tdString reference to use constants where possible otherwise call TypeDescriptor factory methods
//		TypeDescriptor td = ast.getExitType();
//		// TODO temporary whilst we flesh things out:
//		if (td==null) {
//			throw new IllegalStateException("This ast node has no exit type "+ast.getClass()+":"+ast.toString());
//		}
//		insertTypeDescriptorLoad(mv,td);
////		mv.visitFieldInsn(GETSTATIC,"org/springframework/expression/spel/CompiledExpression","tdString","Lorg/springframework/core/convert/TypeDescriptor;");
//		mv.visitMethodInsn(INVOKESPECIAL,"org/springframework/expression/TypedValue","<init>","(Ljava/lang/Object;Lorg/springframework/core/convert/TypeDescriptor;)V");		
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0,0); // computed due to COMPUTE_MAXS
		mv.visitEnd();
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		if (dumpCompiledExpression) {
			Utils.dump(ast.toStringAST(),clazzName, data);
		}
		ChildClassLoader classloader = ccl.get();
		if (ccl == null) {
			throw new IllegalStateException("!");
		}
		Class<? extends CompiledExpression> clazz = (Class<? extends CompiledExpression>) classloader.defineClass(clazzName.replaceAll("/","."),data);
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

	public static void boxIfNecessary(MethodVisitor mv, String descriptor) {
		if (descriptor.length()!=1) {
			return;
		}
		char ch = descriptor.charAt(0);
		switch (ch) {
		case 'I':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
			break;
		case 'C':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
			break;
		case 'J':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
			break;
		case 'Z':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
			break;
		case 'F':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
			break;
		case 'S':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
			break;
		case 'D':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
			break;
		case 'B':
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
			break;
		default:
			throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
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
		compilerActive=false;
		hitCountThreshold=DEFAULT_HIT_COUNT_THRESHOLD;
	}
	
	// For testing, forces a compile
	public static boolean compile(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			spelExpression.compiledAst = SpelCompiler.getCompiler().compile((SpelNodeImpl)spelExpression.getAST(), null);
			return spelExpression.compiledAst !=null;
		}
		return false;
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
				case 5:
					if (name.equals("float")) {
						return "F";
					}
					else if (name.equals("short")) {
						return "S";
					}
				case 6:
					if (name.equals("double")) {
						return "D";
					}
				case 7:
					if (name.equals("boolean")) {
						return "Z";
					}
				default:
					throw new IllegalStateException("nyi "+name);
			}
		} else {
			if (name.charAt(0)!='[') {
				return new StringBuilder("L").append(type.getName().replace('.', '/')).toString();
			} else {
				return name.substring(0,name.length()-1).replace('.','/');
			}
		}
	}

	public static String toDescriptorFromObject(Object value) {
		if (value == null) {
			return "Ljava/lang/Object";
		} else {
			return toDescriptor(value.getClass());
		}
	}

	public static void insertUnboxIfNecessary(MethodVisitor mv, CodeFlow codeflow,char desiredPrimitiveType) {
		String ld = codeflow.lastDescriptor();
		switch (desiredPrimitiveType) {
			case 'Z':
				if (ld.equals("Ljava/lang/Boolean")) {
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
				} else if (!ld.equals("Z")) {
					throw new IllegalStateException("not unboxable to boolean:"+codeflow.lastDescriptor());
				}
				break;
			default:
				throw new IllegalStateException("nyi "+desiredPrimitiveType);
		}
	}

	public static void insertCheckCast(MethodVisitor mv, String exitTypeDescriptor) {
		if (exitTypeDescriptor.length()!=1) {
			if (exitTypeDescriptor.charAt(0)=='[') {
				mv.visitTypeInsn(CHECKCAST, exitTypeDescriptor+";");
			} else {
				// This is chopping off the 'L' to leave us with "java/lang/String"
				mv.visitTypeInsn(CHECKCAST, exitTypeDescriptor.substring(1));
			}
		}
	}

	public static boolean isPrimitive(String descriptor) {
		return descriptor!=null && descriptor.length()==1;
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
	
	

	public static boolean isBooleanCompatible(String descriptor) {
		return descriptor!=null && ( descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean"));
	}
	
	public static boolean isDoubleCompatible(String descriptor) {
		return descriptor!=null && (descriptor.equals("D") || descriptor.equals("Ljava/lang/Double"));
	}

	public static boolean isNumber(String desc) {
		if (desc==null) {
			return false;
		}
		if (desc.length()==1) {
			return "IDFJ".indexOf(desc.charAt(0))!=-1;
		}
		return desc.equals("Ljava/lang/Double") || desc.equals("Ljava/lang/Integer") || desc.equals("Ljava/lang/Float") || desc.equals("Ljava/lang/Long");				
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

	public static boolean isDouble(String desc) {
		return desc.equals("D") || desc.equals("Ljava/lang/Double");
	}

	public static boolean isFloat(String desc) {
		return desc.equals("F") || desc.equals("Ljava/lang/Float");
	}
	
	public static boolean isInteger(String desc) {
		return desc.equals("I") || desc.equals("Ljava/lang/Integer");
	}
	
	public static boolean isLong(String desc) {
		return desc.equals("J") || desc.equals("Ljava/lang/Long");
	}
}
