package ozudo.base.helper;

import org.slf4j.Logger;
import ozudo.base.log.slib_Logger;

public interface RootLogger {
    default Logger getLogger() {
        return slib_Logger.root();
    }

}
