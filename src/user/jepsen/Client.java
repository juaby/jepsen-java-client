package user.jepsen;

import java.util.*;
import java.io.*;
import java.lang.*;

public class Client {
    public static Object setUpDatabase(Object test, Object node) { System.out.println("Setup DB"); return null; }
    public static void teardownDatabase(Object test, Object node) { System.out.println("Torndown Db"); }
    public static void teardownClient(Object args) { System.out.println("Torndown client"); }
    public static boolean invokeClient(Object args) { System.out.println("Invoked client op"); return true; }
    public static void openClient(Object test, Object node) { System.out.println("Have opened client"); }
}