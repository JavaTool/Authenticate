package authenticate.net;

import org.tool.server.io.proto.IMessageSender;

class ServerInfo {
	
	private final String key;
	
	private final String name;
	
	private final String url;
	
	private final IMessageSender sender;
	
	public ServerInfo(String key, String name, String url, IMessageSender sender) {
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

	public IMessageSender getSender() {
		return sender;
	}

}
