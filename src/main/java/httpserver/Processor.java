package httpserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;

/**
 * Processor reads HTTP requests and where possible delivers file-based content.  It supports GET and HEAD requests.  It supports
 * Keep-Alive as well, and thus multiple HTTP requests may be serviced continuously on the same socket.
 * @author kmatassa
 */
public class Processor {
  /**
   * CRLF.
   */
  private final String crLf = "\r\n";
  /**
   * Local logger.
   */
  private Logger logger = Logger.getAnonymousLogger();
  /**
   * Socket connection.
   */
  private Socket client = null;

  /**
   * @param clientSocket is the socket connection to process.
   */
  Processor(final Socket clientSocket) {
    this.client = clientSocket;
  }

  /**
   * Processor accepts GET requests and delivers results.
   * @throws IOException sometimes
   */
  public final void process() throws IOException {
    // Get input and output streams.
    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    OutputStream out = client.getOutputStream();
    boolean noErrors = true;
    boolean keepAliveEnabled = true;
    int numSocketRequests = 1;
    // Loop on this inputStream for either 1 or more requests depending on keep-alive extension.
    do {
      RequestParser request = new RequestParser();
      try {
        // May block if we are in keep-alive
        request.parse(in);
        if (request.hasValidHeaders()) {
          logger.info("http-server request: " + request);
          // Create the extension
          KeepAliveExtension keepAlive = new KeepAliveExtension(request);
          // Process the extension in context with the current http request.
          keepAlive.processKeepAliveOptions(client, numSocketRequests);
          keepAliveEnabled = keepAlive.isKeepAliveEnabled();

          logger.info("http-server keep-alive mode: " + keepAliveEnabled
                      + " number of requests on this socket: " + numSocketRequests++);
          // Minimum methods
          if (request.getMethod().equals(HttpGet.METHOD_NAME) || request.getMethod().equals(HttpHead.METHOD_NAME)) {
            try {
              deliverAFile(out, request, keepAlive);
            } catch (FileNotFoundException e) {
              deliverAnIssue(out, request, HttpStatus.SC_NOT_FOUND);
              noErrors = false;
            } catch (Exception e) {
              deliverAnIssue(out, request, HttpStatus.SC_BAD_REQUEST);
              noErrors = false;
            }
          } else {
            deliverAnIssue(out, request, HttpStatus.SC_NOT_IMPLEMENTED);
            noErrors = false;
          }
        } else {
          deliverAnIssue(out, request, HttpStatus.SC_BAD_REQUEST);
          noErrors = false;
        }
      } catch (SocketTimeoutException e) {
        // Keep-alive idle connection timeout.
        noErrors = false;
        logger.info("socket timed-out, closing...");
      } catch (Exception e) {
        deliverAnIssue(out, request, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        noErrors = false;
      } finally {
        out.flush();
      }
    } while (noErrors && keepAliveEnabled);
    // Falling out of continuous processing either due to an error, or keep-alive is disabled, maxed, or timed-out.
    // Force close the socket as a simplified behaviour.
    client.close();
  }

  /**
   * Delivers an unexpected HTTP code back to the client.
   * @param out is the OutputStream
   * @param request is the RequestParser content
   * @param issue is the Http status code to return
   * @throws IOException sometimes
   */
  private void deliverAnIssue(final OutputStream out, final RequestParser request, final int issue) throws IOException {
    String txt = String.format("%s %d%s", request.getVersion(), issue, crLf);
    out.write(txt.getBytes());
  }

  /**
   * Delivers the request URL if possible.
   * @param out is the OutputStream
   * @param request is the RequestParser content
   * @param keepAlive is the extension that affects the connection response value.
   * @throws IOException sometimes
   */
  private void deliverAFile(final OutputStream out, final RequestParser request, final KeepAliveExtension keepAlive)
      throws IOException {
    File source = this.getFile(request.getUri());
    if (source.exists() && source.isFile()) {
      // Rely on activation library to determine the content type.
      String contentType = new MimetypesFileTypeMap().getContentType(source);
      long contentLength = source.length();
      String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
      String connection = "Connection: close" + crLf;
      if (keepAlive.isKeepAliveSupported()) {
         if (keepAlive.isKeepAliveEnabled()) {
           connection = "Connection: keep-alive" + crLf;
         }
      }
      logger.info("content-type [" + contentType + "]");
      logger.info("content-length [" + contentLength + "]");
      logger.info("Date: [" + date + "]");

      String txt = String.format("%s %d %sContent-Type: %s%sContent-Length: %d%s%sDate: %s%s%s", request.getVersion(),
                                 HttpStatus.SC_OK, crLf,
                                 contentType, crLf,
                                 contentLength, crLf,
                                 connection,
                                 date, crLf,
                                 crLf);
      try {
        out.write(txt.getBytes());
        // Only deliver content for GET.
        if (request.getMethod().equals(HttpGet.METHOD_NAME)) {
          FileUtils.copyFile(source, out);
        }
      } catch (Exception e) {
        logger.severe(e.getMessage());
        throw e;
      }
    } else {
      throw new FileNotFoundException();
    }
  }

  /**
   * Crufts up a file containing the path to the resource, to be delivered as content.
   * @param uri is the basis of where a resource may be.
   * @return File containing the resource
   * @throws IOException sometimes
   */
  private File getFile(final String uri) throws IOException {
    File path = FileSystems.getDefault().getPath("").toAbsolutePath().toFile();
    String httpServerRoot = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "files";
    File dir = FileUtils.getFile(path, httpServerRoot);
    URI u = URI.create(uri);

    // Grab the path component only
    File resourceFileRequested = FileUtils.getFile(dir, u.getPath());
    // Security check for requested resource winding up above httpServerRoot.
    if (!resourceFileRequested.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
      logger.warning("Suspicious request, skipping: " + resourceFileRequested);
      throw new IOException("bad request");
    }
    return resourceFileRequested;
  }
}
