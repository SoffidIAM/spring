/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;

import jakarta.activation.FileTypeMap;
import jakarta.activation.MimetypesFileTypeMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Spring-configurable <code>FileTypeMap</code> implementation that will read
 * MIME type to file extension mappings from a standard JavaMail MIME type
 * mapping file, using a standard <code>MimetypesFileTypeMap</code> underneath.
 *
 * <p>The mapping file should be in the following format, as specified by the
 * Java Activation Framework:
 *
 * <pre>
 * # map text/html to .htm and .html files
 * text/html  html htm HTML HTM</pre>
 *
 * Lines starting with <code>#</code> are treated as comments and are ignored. All
 * other lines are treated as mappings. Each mapping line should contain the MIME
 * type as the first entry and then each file extension to map to that MIME type
 * as subsequent entries. Each entry is separated by spaces or tabs.
 *
 * <p>By default, the mappings in the <code>mime.types</code> file located in the
 * same package as this class are used, which cover many common file extensions
 * (in contrast to the out-of-the-box mappings in <code>activation.jar</code>).
 * This can be overridden using the <code>mappingLocation</code> property.
 *
 * <p>Additional mappings can be added via the <code>mappings</code> bean property,
 * as lines that follow the <code>mime.types<code> file format.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setMappingLocation
 * @see #setMappings
 * @see jakarta.activation.MimetypesFileTypeMap
 */
public class ConfigurableMimeFileTypeMap extends FileTypeMap implements InitializingBean {

	/**
	 * The <code>Resource</code> to load the mapping file from.
	 */
	private Resource mappingLocation = new ClassPathResource("mime.types", getClass());

	/**
	 * Used to configure additional mappings.
	 */
	private String[] mappings;

	/**
	 * The delegate FileTypeMap, compiled from the mappings in the mapping file
	 * and the entries in the <code>mappings</code> property.
	 */
	private FileTypeMap fileTypeMap;


	/**
	 * Specify the <code>Resource</code> from which mappings are loaded.
	 * Needs to follow the <code>mime.types<code> file format, as specified
	 * by the Java Activation Framework, containing lines such as:<br>
	 * <code>text/html  html htm HTML HTM</code>
	 */
	public void setMappingLocation(Resource mappingLocation) {
		this.mappingLocation = mappingLocation;
	}

	/**
	 * Specify additional MIME type mappings as lines that follow the
	 * <code>mime.types<code> file format, as specified by the
	 * Java Activation Framework, for example:<br>
	 * <code>text/html  html htm HTML HTM</code>
	 */
	public void setMappings(String[] mappings) {
		this.mappings = mappings;
	}


	/**
	 * Creates the final merged mapping set.
	 */
	public void afterPropertiesSet() {
		getFileTypeMap();
	}

	/**
	 * Return the delegate FileTypeMap, compiled from the mappings in the mapping file
	 * and the entries in the <code>mappings</code> property.
	 * @see #setMappingLocation
	 * @see #setMappings
	 * @see #createFileTypeMap
	 */
	protected final FileTypeMap getFileTypeMap() {
		if (this.fileTypeMap == null) {
			try {
				this.fileTypeMap = createFileTypeMap(this.mappingLocation, this.mappings);
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						"Could not load specified MIME type mapping file: " + this.mappingLocation);
			}
		}
		return fileTypeMap;
	}

	/**
	 * Compile a FileTypeMap from the mappings in the given mapping file and the
	 * given mapping entries.
	 * <p>Default implementation creates an Activation Framework MimetypesFileTypeMap,
	 * passing in an InputStream from the mapping resource (if any) and registering
	 * the mapping lines programmatically.
	 * @param mappingLocation a <code>mime.types</code> mapping resource (can be <code>null</code>)
	 * @param mappings MIME type mapping lines (can be <code>null</code>)
	 * @return the compiled FileTypeMap
	 * @throws IOException if resource access failed
	 * @see jakarta.activation.MimetypesFileTypeMap#MimetypesFileTypeMap(java.io.InputStream)
	 * @see jakarta.activation.MimetypesFileTypeMap#addMimeTypes(String)
	 */
	protected FileTypeMap createFileTypeMap(Resource mappingLocation, String[] mappings) throws IOException {
		MimetypesFileTypeMap fileTypeMap = (mappingLocation != null) ?
				new MimetypesFileTypeMap(mappingLocation.getInputStream()) : new MimetypesFileTypeMap();
		if (mappings != null) {
			for (int i = 0; i < mappings.length; i++) {
				fileTypeMap.addMimeTypes(mappings[i]);
			}
		}
		return fileTypeMap;
	}


	/**
	 * Delegates to the underlying FileTypeMap.
	 * @see #getFileTypeMap()
	 */
	public String getContentType(File file) {
		return getFileTypeMap().getContentType(file);
	}

	/**
	 * Delegates to the underlying FileTypeMap.
	 * @see #getFileTypeMap()
	 */
	public String getContentType(String fileName) {
		return getFileTypeMap().getContentType(fileName);
	}

}
