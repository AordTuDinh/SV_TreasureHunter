package ozudo.base.helper;

import com.google.gson.Gson;
import game.config.lang.Lang;
import org.apache.commons.lang.RandomStringUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class StringHelper {
    public String[] splitMultiple(String value, List<String> delimiters) {
        String pattern = delimiters.stream().collect(Collectors.joining("|"));
        return value.split(pattern);
    }

    public static int convertVersion2Int(String version) {
        return isEmpty(version) ? 0 : Integer.parseInt(version.replace(".", ""));
    }

    public static String removeInvalidCharacter(String value) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            int tmp = value.charAt(i);
            if (('a' <= tmp && tmp <= 'z') || ('A' <= tmp && tmp <= 'Z') || ('0' <= tmp && tmp <= '9'))
                buff.append(value.charAt(i));
        }
        return buff.toString();
    }

    public static boolean validEmail(String email) {
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return false;
        }
        return true;
    }

    public static boolean validName(String name) {
        if (!name.matches("[a-zA-Z0-9]+")) {
            return false;
        }
        return true;
    }

    static String validCharacter = "\\s\", \\.'\\*?/:;\\-_+=#$%&Z*!<>\\[{}\\]()“”~&^0123456789ABCDEFGHIJKLMNOPQRSTXYUWZVĂÂĐÊÔƠƯÀẢÃÁẠĂẰẲẴẮẶÂẦẨẪẤẬĐÈẺẼÉẸÊỀỂỄẾỆÌỈĨÍỊÒỎÕÓỌÔỒỔỖỐỘƠỜỞỠỚỢÙỦŨÚỤƯỪỬỮỨỰỲỶỸÝỴzxcvbnmasdfghjklqwertyuiopăâđêôơưàảãáạăằẳẵắặâầẩẫấậđèẻẽéẹêềểễếệìỉĩíịòỏõóọôồổỗốộơờởỡớợùủũúụưừửữứựỳỷỹýỵ";

    public static String chatFormat(String msg) {
        String tmp = "";

        // remove invalid character
        for (int i = 0; i < msg.length(); i++) {
            if (validCharacter.contains(String.valueOf(msg.charAt(i)))) {
                tmp += msg.charAt(i);
            }
        }

        // remove double space
        while (tmp.contains("  ")) {
            tmp = tmp.replaceAll("  ", " ");
        }

        return msg;
    }

    public static String removeNonePrintChars(String value) {
        return value.replaceAll("\\p{C}", "");
    }

    /**
     * Add "0" at the head for enough length
     *
     * @return
     */
    public static String formatLengthNumber(int number, int length) {
        String tmp = String.valueOf(number);
        while (tmp.length() < length) {
            tmp = "0" + tmp;
        }
        return tmp;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String nonNullValue(String str) {
        return isEmpty(str) ? "" : str;
    }

    /**
     * Chuyển object thành String và remove các dấu " " để lưu vào db
     *
     * @param obj
     * @return
     */
    public static String toDBString(Object obj) {
        return new Gson().toJson(obj).replace(" ", "");
    }

    public static <T> String toDBList(List<T> values) {
        String str = toDBString(values);
        return "(" + str.substring(1, str.length() - 1) + ")";
    }

    public static String getRandomString(int count) {
        return RandomStringUtils.randomAlphanumeric(count).toUpperCase();
    }


    public static String getRandomStringArr(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }
}
