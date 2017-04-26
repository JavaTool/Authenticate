package org.authenticate;

import org.tool.server.account.Account;

public interface IAccountService {
	
	String signIn(Account account);
	
	String signUp(Account account);
	
	void signOut(Account account);
	
	void change(Account account);
	
	boolean authenticate(Account account);

}
