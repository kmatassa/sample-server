package httpserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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
  private static CloseableHttpClient httpClient;
  private static String port = "8081";

  @BeforeClass
  static public void setup() {
    // Launch http-server, which spins off an initial thread to wait for requests, so these tests can run.
    String args[] = { port };
    App.main(args);
    httpClient = HttpClients.createDefault();    
  }

  @AfterClass
  static public void teardown() {
  }

  @Before
  public void before() {
  }

  @After
  public void after() {
  }

  /**
   * Rigorous Test :-)
   */
  @Test
  public void shouldAnswerWithTrue() throws Exception {
    System.out.println("----------------------");    
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/helloworld.html", true, "keep-alive");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }
  
  @Test
  public void keepAliveWithHttp1_1_is_supported() throws Exception {
    System.out.println("----------------------");  
    // Look for in LOG:
    // INFO: http-server keep-alive mode: true number of requests on this socket: 2     
    // Note also, other tests that still use keep-alive enabled, will increase this socket count accordingly...
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/helloworld.html", false, "keep-alive");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
    rc = this.executeGet(HttpVersion.HTTP_1_1, "/smiley.gif", false, "keep-alive");    
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }  
  
  @Test
  public void keepAliveWithHttp1_1_is_supported_but_disabled_by_client() throws Exception {
    System.out.println("----------------------");  
    // Look for in LOG:
    // INFO: http-server keep-alive mode: true number of requests on this socket: 1     
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/helloworld.html", false, "close");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
    rc = this.executeGet(HttpVersion.HTTP_1_1, "/smiley.gif", false, "close");    
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }  
 
  @Test
  public void keepAliveWithHttp1_0_is_not_supported() throws Exception {
    System.out.println("----------------------");  
    // THIS IS FOR DEMO PURPOSES.  "http-server" doesn't support keep-alive with 1.0.
    // "close" indicates the server has closed the socket.
    int rc = this.executeGet(HttpVersion.HTTP_1_0, "/helloworld.html", false, "close");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }  

  @Test
  public void shouldFetchWithPDFContentInSubDirectory() throws Exception {
    System.out.println("----------------------");    
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/foo/introducing_cairngorm.pdf", false, "keep-alive");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }

  @Test
  public void shouldAnswerWithPNGContent() throws Exception {
    System.out.println("----------------------");    
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/ERROR_chrome_2018-08-01T20-43-00.756Z.png", false, "keep-alive");
    assertEquals("Should be OK", HttpStatus.SC_OK, rc);
  }

  @Test
  public void shouldFailWithNotFound() throws Exception {
    System.out.println("----------------------");    
    int rc = this.executeGet(HttpVersion.HTTP_1_1, "/foo.bar", false, "keep-alive");
    assertEquals("Should be BAD", HttpStatus.SC_NOT_FOUND, rc);
  }

  @Test
  public void shouldFailWithMethodNotAllowed() throws Exception {
    System.out.println("----------------------");    
    String thePath = getBaseUrl() + "/foo.bar";
    HttpPost httpPost = new HttpPost(thePath);    
    CloseableHttpResponse response = this.httpClient.execute(httpPost);
    try {
      StatusLine status = response.getStatusLine();
      assertEquals("Should be BAD", HttpStatus.SC_METHOD_NOT_ALLOWED, status.getStatusCode());      
    }
    finally {
      response.close();
    }
  }
  
  private String getBaseUrl() {
    return ("http://localhost:" + port);
  }

  private int executeGet(HttpVersion version, String path, boolean dumpIt, String connectionHeader) 
      throws ClientProtocolException, IOException {
    String thePath = getBaseUrl() + path;
    HttpGet httpGet = new HttpGet(thePath);
    httpGet.setProtocolVersion(version);
    if (connectionHeader.contains("close")) {
      httpGet.addHeader("Connection", "close");
    }    
    CloseableHttpResponse response = this.httpClient.execute(httpGet);
    try {
      StatusLine status = response.getStatusLine();
      HttpEntity entity = response.getEntity();

      System.out.println("using protocol version: " + version);
      System.out.println("fetching: " + path);
      if (status.getStatusCode() == HttpStatus.SC_OK) {
        System.out.println("content type: " + entity.getContentType());
        System.out.println("content length: " + entity.getContentLength());      
        System.out.println("connection: " + response.getFirstHeader("Connection").getValue());
        
        // Validate the http-server has indicated that the socket has been closed via the header value.
        if (connectionHeader != null) {
          assertTrue("header connection", response.getFirstHeader("Connection").getValue().contains(connectionHeader));
        }
        
        if (dumpIt) {
          System.out.println("----- c o n t e n t ");
          System.out.println(IOUtils.toString(entity.getContent(), "UTF-8"));
        }
        else {
          InputStream in = entity.getContent();
          File dir = new File("target" + File.separator + "fetched");
          FileUtils.forceMkdir(dir);
          String outputFile = "target" + File.separator + "fetched" + File.separator + "file_" + UUID.randomUUID() + 
              path.substring(path.lastIndexOf("."));
          FileOutputStream out = new FileOutputStream(outputFile);
          IOUtils.copy(in, out);

          System.out.println("----- w r i t t e n");
          System.out.println("Look at binary on disk, file written to: " + outputFile);
        }
      } else {
        System.out.println("----- e r r o r");
        System.out.println("status: " + status.getStatusCode());
      }      
      EntityUtils.consume(entity);
      return status.getStatusCode();
    }
    finally {
      response.close();
    }
  }
}
