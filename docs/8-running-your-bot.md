# Running your bot

This document guides you through the process of building and deploying your Botica Java bot,
transforming your code into a container image ready to be orchestrated by the Botica Director.

## Bot entry point

Each Botica Java bot requires a designated entry point that starts the Botica runtime. This is
typically a `main` method in a launcher class that calls `BotLauncher.run(...)`.

```java
package com.myorg;

public class MyBotBootstrap {

  public static void main(String[] args) {
    // Replace 'new MyBot()' with an instance of your BaseBot implementation
    BotLauncher.run(new MyBot(), args);
  }
}
```

> [!NOTE]
> Botica bots are designed to run exclusively within a Botica environment, not as standalone
> applications. You cannot simply run the `main` method of your `BotBootstrap` class manually. Keep
> reading to learn how to run your bot in a Botica environment.

## Step 1: Building your bot's container image

Regardless of how you set up your project, the first step is always to compile your bot's code and
package it into a Docker container image.

### Option 1: The easy way

**If you started your project with
the [botica-seed-java](https://github.com/isa-group/botica-seed-java) template**, this process is
automated for you.

1. **Customize `pom.xml`**: Ensure your `pom.xml` has the correct `<imageTag>` property (e.g.,
   `my-org/my-bot`). This tag identifies your bot's Docker image and must be referenced in your
   Botica environment configuration file to deploy your bot.

   ```xml
   <project>
     ...
     <properties>
       ...
       <mainClass>com.myorg.MyBotBootstrap</mainClass>
       <imageTag>my-org/my-bot</imageTag>
     </properties>
     ...
   </project>
   ```

2. **Run the build script**: Navigate to your project's root directory in a terminal and execute the
   provided build script.

   #### On Linux or macOS

   ```bash
   ./build.sh
   ```

   #### On Windows

   ```bash
   build.bat
   ```

   This script automatically compiles your Maven project, packages it into an executable JAR with
   all dependencies, and then builds a Docker image tagged as specified in your `pom.xml`.

### Option 2: For existing Maven projects (without the template)

If you're integrating `botica-lib-java` into an existing Maven project, you'll need to manually
configure your `pom.xml` for packaging and create a Dockerfile.

1. **Configure `pom.xml` for an executable JAR**: Add the `maven-assembly-plugin` to your `pom.xml`
   to create a "jar-with-dependencies" that includes all necessary libraries, and ensure the
   `mainClass` points to your bot's entry point.

   ```xml
   <build>
     <plugins>
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-assembly-plugin</artifactId>
         <version>3.3.0</version>
         <executions>
           <execution>
             <id>make-assembly</id>
             <phase>package</phase>
             <goals>
               <goal>single</goal>
             </goals>
             <configuration>
               <archive>
                 <manifest>
                   <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                   <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                   <mainClass>com.myorg.MyBotBootstrap</mainClass> <!-- YOUR BOT'S MAIN CLASS HERE -->
                 </manifest>
               </archive>
               <finalName>bot</finalName> <!-- The name of your executable JAR -->
               <appendAssemblyId>false</appendAssemblyId>
               <descriptorRefs>
                 <descriptorRef>jar-with-dependencies</descriptorRef>
               </descriptorRefs>
             </configuration>
           </execution>
         </executions>
       </plugin>
     </plugins>
   </build>
   ```

2. **Create a Dockerfile**: In your project's root directory, create a `Dockerfile` to build your
   bot's container image.

   ```dockerfile
   # Use a Java base image (adjust version as needed)
   FROM eclipse-temurin:21

   # Set the working directory inside the container
   WORKDIR /app

   # Copy the executable JAR from your Maven build output
   # Make sure 'bot.jar' matches the <finalName> in your pom.xml
   COPY target/bot.jar /app/bot.jar

   # Command to run your bot
   ENTRYPOINT ["java", "-jar", "bot.jar"]
   ```

3. **Build the Docker image**: Compile your project and then build the Docker image.

   ```bash
   # Compile your Maven project to generate the JAR
   mvn clean install

   # Build the Docker image (replace my-org/my-bot with your desired image tag)
   docker build -t my-org/my-bot .
   ```

   Ensure `my-org/my-bot` matches the image tag you'll use in your Botica environment configuration.

## Step 2: Running the Botica Director

The Botica Director is the orchestrator for your entire Botica environment. It will launch your bot
containers, set up the message broker, and manage inter-bot communication.

1. **Download the appropriate Director program** from
   the [releases page of the Botica repository](https://github.com/isa-group/botica/releases):
    * For Linux/macOS: Download the `botica-director` executable.
    * For Windows: Download the `botica-director.cmd` executable.
2. **Execute the Director**: Run the downloaded program from your terminal.

   ```bash
   # On Linux/macOS
   ./botica-director

   # On Windows
   botica-director.cmd
   ```

   The first time you run the Director in a directory, it will automatically create a default
   `environment.yml` file if one doesn't exist. This is your Botica environment configuration file.

## Step 3: Configure your environment and launch your bot

Now that your bot's Docker image is built, and you have the Botica Director, you need to tell the
Director about your bot.

1. **Open the `environment.yml` file**: This file defines your entire Botica
   environment.
2. **Add your bot's configuration**: In the `bots` section, define a bot type for your Java bot.
   Crucially, specify the `image` field with the Docker image tag you built in Step 1.

   Example `environment.yml` snippet:

   ```yaml
   bots:
     my_java_bot_type: # This is your bot type ID
       image: "my-org/my-bot" # **MUST MATCH YOUR DOCKER IMAGE TAG**
       replicas: 1 # Number of instances of this bot type to run
       subscribe:
         - key: "data_channel"
           strategy: distributed
   ```

3. **Run the Botica Director again**: With your `environment.yml` updated, run the Director. It will
   now create the necessary Docker containers, including your Java bot, and orchestrate their
   communication.

   ```bash
   # On Linux/macOS
   ./botica-director

   # On Windows
   botica-director.cmd
   ```

   Your Java bot will start inside its container, connect to the message broker, and begin executing
   its configured tasks or listening for orders.

[Back to documentation index](0-index.md)
