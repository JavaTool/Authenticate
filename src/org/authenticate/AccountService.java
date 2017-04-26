package org.authenticate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.tool.server.account.Account;
import org.tool.server.cache.ICache;
import org.tool.server.cache.ICacheHash;
import org.tool.server.persist.IEntityManager;

final class AccountService implements IAccountService {
	
	private static final String ERROR_ACCOUNT_NAME = "1";
	
	private static final String ERROR_ACCOUNT_PASSWORD = "2";
	
	private static final String ERROR_ACCOUNT_EXISTS = "3";
	
	private static final String FROM = "from Account";
	
	private static final String FETCH_ACCOUNT_BY_NAME = FROM + " where name=?0";
	
	private static final String FIELD_KEY = "key";
	
	private static final String FIELD_PASSWORD = "password";
	
	private static final String FIELD_ID = "id";
	
	private final IEntityManager entityManager;
	
	private final ICache<String, String, String> cache;
	
	private final int expire;
	
	public AccountService(IEntityManager entityManager, ICache<String, String, String> cache, int expire) {
		this.entityManager = entityManager;
		this.cache = cache;
		this.expire = expire;
	}

	@Override
	public String signIn(Account account) {
		Account existsAccount = checkNotNull(entityManager.fetch(Account.class, FETCH_ACCOUNT_BY_NAME, account.getName()), ERROR_ACCOUNT_NAME);
		checkArgument(existsAccount.getPassword().equals(account.getPassword()), ERROR_ACCOUNT_PASSWORD);
		return cacheAccount(existsAccount);
	}
	
	private static String generateKey() {
		return UUID.randomUUID().toString();
	}
	
	private String cacheAccount(Account account) {
		String key = generateKey();
		ICacheHash<String, String, String> hash = cache.hash();
		String name = account.getName();
		hash.set(name, FIELD_KEY, key);
		hash.set(name, FIELD_PASSWORD, account.getPassword());
		hash.set(name, FIELD_ID, String.valueOf(account.getId()));
		cache.key().expire(name, expire, TimeUnit.MILLISECONDS);
		return key;
	}

	@Override
	public String signUp(Account account) {
		checkArgument(entityManager.fetch(Account.class, FETCH_ACCOUNT_BY_NAME, account.getName()) != null, ERROR_ACCOUNT_EXISTS);
		entityManager.createSync(account);
		return cacheAccount(account);
	}

	@Override
	public void signOut(Account account) {
		authenticate(account);
	}

	@Override
	public void change(Account account) {
		checkNotNull(entityManager.fetch(Account.class, FETCH_ACCOUNT_BY_NAME, account.getName()), ERROR_ACCOUNT_NAME);
		entityManager.updateSync(account);
	}

	@Override
	public boolean authenticate(Account account) {
		String name = account.getName();
		boolean ret = account.getLoginKey().equals(cache.hash().get(name, FIELD_KEY));
		if (ret) {
			cache.key().delete(name);
		}
		return ret;
	}

}
