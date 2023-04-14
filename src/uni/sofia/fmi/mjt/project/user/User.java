package uni.sofia.fmi.mjt.project.user;

import uni.sofia.fmi.mjt.project.commands.Command;

import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_CUR_DATE;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_NAME;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DATE;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DUE_DATE;
import static uni.sofia.fmi.mjt.project.commands.CommandElements.TASK_DESCRIPTION;

import uni.sofia.fmi.mjt.project.commands.CommandElements;
import uni.sofia.fmi.mjt.project.commands.Commands;
import uni.sofia.fmi.mjt.project.exceptions.AlreadyExistedException;
import uni.sofia.fmi.mjt.project.exceptions.InvalidParametersException;
import uni.sofia.fmi.mjt.project.exceptions.NotFoundException;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class User implements Serializable, UserAPI {
    private final String name;
    private final String password;
    private Set<Task> inbox;
    private Map<LocalDate, Set<Task>> timed;
    private Set<Task> completed;
    private  static final int BUFFER_SIZE = 2048;
    private static final ByteBuffer BUFFER = ByteBuffer.allocate(BUFFER_SIZE);
    private static final Object NOT_FOUND = null;
    private static final int EMPTY = 0;

    public User(String name, String password) {
        this.name = name;
        this.password = password;

        if (inbox == NOT_FOUND) {
            inbox = new HashSet<>();
        }
        if (timed == NOT_FOUND) {
            timed = new HashMap<>();
        }
        if (completed == NOT_FOUND) {
            completed = new HashSet<>();
        }
    }

    @Override
    public ByteBuffer executeTaskCommand(Command command) {

        return switch (command.name()) {
            case Commands.ADD_TASK -> addTask(command);
            case Commands.UPDATE_TASK -> updateTask(command);
            case Commands.DELETE_TASK -> deleteTask(command);
            case Commands.GET_TASK -> getTask(command);
            case Commands.LIST_TASKS -> listTasks(command);
            case Commands.FINISH_TASK -> finishTask(command);
            case Commands.LIST_DASHBOARD -> listDashboard(command);
            default -> null;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;
        return name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", inbox=" + inbox +
                ", timed=" + timed +
                ", completed=" + completed +
                '}';
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    private ByteBuffer listDashboard(Command command) {
        System.out.printf("-> {<%d> tasks were sent to User successfully}\n",
                timed.containsKey(LocalDate.now()) ? timed.get(LocalDate.now()).size() : 0);

        if (!timed.containsKey(LocalDate.now())) {
            putMessageToBuffer(String.format("{For the date <%s> there is no tasks existed}", LocalDate.now()));
        } else {
            putMessageToBuffer(timed.get(LocalDate.now()).toString());
        }

        return BUFFER;
    }
    private ByteBuffer finishTask(Command command) {
        var parseDate = Optional.ofNullable(command.args().get(TASK_DATE) == NOT_FOUND ?
                null : LocalDate.parse(command.args().get(TASK_DATE)));

        validateExistenceOfTask(command, parseDate.orElse(null));
        Task toFinish = findTaskByParameters(command.args(), parseDate);

        if (completed.contains(toFinish)) {
            throw new AlreadyExistedException(String.format("{Task %s is already completed}"
                    , toFinish));
        } else {
            completed.add(toFinish);
        }

        System.out.printf("-> {Task %s became completed since now}\n", toFinish);
        putMessageToBuffer(String.format("{Task %s was finished successfully}\n", toFinish));

        return BUFFER;
    }
    private ByteBuffer listTasks(Command command) {
        if (command.args().isEmpty()) {
            putMessageToBuffer(inbox.isEmpty() ? "{No tasks existed}" : inbox.toString());
        }

        else if (command.args().containsKey(TASK_DATE)) {
            var date = parseToLocalDate(command.args().get(TASK_DATE));

            if (timed.containsKey(date)) {
                putMessageToBuffer(timed.get(date).toString());
            } else {
                putMessageToBuffer(String.format("{There is no tasks for the date <%s> }", date));
            }

        } else {
            putMessageToBuffer(completed.isEmpty() ? "{There is no completed tasks already}"
                    : completed.toString());
        }

        return BUFFER;
    }
    private ByteBuffer getTask(Command command) {
        var parseDate = Optional.ofNullable(command.args().get(TASK_DATE) != NOT_FOUND
                ? LocalDate.parse(command.args().get(TASK_DATE)) : null);

        validateExistenceOfTask(command, parseDate.orElse(null));

        Task toReturn = findTaskByParameters(command.args(), parseDate);
        System.out.println("-> {Successfully sent task " + toReturn + " }");
        putMessageToBuffer(toReturn.toString());

        return BUFFER;
    }
    private Task findTaskByParameters(Map<String, String> params, Optional<LocalDate> date) {
        return date.map(localDate -> timed.get(localDate).stream()
                .filter(t -> t.getName().equals(params.get(TASK_NAME)))
                .findFirst().get())
                .orElseGet(() -> inbox.stream()
                        .filter(t -> t.getName().equals(params.get(TASK_NAME)))
                        .findFirst().get());
    }
    private ByteBuffer deleteTask(Command command) {
        var parseDate = Optional.ofNullable(command.args().get(TASK_DATE) != NOT_FOUND
                ? LocalDate.parse(command.args().get(TASK_DATE)) : null);

        validateExistenceOfTask(command, parseDate.orElse(null));
        Task toDelete = findTaskByParameters(command.args(), parseDate);

        if (toDelete.getDate().isEmpty()) {
            inbox.remove(toDelete);
        } else {
            timed.get(toDelete.getDate().get()).remove(toDelete);
        }

        if (completed.contains(toDelete)) {
            completed.remove(toDelete);
        }

        System.out.printf("-> {Task <name:%s, date:%s> was deleted successfully}\n"
                , command.args().get(TASK_NAME), parseDate);
        putMessageToBuffer(String.format("{Task was deleted successfully from date <%s>}\n", parseDate));

        return BUFFER;
    }
    private void searchBetweenInboxTasks(Command command) {
        if (!inbox.contains(new Task(command.args().get(TASK_NAME)
                , null, null, null))) {

            throw new NotFoundException(String.format("{Task can't be find to perform <%s> ." +
                    "There is no task name <%s> in inbox}", command.name(), command.args().get(TASK_NAME)));
        }
    }
    private void searchBetweenTimedTask(Command command, LocalDate parseDate) {
        if (!timed.containsKey(parseDate)) {
            throw new NotFoundException(String.format("{There is no one task" +
                    " registered for <%s> date}", parseDate));
        }

        if (!timed.get(parseDate).contains(new Task(command.args().get(TASK_NAME),
                parseDate, null, null))) {

            throw new NotFoundException(String.format("{Task named <%s> wasn't found" +
                            " through tasks for the date <%s> }"
                    , command.args().get(TASK_NAME), parseDate));
        }
    }
    private void validateExistenceOfTask(Command command, LocalDate parseDate) {
        if (command.args().get(TASK_DATE) == NOT_FOUND) {
            searchBetweenInboxTasks(command);
        } else {
            searchBetweenTimedTask(command, parseDate);
        }
    }
    private ByteBuffer addTask(Command command) {
        Task toAdd = createTaskFromParams(command.args());

        checkWhetherAlreadyDefinedTask(toAdd);
        addTaskToCollection(toAdd);

        System.out.println("-> {Task " + toAdd + " was added successfully}");
        putMessageToBuffer("{Task was added successfully. Now you can perform actions with it}");

        return BUFFER;
    }
    private ByteBuffer updateTask(Command command) {
        Task toUpdate = createChangedTaskFromParams(command.args());

        var curDate = Optional.ofNullable(command.args().get(TASK_CUR_DATE) == null ?
                null : parseToLocalDate(command.args().get(TASK_CUR_DATE)));

        if (toUpdate.getDate().isPresent() && timed.containsKey(toUpdate.getDate().get())
            && timed.get(toUpdate.getDate().get()).contains(toUpdate)) {

            if (curDate.isEmpty() || !curDate.get().equals(toUpdate.getDate().get())) {
                throw  new AlreadyExistedException(String.format("{Date can't be changed" +
                        ". Tasks for stated new date already contain task" +
                        " with the name <%s> }", toUpdate.getName()));
            }
        }

        performUpdatingTask(toUpdate, curDate);
        synchronizeWithCompletedTasks(toUpdate, curDate);

        System.out.printf("-> {Task was successfully updated to <%s> }\n", toUpdate);
        putMessageToBuffer("{Task was successfully updated. Now you'll use updated parameters of it}");

        return BUFFER;
    }
    private void synchronizeWithCompletedTasks(Task toUpdate, Optional<LocalDate> curDate) {
        Task toDelete = new Task(toUpdate.getName(), curDate.orElse(null), null, null);

        if (completed.contains(toDelete)) {
            completed.remove(toDelete);
            completed.add(toUpdate);
        }
    }
    private void performUpdatingTask(Task toUpdate, Optional<LocalDate> curDate) {
        if (curDate.isEmpty()) {
            inbox.remove(new Task(toUpdate.getName(), null, null, null));
        } else {
            timed.get(curDate.get()).remove(new Task(toUpdate.getName(), curDate.get()
                    , null, null));
        }

        if (toUpdate.getDate().isEmpty()) {
            inbox.add(toUpdate);
        } else {
            timed.putIfAbsent(toUpdate.getDate().get(), new HashSet<>());
            timed.get(toUpdate.getDate().get()).add(toUpdate);
        }
    }
    private void checkWhetherAlreadyDefinedTask(Task toAdd) {
        if (toAdd.getDate().isPresent() && timed.containsKey(toAdd.getDate().get())
                && timed.get(toAdd.getDate().get()).contains(toAdd)) {

            throw new AlreadyExistedException(String.format("{On the date |%s| is already existed" +
                    " task with name <%s>}", toAdd.getDate().get(), toAdd.getName()));
        }
        else if (inbox.contains(toAdd)) {
            throw new AlreadyExistedException(String.format("{task name <%s> is already existed in" +
                    " inbox tasks}", toAdd.getName()));
        }
    }
    private void addTaskToCollection(Task toAdd) {
        if (toAdd.getDate().isPresent()) {
            timed.putIfAbsent(toAdd.getDate().get(), new HashSet<>());
            timed.get(toAdd.getDate().get()).add(toAdd);
        }
        else {
            inbox.add(toAdd);
        }
    }
    private void putMessageToBuffer(String message) {
        BUFFER.clear();
        BUFFER.put(message.getBytes());
        BUFFER.flip();
    }
    private Task createTaskFromParams(Map<String, String> params) {
        return new Task(params.get(TASK_NAME), (params.get(TASK_DATE) == NOT_FOUND)
                ? null : LocalDate.parse(params.get(TASK_DATE)), (params.get(TASK_DUE_DATE) == NOT_FOUND)
                ? null : LocalDate.parse(params.get(TASK_DUE_DATE)), params.get(TASK_DESCRIPTION));
    }
    private Task createChangedTaskFromParams(Map<String, String> params) {
        Optional<LocalDate> parseDate = Optional.ofNullable(params.get(CommandElements.TASK_DATE)
                == NOT_FOUND ? null : LocalDate.parse(params.get(CommandElements.TASK_DATE)));
        Optional<LocalDate> parseDueDate = Optional.ofNullable(params.get(CommandElements.TASK_DUE_DATE)
                == NOT_FOUND ? null : LocalDate.parse(params.get(CommandElements.TASK_DUE_DATE)));

        checkDateDueDate(params, parseDate, parseDueDate);

        return new Task(params.get(TASK_NAME), parseDate.orElseGet(() -> params.get(TASK_CUR_DATE)
                == NOT_FOUND ? null : parseToLocalDate(params.get(TASK_CUR_DATE))),
                parseDueDate.orElseGet(() -> getCurrentDueDate(params)),
                params.get(TASK_DESCRIPTION) == NOT_FOUND ? getCurrentDescription(params)
                : params.get(TASK_DESCRIPTION));
    }
    private LocalDate parseToLocalDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("{Date need to follow exactly the same format as: YYYY-MM-DD" +
                    "-> 2023-10-5}", e);
        }
    }
    private LocalDate getCurrentDueDate(Map<String, String> params) {
        Optional<LocalDate> curDate = Optional.ofNullable(params.get(TASK_CUR_DATE) == null ?
                null : parseToLocalDate(params.get(TASK_CUR_DATE)));

        if (curDate.isEmpty()) {
            return inbox.stream().filter(t -> t.getName()
                    .equals(params.get(TASK_NAME)))
                    .findFirst().get().getDueDate().orElse(null);
        }
        else {
            return timed.get(curDate.get()).stream().filter(t -> t.getName()
                    .equals(params.get(TASK_NAME)))
                    .findFirst().get().getDueDate().orElse(null);
        }
    }
    private String getCurrentDescription(Map<String, String> params) {
        Optional<LocalDate> curDate = Optional.ofNullable(params.get(TASK_CUR_DATE)
                == NOT_FOUND ? null : parseToLocalDate(params.get(TASK_CUR_DATE)));

        if (curDate.isEmpty()) {
            return inbox.stream().filter(t -> t.getName()
                            .equals(params.get(TASK_NAME)))
                    .findFirst().get().getDescription().orElse(null);
        }
        else {
            return timed.get(curDate.get()).stream().filter(t -> t.getName()
                            .equals(params.get(TASK_NAME)))
                    .findFirst().get().getDescription().orElse(null);
        }
    }
    private void tryToFindTaskThroughInboxWithNewDate(Map<String, String> params, Optional<LocalDate> parseDate) {
        if (!params.containsKey(CommandElements.TASK_CUR_DATE)) {
            if (!inbox.contains(new Task(params.get(TASK_NAME), null, null, null))) {
                throw new NotFoundException(String.format("{Task without date and named <%s>" +
                        " should be through inbox tasks}", params.get(TASK_NAME)));
            }
            else {
                var dueDate = inbox.stream().filter(t -> t.getName()
                                .equals(params.get(TASK_NAME)))
                        .findFirst().get()
                        .getDueDate();

                if (dueDate.isPresent() && dueDate.get().isBefore(parseDate.get())) {
                    throw new DateTimeException(String.format("{To update task new \"date\" need" +
                            " to be before or equal to \"due-date\" " +
                            "-> new-date: <%s>, due-date: <%s> }", parseDate.get(), dueDate.get()));
                }
            }
        }
    }
    private void tryToFindTaskThroughTimedWithNewDate(Map<String, String> params, Optional<LocalDate> parseDate) {
        LocalDate curDate = parseToLocalDate(params.get(TASK_CUR_DATE));

        if (!timed.containsKey(curDate)) {
            throw new NotFoundException("{There is no one task registered for the date "
                    + curDate + " }");
        }

        if (!timed.get(curDate).contains(new Task(params.get(TASK_NAME)
                , curDate, null, null))) {

            throw new NotFoundException(String.format("{Task with name <%s> wasn't found through" +
                    " tasks for the date <%s> }", params.get(TASK_NAME), curDate));
        }

        var dueDate = timed.get(curDate).stream().filter(t -> t.getName()
                        .equals(params.get(TASK_NAME)))
                .findFirst().get()
                .getDueDate();

        if (dueDate.isPresent() && dueDate.get().isBefore(parseDate.get())) {
            throw new DateTimeException(String.format("{To update task new \"date\" need" +
                    " to be before or equal to \"due-date\" " +
                    "-> new-date: <%s>, due-date: <%s> }", parseDate.get(), dueDate.get()));
        }
    }
    private void tryToFindTaskThroughInboxWithNewDueDate(Map<String, String> params) {
        if (!inbox.contains(new Task(params.get(TASK_NAME), null, null, null))) {
            throw new NotFoundException(String.format("{Task without date and named <%s>" +
                    " should be through inbox tasks}", params.get(TASK_NAME)));
        }
    }
    private void tryToFindTaskThroughTimedWithNewDueDate(Map<String, String> params, Optional<LocalDate> parseDueDate) {
        LocalDate curDate = parseToLocalDate(params.get(TASK_CUR_DATE));

        if (!timed.containsKey(curDate)) {
            throw new NotFoundException("{There is no one task registered for the date "
                    + curDate + " }");
        }

        if (!timed.get(curDate).contains(new Task(params.get(TASK_NAME)
                , curDate, null, null))) {

            throw new NotFoundException(String.format("{Task with name <%s> wasn't found through" +
                    " tasks for the date <%s> }", params.get(TASK_NAME), curDate));
        }

        if (parseDueDate.get().isBefore(curDate)) {
            throw new DateTimeException(String.format("{To update task \"date\" need" +
                    " to be before or equal to new \"due-date\" " +
                    "-> date: <%s>, new due-date: <%s> }", curDate, parseDueDate.get()));
        }
    }
    private void checkDateDueDate(Map<String, String> params, Optional<LocalDate> parseDate,
                                  Optional<LocalDate> parseDueDate) {

        if (parseDate.isPresent() && parseDueDate.isEmpty()) {
            if (!params.containsKey(TASK_CUR_DATE)) {
                tryToFindTaskThroughInboxWithNewDate(params, parseDate);
            }
            else {
                tryToFindTaskThroughTimedWithNewDate(params, parseDate);
            }
        }

        if (parseDate.isEmpty() && parseDueDate.isPresent()) {
            if (!params.containsKey(TASK_CUR_DATE)) {
                tryToFindTaskThroughInboxWithNewDueDate(params);
            }
            else {
                tryToFindTaskThroughTimedWithNewDueDate(params, parseDueDate);
            }
        }
    }
}
