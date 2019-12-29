package exe;

import java.io.*;
import java.util.Random;
import clojure.lang.ArraySeq;
import user.jepsen.JepsenExecutable;
import user.jepsen.Client;

public class JepsenMain {
    public static class NoopClient implements Client {
        public NoopClient() {}
 
        public Object setUpDatabase(Object test, String node) { System.out.println("Setup DB"); return null; }

        public void teardownDatabase(Object test, String node) { System.out.println("Torndown DB"); }

        public void teardownClient(Object args) { System.out.println("Torndown client"); }

        public Object invokeClient(Object args, String opName, Object inputValue) { System.out.println("Invoked client op " + opName + " with input value "  + inputValue); return true; }

        public void openClient(Object test, String node) { System.out.println("Have opened client"); }

        public String getNemesis() { return "partition-random-halves"; }

        public String generateOp() { return "dummyOp"; }

        public Object getValue(String opName) { return (new Random()).nextInt(100); }
    }

    public static void main(String[] args) {
    	JepsenExecutable exec = new JepsenExecutable("Richards-Macbook-Air-2.local", "richardyu", "jinjin123", 10, new NoopClient());
        exec.launchTest();
    }
}