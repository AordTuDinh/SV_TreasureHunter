package game.config;

/** user_artifact.data = [time, cd, value, range, person] — base rank 1 sau roll tier. */
public final class ArtifactDataSlot {
    public static final int IDX_TIME = 0;
    public static final int IDX_CD = 1;
    public static final int IDX_VALUE = 2;
    public static final int IDX_RANGE = 3;
    public static final int IDX_PERSON = 4;
    public static final int LENGTH = 5;

    public static final int POINT_TIME = -1;
    public static final int POINT_CD = -2;
    public static final int POINT_RANGE = -3;
    public static final int POINT_PERSON = -4;

    private ArtifactDataSlot() {
    }

    public static boolean scalesWithLevel(int idx) {
        return idx == IDX_TIME || idx == IDX_VALUE || idx == IDX_RANGE || idx == IDX_PERSON;
    }
}
