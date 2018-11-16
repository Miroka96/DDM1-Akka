organization := "one.codebase"

scalaVersion := "2.12.7"
val akkaVersion = "2.5.18"

val mainclass = "skynet.cluster.SimpleClusterApp"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "io.kamon" % "sigar-loader" % "1.6.6-rev002",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "com.beust" % "jcommander" % "1.72",
  "com.twitter" %% "chill-akka" % "0.9.3",
  "org.projectlombok" % "lombok" % "1.18.2")

scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")
javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native")
fork in run := true
parallelExecution in Test := false

val additionalPom =
  <properties>
    <encoding>UTF-8</encoding>
  </properties>
  <build>
    <sourceDirectory>src/main/scala</sourceDirectory>
    <testSourceDirectory>src/test/scala</testSourceDirectory>
    <resources>
      <resource>
        <directory>src/main/resources/</directory>
      </resource>
    </resources>
    <testResources>
      <testResource>
        <directory>src/test/resources/</directory>
      </testResource>
    </testResources>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.3</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
          <fork>true</fork>
          <showWarnings>true</showWarnings>
          <showDeprecation>true</showDeprecation>
          <compilerArgument>-Xlint:all</compilerArgument>
        </configuration>
      </plugin>
      <plugin>
        <groupId>net.alchim31.maven</groupId>
        <artifactId>scala-maven-plugin</artifactId>
        <version>3.2.2</version>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
              <goal>testCompile</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <args>
            <!-- work-around for https://issues.scala-lang.org/browse/SI-8358 -->
            <arg>-nobootcp</arg>
          </args>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <version>3.1.0</version>
        <executions>
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <transformers>
                <transformer
                implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <mainClass>{mainclass}</mainClass>
                </transformer>
                <transformer
                implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                  <resource>reference.conf</resource>
                </transformer>
              </transformers>
              <createDependencyReducedPom>false</createDependencyReducedPom>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>

lazy val `skynet` = project
  .in(file("."))
  .settings(
    name := "skynet",
    description := "Akka Skynet Cluster",
    version := "1.3.3.7",
    mainClass in (Compile, run) := Some(mainclass),
    pomExtra := additionalPom
  )
