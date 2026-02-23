package ozudo.base.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class slib_Logger {
    public static Logger access() {
        Logger logger = LoggerFactory.getLogger("ACCESS");
        return logger;
    }

    public static Logger event() {
        Logger logger = LoggerFactory.getLogger("EVENT");
        return logger;
    }

    public static Logger root() {
        Logger logger = LoggerFactory.getLogger("DSON");
        return logger;
    }

    public static Logger api() {
        Logger logger = LoggerFactory.getLogger("API");
        return logger;
    }

    public static Logger act() {
        Logger logger = LoggerFactory.getLogger("ACT");
        return logger;
    }

    public static Logger redis() {
        Logger logger = LoggerFactory.getLogger("REDISLOG");
        return logger;
    }

    public static Logger money() {
        Logger logger = LoggerFactory.getLogger("MONEY");
        return logger;
    }
}
