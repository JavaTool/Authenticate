package authenticate.account;

import java.io.Serializable;

public class Account implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final byte LOGIN_ERROR_NULL = 0;
	
	public static final byte LOGIN_ERROR_REPEAT = 1;
	
	private String name, password;
	
	private String serverName;
	
	private int imoney, id;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getImoney() {
		return imoney;
	}

	public synchronized void setImoney(int imoney) {
		this.imoney = imoney;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "id[" + getId() + "] name[" + getName() + "]";
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

}
