package org.hyperagents.yggdrasil.eventbus.codecs;

enum MessageFields {
  REQUEST_METHOD("requestMethod"),
  REQUEST_URI("requestUri"),
  ENTITY_URI_HINT("slug"),
  AGENT_ID("agentID"),
  AGENT_NAME("agentName"),
  API_KEY("apiKey"),
  WORKSPACE_NAME("workspaceName"),
  SUB_WORKSPACE_NAME("subWorkspaceName"),
  ARTIFACT_NAME("artifactName"),
  ACTION_NAME("actionName"),
  ENTITY_REPRESENTATION("entityRepresentation"),
  ACTION_CONTENT("actionContent"),
  NOTIFICATION_CONTENT("notificationContent"),
  PARENT_WORKSPACE_URI("parentWorkspaceUri"),
  QUERY("query"),
  NAMED_GRAPH_URIS("namedGraphUris"),
  DEFAULT_GRAPH_URIS("defaultGraphUris"),
  CONTENT_TYPE("contentType"),
  CALLBACK_IRI("callbackIri"),
  STORE_RESPONSE("storeResponse"),
  CONTEXT("context"),;


  private static final String PREFIX = "org.hyperagents.yggdrasil.eventbus.fields.";

  private final String name;

  MessageFields(final String name) {
    this.name = PREFIX + name;
  }

  public String getName() {
    return this.name;
  }
}
