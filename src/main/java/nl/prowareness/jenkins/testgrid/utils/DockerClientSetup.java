/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Harm Pauw <h.pauw@prowareness.nl>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nl.prowareness.jenkins.testgrid.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DockerClientSetup {
    private static final String DOCKER_IMAGES_COMMAND = "docker images";
    private static final String PERMISSION_DENIED = "Permission denied";
    private final Runtime runtime;

    public DockerClientSetup(Runtime runtime) {

        this.runtime = runtime;
    }

    public TestResult testConnection() throws IOException, InterruptedException {
        Process p = runtime.exec(DOCKER_IMAGES_COMMAND);
        int retCode = p.waitFor();
        if (retCode != 0) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String output = "";
            while (true) {
                String line = reader.readLine();

                if (line == null) break;

                output += line;
            }
            if (output.contains(PERMISSION_DENIED)) {
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
