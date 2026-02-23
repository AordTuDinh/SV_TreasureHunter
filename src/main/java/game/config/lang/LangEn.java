package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangEn extends Lang {

    static LangEn mInstance;

    public static LangEn instance() {
        if (mInstance == null) {
            mInstance = new LangEn();
        }
        return mInstance;
    }

    public LangEn() {
        locale = LOCALE_EN;
    }

    public String get(String key) {
        return get(key, locale);
    }
}
