package game.cache;

import game.dragonhero.server.service.aop.LogExecutionTime;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class IRedis {

    public final static int EXPIRE_5S = 5;
    public final static int EXPIRE_1M = 60;
    public final static int EXPIRE_1H = 60 * 60;
    public final static int EXPIRE_1D = 60 * 60 * 24;

    protected String PREFIX = "";
    protected String host;
    protected JedisPool pool;

    public JedisPool getPool() {
        return pool;
    }

    public void returnResource(Jedis jedis) {
        try {
            if (jedis != null) {
                jedis.close();
            }
        } catch (Exception ex) {
        }
    }

    //region basic command
    @LogExecutionTime
    public Long decrBy(String key, long value) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.decrBy(key, value);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public Long incrBy(String key, long value) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.incrBy(key, value);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.exists(key);
        } catch (Exception ex) {
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public void expire(String key, long seconds) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            jedis.expire(key, seconds);
        } catch (Exception ex) {
        } finally {
            returnResource(jedis);
        }
    }

    @LogExecutionTime
    public Long setNX(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            long result = jedis.setnx(key, value);
            if (result > 0) jedis.expire(key, EXPIRE_1H);
            return result;
        } catch (Exception ex) {
        } finally {
            returnResource(jedis);
        }
        return 0L;
    }

    @LogExecutionTime
    public String getSet(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.getSet(key, value);
        } catch (Exception ex) {
        } finally {
            returnResource(jedis);
        }
        return "";
    }

    @LogExecutionTime
    public Integer getIntValue(String key) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return Integer.parseInt(jedis.get(key));
        } catch (Exception ex) {
            //ex.printStackTrace();
            Logs.error("getIntValue=" + Util.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public Long getLongValue(String key) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            String value = jedis.get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception ex) {
            // ex.printStackTrace();
            Logs.error("getLongValue=" + Util.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public String getValue(String key) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.get(key);
        } catch (Exception ex) {
            Logs.error("getValue=" + Util.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean setValue(String key, String value) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            String tmp = jedis.set(key, value);
            if (!tmp.equalsIgnoreCase("ok")) {
                Logs.warn("setValue -> " + key + " -> " + value);
            }
            jedis.expire(key, EXPIRE_1D);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            Logs.error("setValue=" + GUtil.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public boolean setByteValue(String key, byte[] value) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            String tmp = jedis.set(key.getBytes(), value);
            if (!tmp.equalsIgnoreCase("ok")) {
                Logs.warn("setValue -> " + key + " -> " + value);
            }
            jedis.expire(key, EXPIRE_1D);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            Logs.error("setValue=" + GUtil.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public boolean setByteValue(String key, byte[] value, int expireTime) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            String tmp = jedis.set(key.getBytes(), value);
            if (!tmp.equalsIgnoreCase("ok")) {
                Logs.warn("setValue -> " + key + " -> " + value);
            }
            jedis.expire(key, expireTime);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            Logs.error("setValue=" + GUtil.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public byte[] getByteValue(String key) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            return jedis.get(key.getBytes());
        } catch (Exception ex) {
            //ex.printStackTrace();
            // Logs.error("getValue=" + Util.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean setValue(String key, String value, int expireTimeSec) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            String tmp = jedis.setex(key, expireTimeSec, value);
            if (!tmp.equalsIgnoreCase("ok")) {
                Logs.warn("setValue -> " + key + " -> " + value);
            }
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
            Logs.error("setValue=" + GUtil.exToString(ex));
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public boolean removeValue(String key) {
        key = PREFIX + key;
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            jedis.del(key);
            return true;
        } catch (Exception ex) {
            //ex.printStackTrace();
        } finally {
            returnResource(jedis);
        }
        return false;
    }
    //endregion

    //region pub sub
    @LogExecutionTime
    public Long publish(String channel, String message) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.publish(channel, message);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return -1L;
    }

    public void subscriberToChannel(String channel, JedisPubSub pubSub) {
        new Thread(() -> {
            while (true) {
                try {
                    System.out.println(String.format("Subscribing to \"%s\"", channel));
                    Jedis jedis = pool.getResource();
                    jedis.subscribe(pubSub, channel);
                    jedis.close();
                    Logs.info("Subscription ended.");
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    Logs.error(GUtil.exToString(ex));
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        Logs.error(e);
                    }
                }
            }
        }).start();
    }
    //endregion

    //region List
    @LogExecutionTime
    public String lpop(String key) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.lpop(key);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean rpush(String key, String value) {
        Jedis jedis = pool.getResource();
        try {
            jedis.rpush(key, value);
            return true;
        } catch (Exception ex) {
            Logs.error(String.format("%s %s", key, value), ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public List<String> lrange(String key, int start, int end) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.lrange(key, start, end);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return new ArrayList<>();
    }

    @LogExecutionTime
    public String ltrim(String key, int start, int end) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.ltrim(key, start, end);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public void ltrim(String key, int number) {
        Jedis jedis = pool.getResource();
        try {
            jedis.ltrim(key, -number, -1);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
    }
    //endregion

    //region Hashes
    @LogExecutionTime
    public Map<String, String> hgetAll(String key) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.hgetAll(key);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean hset(String key, Map<String, String> map, int... expired) {
        Jedis jedis = pool.getResource();
        try {
            jedis.hset(key, map);
            if (expired.length > 0) {
                jedis.expire(key, expired[0]);
            }
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public long hset(String key, String field, String value) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.hset(key, field, value);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return -1;
    }

    @LogExecutionTime
    public String hget(String key, String field) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.hget(key, field);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }
    //endregion

    //region Sorted Set
    @LogExecutionTime
    public Double zscore(String key, String member) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zscore(key, member);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return Double.valueOf(-1000);
    }

    @LogExecutionTime
    public List<Double> zmscore(String key, String... members) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zmscore(key, members);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public List<Tuple> zpopmin(String key, int number) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zpopmin(key, number);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    @LogExecutionTime
    public boolean zadd(String key, double value, String member) {
        Jedis jedis = pool.getResource();
        try {
            jedis.zadd(key, value, member);
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public boolean zadd(String key, Map<String, Double> scoreMembers) {
        Jedis jedis = pool.getResource();
        try {
            jedis.zadd(key, scoreMembers);
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public boolean zincrby(String key, Map<String, Double> scoreMembers) {
        Jedis jedis = pool.getResource();
        try {
            scoreMembers.forEach((member, score) -> jedis.zincrby(key, score, member));
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }

    @LogExecutionTime
    public Double zincrby(String key, String member, double score) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zincrby(key, score, member);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    /**
     * rank from high to low
     *
     * @param key
     * @param member
     * @return
     */
    @LogExecutionTime
    public Long zrevrank(String key, String member) {
        Jedis jedis = pool.getResource();
        try {
            //return jedis.zrank(key, member);
            return jedis.zrevrank(key, member);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return -1L;
    }

    /**
     * range element from high to low
     *
     * @param key
     * @return
     */
    @LogExecutionTime
    public List<String> zrevrange(String key, int start, int stop) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zrevrange(key, start, stop).stream().toList();
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    /**
     * range element from high to low
     *
     * @param key
     * @return
     */
    @LogExecutionTime
    public List<Tuple> zrevrangeWithScores(String key, int start, int stop) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.zrevrangeWithScores(key, start, stop);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }
    //endregion

    //region Sets

    /**
     * Lấy n random phần tử từ Set
     *
     * @param key
     * @param number -> default = 1
     * @return
     */
    @LogExecutionTime
    public Set<String> spop(String key, int... number) {
        Jedis jedis = pool.getResource();
        try {
            int numberPop = number.length == 0 ? 1 : number[0];
            return jedis.spop(key, numberPop);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return null;
    }

    /**
     * @param key
     * @param values
     * @return 1 add ok, 0 already exist, -1 fail
     */
    @LogExecutionTime
    public Long sadd(String key, String... values) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.sadd(key, values);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return -1L;
    }

    @LogExecutionTime
    public Long append(String key, String values) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.append(key, values);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return -1L;
    }

    /**
     * @param key
     * @param value
     * @return true if value is member of set `key`
     */
    @LogExecutionTime
    public boolean sismember(String key, String value) {
        Jedis jedis = pool.getResource();
        try {
            return jedis.sismember(key, value);
        } catch (Exception ex) {
            Logs.error(ex);
        } finally {
            returnResource(jedis);
        }
        return false;
    }
    //endregion
}
