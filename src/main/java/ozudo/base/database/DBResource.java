package ozudo.base.database;

import java.util.List;

public class DBResource extends DBCommon {
    public static DBResource instance = new DBResource();

    public static DBResource getInstance() {
        return instance;
    }
}
