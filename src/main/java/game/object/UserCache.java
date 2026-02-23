package game.object;

import java.io.Serializable;
import java.util.*;

public class UserCache implements Serializable {
    Map<String, Object> mCache = new HashMap<String, Object>();

    public Object get(String key) {
        return mCache.get(key);
    }

    public void set(String key, Object value) {
        mCache.put(key, value);
    }

    public void del(String key) {
        mCache.remove(key);
    }
    //endregion
}
