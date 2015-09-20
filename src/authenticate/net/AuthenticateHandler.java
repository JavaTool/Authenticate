package authenticate.net;

import net.io.mina.DispatchPacket;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticateHandler extends IoHandlerAdapter {
	
	private static final Logger log = LoggerFactory.getLogger(AuthenticateHandler.class);
	
	private final AuthenticateService service;
	
	public AuthenticateHandler(AuthenticateService service) {
		this.service = service;
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.debug(cause.getMessage(), cause);
	}

	@Override  
	public void messageReceived(IoSession session, Object message) throws Exception {
		if (message instanceof DispatchPacket) {
			service.processPacket(session, (DispatchPacket) message);
		}
	}

	@Override  
	public void sessionClosed(IoSession session) throws Exception {  
		service.unregisterServer(session);
	}

	@Override  
	public void sessionCreated(IoSession session) throws Exception {
		service.registerServer(session);
	}

}
