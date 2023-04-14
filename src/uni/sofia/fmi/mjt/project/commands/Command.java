package uni.sofia.fmi.mjt.project.commands;

import java.util.Map;

public record Command(String name, Map<String, String> args) { }