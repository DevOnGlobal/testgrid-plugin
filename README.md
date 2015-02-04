Testgrid for Selenium tests Jenkins Plugin
==========================================

Introduction
------------

This Jenkins Plugin provides an easy way to create an on-demand Selenium Grid that you can use in your
build. After the build is complete, the Grid is automatically stopped and removed. It uses Docker for
the provisioning of the Grid.

Installation
------------

- Install Docker if not already available
- Add Jenkins user to docker group and restart Jenkins
- Pull docker images prowareness/selenium-hub, prowareness/selenium-node-ff, 
prowareness/selenium-node-chrome (e.g.`docker pull prowareness/selenium-node-ff`)
- Install this plugin using Jenkins Plugin manager

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
