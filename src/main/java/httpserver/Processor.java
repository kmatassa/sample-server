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

public class Processor {
  final String CRLF = "\r\n";
  Logger logger = Logger.getAnonymousLogger();
  Socket client = null;

  Processor(Socket client) {
    this.client = client;
  }

  public void process() throws IOException {
    OutputStream out = client.getOutputStream();
    try {
      RequestParser request = new RequestParser();
      request.parse(client);
      logger.info("http-server request: " + request);

      if (request.method.equalsIgnoreCase("GET")) {
        try {
          deliverAFile(out, request);
        }
        catch (FileNotFoundException e) {
          deliverAnIssue(out, HttpStatus.SC_NOT_FOUND);
        }
        catch (Exception e) {
          deliverAnIssue(out, HttpStatus.SC_BAD_REQUEST);
        }
      }
      else {
        deliverAnIssue(out, HttpStatus.SC_BAD_REQUEST);
      }
    }
    catch (Exception e) {
      deliverAnIssue(out, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
    finally {
      client.close();
    }
  }

  protected void deliverAnIssue(OutputStream out, int issue) throws IOException {
    String txt = String.format("HTTP/1.1 %d%s", issue, CRLF);
    out.write(txt.getBytes());
  }

  protected void deliverAFile(OutputStream out, RequestParser request) throws IOException {
    File source = this.getFile(request.uri);
    if (source.exists() && source.isFile()) {
      String contentType = new MimetypesFileTypeMap().getContentType(source);
      long contentLength = source.length();
      logger.info("content-type [" + contentType + "]");
      logger.info("content-length [" + contentLength + "]");
      String txt = String.format("HTTP/1.1 %d %sContent-Type: %s%sContent-Length: %d%s%s", HttpStatus.SC_OK, CRLF, contentType, CRLF,
                                 contentLength, CRLF, CRLF);
      // Will close stream
      // out.print("Connection: close\r\n");
      try {
        out.write(txt.getBytes());
        FileUtils.copyFile(source, out);
      }
      catch (Error ex) {
        logger.severe(ex.getMessage());
      }
      catch (Exception e) {
        logger.severe(e.getMessage());
      }
    }
    else {
      throw new FileNotFoundException();
    }
  }

  File getFile(String uri) {
    File path = FileSystems.getDefault().getPath("").toAbsolutePath().toFile();
    String httpServerRoot = "src/main/resources/files";    
    File dir = FileUtils.getFile(path, httpServerRoot);
    URI u = URI.create(uri);
    
    return FileUtils.getFile(dir, u.getPath());
  }
}
