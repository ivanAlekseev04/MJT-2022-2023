package uni.sofia.fmi.mjt.project.user;

import uni.sofia.fmi.mjt.project.exceptions.AlreadyExistedException;
import uni.sofia.fmi.mjt.project.exceptions.InvalidParametersException;
import uni.sofia.fmi.mjt.project.exceptions.NotFoundException;
import uni.sofia.fmi.mjt.project.commands.Command;

import java.nio.ByteBuffer;

public interface UserAPI {

    /**
     * Accept Client Command. Later method find-out, what type
     * of command connected with tasks client wants to execute.
     * Validate all parameters and proceed {add-task, update-task, delete-task,
     * get-task, list-tasks, finish-task, list-dashboard}
     *
     * @param command Command instance to be analyzed
     * @return ByteBuffer with answer to Server to be than sent to Client
     * @param buffer Collection, that keeps Client's command till it will be read by Server
     * @param remotePort unique port-identificator of every started session of the program
     * @return ByteBuffer instance that contains Server reply for sent Client's command
     * @throws IOException when Client was disconnected Server tries to read command from it and reverse
     * @throws AlreadyExistedException when Client try to add already existed account/task/collaboration
     * @throws IllegalArgumentException when String in null/empty/blank
     * @throws InvalidParametersException when count of parameters > maximum allowed for the command
     * or is less than minimum allowed for the command
     * @throws NotFoundException when Client instance wasn't find among registered Users
     * @throws IllegalAccessException when Client tries to delete something without being owner of it
     */
    ByteBuffer executeTaskCommand(Command command);
}
