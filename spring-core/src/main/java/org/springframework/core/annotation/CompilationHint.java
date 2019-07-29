/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface CompilationHint {

	// When attached to an annotation, this indicates which members in that
	// annotation
	// hold type references, e.g. if on ConditionalOnClass, members={"value","name"}
	// AliasFor might work but do I want to spin up spring infrastructure to do the
	// processing? (Do I have to spin it up to use that capability?)
	String[] member() default {};

	// If types need to specified but aren’t obvious from the annotation/class to
	// which this is
	// attached, the info can be put here (e.g. you have an ImportSelector returning
	// classnames, the possible names should be in here)
	String[] name() default {};

	Class<?>[] value() default {};

	// If true then whatever class is annotated/meta-annotateed with this is useless
	// if
	// the types visible through the names() fields are not found.
	// This would enable dismissing auto configurations if conditionalonclass tests fail
	// possibly going too far right now, don’t necessarily need this on an MVP.
	boolean skipIfTypesMissing() default false;

	// If true, then whatever types are referenced need to be followed because they
	// may
	// be further annotated/meta-annotated with compilation hints
	// This is needed in a system which chases down the minimum set of types to
	// exposed
	// from some root (the application class/spring.components/spring.factories). If
	// all
	// the classes include the configuration in static native-image.properties
	// files, don’t
	// need to chase it down.
	boolean follow() default false;
	
	String[] access() default {}; // reflection:constructors, reflection:methods, reflection:fields, resource

	// Do we need to specify what subset of reflection should be accessible?
	// (Fields/Methods/Ctors)?
	// Reducing the amount could likely help the image size

	// If true, whatever is (meta-)annotated with this must be accessible via
	// getResource too.
	// For MVP just assume yes, but need to test on image size *if* supplying all
	// the reflection/
	// resource metadata statically in fixed files.
	boolean accessibleAsResource() default true;
}
