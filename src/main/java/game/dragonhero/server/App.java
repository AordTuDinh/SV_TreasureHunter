package game.dragonhero.server;

import game.battle.object.MainJob;
import game.cache.JCache;
import game.cache.JCachePubSub;
import game.config.CfgServer;
import game.dragonhero.mapping.main.ConfigEntity;
import game.monitor.Telegram;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleTrigger;
import org.quartz.impl.StdSchedulerFactory;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.Filer;
import ozudo.base.helper.QuartzUtil;
import ozudo.base.log.Config;
import ozudo.base.log.Logs;
import ozudo.net.tcp.RequestDecoder;
import ozudo.net.tcp.TCPHandler;

import java.lang.reflect.Method;
import java.util.*;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class App {
    public static List<Integer> aCounter = Arrays.asList(30, 15, 5, 3, 1);
    //    public static Map<Integer, TurnCounter> mCounter = new HashMap<Integer, TurnCounter>();
    static JobKey oneMinute; // 1phut

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+07:00"));
        init();
        createMainJob();
        startNetwork();
    }

    static void createMainJob() {
        JobDetail job = newJob(MainJob.class).withIdentity("server_job_" + CfgServer.serverId, "all").build();
        SimpleTrigger trigger = newTrigger().withIdentity("sv_trigger_" + CfgServer.serverId, "all").
                startAt(Calendar.getInstance().getTime()).withSchedule(simpleSchedule().withIntervalInSeconds(3).repeatForever()).build();
        // Arena Job
//        JobDetail arenaJob = newJob(ArenaJob.class).withIdentity("arena_job_" + CfgServer.serverId, "all").build();
//        SimpleTrigger triggerArena = newTrigger().withIdentity("arena_trigger_" + CfgServer.serverId, "all").
//                startAt(Calendar.getInstance().getTime()).withSchedule(simpleSchedule().withIntervalInSeconds(3).repeatForever()).build();


        try {
            Scheduler scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);
           // scheduler.scheduleJob(arenaJob, triggerArena);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static void startNetwork() {
        System.out.println("*********************************************");
        System.out.println("************ Start " + CfgServer.serverType + " Server **************");
        System.out.println("************ Port " + CfgServer.runningPort + " **************");
        System.out.println("*********************************************");
        new Thread(() -> {
            {// Configure the server -> socket server
                try {
                    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                    EventLoopGroup workerGroup = new NioEventLoopGroup();
                    try {
                        ServerBootstrap b = new ServerBootstrap();
                        //b.option(ChannelOption.SO_SNDBUF, 2048);
                        b.option(ChannelOption.SO_RCVBUF, 32768);
                        //b.option(ChannelOption.SO_KEEPALIVE, true);
                        //b.option(ChannelOption.TCP_NODELAY, true);

                        //b.option(ChannelOption.SO_LINGER, 20000);
                        b.option(ChannelOption.SO_REUSEADDR, true);

                        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
                                //.handler(new LoggingHandler(LogLevel.INFO))
                                .childHandler(new ChannelInitializer<SocketChannel>() {
                                    @Override
                                    public void initChannel(SocketChannel ch) throws Exception {
                                        ChannelPipeline p = ch.pipeline();
                                        p.addLast("decoder", new RequestDecoder());
                                        p.addLast(new IdleStateHandler(180, 180, 0)); //readerIdleTime , writerIdleTime , allIdleTime
                                        p.addLast(new TCPHandler());
                                    }
                                });

                        // Start the server.
                        ChannelFuture f = b.bind(CfgServer.runningPort).sync();
                        // Wait until the server socket is closed.
                        f.channel().closeFuture().sync();
                    } finally {
                        // Shut down all event loops to terminate all threads.
                        bossGroup.shutdownGracefully();
                        workerGroup.shutdownGracefully();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }

    static void init() throws Exception {
        AppInit.initAll();
        initConfig();
        JCache.getInstance();
        JCachePubSub.getInstance().subscriberGameServer();
//        Telegram.sendNotify(String.format("------------- Server: [%s] start ------------", AppConfig.cfg.name));

    }

    public static void initConfig() throws Exception {
        List<ConfigEntity> listConfig = DBJPA.getList(CfgServer.getCfgTable(), ConfigEntity.class);
        String keyCell = CfgServer.isRealServer() ? "test:server_list" : "aord:server_list";
        ConfigEntity configServerList = (ConfigEntity) DBJPA.getUnique(CfgServer.DB_MAIN + "config_api", ConfigEntity.class, "k", keyCell);
        CfgServer.setServerList(configServerList.getV());
        listConfig.forEach(localConfig -> setConfig(localConfig.getK(), localConfig.getV()));
        loadResource();
    }

    public static void reloadConfig() throws Exception {
        List<ConfigEntity> listConfig = DBJPA.getList(CfgServer.getCfgTable(), ConfigEntity.class);
        listConfig.forEach(localConfig -> setConfig(localConfig.getK(), localConfig.getV()));
    }

    static void loadResource() throws Exception {
        Class[] allClasses = Filer.getClasses("game.dragonhero.service.resource");
        for (Class allClass : allClasses) {
            if (allClass.getName().contains("$")) continue;
            try {
                allClass.getDeclaredMethod("init").invoke(null);
            } catch (Exception ex) {
                Logs.error(ex);
            }
        }
        for (Class allClass : allClasses) {
            try {
                allClass.getDeclaredMethod("lazyInit").invoke(null);
            } catch (NoSuchMethodException ex) {
            } catch (Exception ex) {
                Logs.error(ex);
            }
        }
    }

    public static void setConfig(String key, String value) {
        try {
            if (key.startsWith("config")) {
                if (key.contains(":")) key = key.substring(key.indexOf(":") + 1);
                String cfg = key;
                cfg = cfg.substring(cfg.indexOf("_") + 1);
                cfg = cfg.substring(0, 1).toUpperCase() + cfg.substring(1);
                try {
                    Logs.info("------> Init " + "config.Cfg" + cfg);
                    Method m = Class.forName("game.config.Cfg" + cfg).getDeclaredMethod("loadConfig", String.class);
                    m.invoke(null, value);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                Config.setString(key, value);
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }
}
