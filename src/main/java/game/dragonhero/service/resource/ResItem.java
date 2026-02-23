package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResItemEntity;
import game.dragonhero.mapping.main.ResItemEquipmentEntity;
import ozudo.base.database.DBResource;

import java.util.*;

public class ResItem {
    // item
    static Map<Integer, ResItemEntity> mItem = new HashMap<>();
    // item equipment
    static Map<Integer, ResItemEquipmentEntity> mItemEquipment = new HashMap<>();

    public static ResItemEquipmentEntity getItemEquipment(int itemId) {
        return mItemEquipment.get(itemId);
    }

    public static ResItemEntity getItem(int itemId) {
        return mItem.get(itemId);
    }

    public static final int sizeItemEquipment = 24;

    public static void init() {
        // for item
        List<ResItemEntity> aItem = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item", Arrays.asList("enable", 1), "", ResItemEntity.class);
        mItem.clear();
        aItem.forEach(item -> {
            item.init();
            mItem.put(item.getId(), item);
        });

        // for item equipment
        List<ResItemEquipmentEntity> aItemEquipment = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_equipment", Arrays.asList("enable", 1), "", ResItemEquipmentEntity.class);
        mItemEquipment.clear();
        aItemEquipment.forEach(item -> mItemEquipment.put(item.getId(), item));
    }
}
