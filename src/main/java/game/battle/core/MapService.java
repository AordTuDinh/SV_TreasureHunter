
//    private static int clamp(int v, int min, int max) {
//        return Math.max(min, Math.min(max, v));
//    }
//
//    // ===== AddChunk: gửi nhiều chunk 1 lúc =====
//    public Pbmethod.AddChunk buildAddChunk(List<ChunkObject> enterChunks) {
//        Pbmethod.AddChunk.Builder out = Pbmethod.AddChunk.newBuilder();
//        for (ChunkObject chunk : enterChunks) {
//            out.addChunkSnapshot(toPbChunk(chunk));
//        }
//        return out.build();
//    }
//
//
//    // ===== RemoveChunk: gửi từng chunk =====
//    public Pbmethod.RemoveChunk buildRemoveChunk(ChunkObject leaveChunk) {
//        return Pbmethod.RemoveChunk.newBuilder()
//                .setChunk(Pbmethod.PbChunk.newBuilder().setChunk(leaveChunk.getPos().toProto()).build())
//                .build();
//    }
//
//    // Nếu muốn gửi 1 batch remove chunk (theo cơ chế riêng của bạn)
//    public List<Pbmethod.RemoveChunk> buildRemoveChunks(List<ChunkObject> leaveChunks) {
//        List<Pbmethod.RemoveChunk> out = new ArrayList<>(leaveChunks.size());
//        for (ChunkObject cc : leaveChunks) {
//            out.add(buildRemoveChunk(cc));
//        }
//        return out;
//    }
//
//    // ===== AddCell =====
//    public Pbmethod.AddCell buildAddCell(ChunkObject chunk, int cellId, int x, int y, int type, int state) {
//        return Pbmethod.AddCell.newBuilder()
//                .setChunk(Pbmethod.PbChunk.newBuilder().setChunk(chunk.getPos().toProto()).build())
//                .setCell(Pbmethod.PbCell.newBuilder()
//                        .setCellId(cellId)
//                        .setX(x)
//                        .setY(y)
//                        .setType(type)
//                        .setState(state)
//                        .build())
//                .build();
//    }
//
//    // ===== RemoveCell =====
//    public Pbmethod.RemoveCell buildRemoveCell(int cellId) {
//        return Pbmethod.RemoveCell.newBuilder()
//                .setCellId(cellId)
//                .build();
//    }
//
//    private static Pbmethod.PbChunk toPbChunk(ChunkObject chunk) {
//        Pbmethod.PbChunk.Builder chunkBuilder = Pbmethod.PbChunk.newBuilder()
//                .setChunkId(chunk.getId())
//                .setChunk(Pbmethod.PbPos.newBuilder()
//                        .setX(chunk.getPos().x)
//                        .setY(chunk.getPos().y)
//                        .build());
//
//        for (StaticCell c : chunk.getCells()) {
//            chunkBuilder.addCells(Pbmethod.PbCell.newBuilder()
//                    .setX(c.x())
//                    .setY(c.y())
//                    .setType(c.type())
//                    .setState(0)
//                    .build());
//        }
//        return chunkBuilder.build();
//    }
//
//    private static List<Integer> getVisibleChunkIds(int centerChunkX, int centerChunkY, int radius) {
//        int minX = Math.max(MapConfig.minChunkX, centerChunkX - radius);
//        int maxX = Math.min(MapConfig.maxChunkX, centerChunkX + radius);
//        int minY = Math.max(MapConfig.minChunkY, centerChunkY - radius);
//        int maxY = Math.min(MapConfig.maxChunkY, centerChunkY + radius);
//
//        List<Integer> out = new ArrayList<>();
//        for (int y = minY; y <= maxY; y++) {
//            for (int x = minX; x <= maxX; x++) {
//                out.add(chunkPosToId(x, y));
//            }
//        }
//        return out;
//    }
//
//    private static int worldToChunkX(float worldX) {
//        int wx = clamp((int) Math.floor(worldX), (int) MapConfig.botLeft.x, (int) (MapConfig.topRight.x - 1));
//        int localX = (int) (wx - MapConfig.botLeft.x);
//        int nx = Math.floorDiv(localX, MapConfig.CHUNK_SIZE);
//        return MapConfig.minChunkX + nx;
//    }
//
//    private static int worldToChunkY(float worldY) {
//        int wy = clamp((int) Math.floor(worldY), (int) MapConfig.botLeft.y, (int) (MapConfig.topRight.y - 1));
//        int localY = (int) (wy - MapConfig.botLeft.y);
//        int ny = Math.floorDiv(localY, MapConfig.CHUNK_SIZE);
//        return MapConfig.minChunkY + ny;
//    }
//
//    private static int chunkPosToId(int chunkX, int chunkY) {
//        int nx = chunkX - MapConfig.minChunkX;
//        int ny = chunkY - MapConfig.minChunkY;
//        return ny * MapConfig.MAP_CHUNK_W + nx;
//    }
//
//
//}
