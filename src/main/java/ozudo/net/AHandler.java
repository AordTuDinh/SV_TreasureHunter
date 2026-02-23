//package ozudo.net;
//
//import game.battle.Bullet;
//import game.battle.Player;
//import game.old.Input;
//import game.old.Pos;
//import game.old.XInput;
//import game.protocol.CommonProto;
//import io.netty.channel.Channel;
//import net.sf.json.JSONObject;
//import ozudo.utils.*;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class AHandler {
//    private static AHandler instance = null;
//
//    List<Player> aPlayer = new ArrayList<Player>();
//    List<Bullet> sendBullet = new ArrayList<Bullet>();
//    List<Bullet> aBullet = new ArrayList<>();
//
//    int periodSVUpdate = 45;
//    int periodPhysicUpdate = 15;
//    float server_time = 0f;
//    float local_time = 0.01666f;
//    long _dte = System.currentTimeMillis();
//
//    public static AHandler getInstance() {
//        if (instance == null) {
//            instance = new AHandler();
//        }
//        return instance;
//    }
//
//    private AHandler() {
//        startPhysic();
//    }
//
//    public void removePlayer(Player player) {
//        aPlayer.remove(player);
//
//    }
//
//    public int doAction(Channel channel, JSONObject srcReq, long curTime) {
//        int action = srcReq.getInt("action");
//        return doSyncAction(channel, action, srcReq, curTime);
//    }
//
//    private synchronized int doSyncAction(Channel channel, int actionId, JSONObject srcRequest, long curTime) {
//        int ret = 0;
//        try {
//            switch (actionId) {
//                case IAction.CLIENT_INPUT:
//                    Player player = (Player) ChUtil.get(channel, "player");
//                    if (player == null) {
//                        Debug.Log("Player chua JOIN TABLE");
//                        return 1;
//                    }
//                   // handle_server_input(player, new Input(XInput.parse(srcRequest)));
//                    break;
//                case IAction.CLIENT_DISCONNECTED:
//                    //playerDisconnected(channel);
//                    break;
//                case IAction.JOIN_BATTLE:
//                    joinBattle(channel, srcRequest);
//                    break;
//
//                case IAction.START_PHYSIC_UPDATE:
//                   // startPhysic();
//                    break;
//                default:
//                    //ret = doSubSyncAction(channel, action, srcRequest, curTime);
//                    break;
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            Debug.Log(Util.exToString(ex));
//        }
//        return ret;
//    }
//
//    void handle_server_input(Player client, Input input) {
//        // Store the input on the player instance for processing in the physics
//        client.inputs.add(input);
//    }
//
//    int joinBattle(Channel channel, JSONObject srcReq) {
//        String username = srcReq.getString("name");
//        int teamId = 0;//srcReq.getInt("teamId");
//        Pos ins = null;
//        if (teamId == 1) ins = new Pos(0, 0);
//        else ins = new Pos(0, 0);
//        Player player = new Player(ins, teamId, username, channel);
//        ChUtil.set(channel, Constans.KEY_TABLE, this);
//        ChUtil.set(channel, Constans.KEY_PLAYER, player);
//
//        //gui thong tin player vua join cho chinh no
//        ProtoUtil.sendPlayer(channel, IAction.MAP_DATA, CommonProto.getProtoPlayer(player));
//
//        //Them player vua join battle
//        aPlayer.add(player);
//        //Update lai list player, gui cho tat ca client
//        ProtoUtil.sendJoinState(IAction.JOIN_BATTLE, CommonProto.getProtoJoinBattle(aPlayer), aPlayer);
//        return 1;
//    }
//    void startPhysic() {
//        schedule_server_update();
//        create_physics_simulation();
//    }
//
//    // update server runs at 45ms
//    void schedule_server_update() {
//        Timer t = new Timer();
//        TimerTask tt = new TimerTask() {
//            @Override
//            public void run() {
//                server_update();
//            }
//        };
//        t.scheduleAtFixedRate(tt, 0, periodSVUpdate);
//    }
//
//
//    // physic on server runs at 15ms
//    void create_physics_simulation() {
//        Timer t = new Timer();
//        TimerTask tt = new TimerTask() {
//            @Override
//            public void run() {
//                server_update_physics();
//            }
//        };
//        t.scheduleAtFixedRate(tt, 0, periodPhysicUpdate);
//
//    }
//
//    // 15ms
//    public synchronized void server_update_physics() {
//        Time.updateTime();
//        try {
//            for (int i = 0; i < aPlayer.size(); i++) {
//                this.process_input(aPlayer.get(i));
//            }
//            checkHit();
//            process_bullet();
//        } catch (Exception ex) {
//            Debug.Log(Util.exToString(ex));
//        }
//
//    }
//
//    void process_bullet() {
//        for (int i = 0; i < aBullet.size(); i++) {
//            Bullet b = aBullet.get(i);
//            if (b.isDead) {
//                aBullet.remove(b);
//            } else {
//                b.move();
//            }
//        }
//    }
//
//    void process_input(Player player) {
//        int ic = player.inputs.size();
//        if (player.isAlive() && ic > 0) {
//            boolean hasPiu = false;
//            Pos playerPos = player.pos.clone();
//            //
//            int x_dir = 0;
//            int y_dir = 0;
//            long last_input_seq = 0;
//            double last_input_time = 0;
//            long lastInputSeq = player.last_input_seq;
//            for (int index = 0; index < ic; ++index) {
//                // don't process ones we already have simulated locally
//                if (player.inputs.get(index).data.seq <= lastInputSeq) {
//                    continue;
//                }
//
//                List<Integer> input = player.inputs.get(index).data.keys;
//                int kc = input.size();
//                for (int i = 0; i < kc; ++i) {
//                    int key = input.get(i);
//                    if (key == Input.Left) {
//                        x_dir -= 1;
//                    }
//                    if (key == Input.Right) {
//                        x_dir += 1;
//                    }
//                    if (key == Input.Up) {
//                        y_dir += 1;
//                    }
//                    if (key == Input.Down) {
//                        y_dir -= 1;
//                    }
//
//                    if (key == Input.Skill1) {
//                        hasPiu = true;
//                    }
//                } //for all input values
//
//            } // for each input command
//
//            Pos nd = Pos.physics_movement_vector_from_direction(x_dir, y_dir, player.speed);
//
//            if (player.inputs.size() > 0) {
//                // we can now clear the array since these have been processed
//                player.last_input_time = player.inputs.get(ic - 1).data.clientTime;
//                player.last_input_seq = player.inputs.get(ic - 1).data.seq;
//            }
//
//            player.pos = Pos.v_add(playerPos, nd);
//            player.inputs.clear();
//
//            //co thay doi direction
//            if (!nd.equals(Pos.zero)) {
//                player.direction = nd.normalized();
//            }
//
//            if (hasPiu) {
//                Bullet bullet = new Bullet();
//                bullet.direction = player.direction.clone();
//                bullet.pos = player.pos.clone();
//                bullet.playerId = player.id;
//                bullet.teamId = player.teamId;
//                aBullet.add(bullet);
//                sendBullet.add(bullet);
//            }
//        }
//    }
//
//    // cai nay update 45 ms
//    public synchronized void server_update() {
//        // Update the state of our local clock to match the timer
//        try {
//            long _dt = System.currentTimeMillis() - _dte;
//            //_dte = System.currentTimeMillis();
//            local_time += _dt / 1000.0;
//            server_time = local_time;
//            // Make a snapshot of the current state, for updating the clients
//            ProtoUtil.sendTableState(IAction.TABLE_STATE, CommonProto.getProtoTableState(server_time, aPlayer, sendBullet), aPlayer);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            Debug.Log(Util.exToString(ex));
//        }
//    }
//
//    void checkHit() {
//
////        for (Player  player  :  aPlayer)
////        {
////            aBullet.removeIf(
////                    b -> {
////                        if (player.isHit(b)) {
////                            player.onHit();
////                            return true;
////                        }
////                        return false;
////                    }
////            );
////
////        }
//    }
//}
