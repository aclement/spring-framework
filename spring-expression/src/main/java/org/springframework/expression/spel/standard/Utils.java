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
package org.springframework.expression.spel.standard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;



/**
 * 
 * @author Andy Clement
 * @since 4.0
 */
public class Utils implements Opcodes {

	// public final static boolean assertsOn = true;
	public final static String[] NO_STRINGS = new String[0];

	public static void insertUnboxInsnsIfNecessary(MethodVisitor mv, String type, boolean isObject) {
		if (type.length() != 1) {
			return; // unnecessary
		}
		insertUnboxInsns(mv, type.charAt(0), isObject);
	}

	public static void insertUnboxInsns(MethodVisitor mv, char ch, boolean isObject) {
		switch (ch) {
		case 'I':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
			break;
		case 'Z':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
			break;
		case 'B':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
			break;
		case 'C':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
			break;
		case 'D':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
			break;
		case 'S':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
			break;
		case 'F':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
			break;
		case 'J':
			if (isObject) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
			}
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
			break;
		default:
			throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
		}
	}

	public static void insertBoxInsns(MethodVisitor mv, String type) {
		if (type.length() != 1) {
			return; // not necessary
		}
		insertBoxInsns(mv, type.charAt(0));
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

	// public static void dumpit(String slashedName, byte[] bytes) {
	// try {
	// File f = new File("n:/temp/sl/" +
	// slashedName.substring(slashedName.lastIndexOf("/") + 1) + ""
	// + System.currentTimeMillis() + ".class");
	// FileOutputStream fos = new FileOutputStream(f);
	// fos.write(bytes);
	// fos.close();
	// println("Written " + f.getName());
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

	public static String getInterfaceName(String owner) {
		return owner + "__I";
	}

	public static void assertSlashed(String name) {
		if (name.indexOf(".") != -1) {
			throw new IllegalStateException("Expected a slashed name but was passed " + name);
		}
	}

	public static void assertDotted(String name) {
		if (name.indexOf("/") != -1) {
			throw new IllegalStateException("Expected a dotted name but was passed " + name);
		}
	}

	public static String toOpcodeString(int opcode) {
		switch (opcode) {
		case NOP: // 0
			return "NOP";
		case ACONST_NULL: // 1
			return "ACONST_NULL";
		case ICONST_M1: // 2
			return "ICONST_M1";
		case ICONST_0: // 3
			return "ICONST_0";
		case ICONST_1: // 4
			return "ICONST_1";
		case ICONST_2: // 5
			return "ICONST_2";
		case ICONST_3: // 6
			return "ICONST_3";
		case ICONST_4: // 7
			return "ICONST_4";
		case ICONST_5: // 8
			return "ICONST_5";
		case LCONST_0: // 9
			return "LCONST_0";
		case LCONST_1: // 10
			return "LCONST_1";
		case FCONST_0: // 11
			return "FCONST_0";
		case FCONST_1: // 12
			return "FCONST_1";
		case FCONST_2: // 13
			return "FCONST_2";
		case DCONST_0: // 14
			return "DCONST_0";
		case DCONST_1: // 15
			return "DCONST_1";
		case BIPUSH: // 16
			return "BIPUSH";
		case SIPUSH: // 17
			return "SIPUSH";
		case ILOAD: // 21
			return "ILOAD";
		case LLOAD: // 22
			return "LLOAD";
		case FLOAD: // 23
			return "FLOAD";
		case DLOAD: // 24
			return "DLOAD";
		case ALOAD: // 25
			return "ALOAD";
		case IALOAD: // 46
			return "IALOAD";
		case LALOAD: // 47
			return "LALOAD";
		case FALOAD: // 48
			return "FALOAD";
		case AALOAD: // 50
			return "AALOAD";
		case IASTORE: // 79
			return "IASTORE";
		case AASTORE: // 83
			return "AASTORE";
		case BASTORE: // 84
			return "BASTORE";
		case POP: // 87
			return "POP";
		case POP2: // 88
			return "POP2";
		case DUP: // 89
			return "DUP";
		case DUP_X1: // 90
			return "DUP_X1";
		case DUP_X2: // 91
			return "DUP_X2";
		case DUP2: // 92
			return "DUP2";
		case DUP2_X1: // 93
			return "DUP2_X1";
		case DUP2_X2: // 94
			return "DUP2_X2";
		case SWAP: // 95
			return "SWAP";
		case IADD: // 96
			return "IADD";
		case LSUB: // 101
			return "LSUB";
		case IMUL: // 104
			return "IMUL";
		case LMUL: // 105
			return "LMUL";
		case FMUL: // 106
			return "FMUL";
		case DMUL: // 107
			return "DMUL";
		case ISHR: // 122
			return "ISHR";
		case IAND: // 126
			return "IAND";
		case I2D: // 135
			return "I2D";
		case L2F: // 137
			return "L2F";
		case L2D: // 138
			return "L2D";
		case I2B: // 145
			return "I2B";
		case I2C: // 146
			return "I2C";
		case I2S: // 147
			return "I2S";
		case IFEQ: // 153
			return "IFEQ";
		case IFNE: // 154
			return "IFNE";
		case IFLT: // 155
			return "IFLT";
		case IFGE: // 156
			return "IFGE";
		case IFGT: // 157
			return "IFGT";
		case IFLE: // 158
			return "IFLE";
		case IF_ICMPEQ: // 159
			return "IF_ICMPEQ";
		case IF_ICMPNE: // 160
			return "IF_ICMPNE";
		case IF_ICMPLT: // 161
			return "IF_ICMPLT";
		case IF_ICMPGE: // 162
			return "IF_ICMPGE";
		case IF_ICMPGT: // 163
			return "IF_ICMPGT";
		case IF_ICMPLE: // 164
			return "IF_ICMPLE";
		case IF_ACMPEQ: // 165
			return "IF_ACMPEQ";
		case IF_ACMPNE: // 166
			return "IF_ACMPNE";
		case GOTO: // 167
			return "GOTO";
		case IRETURN: // 172
			return "IRETURN";
		case LRETURN: // 173
			return "LRETURN";
		case FRETURN: // 174
			return "FRETURN";
		case DRETURN: // 175
			return "DRETURN";
		case ARETURN: // 176
			return "ARETURN";
		case RETURN: // 177
			return "RETURN";
		case INVOKEVIRTUAL: // 182
			return "INVOKEVIRTUAL";
		case INVOKESPECIAL: // 183
			return "INVOKESPECIAL";
		case INVOKESTATIC: // 184
			return "INVOKESTATIC";
		case INVOKEINTERFACE: // 185
			return "INVOKEINTERFACE";
		case NEWARRAY: // 188
			return "NEWARRAY";
		case ANEWARRAY: // 189
			return "ANEWARRAY";
		case ARRAYLENGTH: // 190
			return "ARRAYLENGTH";
		case ATHROW: // 191
			return "ATHROW";
		case CHECKCAST: // 192
			return "CHECKCAST";
		case IFNULL: // 198
			return "IFNULL";
		case IFNONNULL: // 199
			return "IFNONNULL";
		default:
			throw new IllegalArgumentException("NYI " + opcode);
		}
	}

	/**
	 * Create a descriptor for some set of parameter types. The descriptor will be of the form "([Ljava/lang/String;)"
	 * 
	 * @param params the (possibly null) list of parameters for which to create the descriptor
	 * @return a descriptor or "()" for no parameters
	 */
	public static String toParamDescriptor(Class<?>... params) {
		if (params == null) {
			return "()";
		}
		StringBuilder s = new StringBuilder("(");
		for (Class<?> param : params) {
			appendDescriptor(param, s);
		}
		s.append(")");
		return s.toString();
	}

	/**
	 * Given a method descriptor, extract the parameter descriptor and convert into corresponding Class objects. This requires a
	 * reference to a class loader to convert type names into Class objects.
	 */
	public static Class<?>[] toParamClasses(String methodDescriptor, ClassLoader classLoader) throws ClassNotFoundException {
		Type[] paramTypes = Type.getArgumentTypes(methodDescriptor);
		Class<?>[] paramClasses = new Class<?>[paramTypes.length];
		for (int i = 0; i < paramClasses.length; i++) {
			paramClasses[i] = toClass(paramTypes[i], classLoader);
		}
		return paramClasses;
	}

	/**
	 * Convert an asm Type into a corresponding Class object, requires a reference to a ClassLoader to be able to convert classnames
	 * to class objects.
	 */
	public static Class<?> toClass(Type type, ClassLoader classLoader) throws ClassNotFoundException {
		switch (type.getSort()) {
		case Type.VOID:
			return void.class;
		case Type.BOOLEAN:
			return boolean.class;
		case Type.CHAR:
			return char.class;
		case Type.BYTE:
			return byte.class;
		case Type.SHORT:
			return short.class;
		case Type.INT:
			return int.class;
		case Type.FLOAT:
			return float.class;
		case Type.LONG:
			return long.class;
		case Type.DOUBLE:
			return double.class;
		case Type.ARRAY:
			Class<?> clazz = toClass(type.getElementType(), classLoader);
			return Array.newInstance(clazz, 0).getClass();
		default:
			// case OBJECT:
			return Class.forName(type.getClassName(), false, classLoader);
		}
	}

	/**
	 * Construct the method descriptor for a method. For example 'String bar(int)' would return '(I)Ljava/lang/String;'. If the
	 * first parameter is skipped, the leading '(' is also skipped (the caller is expect to build the right prefix).
	 * 
	 * @param method method for which the descriptor should be created
	 * @param ignoreFirstParameter whether to include the first parameter in the output descriptor
	 * @return a method descriptor
	 */
	public static String toMethodDescriptor(Method method, boolean ignoreFirstParameter) {
		Class<?>[] params = method.getParameterTypes();
		if (ignoreFirstParameter && params.length < 1) {
			throw new IllegalStateException("Cannot ignore the first parameter when there are none.  method=" + method);
		}
		StringBuilder s = new StringBuilder();
		if (!ignoreFirstParameter) {
			s.append("(");
		}
		for (int i = (ignoreFirstParameter ? 1 : 0), max = params.length; i < max; i++) {
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

	/**
	 * Create the string representation of an integer and pad it to a particular width using leading zeroes.
	 * 
	 * @param value the value to convert to a string
	 * @param width the width (in chars) that the resultant string should be
	 * @return the padded string
	 */
	public static String toPaddedNumber(int value, int width) {
		StringBuilder s = new StringBuilder("00000000").append(Integer.toString(value));
		return s.substring(s.length() - width);
	}

	public static String toMethodDescriptor(Method m) {
		return toMethodDescriptor(m, false);
	}

//	/**
//	 * Access the specified class as a resource accessible through the specified loader and return the bytes. The classname should
//	 * be 'dot' separated (eg. com.foo.Bar) and not suffixed .class
//	 * 
//	 * @param loader the classloader against which getResourceAsStream() will be invoked
//	 * @param dottedclassname the dot separated classname without .class suffix
//	 * @return the byte data defining that class
//	 */
//	public static byte[] loadClassAsBytes(ClassLoader loader, String dottedclassname) {
//		if (GlobalConfiguration.assertsOn) {
//			if (dottedclassname.endsWith(".class")) {
//				throw new IllegalStateException(".class suffixed name should not be passed:" + dottedclassname);
//			}
//			if (dottedclassname.indexOf('/') != -1) {
//				throw new IllegalStateException("Should be a dotted name, no slashes:" + dottedclassname);
//			}
//		}
//		InputStream is = loader.getResourceAsStream(dottedclassname.replace('.', '/') + ".class");
//		if (is == null) {
//			throw new UnableToLoadClassException(dottedclassname);
//		}
//		return Utils.loadBytesFromStream(is);
//	}

//	/**
//	 * Access the specified class as a resource accessible through the specified loader and return the bytes. The classname should
//	 * be 'dot' separated (eg. com.foo.Bar) and not suffixed .class
//	 * 
//	 * @param loader the classloader against which getResourceAsStream() will be invoked
//	 * @param slashedclassname the dot separated classname without .class suffix
//	 * @return the byte data defining that class
//	 */
//	public static byte[] loadClassAsBytes2(ClassLoader loader, String slashedclassname) {
//		if (GlobalConfiguration.assertsOn) {
//			if (slashedclassname.endsWith(".class")) {
//				throw new IllegalStateException(".class suffixed name should not be passed:" + slashedclassname);
//			}
//			if (slashedclassname.indexOf('.') != -1) {
//				throw new IllegalStateException("Should be a slashed name, no dots:" + slashedclassname);
//			}
//		}
//		InputStream is = loader.getResourceAsStream(slashedclassname + ".class");
//		if (is == null) {
//			throw new UnableToLoadClassException(slashedclassname);
//		}
//		return Utils.loadBytesFromStream(is);
//	}

	/**
	 * Load all the byte data from an input stream.
	 * 
	 * @param stream thr input stream from which to read
	 * @return a byte array containing all the data from the stream
	 */
	public static byte[] loadBytesFromStream(InputStream stream) {
		try {
			BufferedInputStream bis = new BufferedInputStream(stream);
			byte[] theData = new byte[10000000];
			int dataReadSoFar = 0;
			byte[] buffer = new byte[1024];
			int read = 0;
			while ((read = bis.read(buffer)) != -1) {
				System.arraycopy(buffer, 0, theData, dataReadSoFar, read);
				dataReadSoFar += read;
			}
			bis.close();
			// Resize to actual data read
			byte[] returnData = new byte[dataReadSoFar];
			System.arraycopy(theData, 0, returnData, 0, dataReadSoFar);
			return returnData;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void assertTrue(boolean condition, String detail) {
		if (!condition) {
			throw new IllegalStateException("Assertion violated: " + detail);
		}
	}

	public static String getDispatcherName(String name, String versionstamp) {
		StringBuilder s = new StringBuilder(name);
		s.append("$$D");
		s.append(versionstamp);
		return s.toString();
	}

	/**
	 * Generate the name for the executor class. Must use '$' so that it is considered by some code (eclipse debugger for example)
	 * to be an inner type of the original class (thus able to consider itself as being from the same source file).
	 */
	public static String getExecutorName(String name, String versionstamp) {
		StringBuilder s = new StringBuilder(name);
		s.append("$$E");
		s.append(versionstamp);
		return s.toString();
	}

//	/**
//	 * Strip the first parameter out of a method descriptor and return the shortened method descriptor. Since primitive types cannot
//	 * be reloadable, there is no need to handle that case - it should always be true that the first parameter will exist and will
//	 * end with a semi-colon. For example: (Ljava/lang/String;II)V becomes (IIV)
//	 * 
//	 * @param descriptor method descriptor to be shortened
//	 * @return new version of input descriptor with first parameter taken out
//	 */
//	public static String stripFirstParameter(String descriptor) {
//		if (GlobalConfiguration.assertsOn) {
//			if (descriptor.indexOf(';') == -1) {
//				throw new IllegalStateException("Input descriptor must have at least one parameter: " + descriptor);
//			}
//		}
//		StringBuilder r = new StringBuilder();
//		r.append('(');
//		r.append(descriptor, descriptor.indexOf(';') + 1, descriptor.length());
//		return r.toString();
//	}
//
//	/**
//	 * Discover the descriptor for the return type. It may be a primitive (so one char) or a reference type (so a/b/c, with no 'L'
//	 * or ';') or it may be an array descriptor ([Ljava/lang/String;).
//	 * 
//	 * @param methodDescriptor method descriptor
//	 * @return return type descriptor (with any 'L' or ';' trimmed off)
//	 */
//	public static ReturnType getReturnTypeDescriptor(String methodDescriptor) {
//		int index = methodDescriptor.indexOf(')') + 1;
//		if (methodDescriptor.charAt(index) == 'L') {
//			return new ReturnType(methodDescriptor.substring(index + 1, methodDescriptor.length() - 1), Kind.REFERENCE);
//		} else {
//			return new ReturnType(methodDescriptor.substring(index), methodDescriptor.charAt(index) == '[' ? Kind.ARRAY
//					: Kind.PRIMITIVE);
//		}
//	}

	public static class ReturnType {
		public final String descriptor;
		public final Kind kind;

		public static final ReturnType ReturnTypeVoid = new ReturnType("V", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeFloat = new ReturnType("F", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeBoolean = new ReturnType("Z", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeShort = new ReturnType("S", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeInt = new ReturnType("I", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeChar = new ReturnType("C", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeByte = new ReturnType("B", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeDouble = new ReturnType("D", Kind.PRIMITIVE);
		public static final ReturnType ReturnTypeLong = new ReturnType("J", Kind.PRIMITIVE);

		/**
		 * Descriptor for a reference type has already been stripped of L and ;
		 * 
		 * @param descriptor descriptor, either one char for a primitive or slashed name for a reference type or [La/b/c; for array
		 *        type
		 * @param kind one of primitive, array or reference
		 */
		private ReturnType(String descriptor, Kind kind) {
			this.descriptor = descriptor;
//			if (GlobalConfiguration.assertsOn) {
//				if (this.kind == Kind.REFERENCE) {
//					if (descriptor.endsWith(";") && !descriptor.startsWith("[")) {
//						throw new IllegalStateException("Should already have been stripped of 'L' and ';': " + descriptor);
//					}
//				}
//			}
			this.kind = kind;
		}

		public static ReturnType getReturnType(String descriptor, Kind kind) {
			if (kind == Kind.PRIMITIVE) {
				switch (descriptor.charAt(0)) {
				case 'V':
					return ReturnTypeVoid;
				case 'F':
					return ReturnTypeFloat;
				case 'Z':
					return ReturnTypeBoolean;
				case 'S':
					return ReturnTypeShort;
				case 'I':
					return ReturnTypeInt;
				case 'B':
					return ReturnTypeByte;
				case 'C':
					return ReturnTypeChar;
				case 'J':
					return ReturnTypeLong;
				case 'D':
					return ReturnTypeDouble;
				default:
					throw new IllegalStateException(descriptor);
				}
			} else {
				return new ReturnType(descriptor, kind);
			}
		}

		public enum Kind {
			PRIMITIVE, ARRAY, REFERENCE
		}

		public boolean isVoid() {
			return kind == Kind.PRIMITIVE && descriptor.charAt(0) == 'V';
		}

		public boolean isPrimitive() {
			return kind == Kind.PRIMITIVE;
		}

		public boolean isDoubleSlot() {
			if (kind == Kind.PRIMITIVE) {
				char ch = descriptor.charAt(0);
				return ch == 'J' || ch == 'L';
			}
			return false;
		}

		public static ReturnType getReturnType(String descriptor) {
			if (descriptor.length() == 1) {
				return getReturnType(descriptor, Kind.PRIMITIVE);
			} else {
				char ch = descriptor.charAt(0);
				if (ch == 'L') {
					String withoutLeadingLorTrailingSemi = descriptor.substring(1, descriptor.length() - 1);
					return ReturnType.getReturnType(withoutLeadingLorTrailingSemi, Kind.REFERENCE);
				} else {
					// must be an array!
//					if (GlobalConfiguration.assertsOn) {
//						Utils.assertTrue(ch == '[', "Expected array leading char: " + descriptor);
//					}
					return ReturnType.getReturnType(descriptor, Kind.ARRAY);
				}
			}
		}
	}

	public static String insertExtraParameter(String classname, String descriptor) {
		StringBuilder r = new StringBuilder("(L");
		r.append(classname).append(';');
		r.append(descriptor, 1, descriptor.length());
		return r.toString();
	}

	/**
	 * Generate the instructions in the specified method visitor to unpack an assumed array (on top of the stack) according to the
	 * specified descriptor. For example, if the descriptor is (I)V then the array contains a single Integer that must be unloaded
	 * from the array then unboxed to an int.
	 * 
	 * @param mv the method visitor to receive the unpack instructions
	 * @param toCallDescriptor the descriptor for the method whose parameters describe the array contents
	 */
	public static void generateInstructionsToUnpackArrayAccordingToDescriptor(MethodVisitor mv, String toCallDescriptor,
			int arrayVariableIndex) {
		int arrayIndex = 0;
		int descriptorIndex = 1;
		char ch;
		while ((ch = toCallDescriptor.charAt(descriptorIndex)) != ')') {
			mv.visitVarInsn(ALOAD, arrayVariableIndex);
			mv.visitLdcInsn(arrayIndex++);
			mv.visitInsn(AALOAD);
			switch (ch) {
			case 'L':
				int semicolon = toCallDescriptor.indexOf(';', descriptorIndex + 1);
				String descriptor = toCallDescriptor.substring(descriptorIndex + 1, semicolon);
				if (!descriptor.equals("java/lang/Object")) {
					mv.visitTypeInsn(CHECKCAST, descriptor);
				}
				descriptorIndex = semicolon + 1;
				break;
			case '[':
				int idx = descriptorIndex;
				// maybe a primitive array or an reference type array
				while (toCallDescriptor.charAt(++descriptorIndex) == '[') {
				}
				// next char is either a primitive or L
				if (toCallDescriptor.charAt(descriptorIndex) == 'L') {
					int semicolon2 = toCallDescriptor.indexOf(';', descriptorIndex + 1);
					descriptorIndex = semicolon2 + 1;
					mv.visitTypeInsn(CHECKCAST, toCallDescriptor.substring(idx, semicolon2 + 1));
				} else {
					mv.visitTypeInsn(CHECKCAST, toCallDescriptor.substring(idx, descriptorIndex + 1));
					descriptorIndex++;
				}
				break;
			case 'I':
			case 'Z':
			case 'S':
			case 'F':
			case 'J':
			case 'D':
			case 'C':
			case 'B':
				Utils.insertUnboxInsns(mv, ch, true);
				descriptorIndex++;
				break;
			default:
				throw new IllegalStateException("Unexpected type descriptor character: " + ch);
			}
		}
	}

	public static boolean isInitializer(String membername) {
		return membername.charAt(0) == '<';
	}

	public static int toCombined(int typeRegistryId, int classId) {
		return (typeRegistryId << 16) + classId;
	}

//	public static void logAndThrow(Logger log, String message) {
//		if (GlobalConfiguration.logging && log.isLoggable(Level.SEVERE)) {
//			log.log(Level.SEVERE, message);
//		}
//		throw new ReloadException(message);
//	}

	/**
	 * Dump the specified bytes under the specified name in the filesystem. If the location hasn't been configured then
	 * File.createTempFile() is used to determine where the file will be put.
	 * 
	 * @param slashname
	 * @param bytesLoaded
	 * @return the path to the file
	 */
	public static String dump(String info, String slashname, byte[] bytesLoaded) {
		if (slashname.indexOf('.') != -1) {
			throw new IllegalStateException("Slashed type name expected, not '" + slashname + "'");
		}
		String dir = "";
		if (slashname.indexOf('/') != -1) {
			dir = slashname.substring(0, slashname.lastIndexOf('/'));
		}
		String dumplocation = null;
		try {
			File tempfile = null;
			tempfile = new File("/tmp");//File.createTempFile("sl_", null);
			tempfile.delete();
			File f = new File(tempfile, dir);
			f.mkdirs();
			dumplocation = tempfile + File.separator + slashname + ".class";
			System.out.println("Expression '"+info+"' compiled code dumped to " + dumplocation);
			f = new File(dumplocation);
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytesLoaded);
			fos.flush();
			fos.close();
			return f.toString();
		} catch (IOException ioe) {
			throw new IllegalStateException("Unexpected problem dumping class " + slashname + " into " + dumplocation, ioe);
		}
	}

	/**
	 * Return the size of a type. The size is usually 1 except for double and long which are of size 2. The descriptor passed in is
	 * the full descriptor, including any leading 'L' and trailing ';'.
	 * 
	 * @param typeDescriptor the descriptor for a single type, may be primitive. For example: I, J, Z, Ljava/lang/String;
	 * @return the size of the descriptor (number of slots it will consume), either 1 or 2
	 */
	public static int sizeOf(String typeDescriptor) {
		if (typeDescriptor.length() != 1) {
			return 1;
		}
		char ch = typeDescriptor.charAt(0);
		if (ch == 'J' || ch == 'D') {
			return 2;
		} else {
			return 1;
		}
	}

	/**
	 * Dump some bytes into the specified file.
	 * 
	 * @param file full filename for where to dump the stuff (e.g. c:/temp/Foo.class)
	 * @param bytes the bytes to write to the file
	 */
	public static void dumpClass(String file, byte[] bytes) {
		File f = new File(file);
		try {
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(bytes);
			fos.flush();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static String toSuperAccessor(String typename, String name) {
		StringBuilder s = new StringBuilder();
		s.append("__super$");
		int idx = typename.lastIndexOf('/');
		if (idx == -1) {
			s.append(typename);
		} else {
			s.append(typename.substring(idx + 1));
		}
		s.append('$');
		s.append(name);
		return s.toString();
	}

	/**
	 * Compute the size required for a specific method descriptor.
	 * 
	 * @param descriptor a method descriptor, for example (Ljava/lang/String;ZZ)V
	 * @return number of stack/var entries necessary for that descriptor
	 */
	public static int getSize(String descriptor) {
		int size = 0;
		int descriptorpos = 1; // start after the '('
		char ch;
		while ((ch = descriptor.charAt(descriptorpos)) != ')') {
			switch (ch) {
			case '[':
				size++;
				// jump to end of array, could be [[[[I
				while (descriptor.charAt(++descriptorpos) == '[') {
					;
				}
				if (descriptor.charAt(descriptorpos) == 'L') {
					descriptorpos = descriptor.indexOf(';', descriptorpos) + 1;
				} else {
					// Just a primitive array
					descriptorpos++;
				}
				break;
			case 'L':
				size++;
				// jump to end of 'L' signature
				descriptorpos = descriptor.indexOf(';', descriptorpos) + 1;
				break;
			case 'J':
				size = size + 2;
				descriptorpos++;
				break;
			case 'D':
				size = size + 2;
				descriptorpos++;
				break;
			case 'F':
			case 'B':
			case 'S':
			case 'I':
			case 'Z':
			case 'C':
				size++;
				descriptorpos++;
				break;
			default:
				throw new IllegalStateException("Unexpected character in descriptor: " + ch);
			}
		}
		return size;
	}

	public static Class<?>[] slashedNamesToClasses(String[] slashedNames, ClassLoader classLoader) throws ClassNotFoundException {
		Class<?>[] classes = new Class<?>[slashedNames.length];
		for (int i = 0; i < slashedNames.length; i++) {
			classes[i] = slashedNameToClass(slashedNames[i], classLoader);
		}
		return classes;
	}

	public static Class<?> slashedNameToClass(String slashedName, ClassLoader classLoader) throws ClassNotFoundException {
		return Class.forName(slashedName.replace('/', '.'), false, classLoader);
	}

	/**
	 * @return true if the possiblyBoxedType is the boxed form of the primitive
	 */
	public static boolean isObjectIsUnboxableTo(Class<?> possiblyBoxedType, char primitive) {
		switch (primitive) {
		case 'I':
			return possiblyBoxedType == Integer.class;
		case 'S':
			return possiblyBoxedType == Short.class;
		case 'J':
			return possiblyBoxedType == Long.class;
		case 'F':
			return possiblyBoxedType == Float.class;
		case 'Z':
			return possiblyBoxedType == Boolean.class;
		case 'C':
			return possiblyBoxedType == Character.class;
		case 'B':
			return possiblyBoxedType == Byte.class;
		case 'D':
			return possiblyBoxedType == Double.class;
		default:
			throw new IllegalStateException("nyi " + possiblyBoxedType + " " + primitive);
		}
	}

	/**
	 * Check that the value we have discovered is of the right type. It may not be if the field has changed type during a reload.
	 * When this happens we will default the value for the new field and forget the one we were holding onto. note: array forms are
	 * not compatible (e.g. int[] and Integer[])
	 * 
	 * @param result the result we have discovered and are about to return - this is never null
	 * @param expectedTypeDescriptor the type we are looking for (will be primitive or Ljava/lang/String style)
	 * @return the result we can return, or null if it is not compatible
	 */
//	public static Object checkCompatibility(TypeRegistry registry, Object result, String expectedTypeDescriptor) {
//		if (GlobalConfiguration.assertsOn) {
//			Utils.assertTrue(result != null, "result should never be null");
//		}
//		String actualType = result.getClass().getName();
//		if (expectedTypeDescriptor.length() == 1
//				&& Utils.isObjectIsUnboxableTo(result.getClass(), expectedTypeDescriptor.charAt(0))) {
//			// boxing is ok
//		} else {
//			if (expectedTypeDescriptor.charAt(0) == 'L') {
//				expectedTypeDescriptor = expectedTypeDescriptor.substring(1, expectedTypeDescriptor.length() - 1).replace('/', '.');
//			}
//			if (!expectedTypeDescriptor.equals(actualType)) {
//				// assignability test
//				if (actualType.charAt(0) == '[' || expectedTypeDescriptor.charAt(0) == '[') {
//					return null;
//				}
//				// In some situations we can't easily see the descriptor for the actualType (e.g. it is loaded by a different, perhaps child, loader)
//				// Let's do something a bit more sophisticated here, we have the type information after all, we don't need to hunt for descriptors:
//				Class<?> actualClazz = result.getClass();
//				if (isAssignableFrom(registry, actualClazz, expectedTypeDescriptor.replace('/', '.'))) {
//					return result;
//				}
//				return null;
//			}
//		}
//		return result;
//	}

//	public static boolean isAssignableFrom(TypeRegistry reg, Class<?> clazz, String lookingFor) {
//		if (clazz == null) {
//			return false;
//		}
//		if (clazz.getName().equals(lookingFor)) {
//			return true;
//		}
//		Class<?>[] intfaces = clazz.getInterfaces();
//		for (Class<?> intface : intfaces) {
//			if (isAssignableFrom(reg, intface, lookingFor)) {
//				return true;
//			}
//		}
//		return isAssignableFrom(reg, clazz.getSuperclass(), lookingFor);
//	}

	/**
	 * Determine if the type specified in lookingFor is a supertype (class/interface) of the specified typedescriptor, i.e. can an
	 * object of type 'candidate' be assigned to a variable of typ 'lookingFor'.
	 * 
	 * @return true if it is a supertype
	 */
//	public static boolean isAssignableFrom(String lookingFor, TypeDescriptor candidate) {
//		String[] interfaces = candidate.getSuperinterfacesName();
//		for (String intface : interfaces) {
//			if (intface.equals(lookingFor)) {
//				return true;
//			}
//			boolean b = isAssignableFrom(lookingFor, candidate.getTypeRegistry().getDescriptorFor(intface));
//			if (b) {
//				return true;
//			}
//		}
//		String supertypename = candidate.getSupertypeName();
//		if (supertypename == null) {
//			return false;
//		}
//		if (supertypename.equals(lookingFor)) {
//			return true;
//		}
//		return isAssignableFrom(lookingFor, candidate.getTypeRegistry().getDescriptorFor(supertypename));
//	}

	public static String toConstructorDescriptor(Class<?>... params) {
		return new StringBuilder(toParamDescriptor(params)).append("V").toString();
	}

	public static boolean isConvertableFrom(Class<?> targetType, Class<?> sourceType) {
		if (targetType.isAssignableFrom(sourceType)) {
			return true;
		} else {
			// The 19 conversions, as per section 5.1.2 in: http://java.sun.com/docs/books/jls/third_edition/html/conversions.html
			if (sourceType == byte.class) {
				if (targetType == short.class || targetType == int.class || targetType == long.class || targetType == float.class
						|| targetType == double.class) {
					return true;
				}
			} else if (sourceType == short.class) {
				if (targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class) {
					return true;
				}
			} else if (sourceType == char.class) {
				if (targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class) {
					return true;
				}
			} else if (sourceType == int.class) {
				if (targetType == long.class || targetType == float.class || targetType == double.class) {
					return true;
				}
			} else if (sourceType == long.class) {
				if (targetType == float.class || targetType == double.class) {
					return true;
				}
			} else if (sourceType == float.class) {
				if (targetType == double.class) {
					return true;
				}
			}
			return false;
		}
	}

}
