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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Resource implementation for <code>java.io.File</code> handles.
 * Obviously supports resolution as File, and also as URL.
 *
 * @author Juergen Hoeller
 * @since 28.12.2003
 * @see java.io.File
 */
public class FileSystemResource extends AbstractResource {

	private final File file;

	private final String path;


	/**
	 * Create a new FileSystemResource.
	 * @param file a File handle
	 */
	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
		this.path = StringUtils.cleanPath(file.getPath());
	}

	/**
	 * Create a new FileSystemResource.
	 * @param path a file path
	 */
	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.file = new File(path);
		this.path = StringUtils.cleanPath(path);
	}

	/**
	 * Return the file path for this resource.
	 */
	public final String getPath() {
		return path;
	}


	/**
	 * This implementation returns whether the underlying file exists.
	 * @see java.io.File#exists()
	 */
	public boolean exists() {
		return this.file.exists();
	}

	/**
	 * This implementation opens a FileInputStream for the underlying file.
	 * @see java.io.FileInputStream
	 */
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(this.file);
	}

	/**
	 * This implementation returns a URL for the underlying file.
	 * @see java.io.File#getAbsolutePath()
	 */
	public URL getURL() throws IOException {
		return new URL(ResourceUtils.FILE_URL_PREFIX + this.file.getAbsolutePath());
	}

	/**
	 * This implementation returns the underlying File reference.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * This implementation creates a FileSystemResource, applying the given path
	 * relative to the path of the underlying file of this resource descriptor.
	 * @see org.springframework.util.StringUtils#applyRelativePath(String, String)
	 */
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FileSystemResource(pathToUse);
	}

	/**
	 * This implementation returns the name of the file.
	 * @see java.io.File#getName()
	 */
	public String getFilename() {
		return this.file.getName();
	}

	/**
	 * This implementation returns a description that includes the absolute
	 * path of the file.
	 * @see java.io.File#getAbsolutePath()
	 */
	public String getDescription() {
		return "file [" + this.file.getAbsolutePath() + "]";
	}


	/**
	 * This implementation compares the underlying File references.
	 */
	public boolean equals(Object obj) {
		return (obj == this ||
		    (obj instanceof FileSystemResource && this.path.equals(((FileSystemResource) obj).path)));
	}

	/**
	 * This implementation returns the hash code of the underlying File reference.
	 */
	public int hashCode() {
		return this.path.hashCode();
	}

}
