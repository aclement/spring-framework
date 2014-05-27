/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.expression;

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.standard.CodeFlow;


/**
 * A compilable property accessor might be able to generate bytecode that represents the access operation, enabling
 * an expression to be evaluated much more quickly.
 *
 * @author Andy Clement
 * @since 4.1
 */
public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

	/**
	 * @return true if this property accessor is currently suitable for compilation.
	 */
	boolean isCompilable();

	/**
	 * Generate the bytecode implementation of accessing this property into the specified MethodVisitor using 
	 * state from the codeflow, where necessary.
	 * @param propertyReference the property reference for which code is being generated
	 * @param mv the Asm method visitor into which code should be generated
	 * @param codeflow the current state of the expression compiler
	 */
	void generateCode(PropertyOrFieldReference propertyReference, MethodVisitor mv, CodeFlow codeflow);

	/**
	 * @return the class of the result of this expression - may only be known once an access has occurred.
	 */
	Class<?> getPropertyType();
}
