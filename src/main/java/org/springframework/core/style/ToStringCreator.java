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

package org.springframework.core.style;

import org.springframework.util.Assert;

/**
 * Utility class that builds pretty-printing <code>toString()</code> methods
 * with pluggable styling conventions. By default, ToStringCreator adheres
 * to Spring's <code>toString()</code> styling conventions.
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class ToStringCreator {

	/**
	 * Default ToStringStyler instance used by this ToStringCreator.
	 */
	private static final ToStringStyler DEFAULT_TO_STRING_STYLER =
			new DefaultToStringStyler(StylerUtils.DEFAULT_VALUE_STYLER);


	private StringBuffer buffer = new StringBuffer(512);

	private ToStringStyler styler;

	private Object object;

	private boolean styledFirstField;


	/**
	 * Create a ToStringBuilder for this object.
	 * @param obj the object to be stringified
	 */
	public ToStringCreator(Object obj) {
		this(obj, (ToStringStyler) null);
	}

	/**
	 * Create a ToStringBuilder for this object with the provided style.
	 * @param obj the object to be stringified
	 * @param styler the ValueStyler encapsulating pretty-print instructions
	 */
	public ToStringCreator(Object obj, ValueStyler styler) {
		this(obj, new DefaultToStringStyler(styler != null ? styler : StylerUtils.DEFAULT_VALUE_STYLER));
	}

	/**
	 * Create a ToStringBuilder for this object with the provided style.
	 * @param obj the object to be stringified
	 * @param styler the ToStringStyler encapsulating pretty-print instructions
	 */
	public ToStringCreator(Object obj, ToStringStyler styler) {
		Assert.notNull(obj, "The object to be styled is required");
		this.object = obj;
		this.styler = (styler != null ? styler : DEFAULT_TO_STRING_STYLER);
		this.styler.styleStart(this.buffer, this.object);
	}


	/**
	 * Append a byte field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, byte value) {
		return append(fieldName, new Byte(value));
	}

	/**
	 * Append a short field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, short value) {
		return append(fieldName, new Short(value));
	}

	/**
	 * Append a integer field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, int value) {
		return append(fieldName, new Integer(value));
	}

	/**
	 * Append a float field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, float value) {
		return append(fieldName, new Float(value));
	}

	/**
	 * Append a double field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, double value) {
		return append(fieldName, new Double(value));
	}

	/**
	 * Append a long field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, long value) {
		return append(fieldName, new Long(value));
	}

	/**
	 * Append a boolean field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, boolean value) {
		return append(fieldName, value ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Append a field value.
	 * @param fieldName the name of the field, usually the member variable name
	 * @param value the field value
	 * @return this, to support call-chaining
	 */
	public ToStringCreator append(String fieldName, Object value) {
		printFieldSeparatorIfNecessary();
		this.styler.styleField(this.buffer, fieldName, value);
		return this;
	}

	private void printFieldSeparatorIfNecessary() {
		if (this.styledFirstField) {
			this.styler.styleFieldSeparator(this.buffer);
		}
		else {
			this.styledFirstField = true;
		}
	}

	/**
	 * Append the provided value.
	 * @param value The value to append
	 * @return this, to support call-chaining.
	 */
	public ToStringCreator append(Object value) {
		this.styler.styleValue(this.buffer, value);
		return this;
	}


	/**
	 * Return the String representation that this ToStringCreator built.
	 */
	public String toString() {
		this.styler.styleEnd(this.buffer, this.object);
		return this.buffer.toString();
	}

}
