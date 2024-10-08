package org.hyperagents.yggdrasil.cartago.entities.impl;

import cartago.Workspace;
import cartago.WorkspaceDescriptor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.hyperagents.yggdrasil.cartago.entities.WorkspaceRegistry;

/**
 * Wrapper class to query all workspaces.
 */
public final class WorkspaceRegistryImpl implements WorkspaceRegistry {
  private final Map<String, WorkspaceDescriptor> workspaceDescriptors;

  public WorkspaceRegistryImpl() {
    this.workspaceDescriptors = new HashMap<>();
  }

  @Override
  public void registerWorkspace(final WorkspaceDescriptor descriptor, final String uri) {
    final var name = descriptor.getWorkspace().getId().getName();
    this.workspaceDescriptors.put(name, descriptor);
  }

  @Override
  public Optional<WorkspaceDescriptor> getWorkspaceDescriptor(final String name) {
    return Optional.ofNullable(this.workspaceDescriptors.get(name));
  }

  @Override
  public Optional<Workspace> getWorkspace(final String name) {
    return Optional.ofNullable(this.workspaceDescriptors.get(name))
        .map(WorkspaceDescriptor::getWorkspace);
  }

  @Override
  public void deleteWorkspace(final String name) {
    this.workspaceDescriptors.remove(name);
  }
}
