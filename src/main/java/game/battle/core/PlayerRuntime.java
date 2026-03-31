package game.battle.core;

import game.battle.model.Chunk;

import java.util.HashSet;
import java.util.Set;

public class PlayerRuntime {
    final long playerId;
    float x;
    float y;
    Chunk currentChunk;
    Set<Chunk> subscribedChunks = new HashSet<>();

    PlayerRuntime(long playerId) {
        this.playerId = playerId;
    }


}
