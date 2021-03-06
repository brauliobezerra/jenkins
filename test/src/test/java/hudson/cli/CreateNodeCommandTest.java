/*
 * The MIT License
 *
 * Copyright 2013 Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.security.Permission;
import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class CreateNodeCommandTest {

    private CLICommandInvoker command;

    @Rule public final JenkinsRule j = new JenkinsRule();

    @Before public void setUp() {

        command = new CLICommandInvoker(j, new CreateNodeCommand());
    }

    @Test public void createNodeShouldFailWithoutAdministerPermision() throws Exception {

        final CLICommandInvoker.Result result = command
                .authorizedTo(Permission.READ)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invoke()
        ;

        assertThat(result.stderr(), containsString("user is missing the Overall/Administer permission"));
        assertThat("No output expected", result.stdout(), isEmptyString());
        assertThat("Command is expected to fail", result.returnCode(), equalTo(-1));
    }

    @Test public void createNode() throws Exception {

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invoke()
        ;

        assertThat("No error output expected", result.stderr(), isEmptyString());
        assertThat("Command is expected to succeed", result.returnCode(), equalTo(0));

        final Slave updatedSlave = (Slave) j.jenkins.getNode("SlaveFromXML");
        assertThat(updatedSlave.getNodeName(), equalTo("SlaveFromXML"));
        assertThat(updatedSlave.getNumExecutors(), equalTo(42));
        assertThat(updatedSlave.getUserId(), equalTo(command.user().getId()));
    }

    @Test public void createNodeSpecifyingNameExplicitly() throws Exception {

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invokeWithArgs("CustomSlaveName")
        ;

        assertThat("No error output expected", result.stderr(), isEmptyString());
        assertThat("Command is expected to succeed", result.returnCode(), equalTo(0));

        assertThat("A slave with original name should not exist", j.jenkins.getNode("SlaveFromXml"), nullValue());

        final Slave updatedSlave = (Slave) j.jenkins.getNode("CustomSlaveName");
        assertThat(updatedSlave.getNodeName(), equalTo("CustomSlaveName"));
        assertThat(updatedSlave.getNumExecutors(), equalTo(42));
        assertThat(updatedSlave.getUserId(), equalTo(command.user().getId()));
    }

    @Test public void createNodeSpecifyingDifferentNameExplicitly() throws Exception {

        final Node originalSlave = j.createSlave("SlaveFromXml", null, null);

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invokeWithArgs("CustomSlaveName")
        ;

        assertThat("No error output expected", result.stderr(), isEmptyString());
        assertThat("Command is expected to succeed", result.returnCode(), equalTo(0));

        assertThat("A slave with original name should be left untouched", j.jenkins.getNode("SlaveFromXml"), equalTo(originalSlave));

        final Slave updatedSlave = (Slave) j.jenkins.getNode("CustomSlaveName");
        assertThat(updatedSlave.getNodeName(), equalTo("CustomSlaveName"));
        assertThat(updatedSlave.getNumExecutors(), equalTo(42));
        assertThat(updatedSlave.getUserId(), equalTo(command.user().getId()));
    }

    @Test public void createNodeShouldFailIfNodeAlreadyExist() throws Exception {

        j.createSlave("SlaveFromXML", null, null);

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invoke()
        ;

        assertThat(result.stderr(), containsString("Node 'SlaveFromXML' already exists"));
        assertThat("No output expected", result.stdout(), isEmptyString());
        assertThat("Command is expected to fail", result.returnCode(), equalTo(-1));
    }

    @Test public void createNodeShouldFailIfNodeAlreadyExistWhenNameSpecifiedExplicitly() throws Exception {

        j.createSlave("ExistingSlave", null, null);

        final CLICommandInvoker.Result result = command
                .authorizedTo(Jenkins.ADMINISTER)
                .withStdin(getClass().getResourceAsStream("node.xml"))
                .invokeWithArgs("ExistingSlave")
        ;

        assertThat(result.stderr(), containsString("Node 'ExistingSlave' already exists"));
        assertThat("No output expected", result.stdout(), isEmptyString());
        assertThat("Command is expected to fail", result.returnCode(), equalTo(-1));
    }
}
