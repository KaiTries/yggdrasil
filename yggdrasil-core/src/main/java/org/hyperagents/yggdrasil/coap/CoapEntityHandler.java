package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;

import io.vertx.core.Vertx;
import java.util.List;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;

/**
 * Handler for coAP requests.
 */
public class CoapEntityHandler {

  private final RdfStoreMessagebox rdfStoreMessagebox;
  private final CartagoMessagebox cartagoMessagebox;
  private final NetworkInterfaceConfig coapConfig;
  private final NetworkInterfaceConfig httpConfig;
  private final boolean environment;

  /**
   * Default Constructor.
   */
  public CoapEntityHandler(final Vertx vertx,
                           final NetworkInterfaceConfig coapConfig,
                           final NetworkInterfaceConfig httpConfig,
                           final EnvironmentConfig environmentConfig) {
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.cartagoMessagebox = new CartagoMessagebox(
        vertx.eventBus(),
        environmentConfig
    );
    this.environment = environmentConfig.isEnabled();
    this.coapConfig = coapConfig;
    this.httpConfig = httpConfig;
  }

  /**
   * Returns the representation of the entity at the given Uri.
   */
  public void handleGetEntity(CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash() + exchange.getRequestOptions().getUriPathString();

    System.out.println("GET " + uri + " from " + exchange.getSourceAddress());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(uri)).onSuccess(
            s -> {
              final var responseBody = s.body().replace(this.httpConfig.getBaseUriTrailingSlash(),
                  this.coapConfig.getBaseUriTrailingSlash());
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              Response response = new Response(CONTENT);
              response.setPayload(responseBody);
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
            });
  }

  /**
   * Handles post requests to a specific workspace.
   */
  public void handlePostWorkspace(final CoapExchange exchange) {
    System.out.println("POST " + exchange.getRequestOptions().getUriPathString() + " from "
        + exchange.getSourceAddress());
    OptionSet optionSet = exchange.advanced().getRequest().getOptions();
    List<Option> options = optionSet.asSortedList();

    var agentId = options.stream().filter(o -> o.getNumber() == 500)
        .findFirst()
        .orElseThrow()
        .getStringValue();

    agentId = "http://localhost:8080/agents/" + agentId;

    final var agentBodyName = options.stream().filter(o -> o.getNumber() == 600)
        .findFirst()
        .orElseThrow()
        .getStringValue();

    // check what the last part of the uri is
    final var uriPathString = exchange.getRequestOptions().getUriPathString().split("/");
    System.out.println(uriPathString[uriPathString.length - 1]);
    switch (uriPathString[uriPathString.length - 1]) {
      case "join":
        this.handleJoinWorkspace(exchange, uriPathString, agentId, agentBodyName);
        break;
      case "leave":
        this.handleLeaveWorkspace(exchange, uriPathString, agentId, agentBodyName);
        break;
      default:
        exchange.reject();
        break;
    }
  }

  /**
   * Handles join requests to a specific workspace.
   */
  public void handleJoinWorkspace(CoapExchange exchange, final String[] uriPathString,
                                  final String agentId, final String agentBodyName) {
    final var metadata = exchange.getRequestText();
    final var workspaceName = uriPathString[uriPathString.length - 2];

    System.out.println("Joining workspace " + workspaceName + " with agent " + agentId);
    System.out.println("Metadata: " + metadata);
    System.out.println("Agent body name: " + agentBodyName);

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.JoinWorkspace(
            agentId,
            agentBodyName,
            workspaceName
        ))
        .compose(response -> this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.CreateBody(
                workspaceName,
                agentId,
                agentBodyName,
                response.body()
            ))
        )
        .compose(r -> this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.UpdateEntity(
                this.httpConfig.getAgentBodyUri(workspaceName, agentBodyName),
                metadata
            )))
        .onSuccess(s -> {
          exchange.accept();
          Response response = new Response(CONTENT);
          response.setPayload(s.body());
          exchange.respond(response);
        }).onFailure(f -> {
          exchange.accept();
          Response response = new Response(CONTENT);
          response.setPayload(f.getMessage());
          exchange.respond(response);
        });
  }

  /**
   * Handles leave requests to a specific workspace.
   */
  public void handleLeaveWorkspace(CoapExchange exchange, final String[] uriPathString,
                                   final String agentId, final String agentBodyName) {
    final var workspaceName = uriPathString[uriPathString.length - 2];

    this.cartagoMessagebox
        .sendMessage(new CartagoMessage.LeaveWorkspace(
            agentId,
            workspaceName
        ))
        .compose(r -> this.rdfStoreMessagebox
            .sendMessage(new RdfStoreMessage.DeleteEntity(
                this.httpConfig.getAgentBodyUriTrailingSlash(
                    workspaceName,
                    agentBodyName
                )
            )))
        .onSuccess(s -> {
          exchange.accept();
          Response response = new Response(CONTENT);
          response.setPayload(s.body());
          exchange.respond(response);
        }).onFailure(f -> {
          exchange.accept();
          Response response = new Response(CONTENT);
          response.setPayload(f.getMessage());
          exchange.respond(response);
        });
  }
}
