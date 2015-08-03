package authenticate;

import java.util.List;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import persist.EntityManager;
import persist.impl.EntityManagerImpl;
import thread.base.ThreadCall;
import thread.base.ThreadPool;
import thread.impl.DefaultThreadPool;
import authenticate.net.AuthenticateService;

public class AuthenticateServer {
	
	private static final Log log = LogFactory.getLog(AuthenticateServer.class);
	
	private static EntityManager entityManager;
	
	private static XMLConfiguration configuration;
	
	private static ThreadPool threadPool;
	
	private static AuthenticateService service;
	
	private static OpcodeInfo[] opcodes;
	
	public static void main(String[] args0) {
		log.info("AuthenticateServer load.");
		threadPool = new DefaultThreadPool(5, 100);
		entityManager = new EntityManagerImpl();
		threadPool.execute(new Loader());
	}

	public static EntityManager getEntityManager() {
		return entityManager;
	}
	
	public static AuthenticateService getAuthenticateService() {
		return service;
	}
	
	public static OpcodeInfo[] getOpcodes() {
		return opcodes;
	}
	
	private static class Loader implements ThreadCall {

		@Override
		public void run() {
			try {
				configuration = new XMLConfiguration("data/config.xml");
				SubnodeConfiguration sub = configuration.configurationAt("authencicate");
				service = new AuthenticateService(sub.getString("address"), sub.getInt("port"));
				@SuppressWarnings("unchecked")
				List<SubnodeConfiguration> list = configuration.configurationsAt("opcodes.opcode");
				opcodes = new OpcodeInfo[list.size()];
				for (int i = 0;i < list.size();i++) {
					SubnodeConfiguration snc = list.get(i);
					opcodes[i] = new OpcodeInfo(snc.getInt("id"), snc.getInt("return"), snc.getString("method"));
				}
				service.bind();
			} catch (Exception e) {
				log.error("Load fail", e);
				System.exit(0);
			}
		}

		@Override
		public void callFinish() {
			log.info("AuthenticateServer start finish.");
			
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
