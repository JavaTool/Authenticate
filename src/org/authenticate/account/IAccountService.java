package org.authenticate.account;

import org.tool.server.account.Account;

public interface IAccountService {
	
	String signIn(Account account);
	
	Account signUp(Account account);
	
	void signOut(Account account);
	
	void change(Account account);
	
	int authenticate(Account account);
	
	Account authorizeApp(Account account);

}
