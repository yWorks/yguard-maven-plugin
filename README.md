yGuard Maven Plugin
===================

This plugin provides the ability to run [yGuard](https://github.com/yWorks/yGuard/) from within Maven. 

Goals
-------------------
+ __yguard-maven-plugin:run__ runs the yGuard byte code obfuscator to rename types and type members in specified Java
  archives.\
  This goal depends on and delegates execution to the
  [Maven AntRun Plugin](https://maven.apache.org/plugins/maven-antrun-plugin/).

Usage
-------------------
```xml
<project>
  [...]
  <build>
    <plugins>
      <plugin>
        <groupId>com.yworks.maven.plugins</groupId>
        <artifactId>yguard-maven-plugin</artifactId>
        <version>5.0.0</version>
        <configuration>
          <yguardVersion>5.0.0</yguardVersion>
          <yguard>

            <!--
              Place your task configuration here using the exact same syntax as for a <yguard> Ant task.
              -->

          </yguard>
        </configuration>
        <executions>
          <execution>
            <goals>
              <goal>rename</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  [...]
</project>
```
See the [yGuard Ant task documentation](https://yworks.github.io/yGuard/task_documentation.html) for detailed
information on how to configure yGuard.

Parameter Details
-------------------

__\<yguard>__

The XML for the yGuard task. You can use the exact same syntax as for a \<yguard> Ant task.\
See the [yGuard Ant task documentation](https://yworks.github.io/yGuard/task_documentation.html) for detailed
information on how to configure yGuard.
+ __Type:__ `org.codehaus.plexus.configuration.PlexusConfiguration`
+ __Since:__ `1.0.0`
+ __Required:__ `Yes`

__\<yguardVersion>__

Specifies the version of yGuard to use for name obfuscation.\
If this property is not set, the project repositories are queried for the highest available version of yGuard.
+ __Type:__ `java.lang.String`
+ __Since:__ `1.0.0`
+ __Required:__ `No`

