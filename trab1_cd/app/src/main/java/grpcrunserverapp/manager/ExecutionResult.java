
package grpcrunserverapp.manager;

public class ExecutionResult {

    private final String state;
    private final String stdout;
    private final String stderr;
    private final int exitCode;

    public ExecutionResult(
            String state,
            String stdout,
            String stderr,
            int exitCode) {

        this.state = state;
        this.stdout = stdout;
        this.stderr = stderr;
        this.exitCode = exitCode;
    }

    public String getState() {
        return state;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }
}
