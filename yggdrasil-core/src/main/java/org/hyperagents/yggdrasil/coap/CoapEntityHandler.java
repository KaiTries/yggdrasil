package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;

import io.vertx.core.Vertx;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.HttpInterfaceConfig;

/**
 * Handler for coAP requests.
 */
public class CoapEntityHandler {

  RdfStoreMessagebox rdfStoreMessagebox;
  HttpInterfaceConfig httpConfig;

  /**
   * Default Constructor.
   */
  public CoapEntityHandler(final Vertx vertx, final HttpInterfaceConfig httpConfig) {
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.httpConfig = httpConfig;

  }

  /**
   * Returns the representation of the entity at the given Uri.
   */
  public void handleGetEntity(final CoapResource resource, CoapExchange exchange) {
    final var uri = this.httpConfig.getBaseUri() + resource.getURI();
    System.out.println(uri);

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(uri)).onSuccess(
            s -> {
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              Response response = new Response(CONTENT);
              response.setPayload(s.body());
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            }
    );
  }
}
