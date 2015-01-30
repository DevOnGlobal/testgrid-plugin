package nl.prowareness.jenkins.testgrid.utils;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;

/**
 * Created by harm on 25-1-15.
 */
public class DockerClient {

    private AbstractBuild build;
    private final Launcher launcher;
    private BuildListener listener;

    public DockerClient(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public void runImage(String imageName, String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format("docker run -d --name %s %s", containerName, imageName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }
}
