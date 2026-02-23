package game.object;

public class PassiveWeapon {
    public int id;
    public float baseValue;
    public float valuePerLevel;

    public PassiveWeapon(float id, float baseValue, float valuePerLevel) {
        this.id = (int) id;
        this.baseValue = baseValue;
        this.valuePerLevel = valuePerLevel;
    }

    public float getValue(int curLevel) {
        return baseValue + (curLevel - 1) * valuePerLevel;
    }
}
