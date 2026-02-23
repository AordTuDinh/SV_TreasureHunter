package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangJp extends Lang {
    static LangJp mInstance;

    public static LangJp instance() {
        if (mInstance == null) {
            mInstance = new LangJp();
        }
        return mInstance;
    }

    public LangJp() {
        locale = LOCALE_JP;
    }

    public String get(String key) {
        return get(key, locale);
    }



}
