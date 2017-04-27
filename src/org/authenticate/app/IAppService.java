package org.authenticate.app;

public interface IAppService {
	
	App getApp(String appId, String appKey);
	
	String authorize(String appId, String appKey, int accountId);
	
	void createApp(App app);
	
	void updateApp(App app);
	
	void removeApp(App app);
	
	String getOpenId(String appId, String appKey, int accountId);

}
