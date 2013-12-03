/**
 *
 * Copyright  2000-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package stni.maven.tools;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.optional.ssh.*;

import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.*;

/**
 * Creates an SSH tunnel that nested tasks can utilize to perform tasks on remote hosts.
 *
 * @author Mike Elmsly mike.elmsly@ihug.co.nz
 * @version $Revision: 1.0 $
 * @created July 5, 2004
 * @since Ant 1.6.1
 */
public class SSHTunnel extends SSHBase implements TaskContainer {
    private String rhost, lport, rport;
    private long maxwait = 0;
    private List<Task> nestedTasks = new ArrayList<Task>();

    public void execute() throws BuildException {
        validateArguments();

        Session session = null;
        try {
            session = startSession();
            executeNestedTasks();
        } finally {
            closeSession(session);
        }
    }

    private void validateArguments() {
        if (getHost() == null) {
            throw new BuildException("Host is required.");
        }
        if (getUserInfo().getName() == null) {
            throw new BuildException("Username is required.");
        }
        if (getUserInfo().getKeyfile() == null && getUserInfo().getPassword() == null) {
            throw new BuildException("Password or Keyfile is required.");
        }
        if (getRhost() == null || getLport() == null || getRport() == null) {
            throw new BuildException("Tunnel information is required. \n Either rhost, lport or rport is not set.");
        }
    }

    private Session startSession() {
        try {
            Session session = openSession();
            session.setTimeout((int) maxwait);
            session.setPortForwardingL(Integer.parseInt(lport), rhost, Integer.parseInt(rport));
            log("SSHTunnel : Connection created successfully.", Project.MSG_INFO);
            return session;
        } catch (Exception e) {
            log("SSHTunnel : Connect Failed", Project.MSG_ERR);
            throw new BuildException("SSHTunnel Task Failed: Unable to create tunnel", e);
        }
    }

    private void executeNestedTasks() throws BuildException {
        try {
            for (Task task : nestedTasks) {
                unwrap(task).perform();
            }
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private Task unwrap(Task task) {
        if (task instanceof UnknownElement) {
            task.maybeConfigure();
            task = ((UnknownElement) task).getTask();
        }
        return task != null
                ? task
                : new Task() {
        };
    }

    private void closeSession(Session session) {
        try {
            log("Attempting Disconnect", Project.MSG_ERR);
            if (session != null) {
                session.disconnect();
            } else {
                log("Session is null", Project.MSG_ERR);
            }
        } catch (Exception e) {
            log("SSHTunnel Disconnect Failed", Project.MSG_ERR);
        }
    }

    public void addTask(Task task) throws BuildException {
        this.nestedTasks.add(task);
    }

    public String getRhost() {
        return rhost;
    }

    public void setRhost(String rhost) {
        this.rhost = rhost;
    }

    public String getLport() {
        return lport;
    }

    public void setLport(String lport) {
        this.lport = lport;
    }

    public String getRport() {
        return rport;
    }

    public void setRport(String rport) {
        this.rport = rport;
    }

    public long getMaxwait() {
        return maxwait;
    }

    public void setMaxwait(long maxwait) {
        this.maxwait = maxwait;
    }
}
