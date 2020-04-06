/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type.classreading;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;


/**
 * 
 * @author andy
 * @since 5.2
 */
public class IndexBackedAnnotationMetaData implements AnnotationMetadata {
	
	private ClassInfo ci;
	private Index index;
	private Map<DotName, List<ClassInfo>> inners;

	private AnnotationMetadata verifier;

	public IndexBackedAnnotationMetaData(ClassInfo ci, Index index, Map<DotName, List<ClassInfo>> inners, AnnotationMetadata verifier) {
		this.ci = ci;
		this.index = index;
		this.inners = inners;
		this.verifier = verifier;
	}

	@Override
	public String getClassName() {
		String result = ci.name().toString();
		if (IndexBasedMetadataReader.DEBUG) {
			System.out.println("IBCMD: getClassName() on "+this.ci+" returning " + result+"    ("+verifier+")");
		}
		if (verifier!=null && !result.equals(verifier.getClassName())) {
			throw new IllegalStateException(
					"AMD.getClassName() " + result + "!=" + verifier.getClassName());
		}
		return result;
	}

	@Override
	public boolean isInterface() {
		boolean result = Modifier.isInterface(ci.flags());
		if (verifier!=null && result!=verifier.isInterface()) {
			throw new IllegalStateException(
					"AMD.isInterface() " + result + "!=" + verifier.isInterface());
		}
		return result;
	}

	@Override
	public boolean isAnnotation() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isAbstract() {
		boolean result = Modifier.isAbstract(ci.flags());
		if (verifier!=null && result!=verifier.isAbstract()) {
			throw new IllegalStateException(
					"AMD.isAbstract() " + result + "!=" + verifier.isAbstract());
		}
		return result;
	}

	@Override
	public boolean isFinal() {
		boolean result = Modifier.isFinal(ci.flags());
		if (verifier!=null && result!=verifier.isFinal()) {
			throw new IllegalStateException(
					"AMD.isFinal() " + result + "!=" + verifier.isFinal());
		}
		return result;
	}

	@Override
	public boolean isIndependent() {
		//	return (this.enclosingClassName == null || this.independentInnerClass);
		boolean result = ci.enclosingClass()==null || Modifier.isStatic(ci.flags());
		if (verifier!=null && result!=verifier.isIndependent()) {
			throw new IllegalStateException(
					"AMD.isIndependent() " + result + "!=" + verifier.isIndependent());
		}
		return result;
	}

	@Override
	public String getEnclosingClassName() {
		if (verifier!=null && !ci.enclosingClass().toString().equals(verifier.getEnclosingClassName())) {
			throw new IllegalStateException("IBAMD.getEnclosingClassName: "+ci.enclosingClass()+" "+verifier.getEnclosingClassName());
		}
		return ci.enclosingClass().toString();
	}

	@Override
	public String getSuperClassName() {
		if (verifier!=null && !ci.superName().toString().equals(verifier.getSuperClassName())) {
			throw new IllegalStateException("IBAMD.getSuperClassName: "+ci.superName()+" "+verifier.getSuperClassName());
		}
		return ci.superName().toString();
	}

	@Override
	public String[] getInterfaceNames() {
		List<DotName> inames = ci.interfaceNames();
		String[] result = new String[inames.size()];
		for (int i=0;i<result.length;i++) {
			result[i]= inames.get(i).toString();
		}
		if (verifier!=null && !Arrays.asList(result).toString().equals(Arrays.asList(verifier.getInterfaceNames()).toString())) {
			throw new IllegalStateException("IBAMD.getInterfaceNames("+getClassName()+") :"+Arrays.asList(inames)+" "+Arrays.asList(verifier.getInterfaceNames()));
		}
		return result; 
	}

	@Override
	public String[] getMemberClassNames() {
		List<ClassInfo> list = inners.get(ci.name());
		String[] result = (list==null||list.size()==0)?new String[0]:new String[list.size()];
		if (list != null) {
		for (int i=0;i<list.size();i++) {
			result[i]=list.get(i).name().toString();
		}
		}
		if (IndexBasedMetadataReader.DEBUG) System.out.println("IBAMD.getMemberClassNames(): returning " + result);
		if (verifier!=null && !Arrays.asList(result).equals(Arrays.asList(verifier.getMemberClassNames()))) {
			throw new IllegalStateException(
					"IBAMD.getMemberClassNames() " + Arrays.asList(result) + "!=" + Arrays.asList(verifier.getClassName()));
		}
		return result;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		Collection<AnnotationInstance> annotations = ci.classAnnotations();
		if (IndexBasedMetadataReader.DEBUG) System.out.println(
				"IBAMD.getAnnotations() on " + getClassName() + " = " + annotations);
		MergedAnnotations result = null;
		if (annotations.size() == 0) {
			result = MergedAnnotations.of(Collections.emptyList());
		}
		else {
			result = asMergedAnnotations(ci.classAnnotations());
		}
		if (verifier!=null) {
			MergedAnnotations originalMAS = verifier.getAnnotations();
			String originalString = toString(originalMAS);
			String computedString = toString(result);
			if (!originalString.equals(computedString)) {
				throw new IllegalStateException(
						"IBAMD.getAnnotations() on " + ci.name() + ":\ncomputed="
								+ computedString + "\noriginal=" + originalString);
			}
		}
		return result;
	}

	private static String toString(MergedAnnotations mas) {
		StringBuilder s = new StringBuilder("MAS:\n");
		SortedSet<String> ss = new TreeSet<>();
		Iterator<MergedAnnotation<Annotation>> i = mas.iterator();
		while (i.hasNext()) {
			MergedAnnotation<Annotation> ma = i.next();
			ss.add(ma.getType()+"\n"); // TODO skipping values in toString for now - does it cause type resolution? which may fail for COC
		}
		s.append(ss.toString());
		return s.toString();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MergedAnnotation asMergedAnnotation(AnnotationInstance ai) {
		MergedAnnotation ma = null;
		try {
			Class annoClass = ClassUtils.forName(ai.name().toString(), null);
			List<AnnotationValue> values = ai.values();
			Map<String, ?> attributes = new HashMap<>();
			for (AnnotationValue av : values) {
				insert(av, attributes);
			}
			ma = MergedAnnotation.of(annoClass, attributes);
		}
		catch (ClassNotFoundException cnfe) {
			throw new IllegalStateException("well... crap... " + ai.name(),
					cnfe);
		}
		return ma;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MergedAnnotations asMergedAnnotations(Map<DotName, List<AnnotationInstance>> annotations) {
		List<MergedAnnotation<?>> mas = new ArrayList<>();
		for (Map.Entry<DotName, List<AnnotationInstance>> anno: annotations.entrySet()) {
			for (AnnotationInstance annoInstance: anno.getValue()) {
				try {
				Class annoClass = ClassUtils.forName(annoInstance.name().toString(),null);
				List<AnnotationValue> values = annoInstance.values();
				Map<String,?> attributes = new HashMap<>();
				for (AnnotationValue av: values) {
					insert(av,attributes);
				}
				mas.add(MergedAnnotation.of(annoClass,attributes));
				} catch (ClassNotFoundException cnfe) {
					throw new IllegalStateException("well... crap... "+annoInstance.name(),cnfe);
				}
			}
		}
		return MergedAnnotations.of(mas);	
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private MergedAnnotations asMergedAnnotations(
			Collection<AnnotationInstance> annotations) {
		List<MergedAnnotation<?>> mas = new ArrayList<>();
		for (AnnotationInstance annoInstance : annotations) {
			String aname = annoInstance.name().toString();
			if (aname.startsWith("java.")) continue; // skip java annotations
			try {
				Class annoClass = ClassUtils.forName(annoInstance.name().toString(),
						null);
				List<AnnotationValue> values = annoInstance.values();
				Map<String, ?> attributes = new HashMap<>();
				for (AnnotationValue av : values) {
					insert(av, attributes);
				}
				mas.add(MergedAnnotation.of(annoClass, attributes));
			}
			catch (ClassNotFoundException cnfe) {
				throw new IllegalStateException("well... crap... " + annoInstance.name(),
						cnfe);
			}
		}
		return MergedAnnotations.of(mas);
	}


	@SuppressWarnings("unchecked")
	void insert(AnnotationValue av,@SuppressWarnings("rawtypes") Map attrs) { 
		switch (av.kind()) {
			case STRING:
				attrs.put(av.name(), av.asString());
				break;
			case BOOLEAN:
				attrs.put(av.name(), av.asBoolean());
				break;
			case INTEGER:
				attrs.put(av.name(), av.asInt());
				break;
			case ARRAY:
				switch (av.componentKind()) {
					case STRING:
						attrs.put(av.name(), av.asStringArray());
						break;
					case CLASS:
						Type[] ts = av.asClassArray();
						List<String> cs = new ArrayList<>();
						for (int i=0;i<ts.length;i++) {
							//try {
							//Class<?> clazz = ClassUtils.resolveClassName(ts[i].name().toString(),null);
							//cs.add(clazz);
							//} catch (IllegalArgumentException iae) {
							//	System.out.println("IBAMD: unable to resolve class ref in annotation: "+ts[i].name());
							//}
							cs.add(ts[i].name().toString());
						}
						attrs.put(av.name(), cs.toArray(new String[0]));
						break;
					case ENUM:
						DotName[] enumType = av.asEnumTypeArray();
						try {
							@SuppressWarnings("rawtypes")
							Class c = ClassUtils.resolveClassName(enumType[0].toString(),
									null);
							String[] asEnumArray = av.asEnumArray();
							@SuppressWarnings("rawtypes")
							Enum[] r = (Enum[]) Array.newInstance(c, asEnumArray.length);
							for (int i = 0; i < asEnumArray.length; i++) {
								@SuppressWarnings("rawtypes")
								Enum valueOf = Enum.valueOf(c, asEnumArray[i]);
								r[i] = valueOf;
							}
							attrs.put(av.name(), r);
						}
						catch (Throwable t) {
							throw new IllegalStateException(
									"IBAMD: unable to resolve enum class and value (array): "
											+ enumType + ":" + av.asEnum(),
									t);
						}
						break;
					default:
						throw new IllegalStateException(
								"Dont handle component type: " + av.componentKind());
				}
				break;
				
			case ENUM:
				DotName asEnumType = av.asEnumType();
				try {
				@SuppressWarnings("rawtypes")
				Class c = ClassUtils.resolveClassName(asEnumType.toString(), null);
				@SuppressWarnings("rawtypes")
				Enum valueOf = Enum.valueOf(c, av.asEnum());
				attrs.put(av.name(), valueOf);
				} catch (Throwable t) {
					throw new IllegalStateException("IBAMD: unable to resolve enum class and value : "+asEnumType+":"+av.asEnum(),t);
				}
				break;
			case CLASS:
				// TODO what about array type class refs?
				// TODO safe to resolve class????
				//try {
				//Class<?> resolveClassName = ClassUtils.resolveClassName(av.asClass().name().toString(), null);
				attrs.put(av.name(), av.asClass().name().toString());
				/*
		} catch (IllegalArgumentException iae) {
			System.out.println("IBAMD: unable to resolve class ref in annotation: "+av.asClass().name());
		}
		*/
				break;
				
				default:
					throw new IllegalStateException("Dont handle: "+av.kind());
		}
	}
	
	/*
	MergedAnnotation<A> annotation = MergedAnnotation.of(
			this.classLoader, this.source, this.annotationType, this.attributes);
			*/

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		List<MethodInfo> mis = ci.methods();
		Set<MethodMetadata> result = new HashSet<>();
		for (MethodInfo mi: mis) {
			if (isAnnotated(mi,annotationName)) {
				result.add(asMethodMetadata(mi));
			}
		}
		if (verifier!=null) {
			Set<MethodMetadata> annotatedMethods = verifier.getAnnotatedMethods(annotationName);
			String original = toString(annotatedMethods);
			String computed = toString(result);
			if (!original.equals(computed)) {
				throw new IllegalStateException("IBAMD.getAnnotatedMethods("+annotationName+") on "+ci.name()+":\noriginal="+original+"\ncomputed="+computed);
			}
		}
		return result;
	}


	private boolean isAnnotated(MethodInfo mi, String annotationName) {
		DotName dn = DotName.createSimple(annotationName);
		List<AnnotationInstance> annotations = mi.annotations();
		for (AnnotationInstance ai: annotations) {
			boolean found = search(dn, ai, new HashSet<>());
			if (found) {
				return true;
			}
		}
		return false;
	}

	private boolean search(DotName dn, AnnotationInstance ai, HashSet<DotName> hashSet) {
		if (hashSet.add(ai.name())) {
			if (dn.equals(ai.name())) {
				return true;
			}
			ClassInfo annotationType = index.getClassByName(ai.name());
			if (annotationType == null) {
				// e.g. Target (because we didn't index rt.jar)
				if (IndexBasedMetadataReader.DEBUG) System.out.println("index did not seem to contain "+ai.name());
			} else {
			Collection<AnnotationInstance> classAnnotations = annotationType.classAnnotations();
			for (AnnotationInstance anno: classAnnotations) {
				boolean b = search(dn,anno,hashSet);
				if (b) {
				  return true;	
				}
			}
			}
		}
		return false;
	}

	private MethodMetadata asMethodMetadata(MethodInfo mi) {
		return new SimpleMethodMetadata(mi.name(), mi.flags(), mi.declaringClass().name().toString(), mi.returnType().name().toString(), 
				asMergedAnnotations(mi.annotations()));
	}
	
	
	private String toString(Set<MethodMetadata> s) {
		StringBuilder buf = new StringBuilder();
		Set<String> sorted = new TreeSet<>();
		for (MethodMetadata mmd: s) {
			//TODO missing flags
			sorted.add(mmd.getMethodName()+mmd.getDeclaringClassName()+mmd.getReturnTypeName()+toString(mmd.getAnnotations()));
		}
		buf.append(sorted);
		return buf.toString();
		
	}

}
