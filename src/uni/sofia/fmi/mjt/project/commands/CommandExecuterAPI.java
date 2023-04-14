package uni.sofia.fmi.mjt.project.commands;

import java.nio.ByteBuffer;
import uni.sofia.fmi.mjt.project.exceptions.AlreadyExistedException;
import uni.sofia.fmi.mjt.project.exceptions.InvalidParametersException;
import uni.sofia.fmi.mjt.project.exceptions.NotFoundException;

public interface CommandExecuterAPI {

    /**
     * Method that accepts a ByteBuffer with written command in it from Client. Then it parse command to Command
     * instance. After that for every possible cays of input the checks start. Passing the validation, command
     * can be executed
     *
     * @param buffer Collection, that keeps Client's command till it will be read by Server
     * @param remotePort unique port-identificator of every started session of the program
     * @return ByteBuffer instance that contains Server reply for sent Client's command
     * @throws IOException when Client was disconnected Server tries to read command from it and reverse
     * @throws AlreadyExistedException when Client try to add already existed account/task/collaboration
     * @throws IllegalArgumentException when String in null/empty/blank
     * @throws InvalidParametersException when count of parameters > maximum allowed for the command
     * or is less than minimum allowed for the command
     * @throws NotFoundException when Client instance wasn't find among registered Users
     */
    ByteBuffer execute(ByteBuffer buffer, int remotePort);
}
