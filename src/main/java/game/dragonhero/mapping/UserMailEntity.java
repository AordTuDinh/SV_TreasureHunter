package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.json.JSONArray;
import ozudo.base.database.DBJPA2;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.*;

@Entity
@Table(name = "user_mail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMailEntity {
    @Id
    int id;
    int userId, receive, senderId, mailIdx;
    String title, senderName, message, bonus;
    Date dateCreated;

    public List<Long> getListBonus() {
        if (bonus != null && bonus.length() > 2) {
            return new Gson().fromJson(bonus, new TypeToken<List<Long>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public boolean updateStatus(int status) {
        return DBJPA2.update("user_mail", Arrays.asList("receive", status), Arrays.asList("user_id", userId, "id", id));
    }

    public UserMailEntity initDefault() {
        this.mailIdx = Integer.parseInt(DateTime.getDateyyyyMMdd(Calendar.getInstance().getTime()));
        return this;
    }

    public protocol.Pbmethod.PbMail.Builder toProto() {
        protocol.Pbmethod.PbMail.Builder builder = protocol.Pbmethod.PbMail.newBuilder();
        builder.setId(id).setSenderId(senderId);
        builder.setTitle(title).setMessage(message);
        builder.setReceive(receive);
        builder.setSenderName(senderName == null ? "..." : senderName);
        if (!StringHelper.isEmpty(bonus)) {
            JSONArray arrBonus = JSONArray.fromObject(bonus);
            for (int i = 0; i < arrBonus.size(); i++) builder.addBonus(arrBonus.getInt(i));
        }
        builder.setTime(dateCreated.getTime());
        return builder;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public Date getDateCreated() {
        return dateCreated == null ? Calendar.getInstance().getTime() : dateCreated;
    }


}
