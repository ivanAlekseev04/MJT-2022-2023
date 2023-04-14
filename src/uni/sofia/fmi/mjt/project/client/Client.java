package uni.sofia.fmi.mjt.project.client;

import uni.sofia.fmi.mjt.project.exceptions.StackTraceConverter;

import static uni.sofia.fmi.mjt.project.validators.StringValidator.validate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 1337;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 2048;
    private static final String LOGGER_FILE_NAME = "Logger.txt";
    private static Writer writer;
    private static ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final String TERMINAL_MESSAGE = "terminate";
    private static final String WRONG_FORMAT_INPUT_MESSAGE = """
            {Command sent by client need to have at least one 
            character excluding (\" \") - whitespace character. 
            Please, follow the input format. Type \"help\" for more information} """;
    private static final String DISCONNECTION_FROM_THE_SERVER_MESSAGE = """
            {Server is not responding/shutted down at the moment.
            Please, restart the program or contact the service provider} """;

    public static void main(String[] args) {
        int localPort = 0;

        try (SocketChannel socketChannel = SocketChannel.open();
             Scanner scanner = new Scanner(System.in)) {

            socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            localPort = socketChannel.socket().getLocalPort();

            System.out.println("<Successfully connected to the server>");
            System.out.println("-> Type \"help\" for getting more information of allowed commands/format\n");

            while (true) {
                System.out.print("> ");
                String command = scanner.nextLine();

                try {
                    validate(command, "command");
                } catch (IllegalArgumentException e) {
                    writeErrorToFile(StackTraceConverter
                            .getStackTrace(e, localPort, WRONG_FORMAT_INPUT_MESSAGE));

                    System.err.println(WRONG_FORMAT_INPUT_MESSAGE + '\n');
                    continue;
                }

                if (command.equals(TERMINAL_MESSAGE)) {
                    break;
                }

                writeMessageToBuffer(command, buffer);
                writeBufferToSocketChannel(buffer, socketChannel);

                readFromSocketChannelToBuffer(buffer, socketChannel);

                System.out.printf("Server reply: %s\n\n", getServerResponse(buffer));
            }

        } catch (IOException e) {
            writeErrorToFile(StackTraceConverter
                    .getStackTrace(e, localPort, DISCONNECTION_FROM_THE_SERVER_MESSAGE));

            System.err.println(DISCONNECTION_FROM_THE_SERVER_MESSAGE);
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
    private static void writeMessageToBuffer(String message, ByteBuffer buffer) {
        buffer.clear();
        buffer.put(message.getBytes());
    }
    private static void writeBufferToSocketChannel(ByteBuffer buffer, SocketChannel socketChannel)
            throws IOException {

        buffer.flip();
        socketChannel.write(buffer);
    }
    private static void readFromSocketChannelToBuffer(ByteBuffer buffer, SocketChannel socketChannel)
            throws IOException {

        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
    }
    private static String getServerResponse(ByteBuffer buffer)
            throws UnsupportedEncodingException {

        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);

        return new String(byteArray, StandardCharsets.UTF_8);
    }
}
