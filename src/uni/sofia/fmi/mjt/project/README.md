# Description of classes
* Client:
  > Represents actual user of the application that sends requests(commands) to the server.
* Command(record):
  > Represents actual command of the user that after convertions looks like HashMap<String, String>: key-parameter, value-parameter value.
* CommandElements:
  > String constants that describe parameters for the commands.
* Commands:
  > String constants that hold names for each available command for user.
* InputRules:
  > String constants that precisely explain syntax of every command, its parameters, what actions can provoke errors and some edge cases.
* CommandExecutor:
  > Actual class that executes every command after receiving buffer of bytes from the Server. That class stands as a local database,
  containing all current users, their tasks and colaborations. This class is backed-up after shutting down the Server and then become
  loaded on the next start of the application. Before executing command bunch of [syntax validations and logical checks](./validators) perform on the input command. 
* exceptions package:
  > Every class there represents special kind of [Runtime exception](https://docs.oracle.com/javase/8/docs/api/java/lang/RuntimeException.html) that
  this application uses.
* Server:
  > Class that is a server that firstly receives clients commands in byte buffer format. Is opens a socket that provides entire communication with clients.
  Except this, it's responsible from saving the entire info of the system BEFORE shutting down the the server(creating back-up) and the loading that info
  on the next start. Back-up without interruptions is guaranteed with [Java shutdown hook](https://docs.oracle.com/javase/8/docs/api/java/lang/Runtime.html#addShutdownHook-java.lang.Thread-)
  and is placed right in the class constructor.
  ```java
  Runtime.getRuntime().addShutdownHook(new Thread(Server::createBackUp));
  ```
* Collaboration:
  > Provides some utility functions on collections like <ins>Set&lt;String&gt; members</ins> to which we don't have direct access.
* Task:
  > Class that represents actual well-formed task with parameters: name, and optional parameters description, date, due-data.
  As soon as there are optional parameters, serialization of that object can't be standard. That's why functions
  ```java
  void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {...}
  void writeObject(ObjectOutputStream out) throws IOException {...}
  ```
  are overriden for this actual class(to prevent errors while writing/reading to/from file).
* User:
  > Class that represents data of the concrete user of the application. Its <ins>inbox, completed and scheduled</ins> tasks etc.
  Every update, completion or addition of new task is done through this class. Actual Task's instance is formed from the HashMap<String, String> params
  right there.
* CommandValidator:
  > Perform validations on users' commands. Check them logically(example: due-date need to be >= date), validate syntax. Checks whether task can be
  changed by certain way, whether user can be added to collaboration etc.  
* StringValidator:
  > Checks whether String instance is non-empty, non-null and non-blank.
  
