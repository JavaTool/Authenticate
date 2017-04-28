package org.authenticate.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.authenticate.account.IAccountService;
import org.tool.server.account.Account;
import org.tool.server.io.http.server.BaseServlet;

public final class AuthorizeAppServlet extends BaseServlet {

	private static final long serialVersionUID = 1L;
	
	private static final String NAME = IAccountService.class.getName();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		work(req, resp, (q, p, j) -> {
			Account account = readJson(q, Account.class);
			account = ((IAccountService) q.getServletContext().getAttribute(NAME)).authorizeApp(account);
			j.put("openId", account.getOpenId());
			writeOK(j, account.getLoginKey());
			return EMPTY_LIST;
		});
	}

}
