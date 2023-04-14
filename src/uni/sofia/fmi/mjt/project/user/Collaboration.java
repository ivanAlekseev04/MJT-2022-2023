package uni.sofia.fmi.mjt.project.user;

import uni.sofia.fmi.mjt.project.exceptions.AlreadyExistedException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Collaboration implements Serializable, CollaborationAPI {
    private String name;
    private String ownerName;
    private Map<String, Set<Task>> assigned;
    private Set<String> members;
    private Set<Task> shared;
    private static final Object UNINITIALIZED = null;

    public Collaboration(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;

        if (members == UNINITIALIZED) {
            members = new HashSet<>();
            members.add(ownerName);
        }
        if (shared == UNINITIALIZED) {
            shared = new HashSet<>();
        }
        if (assigned == UNINITIALIZED) {
            assigned = new HashMap<>();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Collaboration that = (Collaboration) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public boolean isMember(String userName) {
        return members.contains(userName);
    }

    @Override
    public void addMember(String userName) {
        if (isMember(userName)) {
            throw new AlreadyExistedException(String.format("{User with name <%s> is already" +
                    " a member of Collaboration <%s>}", userName, name));
        }

        members.add(userName);
    }

    @Override
    public boolean isShared(Task toCheck) {
        return shared.contains(toCheck);
    }

    @Override
    public void addTask(Optional<String> assignee, Task toAdd) {
        if (assignee.isEmpty() && isShared(toAdd)) {
            throw new AlreadyExistedException(String.format("{Task <%s> is already existed in this collaboration}"
                    , toAdd));
        }

        if (assignee.isPresent() && isAssigned(assignee.get(), toAdd)) {
            throw new AlreadyExistedException(String.format("{Task <%s> is already existed for this" +
                    " member <%s> of collaboration}", toAdd, assignee.get()));
        }

        assignee.ifPresent((e) -> assigned.putIfAbsent(e, new HashSet<>()));
        assignee.ifPresentOrElse((e) -> assigned.get(e).add(toAdd), () -> shared.add(toAdd));
    }

    @Override
    public boolean isAssigned(String userName, Task toCheck) {
        return assigned.containsKey(userName) && assigned.get(userName).contains(toCheck);
    }

    @Override
    public String getMembers() {
        return String.join("\n", members);
    }

    @Override
    public String getTasks(String userName) {
        String res = String.join("\n", shared.toString());

        if (assigned.containsKey(userName)) {
            return res + String.join("\n", assigned.get(userName).toString());
        }

        return res;
    }
}
