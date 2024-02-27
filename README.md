# <a href="https://github.com/fmi/java-course/blob/mjt-2022-2023/course-projects/todoist.md" targer="_blank">Todoist: MJT 2022-2023 course project</a>

## Agenda: 
```
src/uni/sofia/fmi/mjt/project - source code.
test/uni/sofia/fmi/mjt/project/commands - unit-tests provided by JUNIT 5.0.
```

## Conception:
* Console client-server application that sends clients' requests(commands) to the server(localhost), that processes them
and returns result of the command to every client. In case of incorrect syntax/parameters for the command, server will
return error message to the cliend and log with details for each client.
* Simulating functionality similar to the [Todoist](https://todoist.com/?locale=en)

## Non-functional requirements:
* Server need to work("talk") with multiple clients at the same time, e.g. to work in parallel(that was implemented by using
Java non-blocking IO from package [java.nio](https://docs.oracle.com/javase/8/docs/api/java/nio/package-summary.html)).
* After shutting down the server, the entire info need to be saved in a proper way to be loaded successfully at the next server launch.
<ins>Without using any database</ins> **for this specific case** (that was achieved by implementing [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)
interface and writing CommandExecuter object to the **.ser** file).

## Logging error messages:
* In case of incorrect usage of the app by client, proper error need to be sent by server to him and technical info with stack-traces
need to be logged to the concrete file(**.txt** file for simplicity).
> Logged error contains: local date-time, port of the client who provoked error, simple explanation of the error that also sends to the client,
> command from which error message was thrown and detailed stack-trace.   
* Example of logged error situations:
```java
{<2024-02-27 11:02:48>} -> User localPort <50933>
<{All parameters need to have exactly the same names as stated in template #name:<text> #password:<text>}>
<Command to be execute: <login>>
uni.sofia.fmi.mjt.project.validators.CommandValidator.checkRegistration(CommandValidator.java:180)
uni.sofia.fmi.mjt.project.validators.CommandValidator.validateWithTemplate(CommandValidator.java:42)
uni.sofia.fmi.mjt.project.commands.CommandExecutor.execute(CommandExecutor.java:74)
uni.sofia.fmi.mjt.project.server.Server.main(Server.java:80)

{<2024-02-27 11:05:36>} -> User localPort <50943>
<{User with name <Ivan> is already registered in system. Please, choose another name}>
<Command to be execute: <register>>
uni.sofia.fmi.mjt.project.commands.CommandExecutor.processRegisterCommand(CommandExecutor.java:148)
uni.sofia.fmi.mjt.project.commands.CommandExecutor.execute(CommandExecutor.java:77)
uni.sofia.fmi.mjt.project.server.Server.main(Server.java:80)
```

### Detailed explanation of syntax/errors and available commands you can find in [src](https://github.com/ivanAlekseev04/MJT-2022-2023/tree/main/src/uni/sofia/fmi/mjt/project) folder.
