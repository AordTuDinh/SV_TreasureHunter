//package game.battle.object;
//
//import game.battle.model.Player;
//import game.dragonhero.mapping.main.BaseMap;
//import game.dragonhero.mapping.main.ResTeleportEntity;
//import lombok.Data;
//
//import java.io.Serializable;
//import java.util.List;
//
//@Data
//public class CacheBattle implements Serializable {
//    List<Player> aPlayer;
//    ResTeleportEntity teleport;
//    BaseMap mapInfo;
//    String key;
//    int keyChanel;
//
//    // k nghi ra ten : dang dung mapId de chi map to, idInfo de chi subMap
//    public CacheBattle(ResTeleportEntity teleport, BaseMap mapInfo, List<Player> players, String keyRoom) {
//        this.aPlayer = players;
//        this.mapInfo = mapInfo;
//        this.key = keyRoom;
//        this.teleport = teleport;
//        this.keyChanel = Integer.parseInt(key.split("_")[2]);
//    }
//
//    // only for boss god
//    public Long getBossId() { // độ khó sẽ theo map Id
//        return Long.valueOf(mapInfo.getListEnemy().get(mapInfo.getId()));
//    }
//}
