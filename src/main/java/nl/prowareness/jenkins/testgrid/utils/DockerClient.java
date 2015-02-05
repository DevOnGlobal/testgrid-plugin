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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DockerClient {

    private static final String DOCKER_RUN_NOLINK = "docker run -d --name %s %s";
    private static final String DOCKER_RUN_LINK = "docker run -d --name %s  --link %s:%s %s";
    private static final String DOCKER_INSPECT = "docker inspect %s";
    private static final String NETWORK_SETTINGS_FIELD = "NetworkSettings";
    private static final String IP_ADDRESS_FIELD = "IPAddress";
    private static final String DOCKER_KILL = "docker kill %s";
    private static final String DOCKER_RM = "docker rm %s";
    private final AbstractBuild build;
    private final Launcher launcher;
    private final BuildListener listener;

    public DockerClient(AbstractBuild build, Launcher launcher, BuildListener listener) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
    }

    public void runImage(String imageName, String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format(DOCKER_RUN_NOLINK, containerName, imageName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }

    public void runImage(String imageName, String containerName, String linkImage, String linkName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format(DOCKER_RUN_LINK, containerName, linkImage, linkName, imageName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }

    public String getIpAddress(String containerName) throws IOException, InterruptedException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format(DOCKER_INSPECT, containerName));
        ps.stdout(stream);
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();

        JSONArray info = (JSONArray) JSONSerializer.toJSON(stream.toString());

        return (String) ((JSONObject) info.getJSONObject(0).get(NETWORK_SETTINGS_FIELD)).get(IP_ADDRESS_FIELD);
    }

    public void killImage(String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format(DOCKER_KILL, containerName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }

    public void rmImage(String containerName) throws IOException, InterruptedException {
        Launcher.ProcStarter ps = launcher.new ProcStarter();
        ps.cmdAsSingleString(String.format(DOCKER_RM, containerName));
        ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
        Proc p = launcher.launch(ps);
        p.join();
    }
}
