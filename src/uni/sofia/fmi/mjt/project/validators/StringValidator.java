package uni.sofia.fmi.mjt.project.validators;

public abstract class StringValidator {
    public static void validate(String toCheck, String varName) {
        if (toCheck == null || toCheck.isEmpty() || toCheck.isBlank()) {
            throw new IllegalArgumentException(String.format("{ %s } can't be null/empty(length = 0)" +
                    "/blank(consists of white-spaces)", varName));
        }
    }
}
