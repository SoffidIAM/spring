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

package org.springframework.core.io;

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.util.Assert;

/**
 * Default implementation of the ResourceLoader interface.
 * Used by ResourceEditor, but also suitable for standalone usage.
 *
 * <p>Will return an UrlResource if the location value is a URL, and a
 * ClassPathResource if it is a non-URL path or a "classpath:" pseudo-URL.
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see #CLASSPATH_URL_PREFIX
 * @see ResourceEditor
 * @see UrlResource
 * @see ClassPathResource
 */
public class DefaultResourceLoader implements ResourceLoader {

	private ClassLoader classLoader;


	/**
	 * Create a new DefaultResourceLoader.
	 * <p>ClassLoader access will happen via the thread context class loader on actual
	 * access (applying to the thread that does ClassPathResource calls).
	 * @see java.lang.Thread#getContextClassLoader()
	 */
	public DefaultResourceLoader() {
		this.classLoader = Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Create a new DefaultResourceLoader.
	 * @param classLoader the ClassLoader to load class path resources with,
	 * or <code>null</code> if using the thread context class loader on actual access
	 * (applying to the thread that does ClassPathResource calls).
	 */
	public DefaultResourceLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * Specify the ClassLoader to load class path resources with,
	 * or <code>null</code> if using the thread context class loader.
	 * <p>The default is that ClassLoader access will happen via the thread
	 * context class loader.
	 */
	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Return the ClassLoader to load class path resources with,
	 * or <code>null</code> if using the thread context class loader on actual access
	 * (applying to the thread that constructs the ClassPathResource object).
	 * <p>Will get passed to ClassPathResource's constructor for all
	 * ClassPathResource objects created by this resource loader.
	 * @see ClassPathResource
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}


	public Resource getResource(String location) {
		Assert.notNull(location, "Location must not be null");
		if (location.startsWith(CLASSPATH_URL_PREFIX)) {
			return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
		}
		else {
			try {
				// try URL
				URL url = new URL(location);
				return new UrlResource(url);
			}
			catch (MalformedURLException ex) {
				// no URL -> resolve resource path
				return getResourceByPath(location);
			}
		}
	}

	/**
	 * Return a Resource handle for the resource at the given path.
	 * <p>Default implementation supports class path locations. This should
	 * be appropriate for standalone implementations but can be overridden,
	 * e.g. for implementations targeted at a Servlet container.
	 * @param path path to the resource
	 * @return Resource handle
	 * @see ClassPathResource
	 * @see org.springframework.context.support.FileSystemXmlApplicationContext#getResourceByPath
	 * @see org.springframework.web.context.support.XmlWebApplicationContext#getResourceByPath
	 */
	protected Resource getResourceByPath(String path) {
		return new ClassPathResource(path, getClassLoader());
	}

}
