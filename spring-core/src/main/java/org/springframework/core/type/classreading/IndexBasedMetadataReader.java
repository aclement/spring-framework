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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassInfo.NestingType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;


/**
 * 
 * @author andy
 * @since 5.2
 */
public class IndexBasedMetadataReader implements MetadataReader {

	public final static boolean DEBUG = false;

	private Index index;
	private Resource resource;
	private ClassMetadata cmd;
	private AnnotationMetadata amd;
	private String classname;
	private MetadataReader metadataReader;

	public IndexBasedMetadataReader(Index index, String classname, Resource resource, MetadataReader metadataReader) {
		this.index = index;
		this.classname = classname;
		this.resource = resource;
		this.metadataReader = metadataReader;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public ClassMetadata getClassMetadata() {
		if (this.cmd == null) compute();
		return this.cmd;
	}

	@Override
	public AnnotationMetadata getAnnotationMetadata() {
		if (this.amd == null) compute();
		return this.amd;
	}
	
	Map<DotName, List<ClassInfo>> inners = new HashMap<>();

	private void compute() {
		// System.out.println("IBMR for "+classname+"     resource="+resource);
		Collection<ClassInfo> knownClasses = index.getKnownClasses();
		long stime = System.currentTimeMillis();
		// TODO am i blind? Why are these not available directly in the index on ClassInfo objects?
		for (ClassInfo ci: knownClasses) {
			if (ci.nestingType()==NestingType.INNER) {
				DotName outer = ci.enclosingClass();
				List<ClassInfo> ilist = inners.get(outer);
				if (ilist==null) {
					ilist = new ArrayList<>();
					inners.put(outer, ilist);
				}
				ilist.add(ci);
			}
		}
		// System.out.println("Time to compute inners: "+(System.currentTimeMillis()-stime)+"ms");
		DotName dn = DotName.createSimple(classname);
		ClassInfo classByName = index.getClassByName(dn);
		if (classByName == null) {
			throw new IllegalStateException("Why was that not in the index? "+classname+"   (resource="+resource+")");
		}
		this.cmd = new IndexBackedClassMetadata(classByName,inners,index,metadataReader==null?null:metadataReader.getClassMetadata());
		this.amd = new IndexBackedAnnotationMetaData(classByName,index,inners,metadataReader==null?null:metadataReader.getAnnotationMetadata());
	}

}
