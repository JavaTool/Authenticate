package authenticate.net;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tool.server.io.INetServer;
import org.tool.server.io.dispatch.DispatchManager;
import org.tool.server.io.dispatch.IContentFactory;
import org.tool.server.io.dispatch.IDispatchManager;
import org.tool.server.io.dispatch.SimpleContentFactory;
import org.tool.server.io.netty.server.INettyServerConfig;
import org.tool.server.io.netty.server.tcp.NettyTcpServer;
import org.tool.server.io.proto.IMessageSender;
import org.tool.server.io.proto.ProtoHandler;
import org.tool.server.persist.IEntityManager;
import org.tool.server.pubsub.IPubsub;
import org.tool.server.pubsub.impl.EventBusPubsub;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;

import authenticate.account.Account;
import cg.base.event.EventDisconnect;
import cg.base.io.MessageIdTransform;
import cg.base.io.message.interfaces.IVoServer;
import cg.base.io.message.proto.CsAccountAuthenticate;
import cg.base.io.message.proto.ScExecuteError;
import cg.base.io.message.proto.ScServerList;
import cg.base.io.message.proto.ScServerSelect;
import cg.base.io.message.proto.VoServer;
import cg.base.io.proto.MessageIdProto.MessageId;

public class AuthenticateService {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateService.class);
	
	private final int port;
	
	private final String address;
	
	private final Map<String, Account> accounts;
	
	private final Map<String, String> accountSessions;
	
	private final Map<String, ServerInfo> senders;
	
	private final IPubsub<Object> pubsub;
	
	private IEntityManager entityManager;
	
	public AuthenticateService(String address, int port) {
		senders = Maps.newConcurrentMap();
		accounts = Maps.newConcurrentMap();
		accountSessions = Maps.newConcurrentMap();
		this.address = address;
		this.port = port;
		pubsub = new EventBusPubsub();
		pubsub.subscribe(this);
	}
	
	public void bind(IEntityManager entityManager, ScheduledExecutorService scheduler) throws Exception {
		this.entityManager = entityManager;
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
				return new SimpleContentFactory(null);
			}
			
			@Override
			public String getIp() {
				return address;
			}
			
			@Override
			public IDispatchManager getDispatchManager() {
				return new DispatchManager(new ProtoHandler(new MessageIdTransform(), "noProcessorError"), 100, 1, Lists.newLinkedList());
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

	public void registerServer(String ip, String serverName, int port, IMessageSender sender) {
		String name = ip;
		if (!senders.containsKey(name)) {
			senders.put(name, new ServerInfo(name, serverName, ip.split(":")[0] + ":" + port, sender));
			log.info("registerServer : {}", name);
		}
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

	public void registerAccount(String name, String password, IMessageSender sender) {
		if (hasAccountName(name)) {
			ScExecuteError error = new ScExecuteError();
			error.setErrorCode(0);
			error.setMessage("account already register.");
			error.setMessageId(MessageId.MI_CS_ACCOUNT_REGISTER_VALUE);
			sender.send(error);
		} else { // OK
			Account account = new Account();
			account.setName(name);
			account.setPassword(password);
			entityManager.createSync(account);
			sender.send(MessageId.MI_SC_ACCOUNT_REGISTER_VALUE);
		}
	}

	public void accountLogin(String name, String password, IMessageSender sender, String ip) {
		Account account = findAccount(name, password);
		if (account == null) {
			ScExecuteError error = new ScExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(MessageId.MI_CS_ACCOUNT_LOGIN_VALUE);
			sender.send(error);
		} else if (accounts.containsKey(name)) {
			ScExecuteError error = new ScExecuteError();
			error.setErrorCode(0);
			error.setMessage("account already login");
			error.setMessageId(MessageId.MI_CS_ACCOUNT_LOGIN_VALUE);
			sender.send(error);
		} else {
			account.setServerName(ip);
			accounts.put(name, account);
			accountSessions.put(sender.getSessionId(), name);
			log.info("{} login.", name);
			sender.send(MessageId.MI_SC_ACCOUNT_LOGIN_VALUE);
		}
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

	public void accountLogout(IMessageSender sender) {
//		int serial = packet.getInt();
		String sessionId = sender.getSessionId();
		String name = accountSessions.get(sessionId);
		Account account = name == null ? null : accounts.remove(name);
		if (account == null) {
			ScExecuteError error = new ScExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(MessageId.MI_CS_ACCOUNT_LOGOUT_VALUE);
			sender.send(error);
		} else {
			accountSessions.remove(sessionId);
			accounts.remove(name);
			log.info("{} logout.", name);
			sender.send(MessageId.MI_SC_ACCOUNT_LOGOUT_VALUE);
		}
	}

	public void changePassword(String name, String password, String newPassword, IMessageSender sender) {
//		int serial = packet.getInt();
		Account account = findOnlineAccount(name, password);
		if (account == null) {
			ScExecuteError error = new ScExecuteError();
			error.setErrorCode(0);
			error.setMessage("account is null");
			error.setMessageId(MessageId.MI_CS_ACCOUNT_CHANGE_PASSWORD_VALUE);
			sender.send(error);
		} else {
			account.setPassword(newPassword);
			entityManager.updateSync(account);
			sender.send(MessageId.MI_SC_ACCOUNT_CHANGE_PASSWORD_VALUE);
		}
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
	
	public void requestServerList(IMessageSender sender) {
		if (getAccount(sender.getSessionId()) != null) {
			ScServerList responseServerList = new ScServerList();
			List<IVoServer> servers = Lists.newArrayListWithCapacity(senders.size());
			for (ServerInfo serverInfo : senders.values()) {
				VoServer server = new VoServer();
				server.setKey(serverInfo.getKey());
				server.setName(serverInfo.getName());
				server.setUrl("");
				servers.add(server);
			}
			responseServerList.setServers(servers);
			sender.send(responseServerList);
		}
	}
	
	public void requestServerSelect(String key, String ip, IMessageSender sender) {
		String sessionId = sender.getSessionId();
		Account account = getAccount(sessionId);
		if (account != null) {
			if (senders.containsKey(key)) {
				CsAccountAuthenticate requestAccountAuthenticate = new CsAccountAuthenticate();
				requestAccountAuthenticate.setKey(sessionId);
				requestAccountAuthenticate.setAccount(account.getName());
				requestAccountAuthenticate.setAccountId(account.getId());
				requestAccountAuthenticate.setImoney(account.getImoney());
				requestAccountAuthenticate.setIp(ip);
				ServerInfo serverInfo = senders.get(key);
				serverInfo.getSender().send(requestAccountAuthenticate);
				
				ScServerSelect responseServerSelect = new ScServerSelect();
				responseServerSelect.setKey(sessionId);
				responseServerSelect.setUrl(serverInfo.getUrl());
				sender.send(responseServerSelect);
			} else {
				ScExecuteError error = new ScExecuteError();
				error.setErrorCode(0);
				error.setMessage("server is null");
				error.setMessageId(MessageId.MI_CS_SERVER_SELECT_VALUE);
				sender.send(error);
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
