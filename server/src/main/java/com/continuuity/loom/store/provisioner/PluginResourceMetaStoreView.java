package com.continuuity.loom.store.provisioner;

import com.continuuity.loom.provisioner.PluginResourceMeta;

import java.io.IOException;
import java.util.List;

/**
 * A view of the plugin resource metadata store. Each account and resource type pair will see a different view of
 * the store.  Resource metadata essentially serves as a record of what resources have been uploaded and
 * which version of a resource is active.
 */
public interface PluginResourceMetaStoreView {

  /**
   * Checks whether or not the given resource exists.
   *
   * @param meta Metadata to check for
   * @return True if the module version exists, false if not
   * @throws IOException
   */
  public boolean exists(PluginResourceMeta meta) throws IOException;

  /**
   * Write the resource metadata to the store.
   *
   * @param meta Resource metadata to write
   * @throws IOException
   */
  public void write(PluginResourceMeta meta) throws IOException;

  /**
   * Delete the given metadata from the store.
   *
   * @param meta Metadata to delete
   * @throws IOException
   */
  public void delete(PluginResourceMeta meta) throws IOException;

  /**
   * Get an immutable list of all resource metadata.
   *
   * @return Immutable list of all resource metadata
   * @throws IOException
   */
  public List<PluginResourceMeta> getAll() throws IOException;

  /**
   * Get an immutable list of all active resource metadata.
   *
   * @return Immutable list of all active resource metadata
   * @throws IOException
   */
  public List<PluginResourceMeta> getAllActive() throws IOException;

  /**
   * Get an immutable list of all resource metadata for the given resource name.
   *
   * @param resourceName Name of resource to get metadata for
   * @return Immutable list of all resource metadata for the given resource name
   * @throws IOException
   */
  public List<PluginResourceMeta> getAll(String resourceName) throws IOException;

  /**
   * Get the metadata for the active version of the given resource name, or null if none exists.
   *
   * @param resourceName Name of resource to get metadata for
   * @return Metadata for the active version of the given resource name, or null if none exists
   * @throws IOException
   */
  public PluginResourceMeta getActive(String resourceName) throws IOException;

  /**
   * Atomically activate a specific resource version and deactivate the previous active version.
   *
   * @param resourceName Name of resource to activate
   * @param version Version of resource to activate
   * @throws IOException
   */
  public void activate(String resourceName, String version) throws IOException;

  /**
   * Deactivate all versions of a resource.
   *
   * @param resourceName Name of resource to deactivate
   * @throws IOException
   */
  public void deactivate(String resourceName) throws IOException;
}
