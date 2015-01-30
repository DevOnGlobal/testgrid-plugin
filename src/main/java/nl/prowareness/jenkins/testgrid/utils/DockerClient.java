package nl.prowareness.jenkins.testgrid.utils;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by harm on 25-1-15.
 */
public class DockerClient {

    private AbstractBuild build;
    private Launcher launcher;
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

    public void runImage(String imageName, String containerName, String linkImage, String linkName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format("docker run -d --name %s  --link %s:%s %s", containerName, linkImage, linkName, imageName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }

    public String getIpAddress(String containerName) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format("docker inspect %s", containerName));
        ps.stdout(stream);
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();

        JSONArray info = (JSONArray) JSONSerializer.toJSON(stream.toString());

        return (String)((JSONObject)info.getJSONObject(0).get("NetworkSettings")).get("IPAddress");
    }

    public void killImage(String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format("docker kill %s", containerName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }

    public void rmImage(String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format("docker rm %s", containerName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }
}
