package httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Header;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicLineParser;

public class RequestParser {
  public String method;
  public String uri;
  public String version;
  public Map<String, String> headers = new ConcurrentHashMap<String, String>();

  public String parse(Socket client) throws IOException {
    // Get input and output streams to talk to the client
    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    // Note that the readLine() method works across platforms.
    readRequestLine(in);
    readRequestHeaders(in);
    return method;
  }

  /**
   * Parses: "Method SP Request-URI SP HTTP-Version". Steals BasicLineParser.
   * 
   * @param in
   * @throws IOException
   * @see https://hc.apache.org/httpcomponents-core-ga/httpcore
   */
  protected void readRequestLine(BufferedReader in) throws IOException {
    String line = in.readLine();
    RequestLine p = BasicLineParser.parseRequestLine(line, new BasicLineParser());
    this.method = p.getMethod();
    this.uri = p.getUri();
    this.version = p.getProtocolVersion().toString();
  }

  /**
   * Parses headers. Steals BasicLineParser.
   * 
   * @param in
   * @throws IOException
   * @see https://hc.apache.org/httpcomponents-core-ga/httpcore
   */
  protected void readRequestHeaders(BufferedReader in) throws IOException {
    String line;
    while ((line = in.readLine()) != null) {
      if (line.length() == 0)
        break;
      Header p = BasicLineParser.parseHeader(line, new BasicLineParser());
      this.headers.put(p.getName(), p.getValue());
    }
  }

  @Override
  public String toString() {
    return this.method + " " + this.uri + " " + this.version + " " + this.headers.toString();
  }
}
