package authenticate.net;

import java.io.DataOutputStream;

import org.tool.server.anthenticate.IDataAnthenticate;

class DataAnthenticate implements IDataAnthenticate<byte[], DataOutputStream> {
	
	private static final String HEAD = "CrossGateProtoBuf";

	@Override
	public void write(DataOutputStream out) throws Exception {
		out.write(HEAD.getBytes());
	}

	@Override
	public boolean read(byte[] in) {
		try {
			String head = new String(in);
			return HEAD.equals(head);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public int getAnthenticateLength() {
		return HEAD.length();
	}

}
