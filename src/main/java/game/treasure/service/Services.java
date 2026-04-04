package game.treasure.service;

import game.treasure.dao.ClanDAO;
import game.treasure.dao.UserDAO;
import game.treasure.dao.UserMailDAO;

public class Services {
    public static UserMailDAO mailDAO = new UserMailDAO();
    public static UserDAO userDAO = new UserDAO();
    public static UserService userService = new UserService();
    public static ClanDAO clanDAO = new ClanDAO();
}
