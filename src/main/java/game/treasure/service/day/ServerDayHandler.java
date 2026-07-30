package game.treasure.service.day;

/** Một bước xử lý khi server sang ngày mới — đăng ký trong {@link ServerDayPipeline}. */
@FunctionalInterface
public interface ServerDayHandler {
    void onNewDay(ServerDayContext ctx) throws Exception;
}
