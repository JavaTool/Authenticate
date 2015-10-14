package authenticate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import authenticate.net.AuthenticateService;
import dataplatform.persist.IEntityManager;
import dataplatform.persist.impl.EntityManagerImpl;

public class AuthenticateServer {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateServer.class);
	
	private static IEntityManager entityManager;
	
	private static XMLConfiguration configuration;
	
	private static ScheduledExecutorService scheduler;
	
	private static AuthenticateService service;
	
	private static OpcodeInfo[] opcodes;
	
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
	
	public static OpcodeInfo[] getOpcodes() {
		return opcodes;
	}
	
	private static class Loader implements Runnable {

		@Override
		public void run() {
			try {
				configuration = new XMLConfiguration("data/config.xml");
				SubnodeConfiguration sub = configuration.configurationAt("authencicate");
				service = new AuthenticateService(sub.getString("address"), sub.getInt("port"));
				service.bind();
			} catch (Exception e) {
				log.error("Load fail", e);
				System.exit(0);
			}
		}
		
	}
	
	public static class OpcodeInfo {
		
		public final int opcode, returnCode;
		
		public final String method;
		
		public OpcodeInfo(int opcode, int returnCode, String method) {
			this.opcode = opcode;
			this.method = method;
			this.returnCode = returnCode;
		}
		
	}

}
