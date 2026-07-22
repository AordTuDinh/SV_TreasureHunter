package game.treasure.mapping;

import game.treasure.mapping.main.ResItemPointEntity;
import game.treasure.service.resource.ResItemPoint;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
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
@NoArgsConstructor
@Entity
@Table(name = "user_item_point")
public class UserItemPointEntity implements Serializable {
    /** Flush DB tối đa 1 lần / phút cho điểm trừ nhiều (PLOT). */
    public static final long DEFER_FLUSH_MS = 60_000L;

    @Id
    int userId;
    @Id
    int pointId;
    int number;
    int server;
    String data;

    @Transient
    boolean numberDirty;
    @Transient
    long lastNumberFlushMs;

    public UserItemPointEntity(int userId, int pointId, int server) {
        this.userId = userId;
        this.pointId = pointId;
        this.server = server;
        this.number = 0;
        this.data = "[]";
    }

    public ResItemPointEntity getRes() {
        return ResItemPoint.get(pointId);
    }

    public List<Long> getDataLongList() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new ArrayList<>();
        return GsonUtil.strToListLong(data);
    }

    public void setDataLongList(List<Long> list) {
        data = list == null || list.isEmpty() ? "[]" : StringHelper.toDBString(list);
    }

    /** Vé số: data = [eventDay, num1, num2, ...] */
    public void appendTicketNumbers(long eventDay, List<Long> nums) {
        List<Long> sticker = new ArrayList<>(getDataLongList());
        if (sticker.isEmpty() || sticker.get(0) != eventDay) {
            sticker = new ArrayList<>();
            sticker.add(eventDay);
        }
        sticker.addAll(nums);
        setDataLongList(sticker);
        number = Math.max(0, sticker.size() - 1);
    }

    public List<Long> getTicketNumbersForEvent(long eventDay) {
        List<Long> sticker = getDataLongList();
        if (sticker.isEmpty() || sticker.get(0) != eventDay)
            return new ArrayList<>();
        return new ArrayList<>(sticker.subList(1, sticker.size()));
    }

    public protocol.Pbmethod.PbItemPoint.Builder toProto() {
        protocol.Pbmethod.PbItemPoint.Builder pb = protocol.Pbmethod.PbItemPoint.newBuilder();
        pb.setItemKey(pointId);
        pb.setNumber(number);
        return pb;
    }

    public boolean saveOrUpdate() {
        return DBJPA.saveOrUpdate(this);
    }

    public boolean updateNumber(int newNumber) {
        this.number = newNumber;
        return DBJPA.update("user_item_point",
                Arrays.asList("number", number, "server", server, "data", data == null ? "[]" : data),
                Arrays.asList("user_id", userId, "point_id", pointId));
    }

    public boolean persist() {
        return DBJPA.update("user_item_point",
                Arrays.asList("number", number, "server", server, "data", data == null ? "[]" : data),
                Arrays.asList("user_id", userId, "point_id", pointId));
    }

    public void markNumberDirty() {
        numberDirty = true;
    }

    /** Ghi DB nếu dirty; dùng khi logout hoặc đã đủ interval. */
    public boolean flushNumberIfDirty() {
        if (!numberDirty)
            return true;
        if (persist()) {
            numberDirty = false;
            lastNumberFlushMs = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /** Memory luôn cập nhật; DB tối đa 1 lần / {@link #DEFER_FLUSH_MS}. */
    public boolean setNumberDeferred(int newNumber) {
        this.number = newNumber;
        markNumberDirty();
        long now = System.currentTimeMillis();
        if (lastNumberFlushMs == 0) {
            lastNumberFlushMs = now;
            return true;
        }
        if (now - lastNumberFlushMs < DEFER_FLUSH_MS)
            return true;
        return flushNumberIfDirty();
    }
}
