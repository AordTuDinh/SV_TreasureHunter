package game.dragonhero.service.battle;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class Effect {
    public  int trigger;
    public  int mp;
    public  String effect;
    public  int point;
    public  float time;

    @Override
    public String toString() {
        return "Effect{" +
                "trigger=" + trigger +
                ", mp=" + mp +
                ", effect='" + effect + '\'' +
                ", point=" + point +
                ", time=" + time +
                '}';
    }

//    public static void main(String[] args) {
//        String value = "[{\"triger\":1,\"mp\":100,\"effect\":\"atk\",\"point\":3,\"time\":2.2},{\"triger\":1,\"mp\":100,\"effect\":\"brk\",\"point\":3,\"time\":2.3}]";
//        List<Effect> effs = new Gson().fromJson(value, new TypeToken<List<Effect>>() {
//        }.getType());
//        System.out.println("effs.size() = " + effs.get(1).toString());
//    }
}
