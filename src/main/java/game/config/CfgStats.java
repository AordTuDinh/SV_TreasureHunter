package game.config;

import com.google.gson.Gson;
import lombok.Data;
import ozudo.base.helper.NumberUtil;

/**
 * Công thức % hiệu lực chung (Né, Giáp, Crit, Tốc độ đánh, Phản đòn...).
 * DB key {@code config_stats} → {@link #loadConfig(String)}.
 * <p>
 * rate(D) = successMax × (1 − e^(−D / scale)), scale suy từ mốc referenceEffective → successAtReference.
 */
public class CfgStats {

    private static DataConfig cfg = defaultConfig();
    private static float successScale = computeSuccessScale(cfg.referenceEffective, cfg.successMax, cfg.successAtReference);
    private static float elementSuccessScale = computeSuccessScale(cfg.elementReferenceEffective, cfg.successMax, cfg.successAtReference);

    public static void loadConfig(String strJson) {
        if (strJson == null || strJson.isBlank()) {
            return;
        }
        DataConfig loaded = new Gson().fromJson(strJson, DataConfig.class);
        if (loaded == null) {
            return;
        }
        mergeConfig(loaded);
        successScale = computeSuccessScale(cfg.referenceEffective, cfg.successMax, cfg.successAtReference);
        elementSuccessScale = computeSuccessScale(cfg.elementReferenceEffective, cfg.successMax, cfg.successAtReference);
    }

    /** Chỉ số hiệu dụng: stat − counter, cap, ≥ 0. */
    public static long getEffectiveStat(long stat, long counter, int statCap, int counterCap) {
        long s = Math.min(Math.max(stat, 0), statCap);
        long c = Math.min(Math.max(counter, 0), counterCap);
        return Math.max(0, s - c);
    }

    /** Stat dùng công thức success, không có counter (Crit, Giáp, Tốc độ đánh, Độc, Gió...). */
    public static long getEffectiveStatForSuccess(long stat) {
        return getEffectiveStat(stat, 0, cfg.statCap, 0);
    }

    public static long getEffectiveDodge(long dodge, long accuracy) {
        return getEffectiveStat(dodge, accuracy, cfg.statCap, cfg.statCap);
    }

    /** Chính Xác còn dư sau khi trừ Né 1:1. */
    public static long getAccuracyOverflowAfterDodge(long accuracy, long dodge) {
        long cappedAccuracy = Math.min(Math.max(accuracy, 0), cfg.statCap);
        long cappedDodge = Math.min(Math.max(dodge, 0), cfg.statCap);
        return Math.max(0, cappedAccuracy - cappedDodge);
    }

    /** 2 Chính Xác dư trừ 1 Giáp. */
    public static long calcDefenseAfterAccuracy(long defense, long accuracy, long dodge) {
        long armorPierce = getAccuracyOverflowAfterDodge(accuracy, dodge) / 2;
        return Math.max(0, defense - armorPierce);
    }

    /**
     * % hiệu lực 0..successMax (vd. Né, giảm sát thương giáp, tỉ lệ crit/proc).
     * effectiveStat = 450 → ~72% (theo default config).
     */
    public static float calcSuccessRate(long effectiveStat) {
        if (effectiveStat <= 0) {
            return 0f;
        }
        return cfg.successMax * (1f - (float) Math.exp(-effectiveStat / successScale));
    }

    public static boolean rollSuccess(long effectiveStat) {
        float rate = calcSuccessRate(effectiveStat);
        return rate > 0f && NumberUtil.getRandom(1000) < (int) (rate * 1000f);
    }

    /**
     * % proc nguyên tố (Độc/Lửa/Gió/Băng): cùng đường cong success nhưng mốc 600 → 72%.
     */
    public static float calcElementSuccessRate(long effectiveStat) {
        if (effectiveStat <= 0) {
            return 0f;
        }
        return cfg.successMax * (1f - (float) Math.exp(-effectiveStat / elementSuccessScale));
    }

    public static boolean rollElementSuccess(long effectiveStat) {
        float rate = calcElementSuccessRate(effectiveStat);
        return rate > 0f && NumberUtil.getRandom(1000) < (int) (rate * 1000f);
    }

    /**
     * Thời gian giữa 2 đòn (giây). bonus = successRate → đánh nhanh hơn (1 + bonus) lần.
     */
    public static float calcAttackInterval(float baseInterval, long attackSpeedStat) {
        float bonus = calcSuccessRate(getEffectiveStatForSuccess(attackSpeedStat));
        return baseInterval / (1f + bonus);
    }

    /**
     * % giảm sát thương phản đòn từ chỉ số Né (đường thẳng, không dùng success).
     * referenceEffective Né → counterReduceAtReference (25%), trần counterReduceMax (30%).
     */
    public static float calcCounterDamageReduction(long effectiveDodge) {
        if (effectiveDodge <= 0 || cfg.referenceEffective <= 0) {
            return 0f;
        }
        float rate = (effectiveDodge / cfg.referenceEffective) * cfg.counterReduceAtReference;
        return Math.min(cfg.counterReduceMax, rate);
    }

    public static int calcCounterDamageTaken(int baseCounterDmg, long effectiveDodge) {
        if (baseCounterDmg <= 0) {
            return 0;
        }
        float reduceRate = calcCounterDamageReduction(effectiveDodge);
        int dmg = (int) (baseCounterDmg * (1f - reduceRate));
        return Math.max(1, dmg);
    }

    /**
     * % hút máu từ sát thương gây ra (đường thẳng).
     * referenceEffective → lifeStealAtReference (40%), trần lifeStealMax (45%).
     */
    public static float calcLifeStealRate(long lifeStealStat) {
        long effective = getEffectiveStatForSuccess(lifeStealStat);
        if (effective <= 0 || cfg.referenceEffective <= 0) {
            return 0f;
        }
        float rate = (effective / cfg.referenceEffective) * cfg.lifeStealAtReference;
        return Math.min(cfg.lifeStealMax, rate);
    }

    public static int calcLifeStealHeal(long lifeStealStat, long damageDealt) {
        if (damageDealt <= 0) {
            return 0;
        }
        float rate = calcLifeStealRate(lifeStealStat);
        if (rate <= 0f) {
            return 0;
        }
        return (int) (damageDealt * rate);
    }

    /** Tỉ lệ proc Độc: success nguyên tố (600 → 72%). */
    public static boolean rollPoisonProc(long poisonStat) {
        return rollElementSuccess(getEffectiveStatForSuccess(poisonStat));
    }

    /** Tỉ lệ proc Lửa: success nguyên tố (600 → 72%). */
    public static boolean rollFireProc(long fireStat) {
        return rollElementSuccess(getEffectiveStatForSuccess(fireStat));
    }

    /** Sát thương Lửa mỗi giây = Lửa / 2 (trừ thẳng máu), tối thiểu 1. */
    public static int calcFireDamagePerTick(long fireStat) {
        long effective = getEffectiveStatForSuccess(fireStat);
        if (effective <= 0) {
            return 0;
        }
        return Math.max(1, (int) (effective / 2));
    }

    /** Thời gian Lửa: 10s + mỗi 100 Giáp target +1s. */
    public static int calcFireDurationSeconds(long targetDefense) {
        return calcPoisonDurationSeconds(targetDefense);
    }

    /** Damage độc mỗi giây theo % max HP: 150 → 1%, 300 → 2%, 450 → 3%. */
    public static float calcPoisonHpDamageRate(long poisonStat) {
        long effective = getEffectiveStatForSuccess(poisonStat);
        if (effective <= 0) {
            return 0f;
        }
        return effective / 150f * 0.01f;
    }

    /** Thời gian độc: tối thiểu 10s, mỗi 100 Giáp target +1s. */
    public static int calcPoisonDurationSeconds(long targetDefense) {
        long defense = Math.max(0, targetDefense);
        return 10 + (int) (defense / 100);
    }

    /** Giảm tốc chạy khi trúng độc: 450 → 30%, trần 35%. */
    public static float calcPoisonMoveSlowRate(long poisonStat) {
        long effective = getEffectiveStatForSuccess(poisonStat);
        if (effective <= 0 || cfg.referenceEffective <= 0) {
            return 0f;
        }
        float rate = (effective / cfg.referenceEffective) * cfg.poisonSlowAtReference;
        return Math.min(cfg.poisonSlowMax, rate);
    }

    public static int calcPoisonMoveSlowPercent(long poisonStat) {
        return Math.round(calcPoisonMoveSlowRate(poisonStat) * 100f);
    }

    public static float getPoisonSlowMax() {
        return cfg.poisonSlowMax;
    }

    /** Băng hiệu dụng sau khi trừ Giáp target 1:1. */
    public static long getEffectiveFreezeAfterDefense(long freezeStat, long targetDefense) {
        long freeze = Math.min(Math.max(freezeStat, 0), cfg.statCap);
        long defense = Math.min(Math.max(targetDefense, 0), cfg.statCap);
        return Math.max(0, freeze - defense);
    }

    /** Tỉ lệ proc Băng: success nguyên tố (600 → 72%). */
    public static boolean rollFreezeProc(long freezeStat) {
        return rollElementSuccess(getEffectiveStatForSuccess(freezeStat));
    }

    /** Thời gian đóng băng: 50 Băng = 1s sau trừ Giáp; tối thiểu 1s kể cả Giáp ≥ Băng. */
    public static int calcFreezeDurationSeconds(long freezeStat, long targetDefense) {
        if (freezeStat <= 0) {
            return 0;
        }
        long effective = getEffectiveFreezeAfterDefense(freezeStat, targetDefense);
        if (effective <= 0) {
            return 1;
        }
        return Math.max(1, (int) (effective / 50));
    }

    /** Giảm tốc đánh khi đóng băng: 450 Băng → 30%, trần 35%. */
    public static float calcFreezeAttackSlowRate(long freezeStat) {
        long effective = getEffectiveStatForSuccess(freezeStat);
        if (effective <= 0 || cfg.referenceEffective <= 0) {
            return 0f;
        }
        float rate = (effective / cfg.referenceEffective) * cfg.freezeSlowAtReference;
        return Math.min(cfg.freezeSlowMax, rate);
    }

    public static int calcFreezeAttackSlowPercent(long freezeStat) {
        return Math.round(calcFreezeAttackSlowRate(freezeStat) * 100f);
    }

    public static float getFreezeSlowMax() {
        return cfg.freezeSlowMax;
    }

    /** % giảm nguyên tố từ Kháng (đường thẳng): 600 Kháng → 50%, trần resistReduceMax. */
    public static float calcResistReduceRate(long resistStat) {
        long effective = getEffectiveStatForSuccess(resistStat);
        if (effective <= 0 || cfg.resistReferenceEffective <= 0) {
            return 0f;
        }
        float rate = (effective / cfg.resistReferenceEffective) * cfg.resistReduceAtReference;
        return Math.min(cfg.resistReduceMax, rate);
    }

    /** Chỉ số nguyên tố (Độc/Lửa/Gió/Băng) sau khi bị Kháng target trừ %. */
    public static long getEffectiveElementAfterResist(long elementStat, long resistStat) {
        if (elementStat <= 0) {
            return 0;
        }
        float reduce = calcResistReduceRate(resistStat);
        return Math.max(0, (long) (elementStat * (1f - reduce)));
    }

    /** Tỉ lệ proc Gió: success nguyên tố (600 → 72%). */
    public static boolean rollWindProc(long windStat) {
        return rollElementSuccess(getEffectiveStatForSuccess(windStat));
    }

    /** % giảm Né / Chính Xác khi Gió trúng = successRate nguyên tố (Gió). */
    public static float calcWindReduceRate(long windStat) {
        return calcElementSuccessRate(getEffectiveStatForSuccess(windStat));
    }

    /** Thời gian debuff Gió (giây). */
    public static int calcWindDurationSeconds() {
        return cfg.windDurationSeconds;
    }

    /**
     * Rate tăng drop (vàng/gem/item) theo phần nghìn: 600 điểm → 100 (= +10%).
     * Dùng: bonus = base * rate / 1000.
     */
    public static int calcDropIncreaseRate(long dropStat) {
        long effective = getEffectiveStatForSuccess(dropStat);
        if (effective <= 0 || cfg.dropReferenceEffective <= 0) {
            return 0;
        }
        int rate = (int) ((effective / cfg.dropReferenceEffective) * cfg.dropIncreaseAtReference);
        if (cfg.dropIncreaseMax > 0) {
            return Math.min(cfg.dropIncreaseMax, rate);
        }
        return rate;
    }

    public static float getLifeStealAtReference() {
        return cfg.lifeStealAtReference;
    }

    public static float getLifeStealMax() {
        return cfg.lifeStealMax;
    }

    /**
     * % max HP hồi mỗi tick: (min(stat, healEffectiveCap) / healEffectiveCap) × healPercentAtCap.
     * Tại 600 → 10%; scale tuyến tính phía dưới.
     */
    public static float calcHealPercent(long healingStat) {
        if (healingStat < cfg.healMinStat) {
            return 0f;
        }
        long effective = Math.min(healingStat, cfg.healEffectiveCap);
        if (effective <= 0 || cfg.healEffectiveCap <= 0 || cfg.healPercentAtCap <= 0) {
            return 0f;
        }
        return (effective / (float) cfg.healEffectiveCap) * cfg.healPercentAtCap;
    }

    /**
     * HP hồi mỗi tick (10s): floor(maxHp × calcHealPercent / 100).
     * Tại 600 → 10% max HP; dưới {@link #getHealMinStat()} hoặc maxHp ≤ 0 → 0.
     */
    public static int calcHealPerTick(long healingStat, long maxHp) {
        if (healingStat < cfg.healMinStat || maxHp <= 0) {
            return 0;
        }
        long effective = Math.min(healingStat, cfg.healEffectiveCap);
        if (cfg.healEffectiveCap <= 0 || cfg.healPercentAtCap <= 0) {
            return 0;
        }
        return (int) (maxHp * effective * cfg.healPercentAtCap / (cfg.healEffectiveCap * 100L));
    }

    /** Điểm Hồi máu tối thiểu để chạy regen (dưới mức này bỏ qua mỗi tick/hit). */
    public static int getHealMinStat() {
        return cfg.healMinStat;
    }

    /** Khoảng cách giữa 2 lần hồi máu theo chỉ số Hồi máu (giây). */
    public static int calcHealIntervalSeconds() {
        return cfg.healIntervalSeconds;
    }

    /** Mỗi lần bị đánh rút ngắn bao nhiêu giây chu kỳ hồi máu. */
    public static int calcHealAccelSecondsOnHit() {
        return Math.max(0, cfg.healAccelSecondsOnHit);
    }

    /**
     * % tỉ lệ phản đòn (đường thẳng).
     * referenceEffective → counterProcAtReference (60%), trần counterProcMax (65%).
     */
    public static float calcCounterProcRate(long counterStat) {
        long effective = getEffectiveStatForSuccess(counterStat);
        if (effective <= 0 || cfg.referenceEffective <= 0) {
            return 0f;
        }
        float rate = (effective / cfg.referenceEffective) * cfg.counterProcAtReference;
        return Math.min(cfg.counterProcMax, rate);
    }

    public static boolean rollCounterProc(long counterStat) {
        float rate = calcCounterProcRate(counterStat);
        return rate > 0f && NumberUtil.getRandom(1000) < (int) (rate * 1000f);
    }

    public static float getCounterProcAtReference() {
        return cfg.counterProcAtReference;
    }

    public static float getCounterProcMax() {
        return cfg.counterProcMax;
    }

    public static float getCounterReduceAtReference() {
        return cfg.counterReduceAtReference;
    }

    public static float getCounterReduceMax() {
        return cfg.counterReduceMax;
    }

    public static int getStatCap() {
        return cfg.statCap;
    }

    /** @deprecated Counter cũ. Chính Xác hiện dùng {@link #statCap} để còn dư xuyên Giáp. */
    public static int getCounterStatCap() {
        return counterCapFromReference();
    }

    public static float getSuccessMax() {
        return cfg.successMax;
    }

    public static float getSuccessAtReference() {
        return cfg.successAtReference;
    }

    public static float getReferenceEffective() {
        return cfg.referenceEffective;
    }

    private static int counterCapFromReference() {
        return Math.max(0, Math.round(cfg.referenceEffective));
    }

    private static float computeSuccessScale(float referenceEffective, float successMax, float successAtReference) {
        if (successMax <= successAtReference || referenceEffective <= 0) {
            return 280f;
        }
        return referenceEffective / (float) Math.log(successMax / (successMax - successAtReference));
    }

    private static void mergeConfig(DataConfig loaded) {
        if (loaded.successMax > 0) {
            cfg.successMax = loaded.successMax;
        }
        if (loaded.successAtReference > 0) {
            cfg.successAtReference = loaded.successAtReference;
        }
        if (loaded.referenceEffective > 0) {
            cfg.referenceEffective = loaded.referenceEffective;
        }
        if (loaded.elementReferenceEffective > 0) {
            cfg.elementReferenceEffective = loaded.elementReferenceEffective;
        }
        if (loaded.resistReferenceEffective > 0) {
            cfg.resistReferenceEffective = loaded.resistReferenceEffective;
        }
        if (loaded.statCap > 0) {
            cfg.statCap = loaded.statCap;
        } else if (loaded.dodgeStatCap > 0) {
            cfg.statCap = loaded.dodgeStatCap;
        } else if (loaded.critStatCap > 0) {
            cfg.statCap = loaded.critStatCap;
        }
        if (loaded.counterReduceAtReference > 0) {
            cfg.counterReduceAtReference = loaded.counterReduceAtReference;
        }
        if (loaded.counterReduceMax > 0) {
            cfg.counterReduceMax = loaded.counterReduceMax;
        }
        if (loaded.lifeStealAtReference > 0) {
            cfg.lifeStealAtReference = loaded.lifeStealAtReference;
        }
        if (loaded.lifeStealMax > 0) {
            cfg.lifeStealMax = loaded.lifeStealMax;
        }
        if (loaded.counterProcAtReference > 0) {
            cfg.counterProcAtReference = loaded.counterProcAtReference;
        }
        if (loaded.counterProcMax > 0) {
            cfg.counterProcMax = loaded.counterProcMax;
        }
        if (loaded.poisonSlowAtReference > 0) {
            cfg.poisonSlowAtReference = loaded.poisonSlowAtReference;
        }
        if (loaded.poisonSlowMax > 0) {
            cfg.poisonSlowMax = loaded.poisonSlowMax;
        }
        if (loaded.freezeSlowAtReference > 0) {
            cfg.freezeSlowAtReference = loaded.freezeSlowAtReference;
        }
        if (loaded.freezeSlowMax > 0) {
            cfg.freezeSlowMax = loaded.freezeSlowMax;
        }
        if (loaded.windDurationSeconds > 0) {
            cfg.windDurationSeconds = loaded.windDurationSeconds;
        }
        if (loaded.resistReduceAtReference > 0) {
            cfg.resistReduceAtReference = loaded.resistReduceAtReference;
        }
        if (loaded.resistReduceMax > 0) {
            cfg.resistReduceMax = loaded.resistReduceMax;
        }
        if (loaded.healEffectiveCap > 0) {
            cfg.healEffectiveCap = loaded.healEffectiveCap;
        }
        if (loaded.healPercentAtCap > 0) {
            cfg.healPercentAtCap = loaded.healPercentAtCap;
        }
        if (loaded.healMinStat > 0) {
            cfg.healMinStat = loaded.healMinStat;
        }
        if (loaded.healIntervalSeconds > 0) {
            cfg.healIntervalSeconds = loaded.healIntervalSeconds;
        }
        if (loaded.healAccelSecondsOnHit > 0) {
            cfg.healAccelSecondsOnHit = loaded.healAccelSecondsOnHit;
        }
        if (loaded.dropReferenceEffective > 0) {
            cfg.dropReferenceEffective = loaded.dropReferenceEffective;
        }
        if (loaded.dropIncreaseAtReference > 0) {
            cfg.dropIncreaseAtReference = loaded.dropIncreaseAtReference;
        }
        if (loaded.dropIncreaseMax > 0) {
            cfg.dropIncreaseMax = loaded.dropIncreaseMax;
        }
    }

    private static DataConfig defaultConfig() {
        DataConfig d = new DataConfig();
        d.successMax = 0.90f;
        d.successAtReference = 0.72f;
        d.referenceEffective = 450f;
        d.elementReferenceEffective = 600f;
        d.resistReferenceEffective = 600f;
        d.statCap = 800;
        d.counterReduceAtReference = 0.25f;
        d.counterReduceMax = 0.30f;
        d.lifeStealAtReference = 0.40f;
        d.lifeStealMax = 0.45f;
        d.counterProcAtReference = 0.60f;
        d.counterProcMax = 0.65f;
        d.poisonSlowAtReference = 0.30f;
        d.poisonSlowMax = 0.35f;
        d.freezeSlowAtReference = 0.30f;
        d.freezeSlowMax = 0.35f;
        d.windDurationSeconds = 10;
        d.resistReduceAtReference = 0.50f;
        d.resistReduceMax = 0.75f;
        d.healEffectiveCap = 600;
        d.healPercentAtCap = 10;
        d.healMinStat = 30;
        d.healIntervalSeconds = 10;
        d.healAccelSecondsOnHit = 1;
        d.dropReferenceEffective = 600f;
        d.dropIncreaseAtReference = 100;
        d.dropIncreaseMax = 0;
        return d;
    }

    @Data
    public static class DataConfig {
        /** Trần % (0.90 = 90%). */
        public float successMax = 0.90f;
        /** Tại referenceEffective thì đạt bao nhiêu % (0.72 = 72%). */
        public float successAtReference = 0.72f;
        /** Mốc thiết kế (450): D tại đây → successAtReference; cap Chính Xác trừ Né. */
        public float referenceEffective = 450f;
        /** Mốc proc nguyên tố (600): Độc/Lửa/Gió/Băng → successAtReference tại đây. */
        public float elementReferenceEffective = 600f;
        /** Mốc Kháng (600): tại đây → resistReduceAtReference. */
        public float resistReferenceEffective = 600f;
        /** Cap chỉ số thô dùng chung (Né, Crit, Giáp, Phản đòn...). */
        public int statCap = 800;
        /** Tại referenceEffective Né → giảm bao nhiêu % sát thương phản đòn (0.25 = 25%). */
        public float counterReduceAtReference = 0.25f;
        /** Trần % giảm sát thương phản đòn từ Né (0.30 = 30%). */
        public float counterReduceMax = 0.30f;
        /** Tại referenceEffective Hút Máu → hút bao nhiêu % sát thương gây ra (0.40 = 40%). */
        public float lifeStealAtReference = 0.40f;
        /** Trần % hút máu (0.45 = 45%). */
        public float lifeStealMax = 0.45f;
        /** Tại referenceEffective Phản Công → tỉ lệ proc (0.60 = 60%). */
        public float counterProcAtReference = 0.60f;
        /** Trần % tỉ lệ phản đòn (0.65 = 65%). */
        public float counterProcMax = 0.65f;
        /** Tại referenceEffective Độc → giảm tốc chạy (0.30 = 30%). */
        public float poisonSlowAtReference = 0.30f;
        /** Trần % giảm tốc chạy khi trúng độc (0.35 = 35%). */
        public float poisonSlowMax = 0.35f;
        /** Tại referenceEffective Băng → giảm tốc đánh (0.30 = 30%). */
        public float freezeSlowAtReference = 0.30f;
        /** Trần % giảm tốc đánh khi đóng băng (0.35 = 35%). */
        public float freezeSlowMax = 0.35f;
        /** Thời gian giảm Né / Chính Xác khi trúng Gió (giây). */
        public int windDurationSeconds = 10;
        /** Tại resistReferenceEffective Kháng → giảm bao nhiêu % chỉ số nguyên tố (0.50 = 50%). */
        public float resistReduceAtReference = 0.50f;
        /** Trần % giảm nguyên tố từ Kháng (0.75 = 75%). */
        public float resistReduceMax = 0.75f;
        /** Cap hiệu dụng Hồi máu (600 → healPercentAtCap % max HP / tick). */
        public int healEffectiveCap = 600;
        /** % max HP hồi mỗi tick tại healEffectiveCap (10 = 10%). */
        public int healPercentAtCap = 10;
        /** Điểm Hồi máu tối thiểu mới chạy regen (&lt; mức này bỏ qua mỗi tick/hit). */
        public int healMinStat = 30;
        /** Khoảng cách tick hồi máu (giây). */
        public int healIntervalSeconds = 10;
        /** Mỗi lần bị đánh rút ngắn chu kỳ hồi máu (giây). */
        public int healAccelSecondsOnHit = 1;
        /** Mốc drop (600): tại đây → dropIncreaseAtReference phần nghìn. */
        public float dropReferenceEffective = 600f;
        /** Tại dropReferenceEffective → tăng bao nhiêu phần nghìn (100 = +10%). */
        public int dropIncreaseAtReference = 100;
        /** Trần phần nghìn; 0 = không trần (chỉ theo statCap). */
        public int dropIncreaseMax = 0;

        /** @deprecated DB cũ — merge fallback sang {@link #statCap}. */
        public int dodgeStatCap;
        /** @deprecated DB cũ — merge fallback sang {@link #statCap}. */
        public int critStatCap;
    }
}
