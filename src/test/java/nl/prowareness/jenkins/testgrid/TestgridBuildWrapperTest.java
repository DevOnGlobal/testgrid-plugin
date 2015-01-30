package nl.prowareness.jenkins.testgrid;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import nl.prowareness.jenkins.testgrid.utils.DockerClient;
import org.apache.maven.model.Build;
import org.apache.tools.ant.launch.Launcher;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */

public class TestgridBuildWrapperTest {

    private DockerClient dockerClient;
    private final static String firefoxImage = "prowareness/selenium-node-ff";
    private final static String chromeImage = "prowareness/selenium-node-chrome";
    private final static String hubImage = "prowareness/selenium-hub";
    private final static String ipAddress = "192.168.0.2";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private FreeStyleProject runProjectWithWrapper(Boolean useFirefox, Boolean useChrome) throws Exception {
        dockerClient = mock(DockerClient.class);
        FreeStyleProject p = jenkins.createFreeStyleProject();
        TestgridBuildWrapper wrapper = new TestgridBuildWrapper(useFirefox,useChrome);

        JSONObject config = new JSONObject();
        config.put("firefoxImage", firefoxImage);
        config.put("chromeImage", chromeImage);
        config.put("hubImage", hubImage);
        wrapper.getDescriptor().configure(null, config);
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
        form.getInputByName("_.firefoxImage").setValueAttribute(firefoxImage);
        form.getInputByName("_.chromeImage").setValueAttribute(chromeImage);
        form.getInputByName("_.hubImage").setValueAttribute(hubImage);
        ArrayList<HtmlElement> elements = (ArrayList<HtmlElement>) form.getHtmlElementsByTagName("button");
        HtmlButton button = (HtmlButton) elements.get(elements.size() - 1);
        form.submit(button);

        assertEquals(firefoxImage, new TestgridBuildWrapper(false, false).getDescriptor().getFirefoxImage());
        assertEquals(chromeImage, new TestgridBuildWrapper(false, false).getDescriptor().getChromeImage());
        assertEquals(hubImage, new TestgridBuildWrapper(false, false).getDescriptor().getHubImage());
    }

    @Test
    public void TestgridBuildWrapper_whenStarted_shouldPrintToLog () throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, false);

        assertTrue("No log message", p.getLastBuild().getLog(300).contains("Test grid for Selenium tests started"));
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneFFInstance_shouldStartDockerContainer() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, false);

        verify(dockerClient, times(1)).runImage(eq(firefoxImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") + "-ff"));
        assertEquals("http://" + ipAddress + ":4444/wd/hub",((GridUrlEnvBuilder)p.getBuildersList().get(0)).getGridUrl());
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneChromeInstance_shouldStartDockerContainer() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(false, true);

        verify(dockerClient, times(1)).runImage(eq(chromeImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG","error") + "-chrome"));
    }

    @Test
    public void TestgridBuildWrapper_whenStartedWithOneChromeAndFF_shouldStartDockerContainers() throws Exception {
        FreeStyleProject p = runProjectWithWrapper(true, true);

        verify(dockerClient, times(1)).runImage(eq(hubImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") + "-hub"));
        verify(dockerClient, times(1)).runImage(eq(chromeImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG","error") + "-chrome"), any(String.class), any(String.class));
        verify(dockerClient, times(1)).runImage(eq(firefoxImage), eq(p.getLastBuild().getEnvironment(jenkins.createTaskListener()).get("BUILD_TAG", "error") + "-ff"), any(String.class), any(String.class));
    }

    @Test
    public void getUseFirefox_shouldReturnCorrectValue() {
        assertFalse(new TestgridBuildWrapper(false, false).getUseFirefox());
        assertTrue(new TestgridBuildWrapper(true, false).getUseFirefox());
    }

    @Test
    public void getUseChrome_shouldReturnCorrectValue() {
        assertFalse(new TestgridBuildWrapper(false,false).getUseChrome());
        assertTrue(new TestgridBuildWrapper(false, true).getUseChrome());
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
