package game.dragonhero.service;

import game.dragonhero.dao.ClanDAO;
import game.dragonhero.dao.UserDAO;
import game.dragonhero.dao.UserMailDAO;

public class Services {
    public static UserMailDAO mailDAO = new UserMailDAO();
    public static UserDAO userDAO = new UserDAO();
    public static UserService userService = new UserService();
    public static ClanDAO clanDAO = new ClanDAO();
}
