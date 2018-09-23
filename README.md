maven-tools [![Build Status](https://travis-ci.org/nidi3/maven-tools.png?branch=master)](https://travis-ci.org/nidi3/maven-tools)
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

        <taskdef name="sshtunnel" classname="guru.nidi.maven.tools.SSHTunnel"
            classpathref="maven.plugin.classpath" />
to the maven tasks.
1. Usage:

        <sshtunnel host="${tunnel.host}" username="..." password="..." lport="2222"
            rport="22" rhost="${target.host}">
            <!-- tasks to use the tunnel -->
        </sshtunnel>

As an example, there the dependencies of the project:

<img src="https://rawgit.com/nidi3/tools-maven-plugin/master/dependencies.png" width="850">
