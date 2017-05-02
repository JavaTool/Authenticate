package org.authenticate.account;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.authenticate.app.IAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tool.server.account.Account;
import org.tool.server.cache.ICache;
import org.tool.server.cache.ICacheHash;
import org.tool.server.persist.IEntityManager;

public final class AccountService implements IAccountService {
	
	private static final String ERROR_ACCOUNT_NAME = "1";
	
	private static final String ERROR_ACCOUNT_PASSWORD = "2";
	
	private static final String ERROR_ACCOUNT_EXISTS = "3";
	
	private static final String ERROR_NOT_SIGN_UP = "1";
	
	private static final String FROM = "from Account";
	
	private static final String FETCH_ACCOUNT_BY_NAME = FROM + " where name=?0";
	
	private static final String FIELD_KEY = "key";
	
	private static final String FIELD_OPEN_ID = "openId";
	
	private static final String FIELD_ID = "id";
	
	private static final Logger log = LoggerFactory.getLogger(AccountService.class);
	
	private final IEntityManager entityManager;
	
	private final ICache<String, String, String> cache;
	
	private final int expire;
	
	private IAppService appService;
	
	public AccountService(IEntityManager entityManager, ICache<String, String, String> cache, int expire) {
		this.entityManager = entityManager;
		this.cache = cache;
		this.expire = expire;
	}

	@Override
	public String signIn(Account account) {
		checkArgument(entityManager.fetch(Account.class, FETCH_ACCOUNT_BY_NAME, account.getName()) != null, ERROR_ACCOUNT_EXISTS);
		entityManager.createSync(account);
		return cacheAccount(account);
	}
	
	private static String generateKey() {
		return UUID.randomUUID().toString();
	}
	
	private String cacheAccount(Account account) {
		String key = generateKey();
		ICacheHash<String, String, String> hash = cache.hash();
		hash.set(key, FIELD_KEY, key);
		hash.set(key, FIELD_OPEN_ID, account.getOpenId());
		hash.set(key, FIELD_ID, String.valueOf(account.getId()));
		cache.key().expire(key, expire, TimeUnit.MILLISECONDS);
		return key;
	}

	@Override
	public Account signUp(Account account) {
		Account existsAccount = checkNotNull(entityManager.fetch(Account.class, FETCH_ACCOUNT_BY_NAME, account.getName()), ERROR_ACCOUNT_NAME);
		checkArgument(existsAccount.getPassword().equals(account.getPassword()), ERROR_ACCOUNT_PASSWORD);
		account.setId(existsAccount.getId());
		String appId = account.getAppId();
		String appKey = account.getAppKey();
		if (appId != null && appId.length() > 0 && appKey != null && appKey.length() > 0) {
			String openId = appService.authorize(appId, appKey, account.getId());
			account.setOpenId(openId);
		} else {
			account.setOpenId("");
		}
		account.setLoginKey(cacheAccount(account));
		log.info("signUp {}, open id {}.", account.getName(), account.getOpenId());
		return account;
	}

	@Override
	public void signOut(Account account) {
		authenticate(account);
	}

	@Override
	public void change(Account account) {
		checkArgument(authenticate(account), ERROR_NOT_SIGN_UP);
		entityManager.updateSync(account);
	}

	@Override
	public boolean authenticate(Account account) {
		String key = account.getLoginKey();
		boolean ret = account.getOpenId().equals(cache.hash().get(key, FIELD_OPEN_ID));
		if (ret) {
			log.info("authenticate {}.", key);
			cache.key().delete(key);
		}
		return ret;
	}

	public void setAppService(IAppService appService) {
		this.appService = appService;
	}

	@Override
	public Account authorizeApp(Account account) {
		checkArgument(authenticate(account), ERROR_NOT_SIGN_UP);
		String openId = appService.authorize(account.getAppId(), account.getAppKey(), account.getId());
		account.setOpenId(openId);
		return account;
	}

}
