package game.battle.object;

import game.cache.JCache;
import game.config.CfgServer;
import game.monitor.Online;
import io.netty.channel.Channel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        // tạm comment khi chạy không Redis
        // List<Integer> serverNum = new ArrayList<>();
        // for (Map.Entry<Integer, List<Channel>> data : Online.userServer.entrySet()) {
        //     serverNum.add(data.getKey());
        //     int count = 0;
        //     List<Channel> lst  = data.getValue();
        //     for (int i = 0; i < lst.size(); i++) {
        //         if(lst.get(i).isActive()) count++;
        //     }
        //     serverNum.add(count);
        // }
        // if (!serverNum.isEmpty()) {
        //     JCache.getInstance().setValue(CfgServer.getKeyCCU(), StringHelper.toDBString(serverNum));
        // }
    }
}
