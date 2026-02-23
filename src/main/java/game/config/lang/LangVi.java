package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangVi extends Lang {

    static LangVi mInstance;

    public static LangVi instance() {
        if (mInstance == null) {
            mInstance = new LangVi();
        }
        return mInstance;
    }

    public LangVi() {
        locale = LOCALE_VI;
    }

    public String get(String key) {
        return get(key, locale);
    }
}
