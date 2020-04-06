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

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.springframework.core.type.ClassMetadata;

/**
 * 
 * @author andy
 * @since 5.2
 */
public class IndexBackedClassMetadata implements ClassMetadata {

	private ClassInfo ci;
	private Index index;
	private Map<DotName, List<ClassInfo>> inners;
	private ClassMetadata verifier;

	public IndexBackedClassMetadata(ClassInfo classByName, Map<DotName, List<ClassInfo>> inners, Index index, ClassMetadata verifier) {
		this.ci = classByName;
		this.inners = inners;
		this.index = index;
		this.verifier = verifier;
	}

	@Override
	public String getClassName() {
		String result = ci.name().toString();
		if (IndexBasedMetadataReader.DEBUG) System.out.println("IBCMD: returning " + result);
		if (verifier!=null && !result.equals(verifier.getClassName())) {
			throw new IllegalStateException(
					"CMD.getClassName() " + result + "!=" + verifier.getClassName());
		}
		return result;
	}

	@Override
	public boolean isInterface() {
		boolean result = Modifier.isInterface(ci.flags());
		if (verifier!=null && result != verifier.isInterface()) {
			throw new IllegalStateException(
					"CMD.isInterface() " + result + "!=" + verifier.isInterface());
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isFinal() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean isIndependent() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getEnclosingClassName() {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public String getSuperClassName() {
		String result= ci.superName().toString();
			if (verifier!=null && !(result.equals(verifier.getSuperClassName()) || 
				(result.equals("java.lang.Object") && verifier.getSuperClassName()==null))) {
				throw new IllegalStateException("IBCMD.getSuperClassName() of "+ci.name()+" computed="+result+" original="+verifier.getSuperClassName());
			}
			return result;
	}

	@Override
	public String[] getInterfaceNames() {
			List<DotName> inames = ci.interfaceNames();
			String[] result = new String[inames.size()];
			for (int i=0;i<result.length;i++) {
				result[i]= inames.get(i).toString();
			}
			if (verifier!=null && !Arrays.asList(result).toString().equals(Arrays.asList(verifier.getInterfaceNames()).toString())) {
				throw new IllegalStateException("IBCMD.getInterfaceNames("+getClassName()+") :"+Arrays.asList(inames)+" "+Arrays.asList(verifier.getInterfaceNames()));
			}
			return result; 
	}

//	[org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheConfigurationImportSelector, 
//	org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheManagerEntityManagerFactoryDependsOnPostProcessor, 
//	org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheManagerValidator]!=
//	[org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheConfigurationImportSelector, 
//	 org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheManagerValidator, 
//	 org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration$CacheManagerEntityManagerFactoryDependsOnPostProcessor]
	@Override
	public String[] getMemberClassNames() { 
		List<ClassInfo> list = inners.get(ci.name());

		String[] result = (list==null||list.size()==0)?new String[0]:new String[list.size()];
		if (list != null) {
		for (int i=0;i<list.size();i++) {
			result[i]=list.get(i).name().toString();
		}
		}
		if (IndexBasedMetadataReader.DEBUG) System.out.println("IBCMD: returning " + result);
		if (verifier!=null) {
			//&& !Arrays.asList(result).equals(Arrays.asList(original.getMemberClassNames()))) {
			String originalS = toString(verifier.getMemberClassNames());
			String computedS = toString(result);
			if (!originalS.equals(computedS)) {
			throw new IllegalStateException(
					"IBCMD.getMemberClassNames() on "+ci.name()+":\noriginal=" +originalS + "\ncomputed=" +computedS);
			}
		}
		return result;
	}
	
	private String toString(String[] input) {
		Set<String> s = new TreeSet<>(Arrays.asList(input));
		return s.toString();
	}

}
