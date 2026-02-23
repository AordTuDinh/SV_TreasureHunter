package game.dragonhero.mapping;


import game.battle.object.BonusKillEnemy;
import game.battle.type.StateType;
import game.config.aEnum.DetailActionType;
import game.dragonhero.controller.WorldBossHandler;
import game.dragonhero.service.resource.ResParty;
import game.dragonhero.service.user.Bonus;
import game.monitor.Online;
import game.object.MyUser;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_party")
public class UserPartyEntity {
    @Id
    private int userId;
    private String members;
    Date createdAt;
    @Transient
    List<MyUser> channels;
    @Transient
    List<MyUser> channelAttackBoss = new ArrayList<>();

    public UserPartyEntity(int userId) {
        this.userId = userId;
        this.members = "[]";
        this.createdAt = Calendar.getInstance().getTime();
    }

    public List<Integer> getMembers() {
        if (members == null) {
            members = "[]";
        }
        return GsonUtil.strToListInt(members);
    }

    public synchronized void addChannel(MyUser myUser){
        if(channels==null) channels = new ArrayList<>();
        if(!channels.contains(myUser)){
            channels.add(myUser);
        }
    }

    public void addChannelAttackBoss(MyUser myUser){
        if(!channelAttackBoss.contains(myUser)){
            channelAttackBoss.add(myUser);
        }
    }

    public void leaveChannelAttackBoss(MyUser myUser){
        channelAttackBoss.remove(myUser);
    }

    public List<MyUser> getChannelsAttackBoss(){
        for (int i = 0; i <channelAttackBoss.size() ; i++) {
            if(channelAttackBoss.get(i)==null || !channelAttackBoss.get(i).getChannel().isOpen()){
                channelAttackBoss.remove(channelAttackBoss.get(i));
            }
        }
        return channelAttackBoss;
    }

    public void shareBonusParty(MyUser userShare, BonusKillEnemy bonusShare) {
        if (channels == null) checkChannel();

        if (userShare == null ||
                userShare.getPlayer() == null ||
                userShare.getPlayer().getRoom() == null ||
                userShare.getPlayer().getRoom().getKeyRoom() == null)
            return;

        String roomKey = userShare.getPlayer().getRoom().getKeyRoom();

        List<MyUser> shared = channels.stream()
                .filter(c ->
                        c.getUser().getId() != userShare.getUser().getId() &&
                                c != null &&
                                c.getPlayer() != null &&
                                c.getPlayer().getRoom() != null &&
                                roomKey.equals(c.getPlayer().getRoom().getKeyRoom())
                )
                .toList();

        if (shared.isEmpty()) return;

        int numShare = shared.size();
        long addExp = Math.max(1, (long) (bonusShare.getExp() * 0.35f / numShare));
        long addGold = Math.max(1, (long) (bonusShare.getGold() * 0.35f / numShare));
        List<Long> bonusX = Bonus.viewGold(addGold);
        bonusX.addAll(Bonus.viewExp(addExp));

        for (MyUser u : shared) {
            List<Long> bm = Bonus.receiveListItem(u, "ShareExpParty", bonusX);
            u.getPlayer().protoStatus(StateType.BONUS_SHARE_PARTY, bm.size(), bm);
        }

        // set lại 75% cho người chính sau khi chia 35%
        bonusShare.set75();
    }

    private void checkChannel() {
        channels = new ArrayList<>();
        List<Integer> lst = getMembersAndLeader();
        for (int i = 0; i < lst.size(); i++) {
            MyUser channel = Online.getMUser(lst.get(i));
            if (channel != null && channel.getChannel().isOpen()) {
                channels.add(channel);
            }
        }
    }

    public List<Integer> getMembersAndLeader() {
        List<Integer> lst = getMembers();
        lst.add(userId);
        return lst;
    }


    public boolean isLeader(int id) {
        return userId == id;
    }

    public boolean emptyMember() {
        return GsonUtil.strToListInt(members).isEmpty();
    }

    public boolean create() {
        if (DBJPA.save(this)) {
            ResParty.mPartyMap.put(userId, this);
            return true;
        }
        return false;
    }

    public boolean updateMembers(List<Integer> member) {
        String cacheMembers = members;
        members = StringHelper.toDBString(member);
        if (DBJPA.update(this)) {
            checkChannel();
            return true;
        } else {
            members = cacheMembers;
            return false;
        }
    }

    public boolean delete() {
        if (DBJPA.delete("user_party", "user_id", userId)) {
            ResParty.mPartyMap.remove(userId);
            return true;
        }
        return false;
    }

    public void offlineParty(MyUser mUser) {
        // ngừoi dùng logout
        WorldBossHandler.sendInfoToAllUser(this);
    }
}
