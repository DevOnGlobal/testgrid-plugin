Testgrid for Selenium tests Jenkins Plugin
==========================================

> For developers and teams who have better things to do with their time.

Introduction
------------

This Jenkins Plugin provides an easy way to create an on-demand Selenium Grid that you can use in your
build. After the build is complete, the Grid is automatically stopped and removed. It uses Docker for
the provisioning of the Grid.

Prerequisites
-------------

In order to use the plugin, you'll need to have the following things installed:

- Jenkins
- Docker

This plugin uses the command line client instead of the Java implementation. The reason for this is that
the Java version can only use a TCP connection to communicate with the Docker server, which can have some
security issues. This might change in the future.

In order for Jenkins to call Docker, it needs access to the UNIX socket. The easiest way to do this is to
add the jenkins user (or whatever user your Jenkins service uses) to the docker group. Don't forget to
restart Jenkins or use newgrp in order to refresh permissions.

There are a couple of premade Docker images on Docker Hub that you can use. These are

- prowareness/selenium-hub
- prowareness/selenium-node-ff
- prowareness/selenium-node-chrome

Basically, every image that runs Selenium Standalone and exposes port 4444 is suitable. Use `docker pull`
 to download these images.

Installation
------------

Installation is easy, just install the plugin using the Plugin Manager in Jenkins.

Global configuration
--------------------

- Open Global Configuration
- Fill in appropriate docker images
- Test your Docker setup using the Test Docker Setup button

Job configuration
-----------------
- Open Configuration page of a job
- Check "Test grid for Selenium tests"
- Check needed browsers
- Save

Usage
-----

When configured, the plugin will automatically start and stop instances that you can use for 
Selenium tests. An environment variable named `TESTGRID_URL` will be created that you can use 
in your RemoteWebDriver object.
