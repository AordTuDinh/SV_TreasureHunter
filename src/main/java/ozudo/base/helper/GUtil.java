package ozudo.base.helper;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.math.BigInteger;
import java.security.MessageDigest;

public class GUtil {
    public static void main(String[] args) {
        System.out.println(getMD5(String.format("%s|1|1|%s|%s", "04c2831b362bedd72170682911c335db", "20200612112010", "c82a907929b45511ff93a4705c7eddc1")));
    }

    public static String getMD5(String source) {
        try {
            MessageDigest mdEnc = MessageDigest.getInstance("MD5"); // Encryption
            // algorithm
            mdEnc.update(source.getBytes(), 0, source.length());

            String md5 = new BigInteger(1, mdEnc.digest()).toString(16); // Encrypted

            while (md5.length() < 32) {
                md5 = "0" + md5;
            }

            return md5;
        } catch (Exception ex) {
        }
        return "";
    }

    public static String exToString(Exception ex) {
//        StringWriter errors = new StringWriter();
//        ex.printStackTrace(new PrintWriter(errors, true));
        return ExceptionUtils.getFullStackTrace(ex);
    }

    public static String exToString(Throwable ex) {
        String tmp = "";
        for (StackTraceElement e : ex.getStackTrace()) {
            tmp += e.toString() + "\n";
        }
        return ex.toString() + ": " + tmp;
    }
}
