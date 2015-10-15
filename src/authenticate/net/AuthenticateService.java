package authenticate.net;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import authenticate.account.Account;
import cg.base.event.EventDisconnect;
import cg.base.io.SimpleContentHandler;
import cg.base.io.message.RequestAccountChangePassword;
import cg.base.io.message.RequestAccountLogin;
import cg.base.io.message.RequestAccountLogout;
import cg.base.io.message.RequestAccountRegister;
import cg.base.io.message.RequestServerList;
import cg.base.io.message.RequestServerRegister;
import cg.base.io.message.RequestServerUnregister;
import cg.base.io.message.ResponseAccountChangePassword;
import cg.base.io.message.ResponseAccountLogin;
import cg.base.io.message.ResponseAccountLogout;
import cg.base.io.message.ResponseAccountRegister;
import cg.base.io.message.ResponseExecuteError;
import cg.base.log.Log;
import cg.base.util.SenderUtils;
import dataplatform.persist.IEntityManager;
import dataplatform.pubsub.ISimplePubsub;
import dataplatform.pubsub.impl.SimplePubsub;
import net.dipatch.ISender;
import net.io.IMessage;
import net.io.INetServer;
import net.io.SimpleContentFactory;
import net.io.netty.server.NettyTcpServer;

public class AuthenticateService {
	
	private final int port;
	
	private final Map<String, Account> accounts;
	
	private final Map<String, String> accountSessions;
	
	private final Map<String, ISender> senders;
	
	private final ISimplePubsub pubsub;
	
	private final Log log;
	
	private IEntityManager entityManager;
	
	private static class CLog implements Log {
		
		private static org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);

		@Override
		public void info(String info) {
			log.info(info);
		}

		@Override
		public void warning(String warning) {
			log.warn(warning);
		}

		@Override
		public void error(String error) {
			log.error(error);
		}

		@Override
		public void print(String head, String message) {
			log.info("[" + head + "]" + message);
		}

		@Override
		public void error(String error, Throwable t) {
			log.error(error, t);
		}
		
	}
	
	public AuthenticateService(String address, int port) {
		senders = Maps.newConcurrentMap();
		accounts = Maps.newConcurrentMap();
		accountSessions = Maps.newConcurrentMap();
		this.port = port;
		pubsub = new SimplePubsub();
		pubsub.subscribe(this);
		log = new CLog();
	}
	
	public void bind(IEntityManager entityManager, ScheduledExecutorService scheduler) throws Exception {
		this.entityManager = entityManager;
		
		INetServer netServer = new NettyTcpServer(port, new SimpleContentHandler(pubsub), new SimpleContentFactory());
		scheduler.execute(new NetServerStart(netServer));;
		
		log.info("Net bind.");
	}
	
	private class NetServerStart implements Runnable {
		
		private final INetServer netServer;
		
		public NetServerStart(INetServer netServer) {
			this.netServer = netServer;
		}

		@Override
		public void run() {
			try {
				netServer.bind();
			} catch (Exception e) {
				log.error(getClass().getName() + "::" + e.getMessage(), e);
			}
		}
		
	}
	
	@Subscribe
	public void registerServer(RequestServerRegister requestServerRegister) {
		String name = getServerName(requestServerRegister);
		if (!senders.containsKey(name)) {
			senders.put(name, requestServerRegister.getSender());
			log.info("registerServer : " + name);
		}
	}

	@Subscribe
	public void unregisterServer(RequestServerUnregister requestServerUnregister) {
		String name = getServerName(requestServerUnregister);
		if (senders.containsKey(name)) {
			senders.remove(name);
			for (Account account : accounts.values()) {
				if (account.getServerName().equals(name)) {
					accounts.remove(account.getName());
				}
			}
			log.info("unregisterServer : " + name);
		}
	}

	@Subscribe
	public void registerAccount(RequestAccountRegister requestAccountRegister) {
//		int serial = packet.getInt();
		String name = requestAccountRegister.getAccount(), password = requestAccountRegister.getPassword();
		IMessage message = new ResponseAccountRegister();
		if (hasAccountName(name)) {
			ResponseExecuteError error = new ResponseExecuteError();
			error.setErrorCode(0);
			error.setMessage("account already register.");
			error.setMessageId(requestAccountRegister.getMessageId());
			message = error;
		} else { // OK
			Account account = new Account();
			account.setName(name);
			account.setPassword(password);
			entityManager.createSync(account);

			message = new ResponseAccountRegister();
		}
		SenderUtils.send(requestAccountRegister.getSender(), message, log);
	}
	
	private String getServerName(IMessage message) {
		return message.getSender().getIp();
	}

	@Subscribe
	public void accountLogin(RequestAccountLogin requestAccountLogin) {
//		int serial = packet.getInt();
		String name = requestAccountLogin.getAccount(), password = requestAccountLogin.getPassword();
		Account account = findAccount(name, password);
		IMessage message = new ResponseAccountRegister();
		if (account == null) {
			ResponseExecuteError error = new ResponseExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(requestAccountLogin.getMessageId());
			message = error;
		} else if (accounts.containsKey(name)) {
			ResponseExecuteError error = new ResponseExecuteError();
			error.setErrorCode(0);
			error.setMessage("account already login");
			error.setMessageId(requestAccountLogin.getMessageId());
			message = error;
		} else {
			account.setServerName(getServerName(requestAccountLogin));
			accounts.put(name, account);
			accountSessions.put(requestAccountLogin.getSessionId(), name);
			log.info(name + " login.");
			
			message = new ResponseAccountLogin();
		}
		SenderUtils.send(requestAccountLogin.getSender(), message, log);
	}

//	@Subscribe
//	public void deleteAccount(RequestAccount) {
//		int serial = packet.getInt();
//		String name = packet.getString(), password = packet.getString();
//		Account account = findAccount(name, password);
//		Packet pt = new Packet(returnCode);
//		pt.putInt(serial);
//		if (account == null) {
//			pt.put(IOUtil.FALSE);
//		} else {
//			accounts.remove(name);
//			entityManager.deleteSync(account);
//			
//			pt.put(IOUtil.TRUE);
//		}
//		send(session, pt);
//	}

//	@Subscribe
//	public void addImoney(IoSession session, Packet packet, int returnCode) {
//		int serial = packet.getInt();
//		String name = packet.getString(), password = packet.getString();
//		int imoney = packet.getInt();
//		Account account = findAccount(name, password);
//		Packet pt = new Packet(returnCode);
//		pt.putInt(serial);
//		if (account == null) {
//			pt.put(IOUtil.FALSE);
//		} else {
//			int result = account.getImoney() + imoney;
//			if (result < 0) {
//				pt.put(IOUtil.FALSE);
//			} else {
//				account.setImoney(result);
//				entityManager.updateSync(account);
//				
//				pt.put(IOUtil.TRUE);
//				pt.putInt(account.getImoney());
//			}
//		}
//		send(session, pt);
//	}

	@Subscribe
	public void accountLogout(RequestAccountLogout requestAccountLogout) {
//		int serial = packet.getInt();
		String sessionId = requestAccountLogout.getSessionId();
		String name = accountSessions.get(sessionId);
		Account account = name == null ? null : accounts.remove(name);
		IMessage message = new ResponseAccountRegister();
		if (account == null) {
			ResponseExecuteError error = new ResponseExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(requestAccountLogout.getMessageId());
			message = error;
		} else {
			accountSessions.remove(sessionId);
			accounts.remove(name);
			log.info(name + " logout.");
			
			message = new ResponseAccountLogout();
		}
		SenderUtils.send(requestAccountLogout.getSender(), message, log);
	}

	@Subscribe
	public void changePassword(RequestAccountChangePassword requestAccountChangePassword) {
//		int serial = packet.getInt();
		String name = requestAccountChangePassword.getAccount(), password = requestAccountChangePassword.getOldPassword(), newPassword = requestAccountChangePassword.getNewPassword();
		Account account = findOnlineAccount(name, password);
		IMessage message = new ResponseAccountRegister();
		if (account == null) {
			ResponseExecuteError error = new ResponseExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(requestAccountChangePassword.getMessageId());
			message = error;
		} else {
			account.setPassword(newPassword);
			entityManager.updateSync(account);
			
			message = new ResponseAccountChangePassword();
		}
		SenderUtils.send(requestAccountChangePassword.getSender(), message, log);
	}
	
	@Subscribe
	public void eventDisconnect(EventDisconnect eventDisconnect) {
		String name = accountSessions.remove(eventDisconnect.getSessionId());
		if (name != null) {
			accounts.remove(name);
			log.info(name + " logout.");
		}
	}
	
	@Subscribe
	public void requestServerList(RequestServerList requestServerList) {
		
	}
	
	private boolean hasAccountName(String name) {
		return accounts.containsKey(name) ? true : entityManager.count("select count(*) from Account where name=?", name) > 0;
	}
	
	private Account findOnlineAccount(String name, String password) {
		Account account = accounts.get(name);
		return account != null && account.getPassword().equals(password) ? account : null;
	}
	
	private Account findAccount(String name, String password) {
		Account account = findOnlineAccount(name, password);
		return account == null ? entityManager.fetch(Account.class, "from Account where name=? and password=?", name, password) : account;
	}

}
