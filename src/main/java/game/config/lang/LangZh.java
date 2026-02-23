package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangZh extends Lang {
    static LangZh mInstance;

    public static LangZh instance() {
        if (mInstance == null) {
            mInstance = new LangZh();
        }
        return mInstance;
    }

    public LangZh() {
        locale = LOCALE_ZH;
    }

    public String get(String key) {
        return get(key, locale);
    }



}
