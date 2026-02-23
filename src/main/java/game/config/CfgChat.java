package game.config;

import com.google.gson.Gson;
import net.sf.json.JSONObject;
import ozudo.base.database.DBJPA;

import java.util.List;
import java.util.regex.Pattern;

public class CfgChat {
    public static JSONObject json;

    public static DataConfig config;
    static String validCharacter = "\\s\", \\.'\\*?/:;\\-_+=#$%&Z*!<>\\[{}\\]()“”~&^0123456789ABCDEFGHIJKLMNOPQRSTXYUWZVĂÂĐÊÔƠƯÀẢÃÁẠĂẰẲẴẮẶÂẦẨẪẤẬĐÈẺẼÉẸÊỀỂỄẾỆÌỈĨÍỊÒỎÕÓỌÔỒỔỖỐỘƠỜỞỠỚỢÙỦŨÚỤƯỪỬỮỨỰỲỶỸÝỴzxcvbnmasdfghjklqwertyuiopăâđêôơưàảãáạăằẳẵắặâầẩẫấậđèẻẽéẹêềểễếệìỉĩíịòỏõóọôồổỗốộơờởỡớợùủũúụưừửữứựỳỷỹýỵ";
    //static String checkCharacter = "ABCDEFGHIJKLMNOPQRSTXYUWZVĂÂĐÊÔƠƯÀẢÃÁẠĂẰẲẴẮẶÂẦẨẪẤẬĐÈẺẼÉẸÊỀỂỄẾỆÌỈĨÍỊÒỎÕÓỌÔỒỔỖỐỘƠỜỞỠỚỢÙỦŨÚỤƯỪỬỮỨỰỲỶỸÝỴzxcvbnmasdfghjklqwertyuiopăâđêôơưàảãáạăằẳẵắặâầẩẫấậđèẻẽéẹêềểễếệìỉĩíịòỏõóọôồổỗốộơờởỡớợùủũúụưừửữứựỳỷỹýỵ";
    public static int chatSpam = 1;//block time seconds
    public static int maxCharacter = 100; // tối đa 100 kí tự
    public static int maxClanName = 100; // tối đa 100 kí tự
    public static int maxChatMap = 60; // tối đa 60 kí tự
    public static int maxSaveChat = 30; // tối đa 30 tin nhắn
    public static List<String> aChatInvalid;

    public static String replaceInvalidWord(String msg) {
        String newMsg = "";
        for (int i = 0; i < msg.length(); i++) {
            String newChar = String.valueOf(msg.charAt(i));
            if (validCharacter.contains(newChar)) {
                newMsg += newChar;
            }
        }
        // replace phone number
        String cache = newMsg.replaceAll("\\d{8,}", "***");
        String ret = newMsg.toLowerCase();
        boolean edit = false;
        for (String word : aChatInvalid) {
            if (ret.contains(word)) {
                ret = ret.replaceAll("(?i)" + Pattern.quote(word), "***");
                edit = true;
            }
        }
        return edit ? ret : cache;
    }

    public static boolean validName(String msg) {
        boolean valid = !msg.matches("\\d{8,}");
        if(!valid) return valid;
        String ret = msg.toLowerCase();
        for (String word : aChatInvalid) {
            if (ret.contains(word)) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    public static boolean validText(String msg) {
        String ret = msg.toLowerCase();
        for (String word : aChatInvalid) {
            if (ret.contains(word)) {
                return true;
            }
        }
        return false;
    }


    public static String getKeyChatFriend(int userId1, int userId2) {
        return userId1 < userId2 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    public static boolean isValidChat(String newMsg, String lastMsg) {
        if (newMsg.contains("<") || newMsg.contains(">")) return false;
        if (true) return true;
        newMsg = newMsg.toLowerCase();
        lastMsg = lastMsg.toLowerCase();
        String newTmp = "", lastTmp = "";
        for (int i = 0; i < newMsg.length(); i++) {
            String newChar = String.valueOf(newMsg.charAt(i));
            if (validCharacter.contains(newChar)) {
                newTmp += newChar;
            }
        }

        for (int i = 0; i < lastMsg.length(); i++) {
            String newChar = String.valueOf(lastMsg.charAt(i));
            if (validCharacter.contains(newChar)) {
                lastTmp += newChar;
            }
        }

        if (lastMsg.equals(newMsg)) {
            return false;
        }
        return true;
    }

    public static void loadConfig(String strJson) {
        json = JSONObject.fromObject(strJson);
        config = new Gson().fromJson(strJson, DataConfig.class);
        loadConfigChat();
    }

    public static void loadConfigChat() {
        aChatInvalid = DBJPA.getEntityManager().createNativeQuery("select k from " + CfgServer.DB_MAIN + "res_chat_invalid").getResultList();
    }


    public class DataConfig {
        public List<String> invalidChat, invalidUser;
    }
}
