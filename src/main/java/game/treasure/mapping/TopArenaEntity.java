package game.treasure.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Snapshot top 100 arena theo tuần / server.
 * PK: (serverId, week)
 */
@Entity
@Table(name = "top_arena")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopArenaEntity implements Serializable {
    @Id
    int serverId;
    @Id
    int week;
    /** JSON array userId top1 → top100, vd. [101,102,...] */
    String top;
}
