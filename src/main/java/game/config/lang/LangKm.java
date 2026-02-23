package game.config.lang;

import lombok.Data;

/**
 * Created by Mashi on 5/4/2015.
 */
@Data
public class LangKm extends Lang {
    static LangKm mInstance;

    public static LangKm instance() {
        if (mInstance == null) {
            mInstance = new LangKm();
        }
        return mInstance;
    }

    public LangKm() {
        locale = LOCALE_KM;
    }

    public String get(String key) {
        return get(key, locale);
    }



}
