package org.authenticate.account;

import org.tool.server.account.Account;

public interface IAccountService {
	
	String signIn(Account account);
	
	Account signUp(Account account);
	
	void signOut(Account account);
	
	void change(Account account);
	
	boolean authenticate(Account account);
	
	Account authorizeApp(Account account);

}
