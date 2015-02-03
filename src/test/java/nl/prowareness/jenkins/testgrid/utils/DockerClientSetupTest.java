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
