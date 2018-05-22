/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.isis.commons.internal.resources;

import static org.apache.isis.commons.internal.base._With.requires;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.isis.commons.internal.base._Bytes;
import org.apache.isis.commons.internal.base._Strings;
import org.apache.isis.commons.internal.context._Context;

/**
 * <h1>- internal use only -</h1>
 * <p>
 * Utilities for storing and locating resources.
 * </p>
 * <p>
 * <b>WARNING</b>: Do <b>NOT</b> use any of the classes provided by this package! <br/> 
 * These may be changed or removed without notice!
 * </p>
 * @since 2.0.0
 */
public final class _Resource {
	
	// -- CLASS PATH RESOURCE LOADING
	
	public static InputStream load(Class<?> contextClass, String resourceName) {
		
		requires(contextClass, "contextClass");
		requires(resourceName, "resourceName");
		
		final String absoluteResourceName = resolveName(resourceName, contextClass);
		
		return _Context.getDefaultClassLoader()
				.getResourceAsStream(absoluteResourceName);
	}
	
	public static String loadAsString(Class<?> contextClass, String resourceName, Charset charset) throws IOException {
		final InputStream is = load(contextClass, resourceName);
		return _Strings.ofBytes(_Bytes.of(is), charset);
	}
	
	// -- CONTEXT PATH RESOURCE
	
	/**
	 * @return context-path resource (if any) as stored previously by {@link #putContextPathIfPresent(String)}  
	 */
	public final static String getContextPathIfAny() {
		final _Resource_ContextPath resource = _Context.getIfAny(_Resource_ContextPath.class);
		return resource!=null ? resource.getContextPath() : null;
	}

	/**
	 * Stores the {@code contextPath} as an application scoped resource-object.
	 * If {@code contextPath} is null or an empty String, no path-resource object is stored. 
	 * @param contextPath
	 * @throws IllegalArgumentException if an non-empty contextPath evaluates to being 
	 * equivalent to the root-path '/'
	 */
	public final static void putContextPathIfPresent(String contextPath) {
		if(!_Strings.isEmpty(contextPath)) {
			_Context.put(_Resource_ContextPath.class, new _Resource_ContextPath(contextPath), false);	
		} 
	}
	
	public final static String prependContextPathIfPresent(String path) {
		
		if(path==null) {
			return null;
		}
		
		final String contextPath = getContextPathIfAny();
		
		if(contextPath==null) {
			return path;
		}

		if(!path.startsWith("/")) {
			return contextPath + "/" + path;	
		} else {
			return "/" + contextPath + path;
		}
	}

	// -- RESTFUL PATH RESOURCE

	/**
	 * @return restful-path resource (if any) as stored previously by {@link #putRestfulPath(String)}  
	 */
	public final static String getRestfulPathIfAny() {
		final _Resource_RestfulPath resource = _Context.getIfAny(_Resource_RestfulPath.class);
		return resource!=null ? resource.getRestfulPath() : null;
	}

	/**
	 * Stores the {@code restfulPath} as an application scoped resource-object. 
	 * @param restfulPath
	 * @throws IllegalArgumentException if the restfulPath is empty or is the root-path.
	 */
	public final static void putRestfulPath(String restfulPath) {
		_Context.put(_Resource_RestfulPath.class, new _Resource_RestfulPath(restfulPath), false);
	}
	
	// -- HELPER
	
    /*
     * 
     * Adapted copy of JDK 8 Class::resolveName
     */
    private static String resolveName(String name, Class<?> contextClass) {
        if (name == null) {
            return name;
        }
        if (!name.startsWith("/")) {
            Class<?> c = contextClass;
            while (c.isArray()) {
                c = c.getComponentType();
            }
            String baseName = c.getName();
            int index = baseName.lastIndexOf('.');
            if (index != -1) {
                name = baseName.substring(0, index).replace('.', '/')
                    +"/"+name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }


}
