/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ozudo.base.helper;

import com.google.protobuf.AbstractMessage;
import game.config.CfgServer;
import game.config.aEnum.ToastType;
import game.dragonhero.server.Constans;
import game.dragonhero.server.IAction;
import game.object.MyUser;
import game.protocol.CommonProto;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static ozudo.base.helper.Filer.getLogger;

/**
 * @author Hanv86
 */
public class Util {

    static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");

    public static String get_SHA_256_SecurePassword(String passwordToHash, String salt) {
        String genPass = null;
        try {
            String pass = "*098#" + passwordToHash;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest(pass.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            genPass = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return genPass;
    }

    public static Calendar parseDateTime(String strDate) {
        Calendar ca = Calendar.getInstance();
        try {
            Date date = sdf.parse(strDate);
            ca.setTime(date);
            return ca;
        } catch (Exception ex) {
        }
        ca = Calendar.getInstance();
        ca.add(Calendar.HOUR_OF_DAY, -1);
        return ca;
    }

    public static void sendProtoData(Channel channel, AbstractMessage data, int service) {
        debugService(service);
        Pbmethod.ResponseData.Builder builder = Pbmethod.ResponseData.newBuilder();
        Pbmethod.PbAction.Builder action = Pbmethod.PbAction.newBuilder();
        action.setActionId(service);
        if (data != null) {
            action.setData(data.toByteString());
        }
        builder.addAAction(action);
        sendRawData(channel, builder.build().toByteArray());
    }

    public static void sendProtoDataToListChanel(List<Channel> channels, AbstractMessage data, int service) {
        //System.out.println("channels.size() = " + channels.size());
        for (int i = 0; i < channels.size(); i++) {
            sendProtoData(channels.get(i), data, service);
        }
    }

    public static void sendSliderChat(List<Channel> channels, String msg) {
        for (int i = 0; i < channels.size(); i++) {
            sendProtoData(channels.get(i), CommonProto.getCommonVector(msg), IAction.MSG_SLIDE);
        }
    }

    public static void sendToast(List<Channel> channels, ToastType type, String msg) {
        for (int i = 0; i < channels.size(); i++)
            sendProtoData(channels.get(i), type.retToast(msg), IAction.MSG_TOAST);

    }

    public static void sendProtoInGame(Channel channel, AbstractMessage data, int service) {
        Pbmethod.ResponseData.Builder builder = Pbmethod.ResponseData.newBuilder();
        Pbmethod.PbAction.Builder action = Pbmethod.PbAction.newBuilder();
        action.setActionId(service);
        if (data != null) {
            action.setData(data.toByteString());
        }
        builder.addAAction(action);
        sendGameData(channel, builder.build().toByteArray());
    }


//    public static void sendAllPlayer(int service, AbstractMessage data, List<Player> aPlayer) {
//        for (Player player : aPlayer) {
//            if (player != null && player.channel != null) {
//                Util.sendProtoData(player.channel, data, service, System.currentTimeMillis());
//            }
//        }
//    }

    public static void sendRawData(Channel channel, byte[] data) {
        if (channel != null && channel.isWritable() && channel.isOpen()) {
            try {
                data = data == null ? new byte[0] : data;
                ByteBuf buffer = Unpooled.buffer();
                buffer.writeBytes(Constans.MAGIC_OUT_GAME.getBytes());
                buffer.writeInt(data.length);
                buffer.writeBytes(data);
                Integer protocol = ChUtil.getInteger(channel, Constans.KEY_PROTOCOL);
                if (protocol == null || protocol == Constans.PROTOCOL_TCP)
                    channel.writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                else
                    channel.writeAndFlush(new BinaryWebSocketFrame(buffer)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } catch (Exception ex) {
                getLogger().error(Util.exToString(ex));
            }
        }
    }

    public static String resetServer() {
        String ret = "";
        CommandLine oCmdLine = CommandLine.parse("sh /root/test/run.sh");
        DefaultExecutor oDefaultExecutor = new DefaultExecutor();
        oDefaultExecutor.setExitValue(0);
        try {
            return String.valueOf(oDefaultExecutor.execute(oCmdLine));
        } catch (ExecuteException e) {
            ret = "Execution failed.";
            Logs.error(e);
        } catch (IOException e) {
            ret = "permission denied.";
            Logs.error(e);
        }
        return ret;
    }

    public static void sendGameData(Channel channel, byte[] data) {
        sendGameData(channel, data, Constans.MAGIC_IN_PUT);
    }

    public static void sendGameData(Channel channel, byte[] data, String... magic) {
        if (channel != null && channel.isOpen() && channel.isWritable()) {
            try {
                data = data == null ? new byte[0] : data;
                ByteBuf buffer = Unpooled.buffer();
                buffer.writeBytes(magic[0].getBytes());
                buffer.writeInt(data.length);
                buffer.writeBytes(data);
                Integer protocol = ChUtil.getInteger(channel, Constans.KEY_PROTOCOL);
                if (protocol == null || protocol == Constans.PROTOCOL_TCP)
                    channel.writeAndFlush(buffer).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                else
                    channel.writeAndFlush(new BinaryWebSocketFrame(buffer)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } catch (Exception ex) {
                ex.printStackTrace();
                getLogger().error(Util.exToString(ex));
            }
        }
    }

    public static int convertVersion2Int(String version) {
        return version.isEmpty() ? 0 : Integer.parseInt(version.replace(".", ""));
    }

    public static void debug(String value) {
        if (CfgServer.isTestServer()) System.out.println(value);
    }

    public static void debugService(int service) {
        if (CfgServer.isTestServer() && !IAction.notDebug.contains(service)) {
           // debug("send proto data service id : " + service);
        }
    }

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public static String getMD5(String source) {
        try {
            MessageDigest mdEnc = MessageDigest.getInstance("MD5"); // Encryption
            // algorithm
            mdEnc.update(source.getBytes(), 0, source.length());

            String md5 = new BigInteger(1, mdEnc.digest()).toString(16); // Encrypted

            while (md5.length() < 32) {
                md5 = "0" + md5;
            }
            System.out.println("md5 = " + md5);
            return md5;
        } catch (Exception ex) {
        }
        return "";
    }

    public static String exToString(Exception ex) {
        StringWriter errors = new StringWriter();
        ex.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    public static String exToString(Throwable ex) {
        String tmp = "";
        for (StackTraceElement e : ex.getStackTrace()) {
            tmp += e.toString() + "\n";
        }
        return ex.toString() + ": " + tmp;
    }

    public static String printList(List<Integer> cards) {
        if (cards == null) {
            return "";
        }
        String output = "";
        for (int i = 0; i < cards.size(); i++) {
            output += cards.get(i) + " ";
        }
        return output;
    }

    public static int getIntVersion(String version) {
        String[] v = version.split("\\.");
        int ret = 0;
        int count = 2;
        for (String str : v) {
            if (str.length() > 0) {
                int number = 0;
                try {
                    number = Integer.parseInt(str);
                } catch (Exception ex) {
                }
                ret += Math.pow(10, count * 2) * number;
                count--;
            }
        }
        return ret;
    }

    public static int getLevel(int experience, double rate) {
        return (int) Math.pow(experience, 1 / rate);
    }

    public static int getRate(int experience, double rate) {
        int level = (int) Math.pow(experience, 1 / rate);
        double preEx = Math.pow(level, rate);
        double afterEx = Math.pow((level + 1), rate);
        return (int) ((experience - preEx) * 100 / (afterEx - preEx));
    }

    public static int getPoint(List<Integer> userCards) {
        int total = 0;
        for (int j = 0; j < userCards.size(); j++) {
            total += userCards.get(j) / 4 + 1;
        }
        return total;
    }


}
