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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.activation.FileTypeMap;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

/**
 * Production implementation of the JavaMailSender interface.
 * Supports both JavaMail MimeMessages and Spring SimpleMailMessages.
 * Can also be used as plain MailSender implementation.
 *
 * <p>Allows for defining all settings locally as bean properties.
 * Alternatively, a pre-configured JavaMail Session can be specified,
 * possibly pulled from an application server's JNDI environment.
 *
 * <p>Non-default properties in this object will always override the settings
 * in the JavaMail Session. Note that if overriding all values locally, there
 * is no added value in setting a pre-configured Session.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 10.09.2003
 * @see jakarta.mail.internet.MimeMessage
 * @see org.springframework.mail.SimpleMailMessage
 * @see org.springframework.mail.MailSender
 * @see #setJavaMailProperties
 * @see #setHost
 * @see #setPort
 * @see #setUsername
 * @see #setPassword
 * @see #setSession
 */
public class JavaMailSenderImpl implements JavaMailSender {

	public static final String DEFAULT_PROTOCOL = "smtp";

	public static final int DEFAULT_PORT = -1;


	protected final Log logger = LogFactory.getLog(getClass());

	private Session session = Session.getInstance(new Properties());

	private String protocol = DEFAULT_PROTOCOL;

	private String host;

	private int port = DEFAULT_PORT;

	private String username;

	private String password;

	private String defaultEncoding;

	private FileTypeMap defaultFileTypeMap;


	/**
	 * Constructor for JavaMailSenderImpl.
	 * <p>Initializes the "defaultFileTypeMap" property with a default
	 * ConfigurableMimeFileTypeMap.
	 * @see #setDefaultFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	public JavaMailSenderImpl() {
		ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
		fileTypeMap.afterPropertiesSet();
		this.defaultFileTypeMap = fileTypeMap;
	}

	/**
	 * Set JavaMail properties for the Session. A new Session will be created
	 * with those properties. Use either this or setSession, not both.
	 * <p>Non-default properties in this MailSender will override given
	 * JavaMail properties.
	 * @see #setSession
	 */
	public void setJavaMailProperties(Properties javaMailProperties) {
		this.session = Session.getInstance(javaMailProperties);
	}

	/**
	 * Set the JavaMail Session, possibly pulled from JNDI. Default is a new Session
	 * without defaults, i.e. completely configured via this object's properties.
	 * <p>If using a pre-configured Session, non-default properties in this
	 * MailSender will override the settings in the Session.
	 * @see #setJavaMailProperties
	 */
	public void setSession(Session session) {
		if (session == null) {
			throw new IllegalArgumentException("Cannot work with a null Session");
		}
		this.session = session;
	}

	/**
	 * Return the JavaMail Session.
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * Set the mail protocol. Default is SMTP.
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Return the mail protocol.
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Set the mail server host, typically an SMTP host.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Return the mail server host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the mail server port. Default is -1, letting JavaMail
	 * use the default SMTP port (25).
	*/
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Return the mail server port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the username for the account at the mail host, if any.
	 * <p>Note that the underlying JavaMail Session has to be configured
	 * with the property "mail.smtp.auth" set to "true", else the specified
	 * username will not be sent to the mail server by the JavaMail runtime.
	 * If you are not explicitly passing in a Session to use, simply specify
	 * this setting via JavaMailSenderImpl's "javaMailProperties".
	 * @see #setJavaMailProperties
	 * @see #setSession
	 * @see #setPassword
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the username for the account at the mail host.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the password for the account at the mail host, if any.
	 * <p>Note that the underlying JavaMail Session has to be configured
	 * with the property "mail.smtp.auth" set to "true", else the specified
	 * password will not be sent to the mail server by the JavaMail runtime.
	 * If you are not explicitly passing in a Session to use, simply specify
	 * this setting via JavaMailSenderImpl's "javaMailProperties".
	 * @see #setJavaMailProperties
	 * @see #setSession
	 * @see #setUsername
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the password for the account at the mail host.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the default encoding to use for MimeMessages created by this
	 * JavaMailSender. Such an encoding will be auto-detected by MimeMessageHelper.
	 * @see MimeMessageHelper
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the default encoding for MimeMessages, or <code>null</code> if none.
	 */
	public String getDefaultEncoding() {
		return defaultEncoding;
	}

	/**
	 * Set the default Java Activation FileTypeMap to use for MimeMessages
	 * created by this JavaMailSender. A FileTypeMap specified here will be
	 * autodetected by MimeMessageHelper, avoiding the need to specify the
	 * FileTypeMap for each MimeMessageHelper instance.
	 * <p>For example, you can specify a custom instance of Spring's
	 * ConfigurableMimeFileTypeMap here. If not explicitly specified, a default
	 * ConfigurableMimeFileTypeMap will be used, containing an extended set
	 * of MIME type mappings (as defined by the <code>mime.types</code> file
	 * contained in the Spring jar).
	 * @see MimeMessageHelper#setFileTypeMap
	 * @see ConfigurableMimeFileTypeMap
	 */
	public void setDefaultFileTypeMap(FileTypeMap defaultFileTypeMap) {
		this.defaultFileTypeMap = defaultFileTypeMap;
	}

	/**
	 * Return the default Java Activation FileTypeMap for MimeMessages,
	 * or <code>null</code> if none.
	 */
	public FileTypeMap getDefaultFileTypeMap() {
		return defaultFileTypeMap;
	}


	//---------------------------------------------------------------------
	// Implementation of MailSender
	//---------------------------------------------------------------------

	public void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] { simpleMessage });
	}

	public void send(SimpleMailMessage[] simpleMessages) throws MailException {
		List mimeMessages = new ArrayList(simpleMessages.length);
		for (int i = 0; i < simpleMessages.length; i++) {
			SimpleMailMessage simpleMessage = simpleMessages[i];
			if (logger.isDebugEnabled()) {
				logger.debug("Creating new MIME message using the following mail properties: " + simpleMessage);
			}
			MimeMailMessage message = new MimeMailMessage(createMimeMessage());
			simpleMessage.copyTo(message);
			mimeMessages.add(message.getMimeMessage());
		}
		doSend((MimeMessage[]) mimeMessages.toArray(new MimeMessage[mimeMessages.size()]), simpleMessages);
	}


	//---------------------------------------------------------------------
	// Implementation of JavaMailSender
	//---------------------------------------------------------------------

	/**
	 * This implementation creates a SmartMimeMessage, holding the specified default
	 * encoding and default FileTypeMap. This special defaults-carrying message will
	 * be autodetected by MimeMessageHelper, which will use the carried encoding
	 * and FileTypeMap unless explicitly overridden.
	 * @see #setDefaultEncoding
	 * @see #setDefaultFileTypeMap
	 * @see MimeMessageHelper
	 */
	public MimeMessage createMimeMessage() {
		return new SmartMimeMessage(getSession(), getDefaultEncoding(), getDefaultFileTypeMap());
	}

	public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
		try {
			return new MimeMessage(getSession(), contentStream);
		}
		catch (MessagingException ex) {
			throw new MailParseException("Could not parse raw MIME content", ex);
		}
	}

	public void send(MimeMessage mimeMessage) throws MailException {
		send(new MimeMessage[] { mimeMessage });
	}

	public void send(MimeMessage[] mimeMessages) throws MailException {
		doSend(mimeMessages, null);
	}

	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[] { mimeMessagePreparator });
	}

	public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
		try {
			List mimeMessages = new ArrayList(mimeMessagePreparators.length);
			for (int i = 0; i < mimeMessagePreparators.length; i++) {
				MimeMessage mimeMessage = createMimeMessage();
				mimeMessagePreparators[i].prepare(mimeMessage);
				mimeMessages.add(mimeMessage);
			}
			send((MimeMessage[]) mimeMessages.toArray(new MimeMessage[mimeMessages.size()]));
		}
		catch (MailException ex) {
			throw ex;
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
		catch (IOException ex) {
			throw new MailPreparationException(ex);
		}
		catch (Exception ex) {
			throw new MailPreparationException(ex);
		}
	}


	/**
	 * Actually send the given array of MimeMessages via JavaMail.
	 * @param mimeMessages MimeMessage objects to send
	 * @param originalMessages corresponding original message objects
	 * that the MimeMessages have been created from (with same array
	 * length and indices as the "mimeMessages" array), if any
	 * @throws org.springframework.mail.MailAuthenticationException
	 * in case of authentication failure
	 * @throws org.springframework.mail.MailSendException
	 * in case of failure when sending a message
	 */
	protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
		Map failedMessages = new HashMap();
		try {
			Transport transport = getTransport(getSession());
			transport.connect(getHost(), getPort(), getUsername(), getPassword());
			try {
				for (int i = 0; i < mimeMessages.length; i++) {
					MimeMessage mimeMessage = mimeMessages[i];
					try {
						if (mimeMessage.getSentDate() == null) {
							mimeMessage.setSentDate(new Date());
						}
						mimeMessage.saveChanges();
						transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
					}
					catch (MessagingException ex) {
						Object original = (originalMessages != null ? originalMessages[i] : mimeMessage);
						failedMessages.put(original, ex);
					}
				}
			}
			finally {
				transport.close();
			}
		}
		catch (AuthenticationFailedException ex) {
			throw new MailAuthenticationException(ex);
		}
		catch (MessagingException ex) {
			throw new MailSendException("Mail server connection failed", ex);
		}
		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	/**
	 * Get a Transport object for the given JavaMail Session.
	 * Can be overridden in subclasses, e.g. to return a mock Transport object.
	 * @see jakarta.mail.Session#getTransport
	 * @see #getProtocol
	 */
	protected Transport getTransport(Session session) throws NoSuchProviderException {
		return session.getTransport(getProtocol());
	}

}
