package com.yworks.maven.plugins.yguard;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Resolves yGuard dependencies.
 */
class RepositoryUtil {
  private final RepositorySystem repositorySystem;

  RepositoryUtil( final RepositorySystem repositorySystem ) {
    this.repositorySystem = repositorySystem;
  }

  /**
   * Resolves the dependencies for the given version of yGuard and returns an Ant-compatible classpath string for the
   * resolved dependencies.
   */
  String resolveClasspath(
    final List<RemoteRepository> remoteRepositories, final RepositorySystemSession session, final String version
  ) {
    final DefaultArtifact artifact = new DefaultArtifact("com.yworks:yguard:" + version);
    final Dependency dependency = new Dependency(artifact, null);
    final CollectRequest cRequest = new CollectRequest(dependency, remoteRepositories);

    final HashSet<String> coordinates = new HashSet<>();

    try {
      final CollectResult collectResult = repositorySystem.collectDependencies(session, cRequest);
      final DependencyNode root = collectResult.getRoot();
      root.accept(new DependencyCoordinatesCollector(coordinates));
    } catch (DependencyCollectionException dce) {
      // use simple dependency heuristic
    }

    try {
      final DependencyFilter filter = coordinates.isEmpty()
        ? new SimpleDependencyHeuristic() : new CoordinatesDependencyFilter(coordinates);
      final DependencyRequest dRequest = new DependencyRequest(cRequest, filter);
      final DependencyResult result = repositorySystem.resolveDependencies(session, dRequest);
      return toClasspath(result.getArtifactResults());
    } catch (DependencyResolutionException dre) {
      return null;
    }
  }

  /**
   * Generates an Ant-compatible classpath string for the given artifacts.
   */
  private static String toClasspath( final List<ArtifactResult> results ) {
    final String pathSeparator = File.pathSeparator;

    final StringBuilder sb = new StringBuilder();
    String del = "";
    for (ArtifactResult result : results) {
      final Artifact artifact = result.getArtifact();
      if (artifact == null) {
        continue;
      }

      final File file = artifact.getFile();
      if (file == null) {
        continue;
      }

      sb.append(del).append(file.getAbsolutePath());
      del = pathSeparator;
    }
    return del.isEmpty() ? null : sb.toString();
  }


  static boolean isArtifact( final DependencyNode node, final String coordinates ) {
    return coordinates.equals(getCoordinates(node));
  }

  static String getCoordinates( final DependencyNode node ) {
    final Artifact artifact = node.getArtifact();
    return artifact.getGroupId() + ":" + artifact.getArtifactId();
  }


  /**
   * Accepts all dependencies that match one of the given coordinates.
   */
  private static class CoordinatesDependencyFilter implements DependencyFilter {
    private final Collection<String> coordinates;

    CoordinatesDependencyFilter( final Collection<String> coordinates ) {
      this.coordinates = coordinates;
    }

    @Override
    public boolean accept( final DependencyNode node, final List<DependencyNode> parents ) {
      return coordinates.contains(getCoordinates(node));
    }
  }

  /**
   * Accepts yGuard and its direct, non-Ant dependencies.
   * I.e. com.yworks:yguard, com.yworks:annotation, and org.ow2.asm:asm.
   */
  private static final class SimpleDependencyHeuristic implements DependencyFilter {
    @Override
    public boolean accept( final DependencyNode node, final List<DependencyNode> parents ) {
      node.accept(null);
      switch (parents.size()) {
        case 0:
          return isArtifact(node, "com.yworks:yguard");
        case 1:
          return isArtifact(parents.get(0), "com.yworks:yguard") && !isArtifact(node, "org.apache.ant:ant");
        default:
          return false;
      }
    }
  }

  /**
   * Collects the coordinates of all non-Ant dependencies.
   */
  private static final class DependencyCoordinatesCollector implements DependencyVisitor {
    private final Collection<String> coordinates;

    DependencyCoordinatesCollector( final Collection<String> coordinates ) {
      this.coordinates = coordinates;
    }

    @Override
    public boolean visitEnter( final DependencyNode node ) {
      if (isArtifact(node, "org.apache.ant:ant")) {
        return false;
      } else {
        coordinates.add(getCoordinates(node));
        return true;
      }
    }

    @Override
    public boolean visitLeave( final DependencyNode node ) {
      return true;
    }
  }
}
