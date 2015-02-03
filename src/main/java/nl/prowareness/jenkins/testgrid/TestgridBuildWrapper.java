package nl.prowareness.jenkins.testgrid;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import nl.prowareness.jenkins.testgrid.utils.DockerClient;
import nl.prowareness.jenkins.testgrid.utils.DockerClientSetup;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestgridBuildWrapper extends BuildWrapper {

    private final Boolean useFirefox;
    private final Boolean useChrome;
    private transient DockerClient replacementDockerClient;
    private transient String ipAddress;

    @DataBoundConstructor
    public TestgridBuildWrapper(Boolean useFirefox, Boolean useChrome) {
        this.useFirefox = useFirefox;
        this.useChrome = useChrome;
    }

    public Boolean getUseFirefox() {
        return useFirefox;
    }

    public Boolean getUseChrome() {
        return useChrome;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        listener.getLogger().println("Test grid for Selenium tests started");

        final Map<String,String> containers = startContainers(build,launcher,listener);
        final Launcher l = launcher;

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {
                env.put("TESTGRID_URL", String.format("http://%s:4444/wd/hub", ipAddress));
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                stopContainers(build,l,listener,containers.values());
                return super.tearDown(build, listener);
            }
        };
    }

    private void stopContainers(AbstractBuild build, Launcher launcher, BuildListener listener, Collection<String> containers) throws IOException, InterruptedException {
        DockerClient dockerClient = new DockerClient(build,launcher,listener);
        if (replacementDockerClient != null) {
            dockerClient = replacementDockerClient;
        }

        for (String containerName : containers) {
            dockerClient.killImage(containerName);
            dockerClient.rmImage(containerName);
        }
    }

    private Map<String,String> startContainers(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        Map<String,String> containers = new HashMap<String, String>();
        String containerName;
        DockerClient dockerClient = new DockerClient(build,launcher,listener);
        if (replacementDockerClient != null) {
            dockerClient = replacementDockerClient;
        }

        if (useChrome && useFirefox) {
            containerName = build.getEnvironment(listener).get("BUILD_TAG", null) + "-hub";
            dockerClient.runImage(getDescriptor().getHubImage(), containerName);

            containers.put("hub",containerName);
            ipAddress = dockerClient.getIpAddress(containerName);

            containerName = build.getEnvironment(listener).get("BUILD_TAG", null) + "-ff";
            dockerClient.runImage(getDescriptor().getFirefoxImage(), containerName, containers.get("hub"),"hub");
            containers.put("ff", containerName);
            containerName = build.getEnvironment(listener).get("BUILD_TAG", null) + "-chrome";
            dockerClient.runImage(getDescriptor().getChromeImage(), containerName, containers.get("hub"),"hub");
            containers.put("chrome", containerName);
        } else {
            if (useFirefox) {
                containerName = build.getEnvironment(listener).get("BUILD_TAG", null) + "-ff";
                dockerClient.runImage(getDescriptor().getFirefoxImage(), containerName);
                ipAddress = dockerClient.getIpAddress(containerName);
                containers.put("ff", containerName);
            }
            if (useChrome) {
                containerName = build.getEnvironment(listener).get("BUILD_TAG", null) + "-chrome";
                dockerClient.runImage(getDescriptor().getChromeImage(), containerName);
                ipAddress = dockerClient.getIpAddress(containerName);
                containers.put("chrome", containerName);
            }
        }

        return containers;
    }

    @Inject
    public BuildWrapper setDockerClient(DockerClient client) {
        replacementDockerClient = client;

        return this;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        private String firefoxImage;
        private String chromeImage;
        private String hubImage;
        private DockerClientSetup dockerClientSetup;

        public String getFirefoxImage() {
            return firefoxImage;
        }

        public String getChromeImage() {
            return chromeImage;
        }

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            firefoxImage = json.getString("firefoxImage");
            chromeImage = json.getString("chromeImage");
            hubImage = json.getString("hubImage");

            save();

            return super.configure(req, json);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return true;
        }

        @Override
        public String getDisplayName() {

            return "Test grid for Selenium tests";
        }

        public String getHubImage() {
            return hubImage;
        }

        public void setDockerClientSetup(DockerClientSetup dockerClientSetup) {
            this.dockerClientSetup = dockerClientSetup;
        }

        public FormValidation doTestConnection() throws IOException, InterruptedException {
            DockerClientSetup setup = dockerClientSetup;
            if (setup == null) {
                setup = new DockerClientSetup(Runtime.getRuntime());
            }

            FormValidation result;
            switch (setup.testConnection()) {
                case OK:
                    result = FormValidation.ok("Docker can be successfully executed");
                    break;
                case PERMISSION_DENIED:
                    result = FormValidation.error("Permission denied for Jenkins user. Check docker group membership of Jenkins user.");
                    break;
                case OTHER_ERROR:
                    result = FormValidation.error("Other error has occurred");
                    break;
                default:
                    result = FormValidation.error("Unspecified error");
                    break;
            }

            return result;
        }
    }
}
