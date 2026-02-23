package game.battle.object;

import game.battle.calculate.MathLab;
import game.dragonhero.BattleConfig;
import game.object.MapData;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by vieth_000 on 7/14/2016.
 */
public class Pos {
    public float x, y;

    // khoảng cách thực tế đến đoạn 0 , 0
    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    // xác định hướng đi à ?
    public Pos normalized() {
        float mag = this.magnitude();
        if (mag == 0) return Pos.zero();
        float _x = x / mag;
        float _y = y / mag;
        return new Pos(_x, _y);
    }

    public Pos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public static Pos zero() {
        return new Pos(0f, 0f);
    }

    public static Pos right() {
        return new Pos(1f, 0f);
    }

    public static Pos left() {
        return new Pos(-1f, 0f);
    }

    public static Pos up() {
        return new Pos(0f, 1f);
    }

    public static Pos down() {
        return new Pos(0f, -1f);
    }

    public static Pos one() {
        return new Pos(1f, 1f);
    }

    public Pos(Pbmethod.PbPos pbPos) {
        this.x = pbPos.getX();
        this.y = pbPos.getY();
    }

    public Pos(List<Float> pos) {
        this.x = pos.get(0);
        this.y = pos.get(1);
    }

    public Pos reverse() {
        return new Pos(x * -1, y * -1);
    }


    public Pos clone() {
        return new Pos(x, y);
    }

    public Pos cloneAndOffset(float ox, float oy) {
        return new Pos(this.x + ox, y + oy);
    }

    public void addAndRound(Pos p) {
        x += p.x;
        y += p.y;
        x = Math.round(x * 1000f) / 1000f;
        y = Math.round(y * 1000f) / 1000f;
    }


    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void addX(float x) {
        this.x += x;
    }

    public void addY(float y) {
        this.y += y;
    }

    public static Pos randomPos(float x1, float x2, float y1, float y2) {
        return new Pos(NumberUtil.getRandom(x1, x2), NumberUtil.getRandom(y1, y2));
    }

    // random trong 1 khoảng cách main pos và nằm trong vùng panel đồng thời cap 1 khoảng(cách tưởng 1 khoảng collider)
    public static Pos randomPos(Pos mainPos, float x, float y, Pos botLeft, Pos topRight, float cap) {
        float px = mainPos.x + NumberUtil.getRandom(-x, x);
        cap = cap < 0 ? 0 : cap;
        if (px < botLeft.x + cap) px = botLeft.x + cap;
        if (px > topRight.x - cap) px = topRight.x - cap;
        float py = mainPos.y + NumberUtil.getRandom(-y, y);
        if (py < botLeft.y + cap) py = botLeft.y + cap;
        if (py > topRight.y - cap) py = topRight.y - cap;
        return new Pos(px, py).round();
    }

    // check main pos nằm trong vùng panel đồng thời cap 1 khoảng(cách tưởng 1 khoảng collider)
    public static Pos capPos(Pos mainPos, Pos botLeft, Pos topRight, float cap) {
        cap = cap < 0 ? 0 : cap;
        Pos pos = mainPos.clone();
        if (pos.x < botLeft.x + cap) pos.x = botLeft.x + cap;
        if (pos.x > topRight.x - cap) pos.x = topRight.x - cap;
        if (pos.y < botLeft.y + cap) pos.y = botLeft.y + cap;
        if (pos.y > topRight.y - cap) pos.y = topRight.y - cap;
        return pos.round();
    }

    public static Pos randomPos(List<Pos> pos) {
        if (pos.size() < 2) return Pos.zero();
        return randomPos(pos.get(0).x, pos.get(1).x, pos.get(0).y, pos.get(1).y);
    }

    public static Pos randomInPanel(MapData map,float ofsetX, float ofsetY) {
        return randomPos(map.getBotLeft().x-ofsetX, map.getTopRight().x-ofsetX,
                map.getBotLeft().y-ofsetY, map.getTopRight().y-ofsetY);
    }

    public static Pos randomPos(Pos target, float rangeX, float rangeY) {
        return new Pos(NumberUtil.getRandom(target.x - rangeX, target.x + rangeX), NumberUtil.getRandom(target.y - rangeY, target.y + rangeY));
    }

    public static Pos v_add(PanelMap map, Pos a, Pos b) {
        // cai này có vẻ chuẩn rồi, phải làm tròn để trùng với client
        float tmpX = a.x + b.x;
        float tmpY = a.y + b.y;
        return GameCore.checkWall(map, new Pos(tmpX, tmpY).round());
    }

    public static List<Pos> toListPos(List<Float> lst) {
        List<Pos> lstPos = new ArrayList<>();
        for (int i = 0; i < lst.size(); i += 2) {
            lstPos.add(new Pos(lst.get(i), lst.get(i + 1)));
        }
        return lstPos;
    }

    public void v_add(PanelMap map, Pos newPos) {
        Pos check = GameCore.checkWall2(map, newPos, this).round();
        this.x = check.x;
        this.y = check.y;
    }

    public void add(Pos pos) {
        this.x += pos.x;
        this.y += pos.y;
    }

    public void v_addBullet(PanelMap map, Pos newPos) {
        this.x += newPos.x;
        this.y += newPos.y;
    }

    public Pos round() {
        return new Pos(Math.round(x * 1000f) / 1000f, Math.round(y * 1000f) / 1000f);
    }

    public boolean equals(Pos pos) {
        return x == pos.x && y == pos.y;
    }

    public boolean likeEquals(Pos pos) {
        return Math.abs(x - pos.x) < 0.1f && Math.abs(y - pos.y) < 0.1f;
    }

    public double distance(Pos pos) {
        return Math.sqrt(Math.pow(pos.x - x, 2) + Math.pow(pos.y - y, 2));
    }

    public Pos getDirectionTo(Pos target) {
        return MathLab.getDirection(this, target);
    }


    public Pos getRandDiectionTo(Pos target, float angle) {
        Pos direction = getDirectionTo(target);
        int rand = (int) NumberUtil.randomRange(angle);
        return MathLab.angle2Direction(rand, direction);
    }

    public Pos getRandDiectionToTarget(Pos target) {
        Pos direction = getDirectionTo(target);
        int rand = (int) NumberUtil.randomRange(70);
        return MathLab.angle2Direction(rand, direction);
    }

    public String toString() {
        return String.format("(%s, %s)", x, y);
    }

    public Pos oppositeDirection() {
        return new Pos(-x, -y);
    }

    public static Pos moveFromDirection(Pos direction, float speed) {
        return new Pos(direction.x * speed * BattleConfig.hSpeed, direction.y * speed * BattleConfig.hSpeed);
    }

    public static Pos RandomPos() {
        return new Pos(new Random().nextFloat() * 5, new Random().nextFloat() * 5);
    }

    public static Pos RandomDirection() {
        return NumberUtil.getRandom(2) == 0 ? Pos.right() : Pos.left();
    }

    public Pbmethod.PbPos toProto() {
        Pbmethod.PbPos.Builder mPos = Pbmethod.PbPos.newBuilder();
        mPos.setX(x);
        mPos.setY(y);
        return mPos.build();
    }

    public void multiple(float v) {
        x *= v;
        y *= v;
    }
}
