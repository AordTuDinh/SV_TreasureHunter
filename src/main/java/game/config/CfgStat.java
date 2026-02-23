package game.config;

import game.dragonhero.mapping.UserWeaponEntity;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.service.user.Bonus;

import java.util.Arrays;
import java.util.List;

public class CfgStat {
    public static List<Integer> goldAttackIds = Arrays.asList(1, 2, 3, 4);//Point id attack để làm data
    public static List<Integer> goldMAttackIds = Arrays.asList(5, 6, 7, 8);
    public static List<Integer> goldHPIds = Arrays.asList(9, 10, 11, 12);
    public static List<Integer> levelAttackIds = Arrays.asList(1, 18, 21, 24);//Point id attack để làm data
    public static List<Integer> levelMAttackIds = Arrays.asList(2, 19, 22, 25);
    public static List<Integer> levelHPIds = Arrays.asList(3, 20, 23, 26);
    public static List<Integer> lstNum = Arrays.asList(1, 10, 5); // 0 la max


}
