/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Harm Pauw, Prowareness
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

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import nl.prowareness.jenkins.testgrid.utils.DockerClient;
import nl.prowareness.jenkins.testgrid.utils.DockerClientSetup;
import nl.prowareness.jenkins.testgrid.utils.DockerImageNameValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("ALL")
public class TestgridBuildWrapper extends BuildWrapper {

    private static final String LOG_WRAPPER_STARTED = "Test grid for Selenium tests started";
    private static final String URL_ENV_NAME = "TESTGRID_URL";
    private static final String GRID_URL_FORMAT = "http://%s:4444/wd/hub";
    private static final String BUILD_TAG_ENVVAR = "BUILD_TAG";
    public static final String ERROR_STARTING_CONTAINERS = "Error starting containers:";
    public static final String ERROR_STOPPING_CONTAINERS = "Error stopping containers:";
    private List<BrowserInstance> browserInstances;
    private transient DockerClient replacementDockerClient;
    private transient String ipAddress;
    private boolean retainContainersOnFailure;

    @DataBoundConstructor
    public TestgridBuildWrapper(List<BrowserInstance> browserInstances, boolean retainContainersOnFailure) {
        this.browserInstances = browserInstances;
        this.retainContainersOnFailure = retainContainersOnFailure;
    }

    public List<BrowserInstance> getBrowserInstances() {
        if (browserInstances == null) {
            browserInstances = new ArrayList<BrowserInstance>();
        }
        return browserInstances;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        try {
            final Map<String, String> containers = startContainers(build, launcher, listener);
            final Launcher l = launcher;
            listener.getLogger().println(LOG_WRAPPER_STARTED);

            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                    env.put(URL_ENV_NAME, String.format(GRID_URL_FORMAT, ipAddress));
                }

                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    try {
                        if (build.getResult() == Result.SUCCESS || !retainContainersOnFailure) {
                            stopContainers(build, l, listener, containers.values());
                        }
                    } catch (DockerClient.DockerClientException ex) {
                        listener.getLogger().println(ERROR_STOPPING_CONTAINERS);
                        listener.getLogger().println(ex.getMessage());
                        build.setResult(Result.FAILURE);
                        return false;
                    }
                    return super.tearDown(build, listener);
                }
            };
        } catch (DockerClient.DockerClientException ex) {
            listener.getLogger().println(ERROR_STARTING_CONTAINERS);
            listener.getLogger().println(ex.getMessage());
            build.setResult(Result.FAILURE);
            return null;
        }
    }

    private void stopContainers(AbstractBuild build, Launcher launcher, BuildListener listener, Collection<String> containers) throws IOException, InterruptedException, DockerClient.DockerClientException {
        DockerClient dockerClient = new DockerClient(build, launcher, listener);
        if (replacementDockerClient != null) {
            dockerClient = replacementDockerClient;
        }

        for (String containerName : containers) {
            dockerClient.killImage(containerName);
            dockerClient.rmImage(containerName);
        }
    }

    private Map<String, String> startContainers(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, DockerClient.DockerClientException {
        Map<String, String> containers = new HashMap<String, String>();
        String containerName;
        DockerClient dockerClient = new DockerClient(build, launcher, listener);
        if (replacementDockerClient != null) {
            dockerClient = replacementDockerClient;
        }

        if (browserInstances.size() > 1) {
            containerName = build.getEnvironment(listener).get(BUILD_TAG_ENVVAR, null) + "-hub";
            dockerClient.runImage(getDescriptor().getHubImage(), containerName);

            containers.put("hub", containerName);
            ipAddress = dockerClient.getIpAddress(containerName);

            for (int i = 0; i < browserInstances.size(); i++) {
                containerName = build.getEnvironment(listener).get(BUILD_TAG_ENVVAR, null) + "-node" + (i+1);
                dockerClient.runImage(browserInstances.get(i).getImage(), containerName, containers.get("hub"), "hub");
                containers.put(containerName,containerName);
            }
        } else {
            containerName = build.getEnvironment(listener).get(BUILD_TAG_ENVVAR, null);
            dockerClient.runImage(browserInstances.get(0).getImage(), containerName);
            ipAddress = dockerClient.getIpAddress(containerName);
            containers.put(containerName,containerName);
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
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        public static final String DISPLAY_NAME = "Test grid for Selenium tests";
        public static final String OK_MESSAGE = "Docker can be successfully executed";
        public static final String PERMISSION_DENIED_MESSAGE = "Permission denied for Jenkins user. Check docker group membership of Jenkins user.";
        public static final String OTHER_ERROR_MESSAGE = "Other error has occurred";
        private String hubImage;
        private List<DockerImageSettings> dockerImages;

        private transient DockerClientSetup dockerClientSetup;

        public DescriptorImpl() {
            load();
        }

        public String getHubImage() {
            return hubImage;
        }

        public void setHubImage(String hubImage) {
            this.hubImage = hubImage;
        }

        public List<DockerImageSettings> getDockerImages() {
            if (dockerImages == null) {
                dockerImages = new ArrayList<DockerImageSettings>();
            }
            return dockerImages;
        }

        public void setDockerImages(List<DockerImageSettings> dockerImages) {
            this.dockerImages = dockerImages;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);

            save();

            return super.configure(req, json);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return true;
        }

        @Override
        public String getDisplayName() {

            return DISPLAY_NAME;
        }

        public void setDockerClientSetup(DockerClientSetup dockerClientSetup) {
            this.dockerClientSetup = dockerClientSetup;
        }

        public FormValidation doCheckHubImage(@QueryParameter String value) {
            if (DockerImageNameValidator.validate(value)) {
                return FormValidation.ok();
            } else {
                return FormValidation.error("Invalid format.");
            }
        }

        public FormValidation doTestConnection() throws IOException, InterruptedException {
            DockerClientSetup setup = dockerClientSetup;
            if (setup == null) {
                setup = new DockerClientSetup(Runtime.getRuntime());
            }

            FormValidation result;
            result = FormValidation.error(OTHER_ERROR_MESSAGE);
            switch (setup.testConnection()) {
                case OK:
                    result = FormValidation.ok(OK_MESSAGE);
                    break;
                case PERMISSION_DENIED:
                    result = FormValidation.error(PERMISSION_DENIED_MESSAGE);
                    break;
                case OTHER_ERROR:
                    result = FormValidation.error(OTHER_ERROR_MESSAGE);
                    break;
            }

            return result;
        }
    }
}
