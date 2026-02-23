package game.config;

import game.config.aEnum.PetType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;

import java.util.List;

public class CfgItem {


    // check các item đc phép mua không, vd 1 vài item chỉ đc sở hữu 1 lần
    public static String canBuyItem(MyUser mUser, List<Long> items, int number) {
        List<List<Long>> bms = Bonus.parse(items);
        for (int i = 0; i < bms.size(); i++) {
            List<Long> bonus = bms.get(i);
            if (bonus.get(0) == Bonus.BONUS_PET) {
                if (number > 1) return Lang.instance(mUser).get(Lang.err_can_buy_one);
                UserPetEntity uPet = mUser.getResources().getPet(Math.toIntExact(bonus.get(1)), Math.toIntExact(bonus.get(2)));
                if (uPet != null) {
                    if (Math.toIntExact(bonus.get(1)) == PetType.MONSTER.value)
                        return Lang.instance(mUser).get(Lang.err_monster_can_not_buy);
                    else return Lang.instance(mUser).get(Lang.err_pet_can_not_buy);
                }
            }
        }
        return null;
    }

    public static void loadConfig(String strJson) {
    }
}
