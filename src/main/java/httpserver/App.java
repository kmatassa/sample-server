package httpserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * A http-server sample.
 * @author kmatassa
 */
public class App {
  /**
   * Local logger.
   */
  private static Logger logger = Logger.getAnonymousLogger();
  /**
   * Name of this server.
   */
  private static String appName = "http-server";
  /**
   * Default port.
   */
  private static final int DEFAULTPORT = 8080;
  /**
   * Used port.
   */
  private static int usePort;
  /**
   * Default concurrency for this simple case.
   */
  private static final int MAXTHREADS = 10;

  /**
   * Main entry point.
   * @param args can contain an optional alternate port.  8080 is default.  Tests pass 8081.
   */
  public static void main(final String[] args) {
    if (args != null && args.length > 0) {
      try {
        usePort = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Argument" + args[0] + " must be an integer.");
        System.exit(1);
      }
    } else {
      usePort = DEFAULTPORT;
    }
    logger.info(appName + "starting on port: " + usePort);
    run();
  }

  /**
   * Run starts a new thread in which all incoming socket interactions are processed.
   * This allows the unit tests to execute after starting http-server.
   */
  private static void run() {
    ExecutorService executorService = Executors.newFixedThreadPool(MAXTHREADS);
    new Thread() {
      public void run() {
        ServerSocket ss = null;
        try {
          ss = new ServerSocket(usePort);
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
                } catch (IOException e) {
                 logger.severe(e.getMessage());
                }
                logger.info("Completed on thread: [" + Thread.currentThread() + "]");
              }
            });
          }
        } catch (Exception e) {
          logger.severe(e.getMessage());
        }
      }
    }.start();
    logger.info(appName + " started.");
  }
}
