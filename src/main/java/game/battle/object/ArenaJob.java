package game.battle.object;

import game.battle.model.Character;
import game.battle.model.Player;
import game.config.CfgBattle;
import game.config.aEnum.RoomType;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResMap;
import game.dragonhero.table.BaseRoom;
import game.monitor.Online;
import game.object.MyUser;
import game.object.TaskMonitor;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaJob implements Job {
    public static Map<Integer, List<Integer>> userArena = new HashMap<>();
    public static Map<Integer, List<Integer>> userArena2 = new HashMap<>();

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
//        processArena1();
//        processArena2();
    }

//    void processArena1() {
//        for (Map.Entry<Integer, List<Integer>> entry : userArena.entrySet()) {
//            if (entry.getValue() == null) continue;
//            while (entry.getValue().size() >= 2) {
//                int userId1 = entry.getValue().get(0);
//                int userId2 = entry.getValue().get(1);
//                // xoá 2 thằng này để xử lí riêng
//                entry.getValue().remove(0);
//                entry.getValue().remove(0);
//                // add new room
//                RoomType arena = RoomType.ARENA;
//                MyUser my1 = Online.getMUser(userId1);
//                MyUser my2 = Online.getMUser(userId2);
//                Channel channel1 = Online.getChannel(userId1);
//                Channel channel2 = Online.getChannel(userId2);
//                if (my1 == null) { // add lại thằng 2 vô list
//                    entry.getValue().add(0, userId2);
//                }
//                if (my2 == null) {// add lại thằng 1 vô list
//                    entry.getValue().add(0, userId1);
//                }
//                // sắp xếp cho key id thằng user nhỏ hơn lên trước
//                boolean first1 = first1(userId1, userId2);
//                String keyRoom = CfgBattle.getKeyRoom(null, arena.value, first1 ? userId1 : userId2, first1 ? userId2 : userId1);
//                // user 1
//                BaseRoom curRoom1 = (BaseRoom) ChUtil.get(channel1, ChUtil.KEY_ROOM);
//                if (channel1 != null && curRoom1.getKeyRoom().equals(keyRoom)) {
//                    return;
//                }
//                // xóa khỏi room cũ
//                CfgBattle.removeUserToRoom(channel1, keyRoom, userId1);
//
//                // user 1
//                BaseRoom curRoom2 = (BaseRoom) ChUtil.get(channel2, ChUtil.KEY_ROOM);
//                if (channel2 != null && curRoom2.getKeyRoom().equals(keyRoom)) {
//                    return;
//                }
//                // xóa khỏi room cũ
//                CfgBattle.removeUserToRoom(channel2, keyRoom, userId2);
//                // check có room hay chưa, có rồi thì join
//                BaseMap baseMap = ResMap.getMap(arena.value, 0);
//                List<Character> players = new ArrayList<>();
//                Player p1 = my1.getPlayer();
//                Player p2 = my2.getPlayer();
//                p1.setPos(new Pos(-5, 0));
//                p2.setPos(new Pos(5, 0));
//                players.add(p1);
//                players.add(p2);
//                BaseRoom room = new ArenaRoom(baseMap, players, keyRoom);
//                TaskMonitor.getInstance().addRoom(room);
//                ChUtil.set(channel1, ChUtil.KEY_ROOM, room);
//                ChUtil.set(channel2, ChUtil.KEY_ROOM, room);
//                // type,collider,isBattle
//                Util.sendProtoData(channel1, CommonProto.getCommonVector(arena.value, baseMap.getMapData().getPlayerCollider(), 1), IAction.ARENA_FIND_SUCCESS);
//                Util.sendProtoData(channel2, CommonProto.getCommonVector(arena.value, baseMap.getMapData().getPlayerCollider(), 1), IAction.ARENA_FIND_SUCCESS);
//                //tao quai theo ti le mau cua nguoi choi thap hon - dua vao battle power
//                // tao ngan quai
//                // instance quai moi
//
//            }
//        }
//    }

    void processArena2() {
//        for (Map.Entry<Integer, List<Integer>> entry : userArena2.entrySet()) {
//            if (entry.getValue() == null) continue;
//            //System.out.println("entry.getValue().size() = " + entry.getValue().size());
//            while (entry.getValue().size() >= 2) {
//                int userId1 = entry.getValue().get(0);
//                int userId2 = entry.getValue().get(1);
//                // xoá 2 thằng này để xử lí riêng
//                entry.getValue().remove(0);
//                entry.getValue().remove(0);
//                // add new room
//                RoomType arena = RoomType.ARENA2;
//                MyUser my1 = Online.getMUser(userId1);
//                MyUser my2 = Online.getMUser(userId2);
//                Channel channel1 = Online.getChannel(userId1);
//                Channel channel2 = Online.getChannel(userId2);
//                if (my1 == null) { // add lại thằng 2 vô list
//                    entry.getValue().add(0, userId2);
//                }
//                if (my2 == null) {// add lại thằng 1 vô list
//                    entry.getValue().add(0, userId1);
//                }
//                // sắp xếp cho key id thằng user nhỏ hơn lên trước
//                boolean first1 = first1(userId1, userId2);
//                String keyRoom = CfgBattle.getKeyRoom(null, arena.value, first1 ? userId1 : userId2, first1 ? userId2 : userId1);
//                // user 1
//                BaseRoom curRoom1 = (BaseRoom) ChUtil.get(channel1, ChUtil.KEY_ROOM);
//                if (channel1 != null && curRoom1.getKeyRoom().equals(keyRoom)) {
//                    return;
//                }
//                // xóa khỏi room cũ
//                CfgBattle.removeUserToRoom(channel1, keyRoom, userId1);
//
//                // user 1
//                BaseRoom curRoom2 = (BaseRoom) ChUtil.get(channel2, ChUtil.KEY_ROOM);
//                if (channel2 != null && curRoom2.getKeyRoom().equals(keyRoom)) {
//                    return;
//                }
//                // xóa khỏi room cũ
//                CfgBattle.removeUserToRoom(channel2, keyRoom, userId2);
//
//
//                BaseMap baseMap = ResMap.getMap(arena.value, 0);
//                List<Character> players = new ArrayList<>();
//                Player p1 = my1.getPlayer();
//                Player p2 = my2.getPlayer();
//                p1.setPos(new Pos(-5, 0));
//                p2.setPos(new Pos(5, 0));
//                players.add(p1);
//                players.add(p2);
//                BaseRoom room = new Arena2Room(baseMap, players, keyRoom);
//                TaskMonitor.getInstance().addRoom(room);
//                ChUtil.set(channel1, ChUtil.KEY_ROOM, room);
//                ChUtil.set(channel2, ChUtil.KEY_ROOM, room);
//                // type,collider,isBattle
//                Util.sendProtoData(channel1, CommonProto.getCommonVector(arena.value, baseMap.getMapData().getPlayerCollider(), 1), IAction.ARENA_II_FIND_SUCCESS);
//                Util.sendProtoData(channel2, CommonProto.getCommonVector(arena.value, baseMap.getMapData().getPlayerCollider(), 1), IAction.ARENA_II_FIND_SUCCESS);
//                //tao quai theo ti le mau cua nguoi choi thap hon - dua vao battle power
//                // tao ngan quai
//                // instance quai moi
//
//            }
//        }
    }

    public static boolean first1(int id1, int id2) {
        return id1 < id2;
    }
}

