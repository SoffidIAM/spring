/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.core.io.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.SystemPropertyUtils;

/**
 * Editor for Resource descriptor arrays, to automatically convert String
 * location patterns (e.g. "file:C:/my*.txt" or "classpath*:myfile.txt")
 * to Resource array properties. Can also translate a collection or array
 * of location patterns into a merged Resource array.
 *
 * <p>The path may contain ${...} placeholders, to be resolved as
 * system properties: e.g. ${user.dir}.
 *
 * <p>Delegates to a ResourcePatternResolver, by default a
 * PathMatchingResourcePatternResolver.
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 * @see org.springframework.core.io.Resource
 * @see ResourcePatternResolver
 * @see PathMatchingResourcePatternResolver
 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders
 * @see System#getProperty(String)
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

	private final ResourcePatternResolver resourcePatternResolver;


	/**
	 * Create a new ResourceArrayPropertyEditor with a default
	 * PathMatchingResourcePatternResolver.
	 * @see PathMatchingResourcePatternResolver
	 */
	public ResourceArrayPropertyEditor() {
		this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
	}

	/**
	 * Create a new ResourceArrayPropertyEditor with the given ResourcePatternResolver.
	 * @param resourcePatternResolver the ResourcePatternResolver to use
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver) {
		this.resourcePatternResolver = resourcePatternResolver;
	}


	/**
	 * Treat the given text as location pattern and convert it to a Resource array.
	 */
	public void setAsText(String text) {
		String pattern = resolvePath(text).trim();
		try {
			setValue(this.resourcePatternResolver.getResources(pattern));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
			    "Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
		}
	}

	/**
	 * Treat the given value as collection or array and convert it to a Resource array.
	 * Considers String elements as location patterns, and takes Resource elements as-is.
	 */
	public void setValue(Object value) throws IllegalArgumentException {
		if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
			Collection input = (value instanceof Collection ? (Collection) value : Arrays.asList((Object[]) value));
			Set merged = CollectionFactory.createLinkedSetIfPossible(8);
			for (Iterator it = input.iterator(); it.hasNext();) {
				Object element = it.next();
				if (element instanceof String) {
					// A location pattern: resolve it into a Resource array.
					// Might point to a single resource or to multiple resources.
					String pattern = resolvePath((String) element).trim();
					try {
						Resource[] resources = this.resourcePatternResolver.getResources(pattern);
						for (int i = 0; i < resources.length; i++) {
							merged.add(resources[i]);
						}
					}
					catch (IOException ex) {
						throw new IllegalArgumentException(
								"Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
					}
				}
				else if (element instanceof Resource) {
					// A Resource object: add it to the set.
					merged.add(element);
				}
				else {
					throw new IllegalArgumentException("Cannot convert element [" + element +
							"] to Resource: only location Strings and Resource objects supported");
				}
			}
			super.setValue(merged.toArray(new Resource[merged.size()]));
		}

		else {
			// An arbitrary value: probably a String or a Resource array.
			// setAsText will be called for a String; a Resource array will be used as-is.
			super.setValue(value);
		}
	}

	/**
	 * Resolve the given path, replacing placeholders with
	 * corresponding system property values if necessary.
	 * @param path the original file path
	 * @return the resolved file path
	 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders
	 */
	protected String resolvePath(String path) {
		return SystemPropertyUtils.resolvePlaceholders(path);
	}

}
