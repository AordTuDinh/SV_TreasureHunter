package game.client;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class NpcChatEntity {
    @Id
    int id;
    String name, data;
}
