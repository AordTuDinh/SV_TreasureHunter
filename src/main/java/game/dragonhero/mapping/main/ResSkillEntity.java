package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.effect.SkillObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.List;

@Entity
@NoArgsConstructor
public class ResSkillEntity extends BaseEntity {
    @Getter
    @Id
    int id;
    String attackDamage, magicDamage, data, desc;
    @Getter
    @Transient
    SkillObject skill;
    @Getter
    @Transient
    List<Integer> atkDame, magDame;

    public void initData() {
        atkDame = GsonUtil.strToListInt(attackDamage);
        magDame = GsonUtil.strToListInt(magicDamage);
        checkJson(id, attackDamage);
        checkJson(id, magicDamage);
        checkJson(id, data);
        if (!data.isEmpty()) {
            skill = new Gson().fromJson(data, new TypeToken<SkillObject>() {
            }.getType());
        }
    }
}
