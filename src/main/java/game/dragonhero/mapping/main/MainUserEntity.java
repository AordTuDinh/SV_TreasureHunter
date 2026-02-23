package game.dragonhero.mapping.main;

import game.config.CfgServer;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import javax.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "main_user")
@NoArgsConstructor
public class MainUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username, facebook, email, mobile, hpass, cp, os, device, salt;
    private Date lastLogin;
    private String lastLoginIp, udid, last_udid, version, serverIds, reg_ip, operator;

    public MainUserEntity(Pbmethod.PbRegister re, String ip) {
        this.username = re.getUsername();
        this.facebook = "";
        this.email = "";
        this.mobile = re.getPhone();
        this.hpass = Util.get_SHA_256_SecurePassword(re.getPassword(), re.getSalt());
        this.cp = "test";
        this.os = re.getOs();
        this.device = re.getDevice();
        this.salt = re.getSalt();
        this.udid = re.getUdid();
        this.last_udid = re.getUdid();
        this.version = re.getVersion();
        this.reg_ip = ip;
        this.operator = re.getOperatorName();
    }

    public List<Integer> getServerIds() {
        if (StringHelper.isEmpty(serverIds)) return new ArrayList<>();
        return GsonUtil.strToListInt(serverIds);
    }

    public boolean update(List<Object> objs) {
        return DBJPA.update(CfgServer.DB_MAIN + "main_user", objs, List.of("id", id));
    }

    public Pbmethod.PbRegister toProto(String pass) {
        Pbmethod.PbRegister.Builder pb = Pbmethod.PbRegister.newBuilder();
        pb.setUsername(username);
        pb.setPassword(pass);
        pb.setCp(cp);
        pb.setVersion(version);
        return pb.build();
    }

}
