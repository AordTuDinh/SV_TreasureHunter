package game.dragonhero.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
public class ResIAPEntity extends BaseEntity {
    @Getter
    @Id
    int id;
    @Getter
    String name;
    String addBonus, addTitle, bonus;
    @Getter
    int price, vipExp, priceQr;
    @Getter
    String productIdAndroid;
    @Getter
    String productIdIos;


    public void init() {
        checkJson(id, addBonus);
        checkJson(id, bonus);
    }

    public Pbmethod.PpIAP.Builder getPbIap() {
        Pbmethod.PpIAP.Builder pbIap = Pbmethod.PpIAP.newBuilder();
        pbIap.setId(id);
        pbIap.setProductIdAndroid(productIdAndroid);
        pbIap.setProductIdIos(productIdIos);
        pbIap.setName(name);
        pbIap.setPrice(price + "");
        pbIap.setPriceQr(priceQr + "");
        pbIap.addAllBonus(GsonUtil.strToListLong(bonus));
        pbIap.addAllAddBonus(GsonUtil.strToListLong(addBonus));
        pbIap.setVipExp(vipExp);
        pbIap.setAddTitle(addTitle);
        return pbIap;
    }

    public List<Long> getABonus() {
        List<Long> aBonus = GsonUtil.strToListLong(bonus);
        List<Long> aBonusAdd = GsonUtil.strToListLong(addBonus);
        aBonus.addAll(aBonusAdd);
        return new ArrayList<>(aBonus);
    }
}
