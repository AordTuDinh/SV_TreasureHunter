package game.object;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@Entity
public class PointBuff {
    @Id
    int pointId;
    String name;
    long value;
    long timeSeconds;


    public PointBuff(long pointId, long value, long timeSeconds) {
        this.pointId = (int) pointId;
        this.value = value;
        this.timeSeconds = timeSeconds;
    }

    public PointBuff(int pointId, long value) {
        this.pointId = pointId;
        this.value = value;
    }
}
