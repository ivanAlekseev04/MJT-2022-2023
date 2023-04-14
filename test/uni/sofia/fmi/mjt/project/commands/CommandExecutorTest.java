package uni.sofia.fmi.mjt.project.commands;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uni.sofia.fmi.mjt.project.user.Task;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandExecutorTest {
    private static final int PORT1 = 1337;
    private static final int PORT2 = 5510;
    private static final int BUFFER_SIZE = 2048;
    private static final ByteBuffer BUFFER = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static ByteBuffer writeToBuffer(String command) {
        BUFFER.clear();
        BUFFER.put(command.getBytes());
        BUFFER.flip();

        return BUFFER;
    }
    private static String getServerResponse(ByteBuffer buffer) {
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        return new String(byteArray, StandardCharsets.UTF_8);
    }
    private CommandExecutor executer;
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        executer = new CommandExecutor();
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void cleanUp() {
        System.setOut(originalOut);
    }

    @Test
    void testRegisterUserWithRightParameters() {
        assertDoesNotThrow(() -> executer.execute(writeToBuffer("register #name:a #password:b"), PORT1)
                , "User need to be added succsessfully having right parameters");
    }

    @Test
    void testRegisterUserAlreadyRegisteredUser() {
        executer.execute(writeToBuffer("register #name:a #password:b"), PORT2);

        assertDoesNotThrow(() -> executer.execute(writeToBuffer("register #name:a #password:b"), PORT1)
                , "Error - user is already registered");
    }

    @Test
    void testRegisterUserFromLoggedInAccount() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> executer.execute(writeToBuffer("register #name:a #password:b"), PORT1)
                , "Error - can't perform registration. User is logged in another account");
    }

    @Test
    void testRegisterUserWithoutName() {
        assertDoesNotThrow(() -> executer.execute(writeToBuffer("register #name: #password:b"), PORT1)
                , "User can't be added succsessfully with empty/blank name parameter");
    }

    @Test
    void testRegisterUserWithoutPassword() {
        assertDoesNotThrow(() -> executer.execute(writeToBuffer("register #name:a #password:"), PORT1)
                , "User can't be added succsessfully with empty/blank password parameter");
    }

    @Test
    void testLoginWithoutAccountExisted() {
        assertDoesNotThrow(() -> executer.execute(writeToBuffer("login #name:a #password:b"), PORT1)
                , "User can't be logged in non-existed account");
    }

    @Test
    void testLoginWithExistedAccount() {
        assertDoesNotThrow(() -> executer.execute(writeToBuffer("login #name:a #password:b"), PORT1)
                , "User need to be logged in exited account with right parameters");
    }

    @Test
    void testLoginTwice() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> executer.execute(writeToBuffer("login #name:b #password:b"), PORT1)
                , "Error - user can't log in without being logged out");
    }

    @Test
    void testLoginWithWrongPass() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> executer.execute(writeToBuffer("login #name:b #password:c"), PORT1)
                , "Error - user can't log in without stating right password");
    }

    @Test
    void testGetHelpInfo() {
        assertEquals(getServerResponse(executer.execute(writeToBuffer("help"), PORT1)), InputRules.HELP_INFO
                , "Error - user need to receive right info from hardcoded help file");
    }

    @Test
    void testGetHelpTaskInfo() {
        assertEquals(getServerResponse(executer.execute(writeToBuffer("help-task"), PORT1))
                , InputRules.HELP_TASK_INFO
                , "Error - user need to receive right info from hardcoded help-task file");
    }

    @Test
    void testGetHelpCollaborationInfo() {
        assertEquals(getServerResponse(executer.execute(writeToBuffer("help-collaboration"), PORT1))
                , InputRules.HELP_COLLABORATION_INFO
                , "Error - user need to receive right info from hardcoded help-task file");
    }

    @Test
    void testLogoutFromLoggedInAccount() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("logout"), PORT1))
                , "Error - user need to be successfully logged out, because logging session isn't stopped already");
    }

    @Test
    void testAddTaskToUserCorrectlyWithoutDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-task #name:a"), PORT1))
                , "Error - task need to be added successfully with all right parameters");
    }

    @Test
    void testPerformTaskActionWithoutLoggingIn() {
        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-task #name:a"), PORT1))
                , "Error - task can't be added when user is logged out");
    }

    @Test
    void testAddTaskToUserCorrectlyWithDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1))
                , "Error - task need to be added successfully with all right parameters");
    }

    @Test
    void testAddTaskToUserAlreadyDefinedWithDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1))
                , "Error - task is already defined for this user");
    }

    @Test
    void testAddTaskToUserAlreadyDefinedWithoutDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-task #name:a"), PORT1))
                , "Error - task is already defined for this user");
    }

    @Test
    void testUpdateTaskNotExistedTaskWithDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("update-task #name:a #date:2023-03-01"), PORT1))
                , "Error - task not defined for this user");
    }

    @Test
    void testUpdateTaskWithUnParsableDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("update-task #name:a #cur-date:2023-3-01" +
                        " #due-date:2023-03-15"), PORT1)), "Error - wrong format of current date");
    }

    @Test
    void testUpdateTaskNotExistedTaskWithoutDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("update-task #name:a #description:test"), PORT1))
                , "Error - task not defined for this user");
    }

    @Test
    void testUpdateTaskWithCorrectParametersAndDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:Test #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer
                        .execute(writeToBuffer("update-task #name:a #cur-date:2023-03-01 #date:2023-03-02"), PORT1))
                , "Error - task need to be updated successfully since parameters are correct");
    }

    @Test
    void testUpdateTaskWithCorrectParameters() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:Test"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer
                        .execute(writeToBuffer("update-task #name:Test #date:2023-03-02 #description:test 2"), PORT1))
                , "Error - task need to be updated successfully since parameters are correct");
    }

    @Test
    void testUpdateTaskWithDateWithNewDueDateWithoutCurDueDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:Test #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer
                        .execute(writeToBuffer("update-task #name:Test #cur-date:2023-03-01 #due-date:2023-03-03"), PORT1))
                , "Error - task need to be updated successfully since parameters are correct");
    }

    @Test
    void testUpdateTaskWithDateWithNewDueDateWithCurrentDueDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:Test #date:2023-03-01 #due-date:2023-03-02"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer
                        .execute(writeToBuffer("update-task #name:Test #cur-date:2023-03-01 #due-date:2023-03-03"), PORT1))
                , "Error - task need to be updated successfully since parameters are correct");
    }

    @Test
    void testUpdateTaskWithoutDateWithNewDueDateWithCurrentDueDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:Test #due-date:2023-03-02"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer
                        .execute(writeToBuffer("update-task #name:Test #due-date:2023-03-03"), PORT1))
                , "Error - task need to be updated successfully since parameters are correct");
    }

    @Test
    void testUpdateTaskWithNewDateAfterCurrentDueDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01 #due-date:2023-03-02"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("update-task #name:a #cur-date:2023-03-01" +
            " #date:2023-03-03"), PORT1)), "Error - new date must be earlier or equal to current due-date");
    }

    @Test
    void testUpdateTaskWithNewDateCreatingDuplicateTasks() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-05"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("update-task #name:a #cur-date:2023-03-01 #date:2023-03-05")
                , PORT1)), "Error - updating the task creates duplicates in Set. Already existed Task with such name and date");
    }

    @Test
    void testDeleteUnExistedTask() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-task #name:a")
                , PORT1)), "Error - can't find task to delete it");
    }

    @Test
    void testDeleteTaskWithDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-task #name:a #date:2023-03-01")
                , PORT1)), "Error - task need to be deleted with all correct parameters stated");
    }

    @Test
    void testDeleteTaskWithoutDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-task #name:a")
                , PORT1)), "Error - task need to be deleted with all correct parameters stated");
    }

    @Test
    void testGetExistedTask() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("get-task #name:a")
                , PORT1)), "Error - task need to be sent to user since all stated parameters were correct");
    }

    @Test
    void testGetUnExistedTask() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("get-task #name:a")
                , PORT1)), "Error - task task can't be sent to user since it's not existed");
    }

    @Test
    void testListTasksInbox() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-tasks")
                , PORT1)), "Error - all tasks from inbox section must be sent to user");
    }

    @Test
    void testListTasksFromUnExistedDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-tasks #date:2023-03-01")
                , PORT1)), "Error - there is no tasks for the stated date. Nothing must be sent");
    }

    @Test
    void testListTasksFromExistedDate() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a #date:2023-03-01"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-tasks #date:2023-03-01")
                , PORT1)), "Error - all tasks from stated date must be sent to user");
    }

    @Test
    void testListTasksCompleted() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-task #name:a"), PORT1);
        executer.execute(writeToBuffer("finish-task #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-tasks #completed")
                , PORT1)), "Error - all completed tasks must be sent to user");
    }

    @Test
    void testListDashboardWithParameters() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-dashboard #name:a")
                , PORT1)), "Error - can't get tasks for today. Command shouldn't have any parameters");
    }

    @Test
    void testListDashboardCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-dashboard")
                , PORT1)), "Error - all tasks from today need to be sent to user");
    }

    @Test
    void testAddCollaborationWithoutLoggingIn() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-collaboration #name:Test")
                , PORT1)), "Error - without logging in user can't perform collaborations connected actions");
    }

    @Test
    void testAddCollaborationCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("add-collaboration #name:Test")
                , PORT1)), "Error - collaboration must be added successfully since all conditions are followed");
    }

    @Test
    void testListCollaborationsCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-collaborations")
                , PORT1)), "Error - all collaborations of stated user must be sent to him");
    }

    @Test
    void testDeleteCollaborationWithoutLoggingIn() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-collaboration #collaboration:Test")
                , PORT1)), "Error - without logging in user can't perform collaborations connected actions");
    }

    @Test
    void testDeleteUnExistedCollaboration() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-collaboration #collaboration:Test")
                , PORT1)), "Error - can't delete Collaboration that is not existed");
    }

    @Test
    void testDeleteCollaborationNotBeingItsMember() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("register #name:a #password:a"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);
        executer.execute(writeToBuffer("logout"), PORT1);
        executer.execute(writeToBuffer("login #name:a #password:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-collaboration #collaboration:Test")
                , PORT1)), "Error - user that is not member of Collaboration can't delete it");
    }

    @Test
    void testDeleteCollaborationNotBeingItsOwner() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("register #name:a #password:a"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);
        executer.execute(writeToBuffer("add-user #collaboration:Test #name:a"), PORT1);
        executer.execute(writeToBuffer("logout"), PORT1);
        executer.execute(writeToBuffer("login #name:a #password:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-collaboration #collaboration:Test")
                , PORT1)), "Error - only owner can delete collaboration");
    }

    @Test
    void testDeleteCollaborationCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("register #name:a #password:a"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);
        executer.execute(writeToBuffer("add-user #collaboration:Test #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("delete-collaboration #collaboration:Test")
                , PORT1)), "Error - collaboration must be deleted since attempt was made by its owner");
    }

    @Test
    void testCollaborationAddTaskCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("collaboration-add-task" +
                        " #collaboration:Test #name:Test task"), PORT1))
                , "Error - task need to be added successfully since all parameters are correct");
    }

    @Test
    void testCollaborationAddTaskWithAssigneeThatIsNotMember() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("register #name:a #password:a"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("collaboration-add-task" +
                        " #collaboration:Test #name:Test task #assignee:a"), PORT1))
                , "Error - task can't be added since stated assignee isn't even member of this collaboration");
    }

    @Test
    void testCollaborationListTasksCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);
        executer.execute(writeToBuffer("collaboration-add-task" +
                " #collaboration:Test #name:Test task #description:Test description"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("collaboration-list-tasks" +
                        " #collaboration:Test"), PORT1))
                , "Error - all tasks from this collaboration need to sent to user since he is its member");
    }

    @Test
    void testCollaborationListUsersCorrectly() {
        executer.execute(writeToBuffer("register #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("register #name:a #password:a"), PORT1);
        executer.execute(writeToBuffer("login #name:b #password:b"), PORT1);
        executer.execute(writeToBuffer("add-collaboration #name:Test"), PORT1);
        executer.execute(writeToBuffer("add-user #collaboration:Test #name:a"), PORT1);

        assertDoesNotThrow(() -> getServerResponse(executer.execute(writeToBuffer("list-users" +
                        " #collaboration:Test"), PORT1))
                , "Error - all user names from this collaboration need to sent to user since he is its member");
    }

    @Test
    void testWriteAndReadObjectWithOptionalMembersToFile() throws IOException, ClassNotFoundException {
        Task toWrite = new Task("Stoyo must go on", null, LocalDate.parse("5000-01-01")
                , "best course of Java");

        Path pathToBackUp = Files.createTempFile("ForTest", ".ser");

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(pathToBackUp.toString()));
        out.writeObject(toWrite);
        out.flush();
        out.close();

        ObjectInputStream in = new ObjectInputStream(new FileInputStream(pathToBackUp.toString()));

        assertEquals(toWrite, (Task) in.readObject(), "Error - objects must be equal" +
                " since it was written and read correctly");
    }
}
