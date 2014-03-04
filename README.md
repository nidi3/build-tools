maven-tools
===========
Contains some goals to support maven builds.

- confirmation: A simple confirmation dialog (yes/no).
- consoleInput: Set a property to a value given interactively.
- setProperty: Set java runtime properties.
- runSpring: Startup / shutdown a spring container.
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
