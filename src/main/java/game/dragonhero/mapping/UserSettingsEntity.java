package game.dragonhero.mapping;

import game.battle.type.AutoMode;
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
    int autoMode;
    String itemSlot; // trigger - itemId
    String blockChat, chatSetting; //chatSetting size 2

    public UserSettingsEntity(int userId) {
        this.userId = userId;
        this.autoMode = AutoMode.NORMAL.value;
        this.itemSlot = StringHelper.toDBString(NumberUtil.genListInt(4, 0));
        this.blockChat = "[]"; // Danh sách bị mình block chat
        this.chatSetting = StringHelper.toDBString(NumberUtil.genListInt(2, 50));
    }

    public boolean changeMode(AutoMode mode) {
        if (update(Arrays.asList("auto_mode", mode.value))) {
            autoMode = mode.value;
            return true;
        }
        return false;
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

    public boolean saveSlot(MyUser mUser, int slot, int itemId) {
        List<Integer> items = getItemSlot(mUser);
        items.set(slot * 2, itemId);
        return saveSlot(items);
    }

    public boolean saveSlot(List<Integer> slots) {
        if (update(Arrays.asList("item_slot", StringHelper.toDBString(slots)))) {
            itemSlot = slots.toString();
            return true;
        }
        return false;
    }

    public List<Long> getChatSetting() {
        return GsonUtil.strToListLong(chatSetting);
    }

    public List<Integer> getItemSlot(MyUser mUser) {
        List<Integer> items = GsonUtil.strToListInt(itemSlot);
        boolean update = false;
        int id1 = items.get(1);
        if (id1 > 0) {
            UserItemEntity item1 = mUser.getResources().getItem(id1);
            if (item1.getNumber() <= 0) {
                items.set(1, 0);
                update = true;
            }
        }
        int id2 = items.get(3);
        if (id2 > 0) {
            UserItemEntity item1 = mUser.getResources().getItem(id2);
            if (item1.getNumber() <= 0) {
                items.set(3, 0);
                update = true;
            }
        }
        if (update && update(Arrays.asList("item_slot", StringHelper.toDBString(items)))) {
            itemSlot = items.toString();
        }
        return items;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_settings", updateData, Arrays.asList("user_id", userId));
    }

}
