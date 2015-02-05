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

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Matchers;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.*;

public class DockerClientTest {


    private AbstractBuild build;
    private Launcher launcher;
    private BuildListener listener;

    @SuppressWarnings("CanBeFinal")
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void setupMockObjects() throws IOException {
        build = jenkins.createFreeStyleProject().createExecutable();
        launcher = mock(Launcher.class);
        listener = mock(BuildListener.class);
        when(launcher.launch(Matchers.<Launcher.ProcStarter>any())).thenReturn(mock(Proc.class));
    }


    @Test
    public void runImage_withStandaloneImage_shouldStartDockerContainer() throws IOException, InterruptedException {
        DockerClient client = new DockerClient(build,launcher,listener);

        client.runImage("imagename","containername");

        verify(launcher, new VerificationMode() {

            public void verify(VerificationData verificationData) {
                assertEquals(verificationData.getAllInvocations().size(),1);
                Launcher.ProcStarter ps = (Launcher.ProcStarter) verificationData.getAllInvocations().get(0).getRawArguments()[0];
                String cmd = StringUtils.join(ps.cmds()," ");
                assertEquals("docker run -d --name containername imagename",cmd);
            }
        }).launch(Matchers.<Launcher.ProcStarter>any());
    }

    @Test
    public void runImage_withHub_shouldStartDockerContainer() throws IOException, InterruptedException {
        DockerClient client = new DockerClient(build,launcher,listener);

        client.runImage("imagename","containername","linkimage","linkname");

        verify(launcher, new VerificationMode() {
            public void verify(VerificationData verificationData) {
                assertEquals(verificationData.getAllInvocations().size(),1);
                Launcher.ProcStarter ps = (Launcher.ProcStarter) verificationData.getAllInvocations().get(0).getRawArguments()[0];
                String cmd = StringUtils.join(ps.cmds()," ");
                assertEquals("docker run -d --name containername --link linkimage:linkname imagename",cmd);
            }
        }).launch(Matchers.<Launcher.ProcStarter>any());
    }

    @Test
    public void killImage__shouldKillDockerContainer() throws IOException, InterruptedException {
        DockerClient client = new DockerClient(build,launcher,listener);

        client.killImage("containername");
        verify(launcher, new VerificationMode() {

            public void verify(VerificationData verificationData) {
                assertEquals(verificationData.getAllInvocations().size(), 1);
                Launcher.ProcStarter ps = (Launcher.ProcStarter) verificationData.getAllInvocations().get(0).getRawArguments()[0];
                String cmd = StringUtils.join(ps.cmds(), " ");
                assertEquals("docker kill containername", cmd);
            }
        }).launch(Matchers.<Launcher.ProcStarter>any());
    }

    @Test
    public void rmImage__shouldRemoveDockerContainer() throws IOException, InterruptedException {
        DockerClient client = new DockerClient(build,launcher,listener);

        client.rmImage("containername");
        verify(launcher, new VerificationMode() {

            public void verify(VerificationData verificationData) {
                assertEquals(verificationData.getAllInvocations().size(), 1);
                Launcher.ProcStarter ps = (Launcher.ProcStarter) verificationData.getAllInvocations().get(0).getRawArguments()[0];
                String cmd = StringUtils.join(ps.cmds(), " ");
                assertEquals("docker rm containername", cmd);
            }
        }).launch(Matchers.<Launcher.ProcStarter>any());
    }

    @Test
    public void getIpAddress_shouldReturnIpAddress() throws IOException, InterruptedException {
        final String ipAddress = "192.168.0.3";
        DockerClient client = new DockerClient(build,launcher,listener);
        final Proc p = mock(Proc.class);

        given(launcher.launch(any(Launcher.ProcStarter.class))).will(new Answer<Proc>() {

            public Proc answer(InvocationOnMock invocationOnMock) throws Throwable {
                PrintWriter writer = new PrintWriter(((Launcher.ProcStarter) invocationOnMock.getArguments()[0]).stdout());
                writer.write(String.format("[{'NetworkSettings' : { 'IPAddress': '%s'}}]", ipAddress));
                writer.close();

                return p;
            }
        });

        String returnedIpAddress = client.getIpAddress("containername");
        assertEquals(ipAddress, returnedIpAddress);
    }
}
