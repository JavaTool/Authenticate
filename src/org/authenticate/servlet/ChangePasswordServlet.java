package org.authenticate.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.authenticate.account.IAccountService;
import org.tool.server.account.Account;
import org.tool.server.io.http.server.BaseServlet;

public final class ChangePasswordServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		work(req, resp, (q, p, j) -> {
			Account account = readJson(q, Account.class);
			IAccountService accountService = ((IAccountService) q.getServletContext().getAttribute(IAccountService.class.getName()));
			String key = accountService.signIn(account);
			account.setPassword(account.getLoginKey());
			account.setLoginKey(key);
			accountService.change(account);
			writeOK(j);
			return EMPTY_LIST;
		});
	}

}
