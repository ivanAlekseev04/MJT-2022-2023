package uni.sofia.fmi.mjt.project.commands;

import uni.sofia.fmi.mjt.project.exceptions.AlreadyExistedException;
import uni.sofia.fmi.mjt.project.exceptions.IllegalAccessException;
import uni.sofia.fmi.mjt.project.exceptions.InvalidParametersException;
import uni.sofia.fmi.mjt.project.exceptions.NotFoundException;
import uni.sofia.fmi.mjt.project.exceptions.StackTraceConverter;
import uni.sofia.fmi.mjt.project.user.Collaboration;
import uni.sofia.fmi.mjt.project.user.Task;
import uni.sofia.fmi.mjt.project.user.User;
import uni.sofia.fmi.mjt.project.validators.CommandValidator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DATE;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DESCRIPTION;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DUE_DATE;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_NAME;

public class CommandExecutor implements Serializable, CommandExecuterAPI {
    private Map<Integer, User> loginUsers;
    private Set<User> users;
    private Set<Collaboration> collaborations;
    private Map<String, Set<String>> userCollaborations;
    private static final String UNDEFINED_COMMAND_MESSAGE =
            "{There is no command <%s> with such name through available for users}";
    private static final int BUFFER_SIZE = 2048;
    private static final String LOGGER_FILE_NAME = "Logger.txt";
    private static final ByteBuffer TEMP_BUFFER = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final Object UNINITIALIZED = null;
    private static final String NEW_COMMAND_SYMBOL = "#";
    private static final String COMMAND_VALUE_DELIMITER = ":";
    private static final String EMPTY_COMMAND = "";

    public CommandExecutor() {
        if (loginUsers == UNINITIALIZED) {
            loginUsers = new HashMap<>();
        }
        if (users == UNINITIALIZED) {
            users = new HashSet<>();
        }
        if (collaborations == UNINITIALIZED) {
            collaborations = new HashSet<>();
        }
        if (userCollaborations == UNINITIALIZED) {
            userCollaborations = new HashMap<>();
        }
    }

    @Override
    public ByteBuffer execute(ByteBuffer buffer, int remotePort) {
        Command command = (Command) UNINITIALIZED;

        try {
            String message = getMessageFromBuffer(buffer);
            command = parseClientInput(message);

            putMessageToBuffer(buffer, String.format(UNDEFINED_COMMAND_MESSAGE, command.name()));
            CommandValidator.validateWithTemplate(command);

            return switch(command.name()) {
                case Commands.REGISTRATION -> processRegisterCommand(command, remotePort);
                case Commands.HELP -> putMessageToBuffer(buffer, InputRules.HELP_INFO);
                case Commands.LOGIN -> processLoginCommand(command, remotePort);
                case Commands.LOGOUT -> processLogoutCommand(remotePort);
                case Commands.HELP_TASK -> putMessageToBuffer(buffer, InputRules.HELP_TASK_INFO);
                case Commands.HELP_COLLABORATION -> putMessageToBuffer(buffer, InputRules.HELP_COLLABORATION_INFO);
                case Commands.ADD_TASK, Commands.UPDATE_TASK, Commands.DELETE_TASK,
                        Commands.GET_TASK, Commands.LIST_TASKS, Commands.FINISH_TASK
                        , Commands.LIST_DASHBOARD -> processTaskCommand(command, remotePort);
                case Commands.ADD_COLLABORATION, Commands.LIST_COLLABORATIONS
                        -> processCollaborationCommandWithoutMembership(command, remotePort);
                case Commands.DELETE_COLLABORATION, Commands.COLLABORATION_ADD_USER
                        , Commands.COLLABORATION_ADD_TASK, Commands.COLLABORATION_LIST_TASKS
                        , Commands.COLLABORATION_LIST_USERS
                        -> processCollaborationCommandWithMembership(command, remotePort);
                default -> buffer;
            };
        } catch (Throwable e) {
            writeErrorToFile(StackTraceConverter.getStackTrace(e, remotePort
                    , String.format("Command to be execute: <%s>", command.name())));

            return putMessageToBuffer(buffer, e.getMessage());
        }
    }

    private static void writeErrorToFile(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOGGER_FILE_NAME, true))) {
            writer.write(message);

            writer.flush();
        } catch (IOException e) {
            System.err.printf("{Can't write to the file <%s>\n<%s>\n}"
                    , LOGGER_FILE_NAME, e.getMessage());
        }
    }
    private Command parseClientInput(String input) {
        List<String> res = new ArrayList<>(Arrays.stream(input.split(NEW_COMMAND_SYMBOL))
                                                .map(String::strip)
                                                .distinct()
                                                .toList());
        String fCtion = EMPTY_COMMAND;

        if (!res.isEmpty()) {
            fCtion = res.get(0);
            res.remove(0);
        }

        Map<String, String> params = res.stream()
                .map(s -> s.split(COMMAND_VALUE_DELIMITER))
                .collect(Collectors
                        .toMap(a -> (a.length > 0) ? a[0] : EMPTY_COMMAND
                                , a -> (a.length > 1) ? a[1] : EMPTY_COMMAND));

        return new Command(fCtion, params);
    }
    private String getMessageFromBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];

        buffer.get(bytes);
        return new String(bytes);
    }
    private ByteBuffer processRegisterCommand(Command command, int remotePort) {
        User toAdd = new User(command.args().get(CommandElements.USER_NAME)
                , command.args().get(CommandElements.USER_PASSWORD));

        if (loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User can't perform register" +
                    " operations while is logged in. Please, logout and try again}");
        }

        if (users.contains(toAdd)) {
            throw new AlreadyExistedException(String.format("{User with name <%s> " +
                    "is already registered in system. Please, choose another name}", toAdd.getName()));
        }
        users.add(toAdd);

        System.out.printf("-> {new User <name:%s, password:%s> was added to system}\n"
                , toAdd.getName(), toAdd.getPassword());

        return putMessageToBuffer(TEMP_BUFFER, "{New User was added successfully." +
                "Now you can log in}\n");
    }
    private ByteBuffer putMessageToBuffer(ByteBuffer buffer, String message) {
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();

        return buffer;
    }
    private ByteBuffer processLoginCommand(Command command, int remotePort) {
        if (loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User is already logged in some account}");
        }

        User fromCommand = new User(command.args().get(CommandElements.USER_NAME)
                , command.args().get(CommandElements.USER_PASSWORD));

        if (!users.contains(fromCommand)) {
            throw new NotFoundException("{Undefined User account" +
                    ". In system there is no registered User named <" + fromCommand.getName() + "> }");
        }

        User toAdd = users.stream().filter(u -> u.getName().equals(fromCommand.getName()))
                .findFirst().get();

        if (!toAdd.getPassword().equals(fromCommand.getPassword())) {
            throw new InvalidParametersException("{Unable to log in. Wrong password. Please, try again}");
        }

        loginUsers.put(remotePort, toAdd);

        System.out.printf("-> {Client <%d> was logged in User account %s}\n", remotePort, toAdd);
        return putMessageToBuffer(TEMP_BUFFER
                , "{Successfully logged in. Now you can perform operations with your account}");
    }
    private ByteBuffer processLogoutCommand(int remotePort) {
        if (!loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User isn't logged in any account to be logged out}");
        }

        User logOut = loginUsers.remove(remotePort);
        System.out.printf("-> {Client <%d> was logged out from %s}\n", remotePort, logOut);

        return putMessageToBuffer(TEMP_BUFFER
                , "{Successfully logged out from the account}");
    }
    private ByteBuffer processTaskCommand(Command command, int remotePort) {
        if (!loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User isn't logged in any account to perform "
                    + command.name() + "}");
        }

        return loginUsers.get(remotePort).executeTaskCommand(command);
    }
    private ByteBuffer processCollaborationCommandWithoutMembership(Command command, int remotePort) {
        if (!loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User isn't logged in any account to perform "
                    + command.name() + "}");
        }

        return switch(command.name()) {
            case Commands.ADD_COLLABORATION -> processAddCollaboration(command, remotePort);
            case Commands.LIST_COLLABORATIONS -> processListCollaborations(remotePort);
            default -> null;
        };
    }
    private ByteBuffer processAddCollaboration(Command command, int remotePort) {
        String nameCollab = command.args().get(CommandElements.COLLABORATION_NAME);

        if (collaborations.contains(new Collaboration(nameCollab, null))) {
            throw new AlreadyExistedException(String.format("{Can't create new Collaboration" +
                    ". Already existed Collaboration named <%s>}"
                    , nameCollab));
        }

        String creator = loginUsers.get(remotePort).getName();

        collaborations.add(new Collaboration(nameCollab, creator));
        userCollaborations.putIfAbsent(creator, new HashSet<>());
        userCollaborations.get(creator).add(nameCollab);

        System.out.printf("-> {Successfully created Collaboration{name:<%s>, creator:<%s>} }\n",
                nameCollab, creator);

        return putMessageToBuffer(TEMP_BUFFER, "{Successfully created Collaboration}");
    }
    private ByteBuffer processListCollaborations(int remotePort) {
        String userName = loginUsers.get(remotePort).getName();

        if (!userCollaborations.containsKey(userName)) {
            return putMessageToBuffer(TEMP_BUFFER, Collections.emptyList().toString());
        }

        return putMessageToBuffer(TEMP_BUFFER, userCollaborations.get(userName).toString());
    }
    private ByteBuffer processCollaborationCommandWithMembership(Command command, int remotePort) {
        if (!loginUsers.containsKey(remotePort)) {
            throw new IllegalAccessException("{User isn't logged in any account to perform "
                    + command.name() + "}");
        }

        String nameCollab = command.args().get(CommandElements.COLLABORATION);

        if (!collaborations.contains(new Collaboration(nameCollab, null))) {
            throw new NotFoundException(String.format("{Collaboration named <%s> wasn't find}", nameCollab));
        }

        if (!getCollaboration(nameCollab).isMember(loginUsers.get(remotePort).getName())) {
            throw new IllegalAccessException(String.format("Logged User name <%s> isn't" +
                    " member of Collaboration named <%s>", loginUsers.get(remotePort).getName(), nameCollab));
        }

        return switch(command.name()) {
            case Commands.DELETE_COLLABORATION -> processDeleteCollaboration(command, remotePort);
            case Commands.COLLABORATION_ADD_USER -> processAddUser(command);
            case Commands.COLLABORATION_ADD_TASK -> processAddTaskCollaboration(command);
            case Commands.COLLABORATION_LIST_TASKS -> processListTasksCollaboration(command, remotePort);
            case Commands.COLLABORATION_LIST_USERS -> processListUsersCollaboration(command, remotePort);
            default -> null;
        };
    }
    private ByteBuffer processListUsersCollaboration(Command command, int remotePort) {
        String collabName = command.args().get(CommandElements.COLLABORATION);

        System.out.printf("-> {User names linked of this Collaboration <%s> were sent to <%s> }\n",
                collabName, loginUsers.get(remotePort).getName());

        return putMessageToBuffer(TEMP_BUFFER, getCollaboration(collabName).getMembers());
    }
    private ByteBuffer processListTasksCollaboration(Command command, int remotePort) {
        String collabName = command.args().get(CommandElements.COLLABORATION);
        String userName = loginUsers.get(remotePort).getName();

        System.out.printf("-> {Info of all tasks of Collaboration <%s> was sent to <%s> }\n",
                collabName, userName);

        return putMessageToBuffer(TEMP_BUFFER, getCollaboration(collabName).getTasks(userName));
    }
    private Collaboration getCollaboration(String collaborationName) {
        return collaborations.stream().filter(c -> c.getName().equals(collaborationName))
                .findFirst().get();
    }
    private ByteBuffer processDeleteCollaboration(Command command, int remotePort) {
        String nameCollab = command.args().get(CommandElements.COLLABORATION);

        if (!getCollaboration(nameCollab).getOwnerName().equals(loginUsers.get(remotePort).getName())) {
            throw new IllegalAccessException(String.format("Logged User named <%s> isn't" +
                    " owner of Collaboration named <%s>", loginUsers.get(remotePort).getName(), nameCollab));
        }

        collaborations.remove(new Collaboration(nameCollab, null));
        deleteCollaborationFromUsers(nameCollab);

        System.out.printf("-> {Collaboration named <%s> was deleted successfully by <%s> }\n"
                , nameCollab, loginUsers.get(remotePort).getName());
        return putMessageToBuffer(TEMP_BUFFER, "{Collaboration was deleted successfully by its owner}");
    }
    private ByteBuffer processAddUser(Command command) {
        String userName = command.args().get(CommandElements.USER_NAME);
        String collabName = command.args().get(CommandElements.COLLABORATION);

        if (!users.contains(new User(userName, null))) {
            throw new NotFoundException(String.format("{User <%s> can't be added to collaboration" +
                    " -> wasn't find through registered users }", command.args().get(CommandElements.USER_NAME)));
        }

        if (!collaborations.contains(new Collaboration(collabName, null))) {
            throw new NotFoundException(String.format("{Can't find collaboration <%s> }"
                    , command.args().get(CommandElements.COLLABORATION)));
        }

        Collaboration toCheck = getCollaboration(collabName);

        toCheck.addMember(userName);
        userCollaborations.putIfAbsent(userName, new HashSet<>());
        userCollaborations.get(userName).add(collabName);

        System.out.printf("-> {User <%s> was added to Collaboration <%s> }\n", userName, collabName);
        return putMessageToBuffer(TEMP_BUFFER, String.format("{Successfully added to Collaboration <%s> }"
                , collabName));
    }
    private void deleteCollaborationFromUsers(String nameCollab) {
        for (var user : users) {
            if (userCollaborations.containsKey(user.getName())) {
                userCollaborations.get(user.getName()).remove(nameCollab);
            }
        }
    }
    private ByteBuffer processAddTaskCollaboration(Command command) {
        String nameCollab = command.args().get(CommandElements.COLLABORATION);
        Optional<String> assignee = Optional.ofNullable(command.args().get(CommandElements.ASSIGNEE));

        if (assignee.isPresent() && !getCollaboration(nameCollab).isMember(assignee.get())) {
            throw new NotFoundException(String.format("{Assignee named <%s> wasn't find through" +
                    " members of Collaboration <%s> }", assignee.get(), nameCollab));
        }

        Task toAdd = createTaskFromParams(command.args());
        getCollaboration(nameCollab).addTask(assignee, toAdd);

        System.out.printf("-> {Task <%s> was added successfully to Collaboration <%s> }\n", toAdd, nameCollab);
        return putMessageToBuffer(TEMP_BUFFER, "{Task was added successfully to Collaboration}");
    }
    private Task createTaskFromParams(Map<String, String> params) {
        return new Task(params.get(TASK_NAME), (params.get(TASK_DATE) == UNINITIALIZED)
                ? null : LocalDate.parse(params.get(TASK_DATE)), (params.get(TASK_DUE_DATE) == UNINITIALIZED)
                ? null : LocalDate.parse(params.get(TASK_DUE_DATE)), params.get(TASK_DESCRIPTION));
    }
}