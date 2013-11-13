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
import org.springframework.expression.Expression;
import org.springframework.expression.spel.CompiledExpression;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.util.ClassUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A SpelCompiler can generate a class that implements an expression. When an expression is
 * evaluated, the generated class for it will evaluate much faster than using the normal interpreted
 * route. The SpelCompiler is not currently handle all expression types but covers many 
 * of the common cases. The framework is extensible to cover more cases in the future. 
 * For absolute maximum speed there is no checking. Once an expression is evaluated once 
 * in interpreted mode it is suitable for compilation and that compiled version will base 
 * assumptions on what happened during the interpreted run (for example, if a particular type 
 * of data came from a map or a particular method was determined to be the getter for 
 * property access). This means if, on subsequent invocations of the compiled form, if 
 * any of that inferred information is no longer correct, the compiled form of the expression may fail.
 * 
 * @author Andy Clement
 */
public class SpelCompiler implements Opcodes {
	
	public static int DEFAULT_HIT_COUNT_THRESHOLD = 100;
	
	public static boolean compilerActive = false;
	public static boolean dumpCompiledExpression = true;
	public static boolean verbose = true;
	public static int hitCountThreshold = DEFAULT_HIT_COUNT_THRESHOLD;

	// TODO verify not leaking classloaders!
	private static Map<ClassLoader,SpelCompiler> compilers = Collections.synchronizedMap(new WeakHashMap<ClassLoader,SpelCompiler>());
	
	private ChildClassLoader ccl;

	// counter suffix for generated classes within this SpelCompiler instance
	private int suffixId;
	
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
	
	private SpelCompiler(ClassLoader classloader) {
		this.ccl = new ChildClassLoader(classloader);
		this.suffixId = 1;
	}
		
	public CompiledExpression compile(SpelNodeImpl expression, ExpressionState expressionState) {
		if (expression.isCompilable()) {
			if (verbose) {
				System.out.println("SpEL: compiling "+expression.toStringAST());
			}
			Class<? extends CompiledExpression> clazz = createExpressionClass(expression);
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
				System.out.println("SpEL: unable to compile "+expression.toString());
			}
		}
		return null;
	}
	
	private synchronized int getNextSuffix() {
		return suffixId++;
	}

	/**
	 * Generate the class that encapsulates the compiled expression and define it.
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends CompiledExpression> createExpressionClass(SpelNodeImpl expressionToCompile) {
		
		String clazzName = "spel/Ex"+getNextSuffix();		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
		cw.visit(V1_5,ACC_PUBLIC,clazzName,null,"org/springframework/expression/spel/CompiledExpression",null);

		// Create default constructor
		MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "org/springframework/expression/spel/CompiledExpression", "<init>", "()V");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
		
		// Create getValue() method
		mv = cw.visitMethod(ACC_PUBLIC, "getValue", "(Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"org/springframework/expression/EvaluationException"});
		mv.visitCode();
			
		CodeFlow codeflow = new CodeFlow();
		expressionToCompile.generateCode(mv,codeflow);
		CodeFlow.boxIfNecessary(mv,codeflow.lastDescriptor());

		if (codeflow.lastDescriptor() == "V") {
			mv.visitInsn(ACONST_NULL);
		}
		mv.visitInsn(ARETURN);

		mv.visitMaxs(0,0); // computed due to COMPUTE_MAXS
		mv.visitEnd();
		cw.visitEnd();
		byte[] data = cw.toByteArray();
		if (dumpCompiledExpression) {
			Utils.dump(expressionToCompile.toStringAST(),clazzName, data);
		}
		Class<? extends CompiledExpression> clazz = (Class<? extends CompiledExpression>) ccl.defineClass(clazzName.replaceAll("/","."),data);
		return clazz;
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

	public static void reset() {
		compilerActive=false;
		hitCountThreshold=DEFAULT_HIT_COUNT_THRESHOLD;
	}
	
	/**
	 * Request that an attempt is made to compile the specified expression. It may fail if components
	 * of the expression are not suitable for compilation or the data types involved are not suitable
	 * for compilation. 
	 * @return true if the expression was successfully compiled
	 */
	public static boolean compile(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			spelExpression.compiledAst = SpelCompiler.getCompiler().compile((SpelNodeImpl)spelExpression.getAST(), null);
			return spelExpression.compiledAst !=null;
		}
		return false;
	}
	
	/**
	 * Request to revert to the interpreter for expression evaluation. Any compiled form is discarded
	 * but can be recreated by later recompiling again. 
	 */
	public static void revertToInterpreted(Expression expression) {
		if (expression instanceof SpelExpression) {
			SpelExpression spelExpression = (SpelExpression)expression;
			spelExpression.compiledAst = null;
		}
	}
}
