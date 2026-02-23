package game.object;

import org.apache.commons.exec.CommandLine;
import ozudo.base.log.Logs;
import java.io.IOException;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;

public class ServerUtils {
    public static String resetServer() {
        String ret = "";
        CommandLine oCmdLine = CommandLine.parse("sh /root/test/run.sh");
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
        oDefaultExecutor.setExitValue(0);
        try {
            return String.valueOf(oDefaultExecutor.execute(oCmdLine));
        } catch (ExecuteException e) {
            ret = "Execution failed.";
            Logs.error(e);
        } catch (IOException e) {
            ret = "permission denied.";
            Logs.error(e);
        }
        return ret;
    }
}
