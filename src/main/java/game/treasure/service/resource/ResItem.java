package game.treasure.service.resource;

import game.config.CfgServer;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import ozudo.base.database.DBResource;

import java.util.*;

public class ResItem {
    // item
    static Map<Integer, ResItemEntity> mItem = new HashMap<>();
    // item equipment
    static Map<Integer, ResItemEquipmentEntity> mItemEquipment = new HashMap<>();
    // cover / stat materials (res_material)
    static Map<Integer, ResMaterialEntity> mMaterial = new HashMap<>();

    public static ResItemEquipmentEntity getItemEquipment(int itemId) {
        return mItemEquipment.get(itemId);
    }

    public static ResItemEntity getItem(int itemId) {
        return mItem.get(itemId);
    }

    public static ResMaterialEntity getMaterial(int materialId) {
        return mMaterial.get(materialId);
    }

    public static final int sizeItemEquipment = 24;

    public static void init() {
        // for item
        List<ResItemEntity> aItem = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item",  ResItemEntity.class);
        mItem.clear();
        aItem.forEach(item -> {
            item.init();
            mItem.put(item.getId(), item);
        });

        // for item equipment
        List<ResItemEquipmentEntity> aItemEquipment = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_equipment", ResItemEquipmentEntity.class);
        mItemEquipment.clear();
        aItemEquipment.forEach(item -> mItemEquipment.put(item.getId(), item));

        List<ResMaterialEntity> aMaterial = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_material", ResMaterialEntity.class);
        mMaterial.clear();
        aMaterial.forEach(row -> mMaterial.put(row.getId(), row));
    }
}
