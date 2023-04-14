package uni.sofia.fmi.mjt.project.validators;

import uni.sofia.fmi.mjt.project.commands.Command;
import uni.sofia.fmi.mjt.project.commands.CommandElements;
import uni.sofia.fmi.mjt.project.commands.Commands;
import uni.sofia.fmi.mjt.project.exceptions.InvalidParametersException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class CommandValidator {
    private static final List<String> REGISTER = List.of(CommandElements.USER_NAME,
            CommandElements.USER_PASSWORD);
    private static final Set<String> ADD_TASK = Set.of(CommandElements.TASK_NAME, CommandElements.TASK_DATE,
            CommandElements.TASK_DUE_DATE, CommandElements.TASK_DESCRIPTION);
    private static final Set<String> UPDATE_TASK = Set.of(CommandElements.TASK_NAME, CommandElements.TASK_CUR_DATE,
            CommandElements.TASK_DATE, CommandElements.TASK_DUE_DATE, CommandElements.TASK_DESCRIPTION);
    private static final Set<String> ALLOWED_FOR_CHANGES = Set.of(CommandElements.TASK_DATE,
            CommandElements.TASK_DUE_DATE, CommandElements.TASK_DESCRIPTION);
    private static final Set<String> DELETE_TASK = Set.of(CommandElements.TASK_NAME, CommandElements.TASK_DATE);
    private static final Set<String> LIST_TASKS = Set.of(CommandElements.TASK_NAME);
    private static final Set<String> FINISH_TASK = DELETE_TASK;
    private static final List<String> ADD_COLLABORATION = List.of(CommandElements.COLLABORATION_NAME);
    private static final List<String> DELETE_COLLABORATION = List.of(CommandElements.COLLABORATION);
    private static final Set<String> ADD_USER_TO_COLLABORATION = Set.of(CommandElements.COLLABORATION
            , CommandElements.USER_NAME);
    private static final Set<String> ADD_TASK_COLLABORATION = Set.of(CommandElements.TASK_NAME
            , CommandElements.TASK_DATE, CommandElements.TASK_DUE_DATE, CommandElements.TASK_DESCRIPTION
            , CommandElements.COLLABORATION, CommandElements.ASSIGNEE);
    private static final int NO_PARAMETERS = 0;
    private static final Object NOT_FOUND = null;

    public static void validateWithTemplate(Command command) {
        switch (command.name()) {
            case Commands.REGISTRATION,
                    Commands.LOGIN -> checkRegistration(command);
            case Commands.HELP, Commands.HELP_TASK
                    , Commands.LIST_COLLABORATIONS, Commands.LOGOUT,
                    Commands.HELP_COLLABORATION, Commands.LIST_DASHBOARD
                        -> checkNoParameters(command);
            case Commands.ADD_TASK -> checkAddTask(command);
            case Commands.UPDATE_TASK -> checkUpdateTask(command);
            case Commands.DELETE_TASK, Commands.GET_TASK -> checkDeleteTask(command);
            case Commands.LIST_TASKS -> checkListTasks(command);
            case Commands.FINISH_TASK -> checkFinishTask(command);
            case Commands.ADD_COLLABORATION -> checkAddCollaboration(command);
            case Commands.DELETE_COLLABORATION, Commands.COLLABORATION_LIST_TASKS
                    , Commands.COLLABORATION_LIST_USERS -> checkDeleteCollaboration(command);
            case Commands.COLLABORATION_ADD_USER -> checkAddUserCollaboration(command);
            case Commands.COLLABORATION_ADD_TASK -> checkAddTaskCollaboration(command);
        }
    }

    private static void checkAddTaskCollaboration(Command command) {
        if (command.args().size() > ADD_TASK_COLLABORATION.size()) {
            throw new InvalidParametersException("{Count of parameters is insincere" +
                    ". Command must follow template: " +
                    "#collaboration:<text> #assignee:<user-name> #name:<task name> (opt)#date:<YYYY-MM-DD>\n" +
                    "(opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>}");
        }

        findAndCheckStringParameter(command, CommandElements.COLLABORATION);
        findAndCheckStringParameter(command, CommandElements.TASK_NAME);

        if (!ADD_TASK_COLLABORATION.containsAll(command.args().keySet())) {
            throw new InvalidParametersException("{Parameters' names need to be exactly the same" +
                    ", as followed: " + ADD_TASK_COLLABORATION + " to add the task to collaboration}");
        }

        tryParseDateDueDate(command.args());
        checkLogicallyDateDueDate(command.args());

        if (command.args().containsKey(CommandElements.ASSIGNEE)) {
            StringValidator.validate(command.args().get(CommandElements.ASSIGNEE), CommandElements.ASSIGNEE);
        }
    }
    private static void checkAddUserCollaboration(Command command) {
        if (command.args().size() > ADD_USER_TO_COLLABORATION.size()) {
            throw new InvalidParametersException("{Count of parameters is insincere" +
                    ". Command must follow template: " +
                    "#collaboration:<collaboration name> #name:<user-name>");
        }

        if (!command.args().keySet().containsAll(ADD_USER_TO_COLLABORATION)) {
            throw new InvalidParametersException("{Incompatible parameters for the command <add-user>" +
                    ". Need to follow template: #collaboration:<name> #name:<user-name> }");
        }

        command.args().keySet().forEach(c -> StringValidator.validate(command.args().get(c), c));
    }
    private static void checkAddCollaboration(Command command) {
        if (command.args().size() > ADD_COLLABORATION.size()) {
            throw new InvalidParametersException("{Count of parameters is insincere" +
                    ". Command must follow template: " +
                    "#name:<text>");
        }

        findAndCheckStringParameter(command, CommandElements.COLLABORATION_NAME);
    }
    private static void checkDeleteCollaboration(Command command) {
        if (command.args().size() > DELETE_COLLABORATION.size()) {
            throw new InvalidParametersException("{Count of parameters is insincere" +
                    ". Command must follow template: " +
                    "#collaboration:<text>");
        }

        findAndCheckStringParameter(command, CommandElements.COLLABORATION);
    }
    private static void checkFinishTask(Command command) {
        if (command.args().size() > FINISH_TASK.size()) {
            throw new InvalidParametersException("""
                    {Parameters count isn't sincere. 
                    Client's command need to follow template 
                     #name:<text> (opt)#date:<YYYY-MM-DD>
                    """);
        }

        findAndCheckStringParameter(command, CommandElements.TASK_NAME);

        if (!FINISH_TASK.containsAll(command.args().keySet())) {
            throw new InvalidParametersException("{All parameters need to have exactly" +
                    " the same names as stated in template #name:<text> (opt)#date:<YYYY-MM-DD>}");
        }

        tryParseDate(command.args());
    }
    private static void checkListTasks(Command command) {
        if (command.args().size() > LIST_TASKS.size()) {
            throw new InvalidParametersException("""
                    {Parameters count isn't sincere. 
                    Client's command need to follow template 
                     (opt)#date:<YYYY-MM-DD> OR (opt)#completed
                    """);
        }

        Set<String> possibleCommandVariations = Set.of(CommandElements.TASK_DATE
                , CommandElements.TASK_FLAG_COMPLETED);

        if (!possibleCommandVariations.containsAll(command.args().keySet())) {
            throw new InvalidParametersException("{Command and parameters need to follow" +
                    " one of 3 templates: <list-tasks> or <list-tasks #date<YYYY-MM-DD>>" +
                    " or <list-tasks #completed>}");
        }

        tryParseDate(command.args());
    }
    private static void checkDeleteTask(Command command) {
        if (command.args().size() > DELETE_TASK.size()) {
            throw new InvalidParametersException("""
                    {Parameters count isn't sincere. 
                    Client's command need to follow template 
                     #name:<text> (opt)#date:<YYYY-MM-DD>
                    """);
        }

        findAndCheckStringParameter(command, CommandElements.TASK_NAME);

        if (!DELETE_TASK.containsAll(command.args().keySet())) {
            throw new InvalidParametersException("{All parameters need to have exactly" +
                    " the same names as stated in template #name:<text> (opt)#date:<YYYY-MM-DD>}");
        }

        tryParseDate(command.args());
    }
    private static void checkRegistration(Command command) {
        if (command.args().size() != REGISTER.size()) {
            throw new InvalidParametersException("""
                    {Parameters count isn't sincere.
                    Client's command need to follow template
                    #name:<text> #password:<text>}""");
        }

        if (!command.args().keySet().containsAll(REGISTER)) {
            throw new InvalidParametersException("{All parameters need to have exactly" +
                    " the same names as stated in template #name:<text> #password:<text>}");
        }

        command.args().forEach((key, value) -> StringValidator.validate(value, key));
    }
    private static void checkAddTask(Command command) {
        if (command.args().size() > ADD_TASK.size()) {
            throw new InvalidParametersException("{Count of parameters is insincere" +
                    ". Command must follow template: " +
                    "#name:<text> (opt)#date:<YYYY-MM-DD> (opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>}");
        }

        findAndCheckStringParameter(command, CommandElements.TASK_NAME);

        if (!ADD_TASK.containsAll(command.args().keySet())) {
            throw new InvalidParametersException("{Parameters' names need to be exactly the same" +
                    ", as followed: " + ADD_TASK + " to add the task}");
        }

        tryParseDateDueDate(command.args());
        checkLogicallyDateDueDate(command.args());

        if (command.args().containsKey(CommandElements.TASK_DESCRIPTION)) {
            StringValidator.validate(command.args().get(CommandElements.TASK_DESCRIPTION)
                    , CommandElements.TASK_DESCRIPTION);
        }
    }
    private static void checkNoParameters(Command command) {
        if (command.args().size() != NO_PARAMETERS) {
            throw new InvalidParametersException(String.format("{Command <%s> can't have any parameters}"
                    , command.name()));
        }
    }
    private static void checkUpdateTask(Command command) {
        if (command.args().size() > UPDATE_TASK.size()) {
            throw new InvalidParametersException("""
                    {Count of parameters is insincere.
                    Command must follow template:
                    #name:<text> (opt)#cur-date:<YYYY-MM-DD> (opt)#date:<YYYY-MM-DD>
                    (opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>}""");
        }

        findAndCheckStringParameter(command, CommandElements.TASK_NAME);

        if (Collections.disjoint(command.args().keySet(), ALLOWED_FOR_CHANGES)) {
            throw new InvalidParametersException("{Among stated parameters there is no such, that can be changed." +
                    "Like " + ALLOWED_FOR_CHANGES + " }");
        }

        tryParseDateDueDate(command.args());
        checkLogicallyDateDueDate(command.args());

        if (command.args().containsKey(CommandElements.TASK_DESCRIPTION)) {
            StringValidator.validate(command.args().get(CommandElements.TASK_DESCRIPTION)
                    , CommandElements.TASK_DESCRIPTION);
        }
    }
    private static void findAndCheckStringParameter(Command command, String toFind) {
        if (!command.args().containsKey(toFind)) {
            throw new InvalidParametersException(String.format("{There is no stated parameter \"%s\" " +
                    "for this task, that is compulsory and can't be skipped}", toFind));
        }
        else {
            StringValidator.validate(command.args().get(toFind)
                    , "task " + CommandElements.TASK_NAME);
        }
    }
    private static void tryParseDateDueDate(Map<String, String> params) {
        try {
            parseDate(params);
            parseDueDate(params);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("{Date need to follow exactly the same format as: YYYY-MM-DD" +
                    "-> 2023-10-05}", e);
        }
    }
    private static void tryParseDate(Map<String, String> params) {
        try {
            parseDate(params);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("{Date need to follow exactly the same format as: YYYY-MM-DD" +
                    "-> 2023-10-05}", e);
        }
    }
    private static void checkLogicallyDateDueDate(Map<String, String> params) {
        Optional<LocalDate> parseDate = parseDate(params);
        Optional<LocalDate> parseDueDate = parseDueDate(params);

        if (parseDate.isPresent() && parseDate.get().isBefore(LocalDate.now())) {
            throw new DateTimeException("{Date to be executed for task need to be after or equal to current date}");
        }

        if (parseDueDate.isPresent() && parseDueDate.get().isBefore(LocalDate.now())) {
            throw new DateTimeException("{Due-Date till which task need to be executed" +
                    " must be after or equal to current date}");
        }

        if (parseDate.isPresent() && parseDueDate.isPresent()
                && parseDate.get().isAfter(parseDueDate.get())) {

            throw new DateTimeException("{Date to be executed-on can't be after" +
                    " due-date till which task need to done}");
        }
    }
    private static Optional<LocalDate> parseDate(Map<String, String> params) {
        return Optional.ofNullable(params.get(CommandElements.TASK_DATE) == NOT_FOUND
                ? null : LocalDate.parse(params.get(CommandElements.TASK_DATE)));
    }
    private static Optional<LocalDate> parseDueDate(Map<String, String> params) {
        return Optional.ofNullable(params.get(CommandElements.TASK_DUE_DATE) == NOT_FOUND
                ? null : LocalDate.parse(params.get(CommandElements.TASK_DUE_DATE)));
    }
}
