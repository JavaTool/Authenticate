package org.authenticate;

import java.io.File;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.tool.server.cache.ICache;
import org.tool.server.cache.redis.JedisPoolResources;
import org.tool.server.cache.redis.string.RedisStringPoolCache;
import org.tool.server.io.IConfigurationHolder;
import org.tool.server.io.jetty.AbstractStartup;
import org.tool.server.persist.IEntityManager;
import org.tool.server.persist.impl.EntityManagerImpl;

public class Startup extends AbstractStartup {
	
	private IEntityManager entityManager;

	@Override
	protected void init(ServletContextEvent sce) throws Exception {
		ServletContext servletContext = sce.getServletContext();
		// 加载配置文件
		IConfigurationHolder configuration = SingleConfiguration.getInstance();
		// 加载Hibernate
		org.hibernate.cfg.Configuration hc = new org.hibernate.cfg.Configuration();
		hc.configure(new File(configuration.getConfigurationValue(Configuration.HIBERNATE)));
		entityManager = new EntityManagerImpl(hc);
		// redis
		String address = configuration.getConfigurationValue(Configuration.REDIS_ADDRESS);
		ICache<String, String, String> cache = new RedisStringPoolCache(new JedisPoolResources(address, 10, 10, 1000, ""));
		// service
		int expire = Integer.parseInt(configuration.getConfigurationValue(Configuration.EXPIRE));
		IAccountService accountService = new AccountService(entityManager, cache, expire);
		servletContext.setAttribute(IAccountService.class.getName(), accountService);
		log.info("channel server startup.");
	}

	@Override
	protected void shutdown(ServletContextEvent sce) {
		try {
			entityManager.shutdown();
		} catch (Exception e) {
			log.error("", e);
		}
		log.info("channel server end.");
	}

}
