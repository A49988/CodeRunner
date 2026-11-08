package coderunner.runserver;

import java.io.ByteArrayOutputStream;

public class RequestData {

    private final ByteArrayOutputStream program =
            new ByteArrayOutputStream();

    private final ByteArrayOutputStream stdin =
            new ByteArrayOutputStream();

    public void addProgram(byte[] data) {
        program.write(data, 0, data.length);
    }

    public void addStdin(byte[] data) {
        stdin.write(data, 0, data.length);
    }

    public byte[] getProgram() {
        return program.toByteArray();
    }

    public byte[] getStdin() {
        return stdin.toByteArray();
    }
}