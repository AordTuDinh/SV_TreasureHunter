package game.config.aEnum;

/** Index trong user_settings.vip_data — 11 type (0..10). */
public final class VipType {
    public static final int COUNT = 11;

    public static final int PROTECTION_SHIELD_HOUR = 0;
    public static final int MARKET_LISTING_RUBY = 1;
    public static final int MARKET_TAX = 2;
    public static final int GOLD_MINING = 3;
    public static final int STONE_MINING_RATE = 4;
    public static final int GOLD_RECEIVED = 5;
    public static final int DIAMOND_MINING_RATE = 6;
    public static final int GENERAL_MINING_RATE = 7;
    public static final int TRANSMUTE_RATE = 8;
    public static final int UPGRADE_FEE = 9;
    public static final int CRAFT_FEE = 10;

    private VipType() {
    }
}
