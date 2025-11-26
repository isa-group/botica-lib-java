# Botica Java Library Documentation

Welcome to the comprehensive documentation for the `botica-lib-java` library. This guide provides
detailed information on developing powerful, scalable bots for the Botica multi-agent platform using
Java.

## Core Concepts and API Reference

These documents delve into the primary features and API of the `botica-lib-java` library, guiding
you through the implementation of various bot behaviors.

1. **[Listening to orders from other bots](1-listening-to-orders.md)**: Learn how to make your bots
   reactive by defining handlers for incoming orders.
2. **[Proactive bots](2-proactive-bots.md)**: Discover how to implement bots that initiate tasks
   automatically on a schedule.
3. **[Publishing orders](3-publishing-orders.md)**: Learn how to send orders to other bots.
4. **[Bot lifecycle](4-bot-lifecycle.md)**: Understand the complete lifecycle of a Botica Java bot
   and the related events like `configure()` and `onStart()`.
5. **[Handling shutdown requests from the Director](5-handling-shutdown-requests.md)**: Learn how to
   manage bot termination gracefully, performing cleanup and potentially delaying shutdown.
6. **[Accessing environment and utilities](6-accessing-environment-and-utilities.md)**: Find out how
   to retrieve essential runtime information (bot ID, hostname) and utilize shared resources like
   the shared directory.
7. **[Payload serializers and deserializers](7-payload-serializers-and-deserializers.md)**:
   Understand how Botica automatically converts Java objects to string payloads and back, and how to
   implement custom serializers and deserializers for complex data formats.

## Botica Platform Documentation

For a deeper understanding of the overall Botica platform, including concepts like the environment
configuration file, message routing strategies, and inter-bot communication paradigms, please refer
to the main Botica documentation:


- **[The concept of a bot](https://github.com/isa-group/botica/blob/main/docs/1-the-concept-of-a-bot.md)**
- **[Creating process chains](https://github.com/isa-group/botica/blob/main/docs/2-process-chains.md)**
- **[Messaging between bots](https://github.com/isa-group/botica/blob/main/docs/3-messaging-between-bots.md)**
- **[Sharing files between bots](https://github.com/isa-group/botica/blob/main/docs/4-sharing-files-between-bots.md)**
- **[The infrastructure configuration file](https://github.com/isa-group/botica/blob/main/docs/the-infrastructure-configuration-file.md)**

## Example Projects

Explore these real-world and demonstrative projects built with `botica-lib-java` to see the concepts
in action.

- **[Botica Fishbowl](https://github.com/isa-group/botica-infrastructure-fishbowl)**:
  A simulation of a 9x9 fishbowl where multiple fish bots move around and a manager bot tracks their
  positions. This project showcases proactive (fish) and reactive (manager) bots written in both
  Java and Node.js, demonstrating interlanguage communication and file system interaction.
    - [Java Fish Bot](https://github.com/isa-group/botica-bot-fishbowl-fish-java)
    - [Java Manager Bot](https://github.com/isa-group/botica-bot-fishbowl-manager)


- **[Automatic REST API testing system with RESTest](https://github.com/isa-group/botica-infrastructure-restest)**:
  A real-world application automating REST API testing. Generator bots create test cases,
  executor bots run them, and reporter bots analyze results, demonstrating distributed processing
  and complex workflow orchestration using various Java bots.
    - [RESTest Generator Bot](https://github.com/isa-group/botica-bot-restest-generator)
    - [RESTest Executor Bot](https://github.com/isa-group/botica-bot-restest-executor)
    - [RESTest Reporter Bot](https://github.com/isa-group/botica-bot-restest-reporter)
