package org.hyperagents.yggdrasil.utils;

import io.vertx.core.shareddata.Shareable;
import org.eclipse.rdf4j.model.IRI;

/**
 * Represents the configuration for an HTTP interface.
 * This interface extends the Shareable interface from the Vert.x library.
 * It provides methods to retrieve the host, port, base URI,
 * and various URIs related to workspaces, artifacts, agents, etc.
 */
public interface HttpInterfaceConfig extends Shareable {
  /**
   * Gets the host of the HTTP interface.
   *
   * @return the host
   */
  String getHost();

  /**
   * Gets the port of the HTTP interface.
   *
   * @return the port
   */
  int getPort();

  /**
   * Gets the base URI of the HTTP interface.
   *
   * @return the base URI
   */
  String getBaseUri();

  IRI getBaseIrI();


  /**
   * Gets the URI for retrieving all workspaces.
   *
   * @return the workspaces URI
   */
  String getWorkspacesUri();

  /**
   * Gets the URI for retrieving a specific workspace by name.
   *
   * @param workspaceName the name of the workspace
   * @return the workspace URI
   */
  String getWorkspaceUri(String workspaceName);

  /**
   * Gets the URI for retrieving all artifacts within a workspace.
   *
   * @param workspaceName the name of the workspace
   * @return the artifacts URI
   */
  String getArtifactsUri(String workspaceName);

  /**
   * Gets the URI for retrieving a specific artifact within a workspace.
   *
   * @param workspaceName the name of the workspace
   * @param artifactName  the name of the artifact
   * @return the artifact URI
   */
  String getArtifactUri(String workspaceName, String artifactName);

  /**
   * Gets the URI for retrieving all agent bodies within a workspace.
   *
   * @param workspaceName the name of the workspace
   * @return the agent bodies URI
   */
  String getAgentBodiesUri(String workspaceName);

  /**
   * Gets the URI for retrieving a specific agent body within a workspace.
   *
   * @param workspaceName the name of the workspace
   * @param agentName     the name of the agent
   * @return the agent body URI
   */
  String getAgentBodyUri(String workspaceName, String agentName);

  /**
   * Gets the URI for retrieving a specific agent by name.
   *
   * @param agentName the name of the agent
   * @return the agent URI
   */
  String getAgentUri(String agentName);
}
