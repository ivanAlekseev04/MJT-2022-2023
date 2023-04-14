package uni.sofia.fmi.mjt.project.server;

import uni.sofia.fmi.mjt.project.commands.CommandExecutor;
import uni.sofia.fmi.mjt.project.exceptions.StackTraceConverter;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Server {
    private static final int SERVER_PORT = 1337;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 2048;
    private static final int NO_CHANNELS_READY = 0;
    private static final String LOGGER_FILE_NAME = "Logger.txt";
    private static final String PATH_TO_BACKUP = "BackUp.ser";
    private static ByteBuffer buffer;
    private static SelectionKey key;
    private static CommandExecutor executer;
    private static final int MIN_BYTES_COUNT = 0;
    private static final int UNDEFINED_PORT = 0;
    private static SocketChannel sc;
    private static final long TIME_TO_WAIT = TimeUnit.MINUTES.toMillis(3);
    private static final String UNHANDLED_COMMAND_MESSAGE = """
            {Client <%s> -> Processing client's command had caused an error.
            The process was interrupted or the client session is already terminated}""";
    private static final String SERVER_SOCKET_TERMINATION_MESSAGE = """
            {There is a problem with the server socket. Server need to be reloaded}""";
    private static final Object UNINITIALIZED = null;

    public Server() {
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        if (executer == UNINITIALIZED) {
            executer = new CommandExecutor();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(Server::createBackUp));
    }
    public static void main(String[] args) {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            Selector selector = Selector.open();

            backUpFromTheFile();
            Server s = new Server();
            configureServerSocket(serverSocketChannel, selector);

            while (true) {
                try {
                    int readyChannels = selector.select(TIME_TO_WAIT);

                    if (readyChannels == NO_CHANNELS_READY) {
                        break;
                    }

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                    while (keyIterator.hasNext()) {
                        key = keyIterator.next();

                        if (key.isReadable()) {
                            sc = (SocketChannel) key.channel();

                            if (writeToBuffer(sc, buffer)) {
                                sc.write(executer.execute(buffer, sc.socket().getPort()));
                            }
                        } else if (key.isAcceptable()) {
                            acceptKey(key, selector);
                        }

                        keyIterator.remove();
                    }
                } catch (IOException e) {
                    writeErrorToFile(StackTraceConverter.getStackTrace(e, sc.socket().getPort(), String
                            .format(UNHANDLED_COMMAND_MESSAGE, sc.socket().getPort())));

                    System.err.printf(UNHANDLED_COMMAND_MESSAGE + System.lineSeparator(), sc.socket().getPort());
                    key.cancel();
                }
            }

        } catch (IOException e) {
            writeErrorToFile(StackTraceConverter.getStackTrace(e, sc.socket().getPort()
                    , SERVER_SOCKET_TERMINATION_MESSAGE));

            System.err.println(SERVER_SOCKET_TERMINATION_MESSAGE);
        }
    }

    private static void backUpFromTheFile() {
        if (Files.exists(Path.of(PATH_TO_BACKUP))) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(PATH_TO_BACKUP))) {
                executer = (CommandExecutor) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                writeErrorToFile(StackTraceConverter
                        .getStackTrace(e, UNDEFINED_PORT
                                , String.format("{Can't backUp from the file <%s> . Some error occurred}"
                                        , PATH_TO_BACKUP)));

                System.err.println("-> {Can't create backUp file. Some error occurred}");
            }
        }
    }
    private static void createBackUp() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(PATH_TO_BACKUP))) {
            out.writeObject(executer);

            out.flush();
        } catch (IOException e) {
            writeErrorToFile(StackTraceConverter
                    .getStackTrace(e, sc.socket().getPort()
                            , String.format("{Can't create backUp file <%s> . Some error occurred}", PATH_TO_BACKUP)));

            System.err.println("-> {Can't create backUp file. Some error occurred}");
        }
    }
    private static void writeErrorToFile(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOGGER_FILE_NAME, true))) {
            writer.write(message);

            writer.flush();
        } catch (IOException e) {
            System.err.printf("{Can't write to the file <%s>\n<%s>\n}"
                    , LOGGER_FILE_NAME, e.getMessage());
        }
    }
    private static void configureServerSocket(ServerSocketChannel serverSocketChannel, Selector selector)
            throws IOException {

        serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    private static boolean writeToBuffer(SocketChannel sc, ByteBuffer buffer) throws IOException {
        buffer.clear();
        int readBytes = sc.read(buffer);

        if (readBytes < MIN_BYTES_COUNT) {
            sc.close();

            return false;
        }

        buffer.flip();

        return true;
    }
    private static void acceptKey(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel tempChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = tempChannel.accept();

        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);
    }
}
