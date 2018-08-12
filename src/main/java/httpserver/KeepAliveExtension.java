package httpserver;

import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

/**
 * Performs basic support for keep-alive functionality, by looking for the enablement of the feature,
 * and looking for the associated options for tuning the max number of requests or idle connection timeout.
 * @author kmatassa
 */
public class KeepAliveExtension {
  /**
   * Keep-alive header value.
   */
  private static final String KEEP_ALIVE_TIMEOUT = "timeout";
  /**
   * Keep-alive header value.
   */
  private static final String KEEP_ALIVE_MAX = "max";
  /**
   * Connection keep-alive value.
   */
  private static final String KEEP_ALIVE_VALUE = "keep-alive";
  /**
   * Connection header name.
   */
  private static final String HDR_CONNECTION = "Connection";
  /**
   * Keep-alive header name.
   */
  private static final String HDR_KEEP_ALIVE = "Keep-Alive";
  /**
   * Millisecs.
   */
  private static final int MS = 1000;
  /**
   * Max seconds for keep-alive.
   */
  private static final int MAX_SECS = 10;
  /**
   * Contains the parsed http request in a usable format.
   */
  private RequestParser request = null;
  /**
   * Local logger.
   */
  private Logger logger = Logger.getAnonymousLogger();
  /**
   * This feature is supported.
   */
  private boolean keepAliveSupported = true;
  /**
   * This feature is enabled.
   */
  private boolean keepAliveEnabled = true;

  /**
   * @param parser Holds the parsed request.
   */
  KeepAliveExtension(final RequestParser parser) {
    this.setRequest(parser);
  }

  /**
   * Processes the details of this feature. It is given parameters that are relevant to this feature.
   * @param client The client socket, which may require tuning in the context of this feature.
   * @param numSocketRequests The number of requests made on this socket.
   * @throws SocketException sometimes.
   */
  public final void processKeepAliveOptions(final Socket client, final int numSocketRequests) throws SocketException {
    // NOTE: I have arbitrarily designated that HTTP version 1.0 doesn't support keep-alive for this example.
    if (getRequest().isHttpVersionOneO()) {
      this.setKeepAliveEnabled(false);
      this.setKeepAliveSupported(false);
    } else {
      this.setKeepAliveSupported(true);
      setKeepAliveEnabled(hasRequestedKeepAlive());
      // Further conditions to disable keep-alive is based on # requests, or timeout duration of idle.
      if (isKeepAliveEnabled()) {
        if (keepAliveMaxRequests() > 0 && numSocketRequests > keepAliveMaxRequests()) {
          setKeepAliveEnabled(false);
        } else {
          int keepAliveTimeout = keepAliveTimeout();
          // Single digit only
          if (keepAliveTimeout > 0) {
            if (keepAliveTimeout < MAX_SECS) {
              // Set to milliseconds.
              int to = keepAliveTimeout * MS;
              // Only if changed.
              if (client.getSoTimeout() != to) {
                client.setSoTimeout(keepAliveTimeout * MS);
              }
            } else {
              logger.warning("bad keep-alive timeout, skipping");
            }
          }
        }
      }
    }
  }

  /**
   * @return true if there's a valid request for keep-alive.
   */
  private boolean hasRequestedKeepAlive() {
    if (getRequest().getHeaders().containsKey(HDR_CONNECTION)) {
      return getRequest().getHeaders().get(HDR_CONNECTION).equalsIgnoreCase(KEEP_ALIVE_VALUE);
    }
    return false;
  }

  /**
   * @return int value of max number of requests to be processed in the same connection kept alive.
   */
  private int keepAliveMaxRequests() {
    return parseKeepAliveHeader(KEEP_ALIVE_MAX);
  }

  /**
   * @return int value in seconds of the maximum time an idle connection can live with keep-alive.
   */
  private int keepAliveTimeout() {
    return parseKeepAliveHeader(KEEP_ALIVE_TIMEOUT);
  }

  /**
   * Parses parameters relevant to Keep-Alive header.
   * @param attr is the attribute name
   * @return requested attribute value
   */
  private int parseKeepAliveHeader(final String attr) {
    String h = getRequest().getHeaders().get(HDR_KEEP_ALIVE);
    if (h != null) {
      String[] parts = h.split("=");
      if (parts[0].equalsIgnoreCase(attr)) {
        return Integer.parseInt(parts[1]);
      }
    }
    return -1;
  }

  /**
   * @return true if enabled.
   */
  public final boolean isKeepAliveEnabled() {
    return keepAliveEnabled;
  }

  /**
   * @return true if supported.
   */
  public final boolean isKeepAliveSupported() {
    return keepAliveSupported;
  }

  /**
   * @param keepAliveEnabledVal to be set
   */
  private void setKeepAliveEnabled(final boolean keepAliveEnabledVal) {
    this.keepAliveEnabled = keepAliveEnabledVal;
  }

  /**
   * @param keepAliveSupportedVal to be set
   */
  private void setKeepAliveSupported(final boolean keepAliveSupportedVal) {
    this.keepAliveSupported = keepAliveSupportedVal;
  }

  /**
   * @return RquestParser object.
   */
  private RequestParser getRequest() {
    return request;
  }

  /**
   * @param requestObj to be set.
   */
  private void setRequest(final RequestParser requestObj) {
    this.request = requestObj;
  }
}
