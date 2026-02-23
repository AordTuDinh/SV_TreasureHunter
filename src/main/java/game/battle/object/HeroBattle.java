package game.battle.object;

import game.battle.effect.SkillEffect;
import game.battle.model.ArenaHero;
import game.battle.type.CharacterType;
import game.config.aEnum.PetType;
import game.dragonhero.mapping.UserHeroEntity;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.service.resource.ResPet;
import game.dragonhero.table.ArenaRoom;
import game.object.MyUser;
import game.protocol.CommonProto;
import lombok.Data;
import lombok.NoArgsConstructor;
import protocol.Pbmethod;

import java.util.List;

@Data
@NoArgsConstructor
public class HeroBattle {
    int team;
    int id;
    int avatar; // hero key
    int level;
    int slot;
    CharacterType heroType;
    Point point;
    WeaponBattle[] weaponBattles;
    List<Long> buff;
    SkillEffect petSkill;
    float timePetActive;


    public HeroBattle(int team, int id, int avatar, Point point, int slot, WeaponBattle[] weaponBattles, UserPetEntity monster) {
        this.team = team;
        this.id = id;
        this.avatar = avatar;
        this.point = point;
        this.slot = slot;
        this.level = 1;
        this.weaponBattles = weaponBattles;
        this.heroType = CharacterType.HERO;
        // check buff monster
        if (monster != null)
            point.buffListPoint(ResPet.getDataEquipByLevel(monster.getResMonster().getDataEquip(), monster.getStar()));
        // reset base point
        point.resetHpMp();
    }

    public HeroBattle(UserPetEntity uPet, int team, int id, Point point) { // for pet
        this.team = team;
        this.id = id;
        this.avatar = uPet.getPetId();
        this.point = point;
        point.resetHpMp();
        this.weaponBattles = null;
        if (uPet.getType() == PetType.ANIMAL) {
            ResPetEntity res = uPet.getResPet();
            this.heroType = CharacterType.PET;
            petSkill = new SkillEffect(res.getPetSkill(), uPet.getStar());
        } else {
            this.heroType = CharacterType.MONSTER;
            this.buff = ResPet.getDataEquipByLevel(uPet.getResMonster().getDataEquip(), uPet.getStar());
        }
        this.level = uPet.getStar();
    }

    public boolean calPoint(MyUser mUser, Point basePoint, UserPetEntity monster) { // return has update db
        UserHeroEntity uHero = mUser.getResources().getHero(avatar);
        if (uHero == null) return false;
        uHero.calPointHero(mUser, basePoint);
        if (monster != null)
            basePoint.buffListPoint(ResPet.getDataEquipByLevel(monster.getResMonster().getDataEquip(), monster.getStar()));
        if (!this.point.equals(basePoint)) {
            this.point = basePoint;
            return true;
        }
        return false;
    }

    public Pbmethod.PbArenaHeroInfo toProto() {
        Pbmethod.PbArenaHeroInfo.Builder pb = Pbmethod.PbArenaHeroInfo.newBuilder();
        pb.setAvatar(avatar);
        for (int i = 0; i < weaponBattles.length; i++) {
            if (weaponBattles[i] != null) pb.addWeapons(weaponBattles[i].toProto());
            else pb.addWeapons(Pbmethod.PbArenaWeapon.newBuilder().setId(0));
        }
        return pb.build();
    }

    public Pbmethod.PbArenaPetInfo toPetProto() {
        Pbmethod.PbArenaPetInfo.Builder pet = Pbmethod.PbArenaPetInfo.newBuilder();
        pet.setAvatar(avatar);
        pet.setStar(level);
        return pet.build();
    }


    public ArenaHero toArenaHero(ArenaRoom room, int team) {
        if (avatar == 0) return null;
        ArenaHero hero = new ArenaHero(getId(), team, new Pos(posHero()), point, heroType, room);
        //notes buff for test
//        hero.getPoint().addHp(100000);
//        hero.getPoint().resetHpMp();

        if (weaponBattles != null) {
            for (int i = 0; i < weaponBattles.length; i++) {
                if (weaponBattles[i] != null) {
                    hero.getWeaponEquip().add(weaponBattles[i].toShuriken(point, weaponBattles[i].level));
                }
            }
        }
        return hero;
    }

    public ArenaHero toArenaPet(ArenaRoom room) {
        if (avatar == 0) return null;
        ArenaHero pet = new ArenaHero(getId(), team, Pos.zero(), point, heroType, room);
        pet.setPetData(petSkill, timePetActive);
        return pet;
    }

    public Pbmethod.PbBattleArenaHero toArenaProto() {
        Pbmethod.PbBattleArenaHero.Builder pb = Pbmethod.PbBattleArenaHero.newBuilder();
        pb.setId(id);
        pb.setAvatar(avatar);
        CharacterType type = heroType;
        if (type == null) {
            if (slot < 3) type = CharacterType.HERO;
            else if (slot == 3) type = CharacterType.PET;
            else type = CharacterType.MONSTER;
        }
        pb.setHeroType(type.value);
        pb.setSlot(slot);
        pb.setLevel(level);
        Point cloneP = point.cloneInstance();
        cloneP.resetHpMp();
        pb.addAllPoint(cloneP.toProto());
        if (heroType == CharacterType.HERO) {
            pb.addAllPos(posHero());
            pb.addAllDirection(heroDirection());
        } else if (heroType == CharacterType.PET) {
            pb.addAllPos(posPet());
            pb.addAllDirection(heroDirection());
        } else {
            pb.setInfo(CommonProto.getCommonVector(buff));
        }

        if (weaponBattles != null && weaponBattles.length > 0) {
            for (int i = 0; i < weaponBattles.length; i++) {
                if (weaponBattles[i] != null) pb.addWeapons(weaponBattles[i].toBattleProto());
            }
        }
        return pb.build();
    }

    public List<Float> posHero() {
        if (team == 1) {
            return slot == 0 ? List.of(-0.85f, 1.25f) : slot == 2 ? List.of(-1.6f, -1.7f) : List.of(-2.2f, 0f);
        } else {
            return slot == 0 ? List.of(0.85f, 1.25f) : slot == 2 ? List.of(1.6f, -1.7f) : List.of(2.2f, 0f);
        }
    }

    public List<Float> posPet() {
        if (team == 1) {
            return List.of(-1.7f, -2.7f);
        } else {
            return List.of(1.7f, -2.7f);
        }
    }

    public List<Float> heroDirection() {
        if (team == 1) return List.of(1f, 0f);
        return List.of(-1f, 0f);
    }
}
