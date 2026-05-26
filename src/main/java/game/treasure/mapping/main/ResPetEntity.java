package game.treasure.mapping.main;

import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResPetEntity implements Serializable {
    @Id
    int id;
    String name;
    int rank,  showSummon; // showSummon: có thể summon ra

}
