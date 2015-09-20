package authenticate.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.io.mina.DispatchPacket;
import net.io.mina.Packet;
import net.io.mina.server.DispatchUADecoder;
import net.io.mina.server.DispatchUAEncoder;
import net.io.util.IOUtil;

import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import authenticate.AuthenticateServer;
import authenticate.AuthenticateServer.OpcodeInfo;
import dataplatform.persist.IEntityManager;

public class AuthenticateService {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateService.class);
	
	private static final byte DISPATCH_PACKET_ID = -2;
	
	private SocketAcceptor acceptor;
	
	private String address;
	
	private int port;
	
	private Map<String, IoSession> sessions;
	
	private IEntityManager entityManager;
	
	private Map<String, Account> accounts;
	
	private Map<Integer, OpcodeInfo> methods;
	
	public AuthenticateService(String address, int port) {
		sessions = new ConcurrentHashMap<String, IoSession>();
		accounts = new ConcurrentHashMap<String, Account>();
		methods = new HashMap<Integer, OpcodeInfo>();
		this.address = address;
		this.port = port;
	}
	
	public void bind() throws IOException {
		acceptor = new SocketAcceptor();
		SocketAcceptorConfig cfg = new SocketAcceptorConfig();
		cfg.getFilterChain().addLast("codec", new ProtocolCodecFilter(DispatchUAEncoder.class, DispatchUADecoder.class));
		acceptor.bind(new InetSocketAddress(address, port),  new AuthenticateHandler(this), cfg);
		
		entityManager = AuthenticateServer.getEntityManager();

		for (OpcodeInfo opcode : AuthenticateServer.getOpcodes()) {
			methods.put(opcode.opcode, opcode);
		}
	}
	
	public void registerServer(IoSession session) {
		String name = getServerName(session);
		if (!sessions.containsKey(name)) {
			sessions.put(name, session);
			log.info("registerServer : " + name);
		}
	}
	
	public void unregisterServer(IoSession session) {
		String name = getServerName(session);
		if (sessions.containsKey(name)) {
			sessions.remove(name);
			for (Account account : accounts.values()) {
				if (account.getServerName().equals(name)) {
					accounts.remove(account.getName());
				}
			}
			log.info("unregisterServer : " + name);
		}
	}
	
	public void processPacket(IoSession session, DispatchPacket dp) throws Exception {
		OpcodeInfo info = methods.get(dp.getPacket().getOpCode());
		if (info != null) {
			getClass().getMethod(info.method, IoSession.class, Packet.class, int.class).invoke(this, session, dp.getPacket(), info.returnCode);
		}
	}
	
	public void registerAccount(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString();
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (hasAccountName(name)) {
			pt.put(IOUtil.FALSE);
		} else { // OK
			Account account = new Account();
			account.setName(name);
			account.setPassword(password);
			entityManager.createSync(account);

			pt.put(IOUtil.TRUE);
		}
		send(session, pt);
	}
	
	private String getServerName(IoSession session) {
		return session.getRemoteAddress().toString();
	}
	
	public void accountLogin(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString();
		Account account = findAccount(name, password);
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (account == null) {
			pt.put(IOUtil.FALSE);
			pt.put(Account.LOGIN_ERROR_NULL);
		} else if (accounts.containsKey(name)) {
			pt.put(IOUtil.FALSE);
			pt.put(Account.LOGIN_ERROR_REPEAT);
		} else {
			account.setServerName(getServerName(session));
			accounts.put(name, account);
			
			pt.put(IOUtil.TRUE);
			pt.putInt(account.getId());
			pt.putUTF(name);
			pt.putUTF(password);
			pt.putInt(account.getImoney());
		}
		send(session, pt);
	}
	
	public void deleteAccount(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString();
		Account account = findAccount(name, password);
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (account == null) {
			pt.put(IOUtil.FALSE);
		} else {
			accounts.remove(name);
			entityManager.deleteSync(account);
			
			pt.put(IOUtil.TRUE);
		}
		send(session, pt);
	}
	
	public void addImoney(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString();
		int imoney = packet.getInt();
		Account account = findAccount(name, password);
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (account == null) {
			pt.put(IOUtil.FALSE);
		} else {
			int result = account.getImoney() + imoney;
			if (result < 0) {
				pt.put(IOUtil.FALSE);
			} else {
				account.setImoney(result);
				entityManager.updateSync(account);
				
				pt.put(IOUtil.TRUE);
				pt.putInt(account.getImoney());
			}
		}
		send(session, pt);
	}
	
	public void accountLogout(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString();
		Account account = findOnlineAccount(name, password);
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (account == null) {
			pt.put(IOUtil.FALSE);
		} else {
			accounts.remove(name);
			
			pt.put(IOUtil.TRUE);
			pt.putUTF(name);
		}
		send(session, pt);
	}
	
	public void changePassword(IoSession session, Packet packet, int returnCode) {
		int serial = packet.getInt();
		String name = packet.getString(), password = packet.getString(), newPassword = packet.getString();
		Account account = findOnlineAccount(name, password);
		Packet pt = new Packet(returnCode);
		pt.putInt(serial);
		if (account == null) {
			pt.put(IOUtil.FALSE);
		} else {
			account.setPassword(newPassword);
			entityManager.updateSync(account);
			
			pt.put(IOUtil.TRUE);
		}
		send(session, pt);
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
	
	private static void send(IoSession session, Packet packet) {
		session.write(new DispatchPacket(DISPATCH_PACKET_ID, packet));
	}

}
