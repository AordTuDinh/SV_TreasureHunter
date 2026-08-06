package game.treasure.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ResQuestEntity implements Serializable {
    @Id
    int id;
    String desc;
    int  level, bonus, type;
    int number;
}
