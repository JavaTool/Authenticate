package org.authenticate;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tool.server.io.IConfigurationHolder;

public class SingleConfiguration implements IConfigurationHolder {
	
	private static final Logger log = LoggerFactory.getLogger(SingleConfiguration.class);
	
	private final IConfigurationHolder configurationHolder;
	
	private static class SingletonHolder {
		
        private static SingleConfiguration instance = new SingleConfiguration(createFromFile());
        
    }
	
	public static IConfigurationHolder getInstance() {
		return SingletonHolder.instance;
	}
	
	private SingleConfiguration(IConfigurationHolder configurationHolder) {
		this.configurationHolder = configurationHolder;
	}

	@Override
	public String getConfigurationValue(String key) {
		return configurationHolder.getConfigurationValue(key);
	}

	@Override
	public String getConfigurationValue(String key, String defaultValue) {
		return configurationHolder.getConfigurationValue(key, defaultValue);
	}

	public static Configuration createFromFile() {
		try {
			return Configuration.createFromFile(new File("config/configuration.xml"));
		} catch (Exception e) {
			log.error("", e);
			System.exit(1);
			return null;
		}
	}

}
