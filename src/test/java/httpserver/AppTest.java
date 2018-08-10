package httpserver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
  private CloseableHttpClient httpClient;
  private static String port = "8081";

  @BeforeClass
  static public void setup() {
    // Launch http-server, which spins off an initial thread to wait for requests, so these tests can run.
    String args[] = { port };
    App.main(args);
  }

  @AfterClass
  static public void teardown() {
  }

  @Before
  public void before() {
    this.httpClient = HttpClients.createDefault();
  }

  @After
  public void after() {
    try {
      this.httpClient.close();
    }
    catch (IOException e) {
      ;
    }
    this.httpClient = null;
  }

  /**
   * Rigorous Test :-)
   */
  @Test
  public void shouldAnswerWithTrue() throws Exception {
    int rc = this.executeGet("/helloworld.html", true);
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }

  @Test
  public void shouldAnswerWithTrueToo() throws Exception {
    int rc = this.executeGet("/foo/introducing_cairngorm.pdf", false);
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }

  @Test
  public void shouldAnswerWithTrueTooToo() throws Exception {
    int rc = this.executeGet("/ERROR_chrome_2018-08-01T20-43-00.756Z.png", false);
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }

  @Test
  public void shouldFailWithNotFound() throws Exception {
    int rc = this.executeGet("/foo.bar", false);
    assertEquals("Should be BAD", HttpStatus.SC_NOT_FOUND, rc);
  }

  private String getBaseUrl() {
    return ("http://localhost:" + port);
  }

  private int executeGet(String path, boolean dumpIt) throws ClientProtocolException, IOException {

    String thePath = getBaseUrl() + path;
    HttpGet httpGet = new HttpGet(thePath);
    CloseableHttpResponse response = this.httpClient.execute(httpGet);
    try {
      StatusLine status = response.getStatusLine();
      HttpEntity entity = response.getEntity();

      System.out.println("fetching: " + path);
      if (status.getStatusCode() == HttpStatus.SC_OK) {
        System.out.println("content type: " + entity.getContentType());
        System.out.println("content length: " + entity.getContentLength());

        if (dumpIt) {
          System.out.println("-----");
          System.out.println(IOUtils.toString(entity.getContent(), "UTF-8"));
          System.out.println("-----");
        }
        else {
          InputStream in = entity.getContent();
          File dir = new File("fetched");
          FileUtils.forceMkdir(dir);
          String outputFile = "fetched/file_" + UUID.randomUUID() + path.substring(path.lastIndexOf("."));
          FileOutputStream out = new FileOutputStream(outputFile);
          IOUtils.copy(in, out);

          System.out.println("-----");
          System.out.println("File written to: " + outputFile);
          System.out.println("-----");
        }
      }
      EntityUtils.consume(entity);
      return status.getStatusCode();
    }
    finally {
      response.close();
    }
  }
}
