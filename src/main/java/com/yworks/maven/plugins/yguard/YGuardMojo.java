package com.yworks.maven.plugins.yguard;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.antrun.AntRunMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.aether.RepositorySystem;

import java.lang.reflect.Field;
import java.util.List;
import javax.inject.Inject;

/**
 * Runs the yGuard byte code obfuscator to rename types and type members in specified Java archives.
 */
@Mojo(name = "run", threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE)
public class YGuardMojo extends AbstractMojo {
  /**
   * The Maven project instance.
   */
  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  /**
   * The Maven session instance.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession session;

  /**
   * The plugin dependencies.
   */
  @Parameter(property = "plugin.artifacts", required = true, readonly = true)
  private List<org.apache.maven.artifact.Artifact> pluginArtifacts;

  /**
   * The local Maven repository.
   */
  @Parameter(property = "localRepository", readonly = true)
  private ArtifactRepository localRepository;

  /**
   * The version of yGuard to use for name obfuscation.
   * If no version is specified, the project repositories are queried for the highest version of yGuard.
   */
  @Parameter
  private String yguardVersion;

  /**
   * The yGuard Ant task configuration.
   * See the <a href="https://yworks.github.io/yGuard/task_documentation.html">yGuard Ant task documentation</a>.
   */
  @Parameter(required = true)
  private PlexusConfiguration yguard;

  /**
   * The Maven project helper instance.
   */
  private final MavenProjectHelper projectHelper;

  private final RepositoryUtil repositoryUtil;

  @Inject
  public YGuardMojo( final MavenProjectHelper projectHelper, final RepositorySystem repositorySystem ) {
    this.projectHelper = projectHelper;
    this.repositoryUtil = new RepositoryUtil(repositorySystem);
  }

  /**
   * Runs the yGuard Ant task.
   * This implementation delegates running the yGuard Ant task to Maven's AntRun plugin.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final PlexusConfiguration task = yguard;
    if (task == null) {
      getLog().info("No yguard task defined.");
      return;
    }

    final String version = yguardVersion == null ? "[1.0,)" : yguardVersion;
    final String classpath = repositoryUtil.resolveClasspath(
      project.getRemotePluginRepositories(), session.getRepositorySession(), version);
    if (classpath == null) {
      throw new MojoFailureException("Cannot resolve yguard artifacts for yguard version " + version + '.');
    }

    // <taskdef name="yguard" classname="com.yworks.yguard.YGuardTask"/>
    final DefaultPlexusConfiguration taskdef = new DefaultPlexusConfiguration("taskdef");
    taskdef.setAttribute("name", "yguard");
    taskdef.setAttribute("classname", "com.yworks.yguard.YGuardTask");
    taskdef.setAttribute("classpath", classpath);
    getLog().debug("Generated classpath for com.yworks.yguard.YGuardTask: " + classpath);

    // <target name="yguard">
    //   <taskdef name="yguard" .../>
    //   <yguard>
    //     <!-- plugin configuration -->
    //   </yguard>
    // </target>
    final DefaultPlexusConfiguration root = new DefaultPlexusConfiguration("target");
    root.setAttribute("name", "yguard");
    root.addChild(taskdef);
    root.addChild(task);

    final AntRunMojo antRunMojo = new AntRunMojo(projectHelper);
    configure(antRunMojo, root);
    antRunMojo.execute();
  }

  /**
   * Configures the given AntRun mojo to run the yGuard Ant task.
   */
  private void configure(
    final AntRunMojo antRunMojo, final PlexusConfiguration config
  ) throws MojoExecutionException {
    try {
      set(antRunMojo, "mavenProject", project);
      set(antRunMojo, "session", session);
      set(antRunMojo, "pluginArtifacts", pluginArtifacts);
      set(antRunMojo, "localRepository", localRepository);

      set(antRunMojo, "propertyPrefix", "");
      set(antRunMojo, "versionsPropertyName", "maven.project.dependencies.versions");
      set(antRunMojo, "failOnError", Boolean.TRUE);

      set(antRunMojo, "target", config);
    } catch (Exception ex) {
      if (ex instanceof RuntimeException) {
        throw (RuntimeException) ex;
      } else {
        throw new MojoExecutionException(ex);
      }
    }
  }

  /**
   * Sets the value of the given mojo's private field of the given name to the given value.
   */
  private void set(
    final AntRunMojo antRunMojo, final String name, final Object value
  ) throws IllegalAccessException, NoSuchFieldException {
    final Class<?> c = antRunMojo.getClass();
    final Field f = c.getDeclaredField(name);
    f.setAccessible(true);
    f.set(antRunMojo, value);
  }
}
