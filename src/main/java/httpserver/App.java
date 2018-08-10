package httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Hello world!
 */

public class App {
  static Logger logger = Logger.getAnonymousLogger();
  static String APPNAME = "http-server";
  static int defaultPort = 8080;
  static int maxThreads = 10;

  public static void main(String args[]) {
    if (args != null && args.length > 0) {
      try {
        defaultPort = Integer.parseInt(args[0]);
      }
      catch (NumberFormatException e) {
        System.err.println("Argument" + args[0] + " must be an integer.");
        System.exit(1);
      }
    }
    logger.info(APPNAME + "starting on port: " + defaultPort);
    
   
    run();
  }

  static void run() {
    ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);    
    new Thread() {
      public void run() {
        ServerSocket ss = null;
        try {
          ss = new ServerSocket(defaultPort);
          for (;;) {
            Socket client = ss.accept();
            logger.info(client.toString());
//            System.out.println("alive: " + client.getKeepAlive());
            client.setKeepAlive(true);

            executorService.execute(new Runnable() {
              public void run() {
                logger.info("Servicing on thread: [" + Thread.currentThread() + "]");
                Processor p = new Processor(client);
                try {
                  p.process();
                }
                catch (IOException e) {
                 logger.severe(e.getMessage());
                }
                logger.info("Completed on thread: [" + Thread.currentThread() + "]");
              }
            });
          }
        }
        // If anything goes wrong, print an error message
        catch (Exception e) {
          logger.severe(e.getMessage());
        }
      }
    }.start();
    logger.info(APPNAME + " started.");
  }
}
