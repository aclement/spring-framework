/*
 * Copyright 2020 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 
 * @author Andy Clement
 * @since 5.2
 * TODO jdoc
 */
public class IndexBasedMetadataReaderFactory implements MetadataReaderFactory {

	public final static boolean VERIFY_BEHAVIOUR = true;

	private final static String ANNOTATION_INDEX_RESOURCE_LOCATION = "META-INF/spring.annotation.index";	

	private final ResourceLoader resourceLoader;
	private static Index index;
	private final MetadataReaderFactory verifier;

	/**
	 * Create a new IndexBasedMetadataReaderFactory for the default class loader.
	 */
	public IndexBasedMetadataReaderFactory() {
		this.resourceLoader = new DefaultResourceLoader();
		initIndex(resourceLoader);
		// index = loadIndex(resourceLoader);
		if (VERIFY_BEHAVIOUR) {
			this.verifier = new CachingMetadataReaderFactory(resourceLoader);
		}
		if (IndexBasedMetadataReader.DEBUG) System.out.println("New IBMRF instance for "+resourceLoader);
	}
	
	public static void initIndex(ResourceLoader resourceLoader) {
		if (index == null) {
			synchronized(IndexBasedMetadataReaderFactory.class) {
				if (index == null) 
				index = loadIndex(resourceLoader);
			}
		}
	}

	/**
	 * Create a new IndexBasedMetadataReaderFactory for the given resource loader.
	 * @param resourceLoader the Spring ResourceLoader to use
	 * (also determines the ClassLoader to use)
	 */
	public IndexBasedMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
		initIndex(resourceLoader);
		//index = loadIndex(resourceLoader);
		if (VERIFY_BEHAVIOUR) {
			this.verifier = new CachingMetadataReaderFactory(resourceLoader);
		}
		if (IndexBasedMetadataReader.DEBUG) System.out.println("New IBMRF instance for "+resourceLoader);
	}

	/**
	 * Create a new IndexBasedMetadataReaderFactory for the given class loader.
	 * @param classLoader the ClassLoader to use
	 */
	public IndexBasedMetadataReaderFactory(@Nullable ClassLoader classLoader) {
		this.resourceLoader =
			(classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
		initIndex(resourceLoader);
		//index = loadIndex(resourceLoader);
		if (VERIFY_BEHAVIOUR) {
			this.verifier = new CachingMetadataReaderFactory(resourceLoader);
		}
		if (IndexBasedMetadataReader.DEBUG) System.out.println("New IBMRF instance for "+resourceLoader);
	}

	/**
	 * Return the ResourceLoader that this MetadataReaderFactory has been
	 * constructed with.
	 */
	public final ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}
	
	private static Index loadIndex(ResourceLoader resourceLoader) {
		try {
			Enumeration<URL> urls = resourceLoader.getClassLoader().getResources(
					ANNOTATION_INDEX_RESOURCE_LOCATION);
			if (!urls.hasMoreElements()) {
				throw new IllegalStateException(
						"IndexBasedMetadataReaderFactory unable to find index");
			}
			Index index = null;
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				InputStream openStream = url.openStream();
				IndexReader reader = new IndexReader(openStream);
				try {
					if (IndexBasedMetadataReader.DEBUG) System.out.println("Loading index from "+url);
					index = reader.read();
					if (IndexBasedMetadataReader.DEBUG) System.out.println("Index loaded " + index.toString());
					System.out.println("USING INDEX");
				}
				finally {
					openStream.close();
				}
			}
			return index;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to load index from location ["
					+ ANNOTATION_INDEX_RESOURCE_LOCATION + "]", ex);
		}
	}
	
	@Override
	public MetadataReader getMetadataReader(String className) throws IOException {
		String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
				ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
		Resource resource = this.resourceLoader.getResource(resourcePath);
		return new IndexBasedMetadataReader(index, className, resource, verifier==null?null:verifier.getMetadataReader(className));
	}

	@Override
	public MetadataReader getMetadataReader(Resource resource) throws IOException {
		return new IndexBasedMetadataReader(index, null, resource, verifier==null?null:verifier.getMetadataReader(resource));
	}

	public static boolean indexExists(ResourceLoader resourceLoader) {
		try {
			return resourceLoader.getClassLoader().getResources(ANNOTATION_INDEX_RESOURCE_LOCATION).hasMoreElements();
		} catch (IOException ioe) {
			return false;
		}
	}

}
