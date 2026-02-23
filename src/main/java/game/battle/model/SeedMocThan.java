package game.battle.model;

import game.battle.object.Pos;
import lombok.Data;

import java.io.Serializable;


@Data
public class SeedMocThan implements Serializable {
    int id;
    Pos pos;
    boolean alive;
    long timeCreate;
    boolean isActive;


    public SeedMocThan(int id, Pos pos) {
        this.id = id;
        this.pos = pos;
        alive = true;
        isActive = false;
        timeCreate = System.currentTimeMillis();
    }
}
