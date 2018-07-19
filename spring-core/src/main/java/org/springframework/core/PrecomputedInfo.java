/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.ClassUtils;

/**
 * @author Andy Clement
 */
public class PrecomputedInfo {

	static Map<String,Object> info;
	
	final static boolean debug = true;
	
	static {
		try {
			System.out.println("Looking for org.springframework.core.PrecomputedInfoLoader");
			Class<?> precomputedLoaderClass = ClassUtils.forName("org.springframework.core.PrecomputedInfoLoader", Thread.currentThread().getContextClassLoader());
			if (precomputedLoaderClass != null) {
				info = new HashMap<>();
				Loader pil = (Loader)precomputedLoaderClass.newInstance();
//				if (debug) {
//					System.out.println("Calling populate...");
//				}
				pil.populate(info);
				if (debug) {
					System.out.println("Precomputed data:");
					for (Map.Entry<String,Object> entry: info.entrySet()) {
						System.out.println(entry.getKey()+"="+entry.getValue());
					}
				}
			}
		} catch (ClassNotFoundException cnfe) {
			// no problem
		}
		catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	public static Object get(String classnamekey) {
		return (info==null?null:info.get(classnamekey));
	}

	interface Loader {

		void populate(Map<String, Object> info);
		
	}

}
