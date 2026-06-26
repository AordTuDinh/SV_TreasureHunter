package game.treasure.service.user;

import game.battle.model.Player;
import game.battle.object.Pos;
import game.config.ArtifactDataSlot;
import game.config.aEnum.ArtifactType;
import game.object.MyUser;
import game.treasure.mapping.UserArtifactEntity;
import game.treasure.table.BaseRoom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Chọn người nhận buff cổ vật type 1/2/3 trong room. */
public final class ArtifactBuffTargets {
    private ArtifactBuffTargets() {
    }

    public static List<MyUser> resolve(MyUser caster, ArtifactType type, UserArtifactEntity artifact) {
        List<MyUser> result = new ArrayList<>();
        if (caster == null || type == null || artifact == null)
            return result;
        Player casterPlayer = caster.getPlayer();
        if (casterPlayer == null)
            return result;
        BaseRoom room = casterPlayer.getRoom();
        if (room == null)
            return result;

        float range = artifact.getEffectiveSlot(ArtifactDataSlot.IDX_RANGE);
        int personLimit = Math.max(1, Math.round(artifact.getEffectiveSlot(ArtifactDataSlot.IDX_PERSON)));
        long casterClan = caster.getUser().getClan();
        Pos casterPos = casterPlayer.getPos();

        return switch (type) {
            case PERSONAL -> {
                result.add(caster);
                yield result;
            }
            case TEAMMATE -> {
                if (casterClan == 0) {
                    result.add(caster);
                    yield result;
                }
                List<ScoredPlayer> candidates = new ArrayList<>();
                for (Player p : room.listPlayers()) {
                    if (p == null || p.getMUser() == null)
                        continue;
                    MyUser target = p.getMUser();
                    if (target.getUser().getClan() != casterClan)
                        continue;
                    double dist = casterPos.distance(p.getPos());
                    if (dist > range)
                        continue;
                    candidates.add(new ScoredPlayer(target, dist));
                }
                candidates.sort(Comparator.comparingDouble(s -> s.distance));
                for (int i = 0; i < candidates.size() && result.size() < personLimit; i++)
                    result.add(candidates.get(i).user);
                yield result;
            }
            case NERF -> {
                List<ScoredPlayer> candidates = new ArrayList<>();
                for (Player p : room.listPlayers()) {
                    if (p == null || p.getMUser() == null)
                        continue;
                    MyUser target = p.getMUser();
                    if (target.getUser().getId() == caster.getUser().getId())
                        continue;
                    if (target.getUser().getClan() == casterClan)
                        continue;
                    if (target.getUData().getTimeProtected() > System.currentTimeMillis())
                        continue;
                    double dist = casterPos.distance(p.getPos());
                    if (dist > range)
                        continue;
                    candidates.add(new ScoredPlayer(target, dist));
                }
                candidates.sort(Comparator.comparingDouble(s -> s.distance));
                for (int i = 0; i < candidates.size() && result.size() < personLimit; i++)
                    result.add(candidates.get(i).user);
                yield result;
            }
        };
    }

    static final class ScoredPlayer {
        final MyUser user;
        final double distance;

        ScoredPlayer(MyUser user, double distance) {
            this.user = user;
            this.distance = distance;
        }
    }
}
