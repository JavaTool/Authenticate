package authenticate.net;

import org.apache.commons.logging.LogFactory;

import cg.base.log.Log;

class CLog implements Log {
	
	private static org.apache.commons.logging.Log log = LogFactory.getLog(Log.class);

	@Override
	public void info(String info) {
		log.info(info);
	}

	@Override
	public void warning(String warning) {
		log.warn(warning);
	}

	@Override
	public void error(String error) {
		log.error(error);
	}

	@Override
	public void print(String head, String message) {
		log.info("[" + head + "]" + message);
	}

	@Override
	public void error(String error, Throwable t) {
		log.error(error, t);
	}

}
