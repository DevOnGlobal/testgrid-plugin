Testgrid for Selenium tests Jenkins Plugin
==========================================

> Because developers and teams have better things to do with their time.

Introduction
------------

This Jenkins Plugin provides an easy way to create an on-demand Selenium Grid that you can use in your
build. After the build is complete, the Grid is automatically stopped and removed. It uses Docker for
the provisioning of the Grid.

Of course there are other Jenkins plugins that you can use to call Docker. The difference is that this
plugin specifically uses Docker to create an environment for Selenium and that you don't need to 
configure yourself. 

Prerequisites
-------------

In order to use the plugin, you'll need to have the following things installed:

- Jenkins
- Docker

This plugin uses the command line client instead of the Java implementation. The reason for this is that
the Java version can only use a TCP connection to communicate with the Docker server, which can have some
security issues. The command line client uses a UNIX socket for communication ad therefore has a different
 security model. Perhaps in the future, the TCP implementation is also added to the plugin.

In order for Jenkins to call Docker, it needs access to the UNIX socket. The easiest way to do this is to
add the jenkins user (or whatever user your Jenkins service uses) to the docker group. Don't forget to
restart Jenkins or use `newgrp` in order to refresh permissions.

There are a couple of pre-made Docker images on Docker Hub that you can use. These are

- [prowareness/selenium-hub](https://registry.hub.docker.com/u/prowareness/selenium-hub/)
- [prowareness/selenium-node-ff](https://registry.hub.docker.com/u/prowareness/selenium-node-ff/)
- [prowareness/selenium-node-chrome](https://registry.hub.docker.com/u/prowareness/selenium-node-chrome/)

Basically, every image that runs Selenium Standalone and exposes port 4444 is suitable. In fact, we encourage
you to create (and share) your own images. You can look at the Docker build file to get some inspiration. 
Use `docker pull` to download the images that you want to use.

Installation
------------

Installation is easy, just install the plugin using the Plugin Manager in Jenkins like any other plugin.

Global configuration
--------------------

Before you can use the plugin in your jobs, you have to do some global configuration once.

- Open Global Configuration of Jenkins (Manager Jenkins -> Configure System) and scroll to the section
"Test grid for Selenium tests"
- Fill in the Docker Image to use for Hub. This image is used when you configure multiple browser nodes.
- Add the browser images that you'd like to use. You have to give it a name and the name of the Docker image. 
You can optionally use a tag in this image field as well (eg. `prowareness/selenium-node-ff:latest`).
- Test your Docker setup using the Test Docker Setup button. This will try to invoke the Docker client.

Job configuration
-----------------

After you configured the plugin, you can configure your Jenkins job.

- Open the Job Configuration of a job.
- In the section "Build Environment", you'll find a checkbox "Test grid for Selenium tests". Check this checkbox.
 A label "Browser Nodes" with a button "Add" will appear.
- Add all browser nodes needed by clicking the "Add" button and selecting the browser image (configured in the
 global configuration).
 
During a build, an environment variable named `TESTGRID_URL` will be created. You can pass this environment
variable to your test suite so it can use it in its RemoteWebDriver object. For example, when you have a Maven 
project, you can pass this environment variable in "Goals and options" like `test -Dremotewebdriverurl=$TESTGRID_URL`. 
In your code, you can access it using `System.getProperty("remotewebdriverurl")`.
 
- Save your configuration.

** By http://prowareness.nl **