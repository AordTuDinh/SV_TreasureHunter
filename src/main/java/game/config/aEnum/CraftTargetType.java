package game.config.aEnum;

import game.config.CfgCraft;

public enum CraftTargetType {
    EQUIPMENT(1),
    MOUNT(2),
    PET(3),
    SKIN(4),
    ARTIFACT(5);

    public final int id;

    CraftTargetType(int id) {
        this.id = id;
    }

    public static CraftTargetType fromId(int id) {
        for (CraftTargetType t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return null;
    }

    public int getMaxSocket() {
        return CfgCraft.getMaxSocket(this);
    }

    public boolean losesTargetOnCraftFail() {
        return CfgCraft.losesTargetOnCraftFail(this);
    }

    public boolean usesGoldFee() {
        return this == EQUIPMENT;
    }
}
