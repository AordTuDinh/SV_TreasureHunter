package game.object;


import game.config.aEnum.StatusType;

public class StatEntity {
    public int id;
    public StatusType status;
    public int level;

    public StatEntity(int id, StatusType status) {
        this.id = id;
        this.status = status;
        this.level = 0;
    }

    public StatEntity(int id, StatusType status, int level) {
        this.id = id;
        this.status = status;
        this.level = level;
    }
}
