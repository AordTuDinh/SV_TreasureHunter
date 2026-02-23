package game.dragonhero.mapping.main;

import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;

public class BaseEntity {
    public void checkJson(Object id, String json) {
        if (StringHelper.isEmpty(json) && !Util.isJSONValid(json)) {
            System.out.println("--------------> ERROR ----------> JSON INVALID " + getClass().getName() + " id - " + id + " JSON ERROR: " + json);
        }
    }
}
