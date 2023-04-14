package uni.sofia.fmi.mjt.project.exceptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public abstract class StackTraceConverter {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static String getStackTrace(Throwable e) {
        StringBuilder b = new StringBuilder();
        b.append("{").append(LocalDateTime.now().format(formatter))
                .append("}").append(System.lineSeparator()).append(e.getMessage()).append(System.lineSeparator());
        Arrays.stream(e.getStackTrace()).forEach(v -> b.append(v.toString()).append('\n'));
        b.append(System.lineSeparator());

        return b.toString();
    }
    public static String getStackTrace(Throwable e, int localPort, String additionalMessage) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("""
            {<%s>} -> User localPort <%s>
            <%s>
            <%s>
                """, LocalDateTime.now().format(formatter), (localPort == 0) ? "undefined" : localPort
                , e.getMessage(), additionalMessage));

        Arrays.stream(e.getStackTrace()).forEach(v -> b.append(v.toString()).append(System.lineSeparator()));
        b.append(System.lineSeparator());

        return b.toString();
    }
}
