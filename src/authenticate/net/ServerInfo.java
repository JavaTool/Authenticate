package authenticate.net;

import net.dipatch.ISender;

class ServerInfo {
	
	private final String key;
	
	private final String name;
	
	private final ISender sender;
	
	public ServerInfo(String key, String name, ISender sender) {
		this.key = key;
		this.name = name;
		this.sender = sender;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public ISender getSender() {
		return sender;
	}

}
