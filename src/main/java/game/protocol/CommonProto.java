package game.protocol;

import com.google.protobuf.AbstractMessage;
import protocol.Pbmethod.*;

import java.util.Arrays;
import java.util.List;

public class CommonProto {

    public static PbAction getPbAction(int action, AbstractMessage msg) {
        PbAction.Builder builder = PbAction.newBuilder();
        builder.setActionId(action);
        if (msg != null) {
            builder.setData(msg.toByteString());
        }
        return builder.build();
    }

    public static PbAction getCommonVectorAction(int service, List<Long> aLong, List<String> aStr) {
        PbAction.Builder builder = PbAction.newBuilder();
        builder.setActionId(service);
        builder.setData(getCommonVectorProto(aLong, aStr).toByteString());
        return builder.build();
    }

    public protocol.Pbmethod.CommonVector getCommonVector(Object... values) {
        protocol.Pbmethod.CommonVector.Builder builder = protocol.Pbmethod.CommonVector.newBuilder();
        for (Object value : values) {
            builder.addALong(Long.parseLong(value.toString()));
        }
        return builder.build();
    }

    public static protocol.Pbmethod.CommonVector getCommonVector(String... values) {
        protocol.Pbmethod.CommonVector.Builder builder = protocol.Pbmethod.CommonVector.newBuilder();
        for (String value : values) {
            builder.addAString(value);
        }
        return builder.build();
    }

    public static protocol.Pbmethod.CommonVector getCommonVector(Long... values) {
        return CommonProto.getCommonVectorProto(values);
    }

    public static protocol.Pbmethod.CommonVector getCommonVector(Integer... values) {
        return CommonProto.getCommonIntVectorProto(values);
    }

    public static protocol.Pbmethod.CommonVector getCommonVector(List<Long> values) {
        return CommonProto.getCommonVectorProto(values);
    }

    public static protocol.Pbmethod.CommonVector getCommonIntVector(List<Integer> values) {
        return CommonProto.getCommonIntVectorProto(values);
    }

    public static CommonVector getErrorMsg(String msg) {
        return getCommonVectorProto(null, Arrays.asList(msg));
    }

    public static CommonVector getCommonIntVectorProto(Integer... listInt) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listInt != null) {
                for (int i = 0; i < listInt.length; i++) {
                    cmm.addALong(listInt[i]);
                }
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonIntVectorProto(List<Integer> listInt) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listInt != null) {
                for (int i = 0; i < listInt.size(); i++) {
                    cmm.addALong(listInt.get(i));
                }
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonLongVectorProto(List<Long> listLong, List<String> listString) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listLong != null) {
                for (Long i : listLong) {
                    cmm.addALong(i);
                }
            }
            if (listString != null) {
                for (String str : listString) {
                    cmm.addAString(str);
                }
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonIntVectorProto(List<Integer> listInt, List<String> listString) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listInt != null) {
                for (int i = 0; i < listInt.size(); i++) {
                    cmm.addALong(listInt.get(i));
                }
            }
            if (listString != null) {
                cmm.addAllAString(listString);
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonVectorProto(Long... listLong) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listLong != null) {
                for (int i = 0; i < listLong.length; i++) {
                    cmm.addALong(listLong[i]);
                }
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonVectorProto(List<Long> listLong) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listLong != null) {
                cmm.addAllALong(listLong);
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static CommonVector getCommonVectorProto(List<Long> listLong, List<String> listString) {
        try {
            CommonVector.Builder cmm = CommonVector.newBuilder();
            if (listLong != null) {
                cmm.addAllALong(listLong);
            }
            if (listString != null) {
                cmm.addAllAString(listString);
            }
            return cmm.build();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static ListCommonVector parseListCommonVector(byte[] data) {
        if (data != null) {
            try {
                return ListCommonVector.parseFrom(data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static CommonVector parseCommonVector(byte[] data) {
        if (data != null) {
            try {
                return CommonVector.parseFrom(data);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static PbAction parseAction(byte[] data) {
        try {
            return PbAction.parseFrom(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static RequestData parseRequest(byte[] data) {
        try {
            return RequestData.parseFrom(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

//    public static PbUserBattle parsePbUBattle(byte[] data) {
//        try {
//            return PbUserBattle.parseFrom(data);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return null;
//    }

    public static PbRegister parsePbRegister(byte[] data) {
        try {
            return PbRegister.parseFrom(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

//    public static PbLogin parsePbLogin(byte[] data) {
//        try {
//            return PbLogin.parseFrom(data);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return null;
//    }

    public static PbPos parsePbPos(byte[] data) {
        try {
            return PbPos.parseFrom(data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


//    public static PbInitRoom initMap(RoomMode mode, List<Player> aPlayer, List<Enemy> aEnemy, int mapId, int subMapId) {
//        PbInitRoom.Builder map = PbInitRoom.newBuilder();
//        map.setMode(mode.value);
//        map.setMapId(CfgBattle.getKeyMap(mapId, subMapId));
//        map.setTopRight(GameConfig.TopRight.toProto());
//        map.setBotLeft(GameConfig.BotLeft.toProto());
//        for (Player player : aPlayer) {
//            map.addPlayer(player.toProtoInfo());
//        }
//        for (Enemy en : aEnemy) {
//            map.addMonster(en.toProtoInfo());
//        }
//        return map.build();
//    }

}
