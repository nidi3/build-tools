maven-tools 
[![Build Status](https://travis-ci.org/nidi3/build-tools.svg)](https://travis-ci.org/nidi3/build-tools)
[![codecov](https://codecov.io/gh/nidi3/build-tools/branch/master/graph/badge.svg)](https://codecov.io/gh/nidi3/build-tools)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

===========
Contains some goals to support maven builds.

- confirmation: A simple confirmation dialog (yes/no).
- consoleInput: Set a property to a value given interactively.
- setProperty: Set java runtime properties.
- runSpring: Startup / shutdown a spring container.
- runMain: Run any main method.
- dependency: Create a graphical view of the dependencies of a project.
- backport7to6: Make a project compiled with Java 7 runnable on Java 6.
- startMySql: Start a docker container with MySQL, execute any SQL scripts on it.
- stopMySql: Stop a docker container running MySQL
- An ssh tunnel ant task to be used together with the antrun plugin:

1. Add this plugin to the dependencies of the antrun plugin.
1. Add

        <taskdef name="sshtunnel" classname="SSHTunnel"
            classpathref="maven.plugin.classpath" />
to the maven tasks.
1. Usage:

        <sshtunnel host="${tunnel.host}" username="..." password="..." lport="2222"
            rport="22" rhost="${target.host}">
            <!-- tasks to use the tunnel -->
        </sshtunnel>

As an example, there the dependencies of the project:

<img src="https://rawgit.com/nidi3/build-tools/master/build-tools/dependencies.png" width="850">
