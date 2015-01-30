package nl.prowareness.jenkins.testgrid;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.Map;

/**
 * Created by harm on 23-1-15.
 */
public class TestgridBuildWrapper extends BuildWrapper {

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        listener.getLogger().println("Test grid for Selenium tests started");

        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps = ps.cmdAsSingleString("docker images").stdout(listener);
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc proc = launcher.launch(ps);
        proc.join();

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                super.buildEnvVars(env);
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                return super.tearDown(build, listener);
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public boolean isApplicable(AbstractProject<?, ?> abstractProject) {
            return true;
        }

        @Override
        public String getDisplayName() {

            return "Test grid for Selenium tests";
        }
    }
}
