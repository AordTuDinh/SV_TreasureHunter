package game.treasure.mapping;

import game.config.CfgMaterial;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@Table(name = "user_settings")
@NoArgsConstructor
public class UserSettingsEntity implements Serializable {
    public static final int AUTO_SELL_ITEM_SIZE = 16;

    @Id
    int userId;
    String blockChat, chatSetting; //chatSetting s;ize 2
    String autoSellItem,autoSellMaterial;


    public UserSettingsEntity(int userId) {
        this.userId = userId;
        this.blockChat = "[]"; // Danh sách bị mình block chat
        this.chatSetting = StringHelper.toDBString(NumberUtil.genListInt(2, 50));
        this.autoSellItem = StringHelper.toDBString(NumberUtil.genListInt(AUTO_SELL_ITEM_SIZE, 0));
        this.autoSellMaterial = StringHelper.toDBString(NumberUtil.genListInt(CfgMaterial.getAutoSellMaterialSize(), 0));
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

    public List<Integer> getAutoSellItemList() {
        List<Integer> list = StringHelper.isEmpty(autoSellItem)
                ? new ArrayList<>()
                : new ArrayList<>(GsonUtil.strToListInt(autoSellItem));
        while (list.size() < AUTO_SELL_ITEM_SIZE) {
            list.add(0);
        }
        return list;
    }

    public boolean updateAutoSellItem(String autoSellItem) {
        if (update(Arrays.asList("auto_sell_item", autoSellItem))) {
            this.autoSellItem = autoSellItem;
            return true;
        }
        return false;
    }

    public List<Integer> getAutoSellMaterialList() {
        List<Integer> list = StringHelper.isEmpty(autoSellMaterial)
                ? new ArrayList<>()
                : new ArrayList<>(GsonUtil.strToListInt(autoSellMaterial));
        while (list.size() < CfgMaterial.getAutoSellMaterialSize()) {
            list.add(0);
        }
        return list;
    }

    public boolean updateAutoSellMaterial(String autoSellMaterial) {
        if (update(Arrays.asList("auto_sell_material", autoSellMaterial))) {
            this.autoSellMaterial = autoSellMaterial;
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_settings", updateData, Arrays.asList("user_id", userId));
    }

}
