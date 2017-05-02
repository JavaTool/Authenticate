package org.authenticate.app;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.UUID;

import org.tool.server.persist.IEntityManager;

public final class AppService implements IAppService {
	
	private static final String ERROR_APP_NULL = "1";
	
	private static final String FROM = "from App";
	
	private static final String FETCH = FROM + " where appId=?0 and appKey=?1";
	
	private final IEntityManager entityManager;
	
	public AppService(IEntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public App getApp(String appId, String appKey) {
		return checkNotNull(entityManager.fetch(App.class, FETCH, appId, appKey), ERROR_APP_NULL);
	}

	@Override
	public String authorize(String appId, String appKey, int accountId) {
		App app = getApp(appId, appKey);
		String openId = getOpenId(app, accountId);
		if (openId == null) {
			openId = UUID.randomUUID().toString();
			entityManager.nativeSQLUpate("insert into " + app.getAppTable() + " set accountId=?0, openId=?1", accountId, openId);
			return openId;
		} else {
			return openId;
		}
	}

	@Override
	public void createApp(App app) {
		entityManager.createSync(app);
	}

	@Override
	public void updateApp(App app) {
		entityManager.updateSync(app);
	}

	@Override
	public void removeApp(App app) {
		entityManager.deleteSync(app);
	}

	@Override
	public String getOpenId(String appId, String appKey, int accountId) {
		return getOpenId(getApp(appId, appKey), accountId);
	}

	private String getOpenId(App app, int accountId) {
		@SuppressWarnings("rawtypes")
		List list = entityManager.nativeQuery("select openId from " + app.getAppEntry() + " where accountId=?0", accountId);
		return list.size() > 0 ? list.get(0).toString() : null;
	}

}
