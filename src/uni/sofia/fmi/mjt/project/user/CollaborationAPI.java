package uni.sofia.fmi.mjt.project.user;

import java.util.Optional;

public interface CollaborationAPI {

    /**
     * Checks whether stated userName is existed among members for this Collaboration
     *
     * @param userName name of User to find
     * @return boolean result, whether existed or not
     */
    boolean isMember(String userName);

    /**
     * Add member to members of this Collaboration
     *
     * @param userName name of User to be added
     */
    void addMember(String userName);

    /**
     * Checks, whether stated Task instance belongs to SharedTasks - common for all members of this Collaboration
     *
     * @param toCheck Task instance to be find
     * @return boolean result, whether is "shared" or "assigned" to concrete User
     */
    boolean isShared(Task toCheck);

    /**
     * Add task to this Collaboration that can be specified as "assigned",
     * if Optional parameter assignee exists or as "shared", if not
     *
     * @param assignee name of User new task to be assigned to
     * @param toAdd Task instance to be added to this Collaboration
     */
    void addTask(Optional<String> assignee, Task toAdd);

    /**
     * Checks whether stated Task instance is assigned to stated userName of some User
     *
     * @param userName name of User
     * @param toCheck Task instance
     * @return boolean result, whether Task is "assigned" or "shared"
     */
    boolean isAssigned(String userName, Task toCheck);

    /**
     * Get all Usernames of members of this Collaboration
     *
     * @return all names of members concatenated to one String
     */
    String getMembers();

    /**
     * Get all tasks of this Collaboration that are "shared"
     * and add all "assigned" to this userName if exist
     *
     * @param userName name of user member
     * @return all tasks, that can be accessed by stated User concatenated to one String
     */
    String getTasks(String userName);
}
