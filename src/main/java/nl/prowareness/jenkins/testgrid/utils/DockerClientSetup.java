package nl.prowareness.jenkins.testgrid.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DockerClientSetup {
    private Runtime runtime;

    public DockerClientSetup(Runtime runtime) {

        this.runtime = runtime;
    }

    public TestResult testConnection() throws IOException, InterruptedException {
        Process p = runtime.exec("docker images");
        int retCode = p.waitFor();
        if (retCode != 0) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String output = "";
            while (true) {
                String line = reader.readLine();

                if (line == null) break;

                output += line;
            }
            if (output.contains("Permission denied")) {
                return TestResult.PERMISSION_DENIED;
            } else {
                return TestResult.OTHER_ERROR;
            }
        }
        return TestResult.OK;
    }

    public enum TestResult {
        OK,
        PERMISSION_DENIED,
        OTHER_ERROR
    }
}
