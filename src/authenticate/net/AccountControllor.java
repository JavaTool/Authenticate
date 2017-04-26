package authenticate.net;

import org.tool.server.io.proto.IMessageSender;

import cg.base.io.message.interfaces.ICsAccountChangePassword;
import cg.base.io.message.interfaces.ICsAccountLogin;
import cg.base.io.message.interfaces.ICsAccountRegister;
import cg.base.io.message.interfaces.ICsAccountRoleList;
import cg.base.io.message.interfaces.ICsServerSelect;
import cg.base.io.message.processor.IAccountProtosProcessor;

final class AccountControllor implements IAccountProtosProcessor {
	
	private AuthenticateService authenticateService;

	@Override
	public void processAccountRegister(ICsAccountRegister csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processAccountLogin(ICsAccountLogin csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processAccountRoleList(ICsAccountRoleList csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processServerSelect(ICsServerSelect csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processAccountChangePassword(ICsAccountChangePassword csMessage, IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processServerList(IMessageSender sender) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processAccountLogout(IMessageSender sender) {
		// TODO Auto-generated method stub

	}

}
