package authenticate.net;

import net.dipatch.ISender;

class ServerInfo {
	
	private final String key;
	
	private final String name;
	
	private final String url;
	
	private final ISender sender;
	
	public ServerInfo(String key, String name, String url, ISender sender) {
		this.key = key;
		this.name = name;
		this.url = url;
		this.sender = sender;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public ISender getSender() {
		return sender;
	}

}
