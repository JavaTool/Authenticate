package authenticate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import authenticate.net.AuthenticateService;
import dataplatform.persist.IEntityManager;
import dataplatform.persist.impl.EntityManagerImpl;

class AuthenticateServer {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateServer.class);
	
	public static void main(String[] args0) {
		log.info("AuthenticateServer load.");
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
		scheduler.execute(new Loader(scheduler));
	}
	
	private static class Loader implements Runnable {
		
		private final ScheduledExecutorService scheduler;
		
		public Loader(ScheduledExecutorService scheduler) {
			this.scheduler = scheduler;
		}

		@Override
		public void run() {
			try {
//				XMLConfiguration configuration = new XMLConfiguration("data/config.xml");
//				SubnodeConfiguration sub = configuration.configurationAt("authencicate");
//				service = new AuthenticateService(sub.getString("address"), sub.getInt("port"));
				IEntityManager entityManager = new EntityManagerImpl(null);
				AuthenticateService service = new AuthenticateService("", 9001);
				service.bind(entityManager, scheduler);
			} catch (Exception e) {
				log.error("Load fail", e);
				System.exit(0);
			}
		}
		
	}

}
