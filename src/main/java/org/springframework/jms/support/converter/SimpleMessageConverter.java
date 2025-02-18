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

package org.springframework.jms.support.converter;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * A simple message converter that can handle TextMessages, BytesMessages,
 * MapMessages, and ObjectMessages. Used as default by JmsTemplate, for
 * <code>convertAndSend</code> and <code>receiveAndConvert</code> operations.
 *
 * <p>Converts a String to a JMS TextMessage, a byte array to a JMS BytesMessage,
 * a Map to a JMS MapMessage, and a Serializable object to a JMS ObjectMessage
 * (or vice versa).
 *
 * <p>This converter implementation works for both JMS 1.1 and JMS 1.0.2,
 * except when extracting a byte array from a BytesMessage. So for converting
 * BytesMessages with a JMS 1.0.2 provider, use SimpleMessageConverter102.
 * (JmsTemplate102 uses SimpleMessageConverter102 as default.)
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#convertAndSend
 * @see org.springframework.jms.core.JmsTemplate#receiveAndConvert
 * @see org.springframework.jms.core.JmsTemplate102
 * @see SimpleMessageConverter102
 * @see jakarta.jms.TextMessage
 * @see jakarta.jms.BytesMessage
 * @see jakarta.jms.MapMessage
 * @see jakarta.jms.ObjectMessage
 */
public class SimpleMessageConverter implements MessageConverter {

	/**
	 * This implementation creates a TextMessage for a String, a
	 * BytesMessage for a byte array, a MapMessage for a Map,
	 * and an ObjectMessage for a Serializable object.
	 * @see #createMessageForString
	 * @see #createMessageForByteArray
	 * @see #createMessageForMap
	 * @see #createMessageForSerializable
	 */
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		if (object instanceof String) {
			return createMessageForString((String) object, session);
		}
		else if (object instanceof byte[]) {
			return createMessageForByteArray((byte[]) object, session);
		}
		else if (object instanceof Map) {
			return createMessageForMap((Map) object, session);
		}
		else if (object instanceof Serializable) {
			return createMessageForSerializable(((Serializable) object), session);
		}
		else {
			throw new MessageConversionException("Cannot convert object [" + object + "] to JMS message");
		}
	}

	/**
	 * This implementation converts a TextMessage back to a String, a
	 * ByteMessage back to a byte array, a MapMessage back to a Map,
	 * and an ObjectMessage back to a Serializable object.
	 * @see #extractStringFromMessage
	 * @see #extractByteArrayFromMessage
	 * @see #extractMapFromMessage
	 * @see #extractSerializableFromMessage
	 */
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		if (message instanceof TextMessage) {
			return extractStringFromMessage((TextMessage) message);
		}
		else if (message instanceof BytesMessage) {
			return extractByteArrayFromMessage((BytesMessage) message);
		}
		else if (message instanceof MapMessage) {
			return extractMapFromMessage((MapMessage) message);
		}
		else if (message instanceof ObjectMessage) {
			return extractSerializableFromMessage((ObjectMessage) message);
		}
		else {
			throw new MessageConversionException("Cannot convert JMS message [" + message + "] to object");
		}
	}


	/**
	 * Create a JMS TextMessage for the given String.
	 * @param text the String to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see jakarta.jms.Session#createTextMessage
	 */
	protected TextMessage createMessageForString(String text, Session session) throws JMSException {
		return session.createTextMessage(text);
	}

	/**
	 * Create a JMS BytesMessage for the given byte array.
	 * @param bytes the byyte array to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see jakarta.jms.Session#createBytesMessage
	 */
	protected BytesMessage createMessageForByteArray(byte[] bytes, Session session) throws JMSException {
		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bytes);
		return message;
	}

	/**
	 * Create a JMS MapMessage for the given Map.
	 * @param map the Map to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see jakarta.jms.Session#createMapMessage
	 */
	protected MapMessage createMessageForMap(Map map, Session session) throws JMSException {
		MapMessage message = session.createMapMessage();
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (!(entry.getKey() instanceof String)) {
				throw new MessageConversionException(
						"Cannot convert non-String key of type [" +
						(entry.getKey() != null ? entry.getKey().getClass().getName() : null) +
						"] to MapMessage entry");
			}
			message.setObject((String) entry.getKey(), entry.getValue());
		}
		return message;
	}

	/**
	 * Create a JMS ObjectMessage for the given Serializable object.
	 * @param object the Serializable object to convert
	 * @param session current JMS session
	 * @return the resulting message
	 * @throws JMSException if thrown by JMS methods
	 * @see jakarta.jms.Session#createObjectMessage
	 */
	protected ObjectMessage createMessageForSerializable(Serializable object, Session session) throws JMSException {
		return session.createObjectMessage(object);
	}


	/**
	 * Extract a String from the given TextMessage.
	 * @param message the message to convert
	 * @return the resulting String
	 * @throws JMSException if thrown by JMS methods
	 */
	protected String extractStringFromMessage(TextMessage message) throws JMSException {
		return message.getText();
	}

	/**
	 * Extract a byte array from the given TextMessage.
	 * @param message the message to convert
	 * @return the resulting byte array
	 * @throws JMSException if thrown by JMS methods
	 */
	protected byte[] extractByteArrayFromMessage(BytesMessage message) throws JMSException {
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		return bytes;
	}

	/**
	 * Extract a Map from the given TextMessage.
	 * @param message the message to convert
	 * @return the resulting Map
	 * @throws JMSException if thrown by JMS methods
	 */
	protected Map extractMapFromMessage(MapMessage message) throws JMSException {
		Map map = new HashMap();
		Enumeration en = message.getMapNames();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			map.put(key, message.getObject(key));
		}
		return map;
	}

	/**
	 * Extract a Serializable object from the given TextMessage.
	 * @param message the message to convert
	 * @return the resulting Serializable object
	 * @throws JMSException if thrown by JMS methods
	 */
	protected Serializable extractSerializableFromMessage(ObjectMessage message) throws JMSException {
		return message.getObject();
	}

}
