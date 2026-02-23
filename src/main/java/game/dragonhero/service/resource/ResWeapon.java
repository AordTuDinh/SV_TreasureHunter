package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.config.aEnum.PieceType;
import game.dragonhero.mapping.main.ResComboWeaponEntity;
import game.dragonhero.mapping.main.ResPieceEntity;
import game.dragonhero.mapping.main.ResWeaponEntity;
import game.dragonhero.mapping.main.ResWeaponMapStoneEntity;
import ozudo.base.database.DBResource;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResWeapon {
    static Map<Integer, ResWeaponEntity> mWeapon = new HashMap<>();
    public static Map<Integer, ResComboWeaponEntity> mComboWeapon = new HashMap<>();
    public static Map<Integer, List<ResWeaponEntity>> mWeaponSummon = new HashMap<>(); // rank ,list id
    static Map<Integer, ResPieceEntity> mPieceWeapon = new HashMap<>();
    static Map<Integer, Integer> mWeaponMapStone = new HashMap<>(); // dung de map id weapon nang cap thi can id item nao

    public static ResWeaponEntity getWeapon(int itemId) {
        return mWeapon.get(itemId);
    }

    public static ResPieceEntity getPiece(int type, int id) {
        switch (PieceType.get(type)) {
            case WEAPON -> {
                return mPieceWeapon.get(id);
            }
        }
        return null;
    }

    public static Integer getWeaponMapStone(int id) {
        return mWeaponMapStone.get(id);
    }

    public static void init() {
        List<ResWeaponEntity> aWeapon = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_weapon", ResWeaponEntity.class);
        mWeapon.clear();
        mWeaponSummon.clear();
        aWeapon.forEach(item -> {
            item.init();
            mWeapon.put(item.getId(), item);
            if (item.getRank() > 0) { // =0 là của quái
                if (!mWeaponSummon.containsKey(item.getRank())) {
                    mWeaponSummon.put(item.getRank(), new ArrayList<>());
                }
                mWeaponSummon.get(item.getRank()).add(item);
            }
        });
        // combo weapon
        List<ResComboWeaponEntity> aComboWeapon = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_combo_weapon", ResComboWeaponEntity.class);
        mComboWeapon.clear();
        aComboWeapon.forEach(item -> {
            item.initData();
            mComboWeapon.put(item.getId(), item);
        });
        // piece weapon
        List<ResPieceEntity> aPieceWeapon = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_piece", ResPieceEntity.class);
        mPieceWeapon.clear();
        aPieceWeapon.forEach(piece -> {
            if (piece.getType() == PieceType.WEAPON.value) mPieceWeapon.put(piece.getId(), piece);
        });
        // weapon map stone
        List<ResWeaponMapStoneEntity> aWeaponMapStone = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_weapon_map_stone", ResWeaponMapStoneEntity.class);
        mWeaponMapStone.clear();
        aWeaponMapStone.forEach(item -> mWeaponMapStone.put(item.getWeaponId(), item.getStoneId()));
    }

    public static int randomItemByRank(int rank) {
        List<ResWeaponEntity> lst = mWeaponSummon.get(rank);
        int rand = NumberUtil.getRandom(lst.size());
        return lst.get(rand).getId();
    }
}
