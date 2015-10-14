package authenticate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import authenticate.net.AuthenticateService;
import dataplatform.persist.IEntityManager;
import dataplatform.persist.impl.EntityManagerImpl;

public class AuthenticateServer {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateServer.class);
	
	private static IEntityManager entityManager;
	
	private static ScheduledExecutorService scheduler;
	
	private static AuthenticateService service;
	
	public static void main(String[] args0) {
		log.info("AuthenticateServer load.");
		scheduler = Executors.newScheduledThreadPool(3);
		entityManager = new EntityManagerImpl(null);
		scheduler.execute(new Loader());
	}

	public static IEntityManager getEntityManager() {
		return entityManager;
	}
	
	public static AuthenticateService getAuthenticateService() {
		return service;
	}
	
	private static class Loader implements Runnable {

		@Override
		public void run() {
			try {
//				XMLConfiguration configuration = new XMLConfiguration("data/config.xml");
//				SubnodeConfiguration sub = configuration.configurationAt("authencicate");
//				service = new AuthenticateService(sub.getString("address"), sub.getInt("port"));
				service = new AuthenticateService("", 9001);
				service.bind();
			} catch (Exception e) {
				log.error("Load fail", e);
				System.exit(0);
			}
		}
		
	}

}
