package game.treasure.mapping.main;


import game.config.aEnum.QuestTutType;
import game.object.MyUser;
import lombok.Getter;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ResTutorialQuestEntity extends BaseEntity {
    @Getter
    @Id
    private int id;
    @Getter
    private String name;
    @Getter
    private int num, gotoId;
    @Getter
    private String bonus;

    private String questType;
    @Transient
    List<Long> aBonus;
    @Transient
    List<Integer> quest;

    public void init() {
        aBonus = bonus == null || bonus.isEmpty() ? new ArrayList<>() : GsonUtil.strToListLong(bonus);
        quest = questType == null || questType.isEmpty() ? new ArrayList<>() : GsonUtil.strToListInt(questType);
        if (bonus != null && !bonus.isEmpty()) checkJson(id, bonus);
        if (questType != null && !questType.isEmpty()) checkJson(id, questType);
    }

    public String getTitle(MyUser mUser) {
        if (name != null && !name.isEmpty()) return name;
        QuestTutType questTut = getType();
        return questTut == null ? "" : questTut.label;
    }

    public QuestTutType getType() {
        if (quest == null || quest.isEmpty()) return QuestTutType.NULL;
        QuestTutType type = QuestTutType.get(quest.get(0));
        return type == null ? QuestTutType.NULL : type;
    }

    public int getIdInfo() {
        return quest != null && quest.size() > 1 ? quest.get(1) : 0;
    }

    public List<Long> getABonus() {
        return aBonus == null ? new ArrayList<>() : new ArrayList<>(aBonus);
    }


}
