package game.cache;

import game.config.CfgServer;
import game.config.lang.Lang;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import game.monitor.Telegram;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.DBHelper;
import ozudo.base.helper.GUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class GenGiftCodeJob {

    public static void main(String args[]) throws Exception {
        new GenGiftCodeJob().process();
        System.exit(0);
    }

    private void process() {
        try {
           //AppInit.initAll();
           // App.initConfig();
            processGiftCode();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }

    static SecureRandom rd = new SecureRandom();
    static String gen() {
        String a = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < 8; i++) s.append(a.charAt(rd.nextInt(a.length())));
        return s.toString();
    }

    void processGiftCode(){
        int COUNT = 100; // số lượng giftcode muốn tạo

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO dson_main.res_gift_code (id, name, type, data, bonus, time_end) VALUES\n");
        List<String> gift=  new ArrayList<>();
        for(int i = 0; i < COUNT; i++){
            String c = gen();
            gift.add(c);
            sql.append("('").append(c)
                    .append("', '").append(org.apache.commons.lang.StringEscapeUtils.escapeSql(Lang.getTitle(CfgServer.config.mainLanguage, Lang.mail_gift_loan_tin))).append("', 1, '', ")
                    .append("'[15,10,6,1,100,2,300,6,87,10,6,26,5,6,75,3,6,115,2]', '2030-05-09 16:04:35')");
            if(i < COUNT - 1) sql.append(",\n");
            else sql.append(";\n");
        }

        System.out.println(sql);
        System.out.println("-------------- DONE --------------");
        System.out.println("gift = " + gift);
    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }
    
}
