package game.object;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.Util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Test {
//    public static void main(String[] args) {
//        int stat = 3;
//        int value = 1;
//        String innit = "[1,";
//        for (int i = 0; i < 50000; i++) {
//            value = value + stat;
//            innit += (value) + ",";
//            stat += new Random().nextInt(2) + 1;
//        }
//        innit += "]";
//        System.out.println(innit);
//    }


    public static void main(String[] args) {
//        List<Integer> lst = Arrays.asList(1, 2, 3, 4, 5);
//        int start = lst.get(0);
//        for (int i = 0; i < 10; i++) {
//          if(lst.indexOf(start).h)
//        }


        List<Integer> formula = List.of(5, 5, 5, 50, 50, 30, 20, 50, 50, 20, 10, 10, 10);
        List<Integer> levels = List.of(140, 140, 140, 50, 50, 100, 100, 100, 100, 100, 100, 100, 100);
        int gold = 0;
        for (int i = 0; i < levels.size(); i++) {
            System.out.println("levels.get(i) = " + levels.get(i) + " --- gold = " + gold);
            for (int j = 1; j <= levels.get(i); j++) {
                gold += formula.get(i) * j;
            }

        }
        System.out.println("gold = " + gold);
    }


    private class Effect {
        public int trigger;
        public int mp;
        public int effId;
        public int point;
        public float time;

        @Override
        public String toString() {
            return "Effect{" +
                    "trigger=" + trigger +
                    ", mp=" + mp +
                    ", effId=" + effId +
                    ", point=" + point +
                    ", time=" + time +
                    '}';
        }
    }
}

