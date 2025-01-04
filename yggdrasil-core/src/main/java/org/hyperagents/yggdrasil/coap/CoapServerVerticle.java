package org.hyperagents.yggdrasil.coap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;
import org.hyperagents.yggdrasil.utils.WebSubConfig;

/**
 * CoAP server verticle, manages lifetime of coap server.
 */
public class CoapServerVerticle extends AbstractVerticle {

  private CoapServer server;
  private NetworkInterfaceConfig coapConfig;
  private NetworkInterfaceConfig httpConfig;
  private EnvironmentConfig environmentConfig;
  private WebSubConfig notificationConfig;

  @Override
  public void start(final Promise<Void> startPromise) {
    this.coapConfig = this.vertx.sharedData()
        .<String, NetworkInterfaceConfig>getLocalMap("coap-config")
        .get("default");
    this.httpConfig = this.vertx.sharedData()
        .<String, NetworkInterfaceConfig>getLocalMap("http-config")
        .get("default");
    this.environmentConfig = this.vertx
        .sharedData()
        .<String, EnvironmentConfig>getLocalMap("environment-config")
        .get("default");
    this.notificationConfig = this.vertx.sharedData()
        .<String, WebSubConfig>getLocalMap("notification-config")
        .get("default");

    startCoAPServer();
    startPromise.complete();
  }

  @Override
  public void stop(final Promise<Void> stopPromise) {
    this.server.stop();
    stopPromise.complete();
  }

  private void startCoAPServer() {
    CoapConfig.register();
    UdpConfig.register();
    this.server = new YggdrasilCoAPServer(vertx, coapConfig, httpConfig, environmentConfig,notificationConfig);
    this.server.start();
  }
}
