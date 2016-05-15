package authenticate.net;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import authenticate.account.Account;
import cg.base.event.EventDisconnect;
import cg.base.io.SimpleContentHandler;
import cg.base.io.message.RequestAccountAuthenticate;
import cg.base.io.message.RequestAccountChangePassword;
import cg.base.io.message.RequestAccountLogin;
import cg.base.io.message.RequestAccountLogout;
import cg.base.io.message.RequestAccountRegister;
import cg.base.io.message.RequestServerList;
import cg.base.io.message.RequestServerRegister;
import cg.base.io.message.RequestServerSelect;
import cg.base.io.message.RequestServerUnregister;
import cg.base.io.message.ResponseAccountChangePassword;
import cg.base.io.message.ResponseAccountLogin;
import cg.base.io.message.ResponseAccountLogout;
import cg.base.io.message.ResponseAccountRegister;
import cg.base.io.message.ResponseExecuteError;
import cg.base.io.message.ResponseServerList;
import cg.base.io.message.ResponseServerSelect;
import cg.base.io.message.VoServer;
import cg.base.util.SenderUtils;
import dataplatform.persist.IEntityManager;
import dataplatform.pubsub.ISimplePubsub;
import dataplatform.pubsub.impl.SimplePubsub;
import net.content.IContent;
import net.content.IContentFactory;
import net.content.SimpleContentFactory;
import net.dipatch.IDispatchManager;
import net.io.INetServer;
import net.io.netty.server.INettyServerConfig;
import net.io.netty.server.tcp.NettyTcpServer;
import net.message.IMessage;

public class AuthenticateService {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateService.class);
	
	private final int port;
	
	private final String address;
	
	private final Map<String, Account> accounts;
	
	private final Map<String, String> accountSessions;
	
	private final Map<String, ServerInfo> senders;
	
	private final ISimplePubsub pubsub;
	
	private IEntityManager entityManager;
	
	public AuthenticateService(String address, int port) {
		senders = Maps.newConcurrentMap();
		accounts = Maps.newConcurrentMap();
		accountSessions = Maps.newConcurrentMap();
		this.address = address;
		this.port = port;
		pubsub = new SimplePubsub();
		pubsub.subscribe(this);
	}
	
	public void bind(IEntityManager entityManager, ScheduledExecutorService scheduler) throws Exception {
		this.entityManager = entityManager;
		
//		INetServer netServer = new NettyTcpServer(address, port, new SimpleContentHandler(pubsub), new SimpleContentFactory(new DataAnthenticate()));
		INetServer netServer = new NettyTcpServer(new INettyServerConfig() {
			
			@Override
			public long getWriterIdleTime() {
				return 60;
			}
			
			@Override
			public int getSoBacklog() {
				return 100;
			}
			
			@Override
			public long getReaderIdleTime() {
				return 60;
			}
			
			@Override
			public int getPort() {
				return port;
			}
			
			@Override
			public int getParentThreadNum() {
				return 0;
			}
			
			@Override
			public IContentFactory getNettyContentFactory() {
				return new SimpleContentFactory(new DataAnthenticate());
			}
			
			@Override
			public String getIp() {
				return address;
			}
			
			@Override
			public IDispatchManager getDispatchManager() {
				return new IDispatchManager() {
					
					private SimpleContentHandler contentHandler = new SimpleContentHandler(pubsub);
					
					@Override
					public void fireDispatch(IContent content) {
						try {
							contentHandler.handle(content);
						} catch (Exception e) {
							log.error("", e);
						}
					}
					
					@Override
					public void addDispatch(IContent content) {
						fireDispatch(content);
					}
					
					@Override
					public void disconnect(IContent content) {
						contentHandler.disconnect(content.getSessionId(), content.getSender().getIp());
					}
					
				};
			}
			
			@Override
			public int getChildThreadNum() {
				return 0;
			}
			
			@Override
			public long getAllIdleTime() {
				return 60;
			}
			
		});
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
				log.error("", e);
			}
		}
		
	}
	
	@Subscribe
	public void registerServer(RequestServerRegister requestServerRegister) {
		String name = getServerName(requestServerRegister);
		if (!senders.containsKey(name)) {
			senders.put(name, new ServerInfo(name, requestServerRegister.getName(), requestServerRegister.getSender().getIp().split(":")[0] + ":" + requestServerRegister.getPort(), requestServerRegister.getSender()));
			log.info("registerServer : {}", name);
		}
	}

	@Subscribe
	public void unregisterServer(RequestServerUnregister requestServerUnregister) {
		unregisterServer(getServerName(requestServerUnregister));
	}
	
	private void unregisterServer(String name) {
		if (senders.containsKey(name)) {
			senders.remove(name);
			for (Account account : accounts.values()) {
				if (account.getServerName().equals(name)) {
					accounts.remove(account.getName());
				}
			}
			log.info("unregisterServer : {}", name);
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
		SenderUtils.send(requestAccountRegister.getSender(), message);
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
			message = error;
		} else {
			account.setServerName(getServerName(requestAccountLogin));
			accounts.put(name, account);
			accountSessions.put(requestAccountLogin.getSessionId(), name);
			log.info("{} login.", name);
			
			message = new ResponseAccountLogin();
		}
		SenderUtils.send(requestAccountLogin.getSender(), message);
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
			log.info("{} logout.", name);
			
			message = new ResponseAccountLogout();
		}
		SenderUtils.send(requestAccountLogout.getSender(), message);
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
		SenderUtils.send(requestAccountChangePassword.getSender(), message);
	}
	
	@Subscribe
	public void eventDisconnect(EventDisconnect eventDisconnect) {
		String name = accountSessions.remove(eventDisconnect.getSessionId());
		if (name != null) {
			accounts.remove(name);
			log.info("{} logout.", name);
		} else {
			unregisterServer(eventDisconnect.getAddress());
		}
	}
	
	@Subscribe
	public void requestServerList(RequestServerList requestServerList) {
		if (getAccount(requestServerList.getSessionId()) != null) {
			ResponseServerList responseServerList = new ResponseServerList();
			List<VoServer> servers = Lists.newArrayListWithCapacity(senders.size());
			for (ServerInfo serverInfo : senders.values()) {
				VoServer server = new VoServer();
				server.setKey(serverInfo.getKey());
				server.setName(serverInfo.getName());
				server.setUrl("");
				servers.add(server);
			}
			responseServerList.setServers(servers);
			SenderUtils.send(requestServerList.getSender(), responseServerList);
		}
	}
	
	@Subscribe
	public void requestServerSelect(RequestServerSelect requestServerSelect) {
		String sessionId = requestServerSelect.getSessionId();
		Account account = getAccount(sessionId);
		if (account != null) {
			String key = requestServerSelect.getKey();
			if (senders.containsKey(key)) {
				RequestAccountAuthenticate requestAccountAuthenticate = new RequestAccountAuthenticate();
				requestAccountAuthenticate.setKey(sessionId);
				requestAccountAuthenticate.setAccount(account.getName());
				requestAccountAuthenticate.setAccountId(account.getId());
				requestAccountAuthenticate.setImoney(account.getImoney());
				requestAccountAuthenticate.setIp(requestServerSelect.getSender().getIp().split(":")[0]);
				ServerInfo serverInfo = senders.get(key);
				SenderUtils.send(serverInfo.getSender(), requestAccountAuthenticate);
				
				ResponseServerSelect responseServerSelect = new ResponseServerSelect();
				responseServerSelect.setKey(sessionId);
				responseServerSelect.setUrl(serverInfo.getUrl());
				SenderUtils.send(requestServerSelect.getSender(), responseServerSelect);
			} else {
				ResponseExecuteError error = new ResponseExecuteError();
				error.setErrorCode(0);
				error.setMessage("server is null");
				error.setMessageId(requestServerSelect.getMessageId());
				SenderUtils.send(requestServerSelect.getSender(), error);
			}
		}
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
	
	private Account getAccount(String sessionId) {
		String name = accountSessions.get(sessionId);
		return name == null ? null : accounts.get(name);
	}

}
