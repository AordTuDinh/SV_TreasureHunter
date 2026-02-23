package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangRu extends Lang {
    static LangRu mInstance;

    public static LangRu instance() {
        if (mInstance == null) {
            mInstance = new LangRu();
        }
        return mInstance;
    }

    public LangRu() {
        locale = LOCALE_RU;
    }

    public String get(String key) {
        return get(key, locale);
    }



}
