package uni.sofia.fmi.mjt.project.user;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class Task implements Serializable {
    private String name;
    private Optional<LocalDate> date;
    private Optional<LocalDate> dueDate;
    private Optional<String> description;

    public Task(String name, LocalDate date, LocalDate dueDate, String description) {
        this.name = name;

        this.date = Optional.ofNullable(date);
        this.dueDate = Optional.ofNullable(dueDate);
        this.description = Optional.ofNullable(description);
    }

    public String getName() {
        return name;
    }

    public Optional<LocalDate> getDate() {
        return date;
    }

    public Optional<LocalDate> getDueDate() {
        return dueDate;
    }

    public Optional<String> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Task{" +
                "name='" + name + '\'' +
                ", date=" + (date.isEmpty() ? " " : date.toString()) +
                ", dueDate=" + (dueDate.isEmpty() ? " " : dueDate.toString()) +
                ", description=" + (description.orElse(" ")) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Task task = (Task) o;

        return name.equals(task.name) && Objects.equals(date, task.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, date);
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        name = in.readUTF();

        boolean datePresent = in.readBoolean();
        date = (datePresent) ? Optional.of(LocalDate.parse(in.readUTF())) : Optional.empty();

        boolean dueDatePresent = in.readBoolean();
        dueDate = (dueDatePresent) ? Optional.of(LocalDate.parse(in.readUTF())) : Optional.empty();

        boolean descriptionPresent = in.readBoolean();
        description = (descriptionPresent) ? Optional.of(in.readUTF()) : Optional.empty();
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(name);

        out.writeBoolean(date.isPresent());
        date.ifPresent(str -> {
            try {
                out.writeUTF(str.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        out.writeBoolean(dueDate.isPresent());
        dueDate.ifPresent(str -> {
            try {
                out.writeUTF(str.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        out.writeBoolean(description.isPresent());
        description.ifPresent(str -> {
            try {
                out.writeUTF(str);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
