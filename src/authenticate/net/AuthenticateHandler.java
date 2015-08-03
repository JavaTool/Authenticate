package authenticate.net;

import io.DispatchPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;

public class AuthenticateHandler extends IoHandlerAdapter {
	
	private static final Log log = LogFactory.getLog(AuthenticateHandler.class);
	
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
