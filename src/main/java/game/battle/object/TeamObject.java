package game.battle.object;

import game.dragonhero.server.App;
import game.object.MyUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TeamObject{
    public static final int HOME = 0;
    public static final int SOLO = 1;
    public static final int RANK = 2;
    public static final int BOSS = 3;
    public static final int TRAIN = 4;
    public static final int TUTORIAL = 5;
    //
    public static final List<Integer> TYPE_TIMER = Arrays.asList(RANK, BOSS, TRAIN);

    public static final int STATUS_NONE = 0;
    public static final int STATUS_SEARCH = 1;
    public static final int STATUS_PLAY = 2;
    //
    public List<MyUser> aUser = new ArrayList<>();
    //
    public long lastAction = System.currentTimeMillis();
    static int counterId = 0;
    public int map = 0, mode = 1;
    public int type, id, hostId, status;
    public int maxPlayer = 2;


    static synchronized int getCounterId() {
        if (++counterId == 100000) counterId = 1;
        return counterId;
    }

    public TeamObject(int type, MyUser hostUser) {
        this.id = getCounterId();
        this.type = type;
        this.aUser.add(hostUser);
        this.hostId = hostUser.getUser().getId();
        this.status = STATUS_NONE;
        this.map = 0;
        hostUser.setTeam(this);
//        if (TYPE_TIMER.contains(type)) doExpireTurn(0);
    }

}
