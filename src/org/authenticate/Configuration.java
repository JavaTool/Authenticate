package org.authenticate;

import com.google.common.collect.Maps;
import java.io.File;
import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.JAXBException;
import org.tool.server.io.IConfigurationHolder;

/**
 * Generate by classMaker.
 */
@XmlRootElement(name="configuration")
public class Configuration implements IConfigurationHolder {

	/**
	 * Generate by classMaker.
	 */
	private final Map<String, String> elements = Maps.newHashMap();

	/**
	 * Generate by classMaker.
	 */
	public static final String PORT = "port";

	/**
	 * Generate by classMaker.
	 */
	public static final String RESOURCE = "resource";

	/**
	 * Generate by classMaker.
	 */
	public static final String HIBERNATE = "hibernate";

	/**
	 * Generate by classMaker.
	 */
	public String getConfigurationValue(String key) {
		return elements.get(key);
	}

	/**
	 * Generate by classMaker.
	 */
	public String getConfigurationValue(String key, String defaultValue) {
		return elements.containsKey(key) ? elements.get(key) : defaultValue;
	}

	/**
	 * Generate by classMaker.
	 */
	public static Configuration createFromFile(File file) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
		Configuration configuration = (Configuration) unmarshaller.unmarshal(file);
		return configuration;
	}

	/**
	 * Generate by classMaker.
	 */
	@XmlElement
	private void setPort(String value) {
		elements.put(PORT, value);
	}

	/**
	 * Generate by classMaker.
	 */
	@XmlElement
	private void setResource(String value) {
		elements.put(RESOURCE, value);
	}

	/**
	 * Generate by classMaker.
	 */
	@XmlElement
	private void setHibernate(String value) {
		elements.put(HIBERNATE, value);
	}

}
