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

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class DockerClientSetupTest {
    @Test
    public void testConnection_whenSetupCorrectly_returnsOK() throws IOException, InterruptedException {
        Runtime runtime = getRuntimeStub(0, "ok");
        DockerClientSetup setup = new DockerClientSetup(runtime);

        DockerClientSetup.TestResult result = setup.testConnection();

        assertEquals(DockerClientSetup.TestResult.OK,result);
    }

    @Test
    public void testConnection_whenPermissionDenied_returnsPermissionDenied() throws IOException, InterruptedException {
        Runtime runtime = getRuntimeStub(1, "Permission denied");
        DockerClientSetup setup = new DockerClientSetup(runtime);

        DockerClientSetup.TestResult result = setup.testConnection();

        assertEquals(DockerClientSetup.TestResult.PERMISSION_DENIED,result);
    }

    @Test
    public void testConnection_whenSomeOtherError_returnsOtherError() throws IOException, InterruptedException {
        Runtime runtime = getRuntimeStub(1, "some other error");
        DockerClientSetup setup = new DockerClientSetup(runtime);

        DockerClientSetup.TestResult result = setup.testConnection();

        assertEquals(DockerClientSetup.TestResult.OTHER_ERROR,result);
    }

    private Runtime getRuntimeStub(int retCode, String message) throws IOException, InterruptedException {
        Runtime runtime = mock(Runtime.class);
        Process process = mock(Process.class);
        when(runtime.exec(any(String.class))).thenReturn(process);
        when(process.waitFor()).thenReturn(retCode);
        when(process.getErrorStream()).thenReturn(IOUtils.toInputStream(message));
        return runtime;
    }

}
