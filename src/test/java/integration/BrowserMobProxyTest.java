package integration;

import com.codeborne.selenide.WebDriverRunner;
import org.apache.http.*;
import org.apache.http.protocol.HttpContext;
import org.browsermob.core.har.HarEntry;
import org.browsermob.proxy.ProxyServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.net.PortProber.findFreePort;

public class BrowserMobProxyTest extends IntegrationTest {
  ProxyServer proxyServer;

  @Before
  public void closePreviousWebdriver() {
    WebDriverRunner.closeWebDriver();
  }

  @Before
  public void startBrowserMobProxyServer() throws Exception {
    proxyServer = new ProxyServer(findFreePort());
    proxyServer.start();
  }

  @After
  public void stopBrowserMobProxyServer() throws Exception {
    proxyServer.stop();
  }

  @After
  public void resetWebdriverProxySettings() {
    WebDriverRunner.setProxy(null);
    WebDriverRunner.closeWebDriver();
  }

  private int requestCounter = 0;
  
  @Test
  public void canUseBrowserMobProxy() {
    proxyServer.addRequestInterceptor(new HttpRequestInterceptor() {
      @Override
      public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        if (!"/favicon.ico".equals(httpRequest.getRequestLine().getUri())) {
          requestCounter++;
        }
      }
    });

    proxyServer.newHar("some-har");

    WebDriverRunner.setProxy(proxyServer.seleniumProxy());
    
    openFile("file_upload_form.html");
    $("#cv").uploadFromClasspath("hello_world.txt");
    $("#avatar").uploadFromClasspath("firebug-1.11.4.xpi");
    $("#submit").click();
    assertEquals(2, server.uploadedFiles.size());

    assertEquals(2, requestCounter);
    
    List<HarEntry> harEntries = proxyServer.getHar().getLog().getEntries();
    assertTrue(harEntries.get(0).getRequest().getUrl().endsWith("/file_upload_form.html"));
    assertTrue(harEntries.get(harEntries.size()-1).getRequest().getUrl().endsWith("/upload"));
  }
}