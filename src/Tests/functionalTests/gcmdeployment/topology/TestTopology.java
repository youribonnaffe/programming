/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2007 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version
 * 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 */
package functionalTests.gcmdeployment.topology;

import java.io.FileNotFoundException;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.extra.gcmdeployment.API;
import org.objectweb.proactive.extra.gcmdeployment.GCMApplication.GCMApplicationDescriptor;
import org.objectweb.proactive.extra.gcmdeployment.core.GCMHost;
import org.objectweb.proactive.extra.gcmdeployment.core.GCMRuntime;
import org.objectweb.proactive.extra.gcmdeployment.core.Topology;

import functionalTests.gcmdeployment.Abstract;


public class TestTopology extends Abstract {
    GCMApplicationDescriptor gcma;

    @Test
    public void test() throws ProActiveException, FileNotFoundException {
        gcma = API.getGCMApplicationDescriptor(getDescriptor(this));
        gcma.startDeployment();
        waitAllocation();
        waitAllocation();

        Topology topology = gcma.getCurrentTopology();
        Topology topology2 = gcma.getCurrentTopology();

        Assert.assertNotSame(topology2, topology);
        System.out.println("----------------------------");
        Assert.assertEquals(3, topology.getChildren().size());
        traverseTopology(topology);
    }

    static private void traverseTopology(Topology topology) {
        printNode(topology);
        if (!checkNode(topology)) {
            throw new IllegalStateException(topology.getDeploymentPathStr());
        }
        for (Topology child : topology.getChildren()) {
            traverseTopology(child);
        }
    }

    static private boolean checkNode(Topology topology) {
        // TODO find something to test
        return true;
    }

    static private void printNode(Topology topology) {
        System.out.println();
        System.out.println("Deployment Path: " +
            topology.getDeploymentPathStr());
        System.out.println("App Desc Path: " +
            topology.getApplicationDescriptorPath());
        System.out.println("Dep Desc Path" +
            topology.getApplicationDescriptorPath());
        System.out.println("Node Provider:" + topology.getNodeProvider());
        System.out.println("Children:" + topology.getChildren().size());

        for (GCMHost host : topology.getHosts()) {
            System.out.println("\t" + host.getHostname());
            for (GCMRuntime runtime : host.getRuntimes()) {
                System.out.println("\t\t" + runtime.getName());
                for (Node node : runtime.getNodes()) {
                    System.out.println("\t\t\t" +
                        node.getNodeInformation().getName());
                }
            }
        }
    }
}