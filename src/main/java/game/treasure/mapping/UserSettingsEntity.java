package game.treasure.mapping;

import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@Table(name = "user_settings")
@NoArgsConstructor
public class UserSettingsEntity implements Serializable {
    @Id
    int userId;
    String blockChat, chatSetting; //chatSetting size 2

    public UserSettingsEntity(int userId) {
        this.userId = userId;
        this.blockChat = "[]"; // Danh sách bị mình block chat
        this.chatSetting = StringHelper.toDBString(NumberUtil.genListInt(2, 50));
    }


    public List<Integer> listBlockChat() {
        return GsonUtil.strToListInt(blockChat);
    }

    public boolean blockChatId(int idBlock) {
        List<Integer> block = listBlockChat();
        block.add(idBlock);
        return updateBlock(block.toString());
    }

    public boolean unBlockChat(int idUnBlock) {
        List<Integer> block = listBlockChat();
        if (block.contains(idUnBlock)) {
            block.remove(idUnBlock);
            return updateBlock(block.toString());
        }
        return true;
    }

    public boolean updateBlock(String blockChat) {
        if (update(Arrays.asList("block_chat", blockChat))) {
            this.blockChat = blockChat;
            return true;
        }
        return false;
    }


    public List<Long> getChatSetting() {
        return GsonUtil.strToListLong(chatSetting);
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_settings", updateData, Arrays.asList("user_id", userId));
    }

}
