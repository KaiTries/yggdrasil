package org.hyperagents.yggdrasil.coap;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.CONTENT;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.hyperagents.yggdrasil.eventbus.messageboxes.CartagoMessagebox;
import org.hyperagents.yggdrasil.eventbus.messageboxes.RdfStoreMessagebox;
import org.hyperagents.yggdrasil.eventbus.messages.CartagoMessage;
import org.hyperagents.yggdrasil.eventbus.messages.RdfStoreMessage;
import org.hyperagents.yggdrasil.utils.EnvironmentConfig;
import org.hyperagents.yggdrasil.utils.NetworkInterfaceConfig;
import org.hyperagents.yggdrasil.utils.RepresentationFactory;
import org.hyperagents.yggdrasil.utils.WebSubConfig;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryFactory;
import org.hyperagents.yggdrasil.utils.impl.RepresentationFactoryTDImplt;

/**
 * Handler for coAP requests.
 */
public class CoapEntityHandler {
  private final static String JSON = "application/json";

  private final RdfStoreMessagebox rdfStoreMessagebox;
  private final CartagoMessagebox cartagoMessagebox;
  private final NetworkInterfaceConfig coapConfig;
  private final NetworkInterfaceConfig httpConfig;
  private final RepresentationFactory representationFactory;
  private final boolean environment;

  /**
   * Default Constructor.
   */
  public CoapEntityHandler(final Vertx vertx,
                           final NetworkInterfaceConfig coapConfig,
                           final NetworkInterfaceConfig httpConfig,
                           final EnvironmentConfig environmentConfig,
                           final WebSubConfig webSubConfig) {
    this.representationFactory = RepresentationFactoryFactory.getRepresentationFactory(
        environmentConfig.getOntology(), webSubConfig,httpConfig);
    this.rdfStoreMessagebox = new RdfStoreMessagebox(vertx.eventBus());
    this.cartagoMessagebox = new CartagoMessagebox(
        vertx.eventBus(),
        environmentConfig
    );
    this.environment = environmentConfig.isEnabled();
    this.coapConfig = coapConfig;
    this.httpConfig = httpConfig;
  }

  private String cleanPath(String path) {
    return path.split("#")[0];
  }

  /**
   * Returns the representation of the entity at the given Uri.
   */
  public void handleGetEntity(final CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash()
            + cleanPath(exchange.getRequestOptions().getUriPathString());

    System.out.println("[handleGetEntity] GET " + uri + " from " + exchange.getSourceAddress());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(uri)).onSuccess(
            s -> {
              final var responseBody = s.body().replace(this.httpConfig.getBaseUriTrailingSlash(),
                  this.coapConfig.getBaseUriTrailingSlash());
              System.out.println("[handleGetEntity] responsePayload: " + responseBody);
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              final Response response = new Response(CONTENT);
              response.setPayload(responseBody);
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              System.out.println("failed: " + f.getMessage());
              exchange.accept();
              final Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            });
  }

  public void handleGetArtifact(final CoapExchange exchange) {
    final var uri =
      this.httpConfig.getBaseUriTrailingSlash()
          + cleanPath(exchange.getRequestOptions().getUriPathString());

    System.out.println("[handleGetArtifact] GET " + uri + " from " + exchange.getSourceAddress());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetEntity(uri)).onSuccess(
            s -> {
              final var responseBody = s.body().replace(this.httpConfig.getBaseUriTrailingSlash(),
                  this.coapConfig.getBaseUriTrailingSlash());
              System.out.println("[handleGetArtifact] responsePayload: " + responseBody);
              exchange.accept();
              final Response response = new Response(CONTENT);
              response.setPayload(responseBody);
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              System.out.println("failed: " + f.getMessage());
              exchange.accept();
              final Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            });
  }

  public void handleGetArtifacts(final CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash()
            + cleanPath(exchange.getRequestOptions().getUriPathString());
    System.out.println("[handleGetArtifacts] GET " + uri + " from " + exchange.getSourceAddress());

    final var uriPathString = exchange.getRequestOptions().getUriPathString().split("/");
    final var workspaceName = uriPathString[uriPathString.length - 2];

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetArtifacts(workspaceName, JSON)).onSuccess(
            s -> {
              final var temp = JsonArray.of(
                  JsonObject.of(
                      "name",
                      "datalake",
                      "port",
                      5685
                  ),
                  JsonObject.of(
                      "name",
                      "omi",
                      "port",
                      5686
                  )
              ).encode();
              final var responseBody = s.body().replace(this.httpConfig.getBaseUriTrailingSlash(),
                  this.coapConfig.getBaseUriTrailingSlash());
              System.out.println("[handleGetArtifacts] responsePayload: " + temp);
              exchange.accept();
              final Response response = new Response(CONTENT);
              response.setPayload(temp);
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              System.out.println("failed: " + f.getMessage());
              exchange.accept();
              final Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            });

  }

  public void handleGetWorkspaces(final CoapExchange exchange) {
    final var uri =
        this.httpConfig.getBaseUriTrailingSlash()
            + cleanPath(exchange.getRequestOptions().getUriPathString());
    System.out.println("[handleGetWorkspaces] GET " + uri + " from " + exchange.getSourceAddress());

    this.rdfStoreMessagebox
        .sendMessage(new RdfStoreMessage.GetWorkspaces(this.httpConfig.getBaseUriTrailingSlash()
            , JSON)).onSuccess(
            s -> {
              final var responseBody = s.body().replace(this.httpConfig.getBaseUriTrailingSlash(),
                  this.coapConfig.getBaseUriTrailingSlash());
              System.out.println("[handleGetWorkspaces] responsePayload: " + responseBody);
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              final Response response = new Response(CONTENT);
              response.setPayload(responseBody);
              exchange.respond(response);
            }
        )
        .onFailure(
            f -> {
              System.out.println("failed: " + f.getMessage());
              exchange.accept();
              // start asynchronous processing, passing the exchange to a result callback
              final Response response = new Response(CONTENT);
              response.setPayload(f.getMessage());
              exchange.respond(response);
            });

  }

  /**
   * Handles post requests to a specific workspace.
   */
  public void handlePostWorkspace(final CoapExchange exchange) {
    System.out.println("[handlePostWorkspace] POST " + exchange.getRequestOptions().getUriPathString() + " from "
        + exchange.getSourceAddress());

    JsonObject jsonObject = new JsonObject(exchange.getRequestText());
    final var agentBodyName = jsonObject.getString("agentId");
    final var agentId = "http://localhost:8080/agents/" + agentBodyName;

    // check what the last part of the uri is
    final var uriPathString = exchange.getRequestOptions().getUriPathString().split("/");
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
  public void handleJoinWorkspace(final CoapExchange exchange, final String[] uriPathString,
                                  final String agentId, final String agentBodyName) {
    final var workspaceName = uriPathString[uriPathString.length - 2];

    if (environment) {
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
          .onSuccess(s -> {
            exchange.accept();
            final var agentUri = this.httpConfig.getAgentBodyUri(workspaceName, agentBodyName);
            final Response response = new Response(CoAP.ResponseCode.CREATED);
            response.setPayload(agentUri);
            exchange.respond(response);
          }).onFailure(f -> {
            exchange.accept();
            final Response response = new Response(CoAP.ResponseCode.BAD_REQUEST);
            exchange.respond(response);
          });
    } else {
      this.rdfStoreMessagebox
              .sendMessage(new RdfStoreMessage.CreateBody(
                  workspaceName,
                  agentId,
                  agentBodyName,
                  this.representationFactory.createBodyRepresentation(
                      workspaceName,
                      agentBodyName,
                      new LinkedHashModel()
                  )
              )
          )
          .onSuccess(s -> {
            exchange.accept();
            final Response response = new Response(CoAP.ResponseCode.CREATED);
            exchange.respond(response);
          }).onFailure(f -> {
            exchange.accept();
            final Response response = new Response(CoAP.ResponseCode.BAD_REQUEST);
            exchange.respond(response);
          });
    }
  }

  /**
   * Handles leave requests to a specific workspace.
   */
  public void handleLeaveWorkspace(final CoapExchange exchange, final String[] uriPathString,
                                   final String agentId, final String agentBodyName) {
    final var workspaceName = uriPathString[uriPathString.length - 2];

    if (environment) {
      this.cartagoMessagebox
          .sendMessage(new CartagoMessage.LeaveWorkspace(
              agentId,
              workspaceName
          ))
          .compose(r -> this.rdfStoreMessagebox
              .sendMessage(new RdfStoreMessage.DeleteEntity(
                  workspaceName,
                  "body_" + agentBodyName
              )))
          .onSuccess(s -> {
            exchange.accept();
            final Response response = new Response(CoAP.ResponseCode.DELETED);
            exchange.respond(response);
          }).onFailure(f -> {
            exchange.accept();
            final Response response = new Response(CoAP.ResponseCode.BAD_REQUEST);
            exchange.respond(response);
          });
    } else {
      this.rdfStoreMessagebox
          .sendMessage(new RdfStoreMessage.DeleteEntity(
              workspaceName,
              "body_" + agentBodyName
          ))
          .onSuccess(s -> {
        exchange.accept();
        final Response response = new Response(CoAP.ResponseCode.DELETED);
        exchange.respond(response);
      }).onFailure(f -> {
        exchange.accept();
        final Response response = new Response(CoAP.ResponseCode.BAD_REQUEST);
        exchange.respond(response);
      });
    }
  }
}
