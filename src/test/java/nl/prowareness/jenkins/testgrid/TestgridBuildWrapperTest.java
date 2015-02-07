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

package nl.prowareness.jenkins.testgrid;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import nl.prowareness.jenkins.testgrid.utils.DockerClient;
import nl.prowareness.jenkins.testgrid.utils.DockerClientSetup;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */

@SuppressWarnings("ALL")
public class TestgridBuildWrapperTest {

    private DockerClient dockerClient;
    private final static String firefoxImage = "prowareness/selenium-node-ff";
    private final static String chromeImage = "prowareness/selenium-node-chrome";
    private final static String hubImage = "prowareness/selenium-hub";
    private final static String ipAddress = "192.168.0.2";

    @SuppressWarnings("CanBeFinal")
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private FreeStyleProject runProjectWithWrapper(Boolean useFirefox, Boolean useChrome) throws Exception {
        dockerClient = mock(DockerClient.class);
        FreeStyleProject p = jenkins.createFreeStyleProject();
        List<BrowserInstance> instances = new ArrayList<BrowserInstance>();
        if (useFirefox) {
            instances.add(new BrowserInstance(firefoxImage));
        }
        if (useChrome) {
            instances.add(new BrowserInstance(chromeImage));
        }
        
        TestgridBuildWrapper wrapper = new TestgridBuildWrapper(instances);
        TestgridBuildWrapper.DescriptorImpl descriptor = wrapper.getDescriptor();
        descriptor.setHubImage(hubImage);
        
        p.getBuildWrappersList().add(wrapper.setDockerClient(dockerClient));
        when(dockerClient.getIpAddress(any(String.class))).thenReturn(ipAddress);
        p.getBuildersList().add(new GridUrlEnvBuilder());
        jenkins.getInstance().rebuildDependencyGraph();
        jenkins.buildAndAssertSuccess(p);
        jenkins.waitUntilNoActivity();

        return p;
    }

    @Test
    public void globalConfiguration_shouldSaveConfig() throws IOException, SAXException {
        HtmlForm form = jenkins.createWebClient().goTo("configure").getFormByName("config");
        form.getInputByName("_.hubImage").setValueAttribute(hubImage);
        ArrayList<HtmlElement> elements = (ArrayList<HtmlElement>) form.getHtmlElementsByTagName("button");
        HtmlButton button = (HtmlButton) elements.get(elements.size() - 1);
        form.submit(button);

        assertEquals(hubImage, new TestgridBuildWrapper(new ArrayList<BrowserInstance>()).getDescriptor().getHubImage());
    }

    @Test
    public void TestgridBuildWrapper_whenStarted_shouldPrintToLog () throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, false);

        assertTrue("No log message", p.getLastBuild().getLog(300).contains("Test grid for Selenium tests started"));
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneFFInstance_shouldStartDockerContainer() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, false);

        verify(dockerClient, times(1)).runImage(eq(firefoxImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error")));
        assertEquals("http://" + ipAddress + ":4444/wd/hub", ((GridUrlEnvBuilder) p.getBuildersList().get(0)).getGridUrl());
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneChromeInstance_shouldStartDockerContainer() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(false, true);

        verify(dockerClient, times(1)).runImage(eq(chromeImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") ));
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneChromeAndFF_shouldStartDockerContainers() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, true);

        verify(dockerClient, times(1)).runImage(eq(hubImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") + "-hub"));
        verify(dockerClient, times(1)).runImage(eq(chromeImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG","error") + "-node2"), any(String.class), any(String.class));
        verify(dockerClient, times(1)).runImage(eq(firefoxImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") + "-node1"), any(String.class), any(String.class));
    }

    @Test
    public void testDescriptorImpl_isApplicable_shouldReturnTrue() throws IOException {
        TestgridBuildWrapper.DescriptorImpl descriptor = new TestgridBuildWrapper.DescriptorImpl();
        assertTrue(descriptor.isApplicable(jenkins.createFreeStyleProject()));
    }

    @Test
    public void testDescriptorImpl_getDisplayName_shouldReturnNonEmptyString() {
        TestgridBuildWrapper.DescriptorImpl descriptor = new TestgridBuildWrapper.DescriptorImpl();

        String displayName = descriptor.getDisplayName();

        assertNotNull(displayName);
        assertTrue(displayName.length() > 0);
    }

    @Test
    public void doTestConnection_whenOK_shouldReturnOk() throws IOException, InterruptedException {
        TestgridBuildWrapper.DescriptorImpl descriptor = new TestgridBuildWrapper.DescriptorImpl();
        DockerClientSetup setup = mock(DockerClientSetup.class);
        when(setup.testConnection()).thenReturn(DockerClientSetup.TestResult.OK);
        descriptor.setDockerClientSetup(setup);

        FormValidation validation = descriptor.doTestConnection();

        assertEquals(FormValidation.Kind.OK, validation.kind);
    }

    @Test
    public void doTestConnection_whenPermissionDenied_shouldReturnErrorWithMessage() throws IOException, InterruptedException {
        TestgridBuildWrapper.DescriptorImpl descriptor = new TestgridBuildWrapper.DescriptorImpl();
        DockerClientSetup setup = mock(DockerClientSetup.class);
        when(setup.testConnection()).thenReturn(DockerClientSetup.TestResult.PERMISSION_DENIED);
        descriptor.setDockerClientSetup(setup);

        FormValidation validation = descriptor.doTestConnection();

        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        assertEquals(validation.getMessage(),"Permission denied for Jenkins user. Check docker group membership of Jenkins user.");
    }

    @Test
    public void doTestConnection_whenOtherError_shouldReturnErrorWithMessage() throws IOException, InterruptedException {
        TestgridBuildWrapper.DescriptorImpl descriptor = new TestgridBuildWrapper.DescriptorImpl();
        DockerClientSetup setup = mock(DockerClientSetup.class);
        when(setup.testConnection()).thenReturn(DockerClientSetup.TestResult.OTHER_ERROR);
        descriptor.setDockerClientSetup(setup);

        FormValidation validation = descriptor.doTestConnection();

        assertEquals(FormValidation.Kind.ERROR, validation.kind);
        assertEquals(validation.getMessage(),"Other error has occurred");
    }

    private class GridUrlEnvBuilder extends TestBuilder {
        private String gridUrl;

        public String getGridUrl() {
            return gridUrl;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> abstractBuild, hudson.Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
            gridUrl = abstractBuild.getEnvironment(buildListener).get("TESTGRID_URL");
            return true;
        }
    }
}
