package httpserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.file.FileSystems;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;

/**
 * Processor reads a request and returns the result.
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
    OutputStream out = client.getOutputStream();
    try {
      RequestParser request = new RequestParser();
      request.parse(client);
      logger.info("http-server request: " + request);

      if (request.getMethod().equalsIgnoreCase("GET")) {
        try {
          deliverAFile(out, request);
        } catch (FileNotFoundException e) {
          deliverAnIssue(out, HttpStatus.SC_NOT_FOUND);
        } catch (Exception e) {
          deliverAnIssue(out, HttpStatus.SC_BAD_REQUEST);
        }
      } else {
        deliverAnIssue(out, HttpStatus.SC_BAD_REQUEST);
      }
    } catch (Exception e) {
      deliverAnIssue(out, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    } finally {
      client.close();
    }
  }

  /**
   * Delivers an unexpected HTTP code back to the client.
   * @param out is the OutputStream
   * @param issue is the Http status code to return
   * @throws IOException sometimes
   */
  private void deliverAnIssue(final OutputStream out, final int issue) throws IOException {
    String txt = String.format("HTTP/1.1 %d%s", issue, crLf);
    out.write(txt.getBytes());
  }

  /**
   * Delivers the request URL if possible.
   * @param out is the OutputStream
   * @param request is the RequestParser content
   * @throws IOException sometimes
   */
  private void deliverAFile(final OutputStream out, final RequestParser request) throws IOException {
    File source = this.getFile(request.getUri());
    if (source.exists() && source.isFile()) {
      String contentType = new MimetypesFileTypeMap().getContentType(source);
      long contentLength = source.length();
      logger.info("content-type [" + contentType + "]");
      logger.info("content-length [" + contentLength + "]");
      String txt = String.format("HTTP/1.1 %d %sContent-Type: %s%sContent-Length: %d%s%s", HttpStatus.SC_OK, crLf, contentType, crLf,
                                 contentLength, crLf, crLf);
      // Will close stream
      // out.print("Connection: close\r\n");
      try {
        out.write(txt.getBytes());
        FileUtils.copyFile(source, out);
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
   */
  private File getFile(final String uri) {
    File path = FileSystems.getDefault().getPath("").toAbsolutePath().toFile();
    String httpServerRoot = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "files";
    File dir = FileUtils.getFile(path, httpServerRoot);
    URI u = URI.create(uri);

    return FileUtils.getFile(dir, u.getPath());
  }
}
