package org.hyperagents.yggdrasil.coap;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

/**
 * CoAP server verticle, manages lifetime of coap server.
 */
public class CoapServerVerticle extends AbstractVerticle {

  private CoapServer server;
  private HttpInterfaceConfig httpConfig;
  private EnvironmentConfig environmentConfig;

  @Override
  public void start(final Promise<Void> startPromise) {
    this.httpConfig = this.vertx.sharedData()
        .<String, HttpInterfaceConfig>getLocalMap("http-config")
        .get("default");
    this.environmentConfig = this.vertx
        .sharedData()
        .<String, EnvironmentConfig>getLocalMap("environment-config")
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
    final CoapEntityHandler handler = new CoapEntityHandler(vertx, httpConfig, environmentConfig);
    this.server = new YggdrasilCoAPServer(handler);
    this.server.start();
  }
}
