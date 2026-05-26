package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ArtifactType {
    PERSONAL(1, "Personal"),
    TEAMMATE(2, "Teammate"),
    NERF(3, "Nerf"),
    ;

    public final int value;
    public final String name;

    ArtifactType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    static final Map<Integer, ArtifactType> lookup = new HashMap<>();

    static {
        for (ArtifactType t : values()) {
            lookup.put(t.value, t);
        }
    }

    public static ArtifactType get(int type) {
        return lookup.get(type);
    }
}
