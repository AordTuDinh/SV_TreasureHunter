package game.battle.model;

import java.util.List;

public record DiffResult(List<Chunk> enter, List<Chunk> leave) {}
