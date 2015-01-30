Testgrid for Selenium tests Jenkins Plugin
==========================================

Installation
------------

- Install Docker is not already available
- Add Jenkins user to docker group
- Pull docker images prowareness/selenium-hub, prowareness/selenium-node-ff, prowareness/selenium-node-chrome (e.g.`docker pull prowareness/selenium-node-ff`)
- Install plugin using Jenkins Plugin manager

Global configuration
--------------------

- Open Global Configuration
- Fill in appropriate docker images

Job configuration
-----------------
- Open Configuration page of a job
- Check "Test grid for Selenium tests"
- Check needed instances
- Save

Usage
-----

When configured, the plugin will automatically start and stop instances that you can use for Selenium tests. An environment variable named `TESTGRID_URL` will be created that you can use in your RemoteWebDriver object.
