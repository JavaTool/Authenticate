package org.authenticate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tool.server.io.INetServer;
import org.tool.server.io.jetty.IJettyConfig;
import org.tool.server.io.jetty.JettyServer;

final class AuthenticateServer implements IJettyConfig {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateServer.class);
	
	private int port;
	
	public static void main(String[] args) {
		INetServer server = new JettyServer(new AuthenticateServer());
		try {
			server.bind();
		} catch (Exception e) {
			log.error("", e);
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPort() {
		return port;
	}

}
