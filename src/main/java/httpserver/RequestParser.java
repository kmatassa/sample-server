package httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicLineParser;

/**
 * RequestParse holds the parsed Http Request in a usable format.
 * @author kmatassa
 */
public class RequestParser {
  /**
   * Http method.
   */
  private String method;
  /**
   * Http uri.
   */
  private String uri;
  /**
   * Http protocol version.
   */
  private String version;
  /**
   * Map of Http headers.
   */
  private Map<String, String> headers = new ConcurrentHashMap<String, String>();

  /**
   * Parses the incoming request into this object for easier handling.
   * @param in BufferedReader holding request content
   * @return String containing the method received in the request.
   * @throws IOException sometimes
   */
  public final String parse(final BufferedReader in) throws IOException {
    // Note that the readLine() method works across platforms.
    readRequestLine(in);
    readRequestHeaders(in);
    return getMethod();
  }

  /**
   * Parses: "Method SP Request-URI SP HTTP-Version". Steals BasicLineParser.
   * @param in BufferedReader holding request content
   * @throws IOException sometimes
   * @see https://hc.apache.org/httpcomponents-core-ga/httpcore
   */
  private void readRequestLine(final BufferedReader in) throws IOException {
    String line = in.readLine();
    RequestLine p = BasicLineParser.parseRequestLine(line, new BasicLineParser());
    this.setMethod(p.getMethod());
    this.setUri(p.getUri());
    this.setVersion(p.getProtocolVersion().toString());
  }

  /**
   * Parses headers. Steals BasicLineParser.
   * @param in BufferedREader holding request content
   * @throws IOException sometimes
   * @see https://hc.apache.org/httpcomponents-core-ga/httpcore
   */
  private void readRequestHeaders(final BufferedReader in) throws IOException {
    String line;
    while ((line = in.readLine()) != null) {
      if (line.length() == 0) {
        break;
      }
      Header p = BasicLineParser.parseHeader(line, new BasicLineParser());
      this.getHeaders().put(p.getName(), p.getValue());
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public final String toString() {
    return this.getMethod() + " " + this.getUri() + " " + this.getVersion() + " " + this.getHeaders().toString();
  }

  /**
   * @return String containing request method.
   */
  public final String getMethod() {
    return method;
  }

  /**
   * @param methodValue Sets the request method.
   */
  public final void setMethod(final String methodValue) {
    this.method = methodValue;
  }

  /**
   * @return String containing the uri.
   */
  public final String getUri() {
    return uri;
  }

  /**
   * @param uriValue Sets the uri.
   */
  public final void setUri(final String uriValue) {
    this.uri = uriValue;
  }

  /**
   * @return String containing the version.
   */
  public final String getVersion() {
    return version;
  }

  /**
   * @param versionValue Sets the version.
   */
  public final void setVersion(final String versionValue) {
    this.version = versionValue;
  }

  /**
   * @return Map containg the headers.
   */
  public final Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * @return true if the current http request is version 1.0
   */
  public final boolean isHttpVersionOneO() {
    return getVersion().contains("1.0");
  }

  /**
   * @param headersValue Sets the headers.
   */
  public final void setHeaders(final Map<String, String> headersValue) {
    this.headers = headersValue;
  }
}
