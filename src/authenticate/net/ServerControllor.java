package authenticate.net;

import org.tool.server.io.proto.IMessageSender;

import cg.base.io.message.interfaces.ICsAccountAuthenticate;
import cg.base.io.message.interfaces.ICsServerRegister;
import cg.base.io.message.processor.IServerProtosProcessor;

public class ServerControllor implements IServerProtosProcessor {
	
	private AuthenticateService authenticateService;

	@Override
	public void processServerRegister(ICsServerRegister csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processAccountAuthenticate(ICsAccountAuthenticate csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

}
