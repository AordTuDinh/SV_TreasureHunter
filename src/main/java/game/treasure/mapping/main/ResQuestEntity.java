package game.treasure.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResQuestEntity implements Serializable {
    @Id
    int id;
    String desc;
    int level, point, type;
    int number;
    String bonus;
    int go;

    public List<Long> getBonusList() {
        if (bonus == null || bonus.isEmpty() || "[]".equals(bonus.trim())) {
            return new ArrayList<>();
        }
        return GsonUtil.strToListLong(bonus);
    }
}
