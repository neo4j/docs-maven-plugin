Maven plugin to package docs
============================

After installing it, add it to the project:

<plugin>
  <artifactId>docs-maven-plugin</artifactId>
  <groupId>org.neo4j.build.plugins</groupId>
  <version>0.1-SNAPSHOT</version>
  <!--  the directories below will be added by default,
    using the config element will _replace_ the defaults.
  <configuration>
    <sourceDirectories>
      <sourceDirectory>${basedir}/src/docs</sourceDirectory>
      <sourceDirectory>${project.build.directory}/docs</sourceDirectory>
    </sourceDirectories>
  </configuration>
   -->
</plugin>

Configuring execution from the pom.xml doesn't work yet.

There's two new commands you can use:

mvn docs:assembly
- creates the docs.jar
- attaches the created jar to the project
- mvn2 and mvn3

mvn docs:install
- creates and then installs the docs.jar
- mvn3 only
- CLI only

mvn docs:install -Dtest=DocsTest
- executes test, then assembles and installs docs.
- needs more work, but the basic functionality is there
