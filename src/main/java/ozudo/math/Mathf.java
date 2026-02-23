package ozudo.math;

import game.battle.object.Pos;

public class Mathf {
    void Update() {
        float angle = 0;
        float radius = 1f;
        float speed = 1f;
        Pos center = Pos.zero();
        float deltaTime = 0.005f;
        float posX = (float) (center.x + Math.cos(angle) * radius);
        float posy = (float) (center.y + Math.sin(angle) * radius);
        angle += deltaTime * speed;
        if (angle >= 360) {
            angle = 0;
        }
    }
}
