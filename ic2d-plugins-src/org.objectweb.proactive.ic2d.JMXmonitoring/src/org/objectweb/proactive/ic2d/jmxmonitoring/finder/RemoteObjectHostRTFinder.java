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
package org.objectweb.proactive.ic2d.jmxmonitoring.finder;

import java.net.URI;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.jmx.naming.FactoryName;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.remoteobject.RemoteObject;
import org.objectweb.proactive.core.remoteobject.RemoteObjectHelper;
import org.objectweb.proactive.core.runtime.ProActiveRuntime;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.core.util.URIBuilder;
import org.objectweb.proactive.ic2d.console.Console;
import org.objectweb.proactive.ic2d.jmxmonitoring.Activator;
import org.objectweb.proactive.ic2d.jmxmonitoring.data.HostObject;
import org.objectweb.proactive.ic2d.jmxmonitoring.data.RuntimeObject;


/**
 * Utility class for looking for the ProActiveRuntimes on a given host.
 * 
 * @author The ProActive Team
 * 
 */
public final class RemoteObjectHostRTFinder implements RuntimeFinder {

    /**
     * The static instance of this class
     */
    private static RemoteObjectHostRTFinder instance;

    /**
     * The url of the local proactive runtime
     */
    private String localRuntimeUrl;
    /**
     * The url of the local default node
     */
    private String localDefaultNodeUrl;
    /**
     * The url of the local half bodies node
     */
    private String localHalfBodiesNodeUrl;

    private RemoteObjectHostRTFinder() {
        try {
            this.localRuntimeUrl = ProActiveRuntimeImpl.getProActiveRuntime().getURL();
            this.localDefaultNodeUrl = NodeFactory.getDefaultNode().getNodeInformation().getURL();
            this.localHalfBodiesNodeUrl = NodeFactory.getHalfBodiesNode().getNodeInformation().getURL();
        } catch (Exception e) {
            Console.getInstance(Activator.CONSOLE_NAME).err(
                    "Could not get url of local runtime, default node or half bodies node " + e.getMessage());
        }
    }

    public static RemoteObjectHostRTFinder getInstance() {
        if (RemoteObjectHostRTFinder.instance == null) {
            RemoteObjectHostRTFinder.instance = new RemoteObjectHostRTFinder();
        }
        return RemoteObjectHostRTFinder.instance;
    }

    //
    // -- PUBLIC METHODS -----------------------------------------------
    //

    /**
     * 
     * @see org.objectweb.proactive.ic2d.jmxmonitoring.finder.RuntimeFinder#getRuntimeObjects(HostObject)
     */
    public Collection<RuntimeObject> getRuntimeObjects(final HostObject hostObject) {
        int nbZombieStubs = 0;

        final String hostUrl = hostObject.getUrl();

        final Map<String, RuntimeObject> runtimeObjects = new HashMap<String, RuntimeObject>();

        final Console console = Console.getInstance(Activator.CONSOLE_NAME);
        console.log("Exploring " + hostObject + " with RMI on port " + hostObject.getPort());

        URI[] uris = null;
        try {
            URI target = URIBuilder.buildURI(hostObject.getHostName(), null, hostObject.getProtocol(),
                    hostObject.getPort());
            try {
                uris = RemoteObjectHelper.getRemoteObjectFactory(hostObject.getProtocol()).list(target);
            } catch (ProActiveException e) {
                if (e.getCause() instanceof ConnectException) {
                    console.err("Connection refused to " + hostObject);
                    return runtimeObjects.values();
                } else {
                    throw e;
                }
            }

            if (uris != null) {
                // Search all ProActive Runtimes
                for (URI url : uris) {
                    final String urlString = url.toString();
                    // In order to avoid self-monitoring we must skip the local
                    // runtime url or the local node name
                    if (urlString.equals(this.localRuntimeUrl) ||
                        urlString.equals(this.localDefaultNodeUrl) ||
                        urlString.equals(this.localHalfBodiesNodeUrl)) {
                        continue;
                    }

                    try {
                        RemoteObject<?> ro = null;
                        try {
                            ro = RemoteObjectHelper.lookup(url);
                        } catch (ProActiveException e) {
                            nbZombieStubs++;
                            continue;
                        }

                        // * Object stub = ro.getObjectProxy(); */
                        Object stub = null;
                        try {
                            stub = RemoteObjectHelper.generatedObjectStub(ro);
                        } catch (Exception e) {
                            nbZombieStubs++;
                            System.out.println("Could not generate stub for " + url);
                            continue;
                        }

                        if (stub instanceof ProActiveRuntime) {
                            final ProActiveRuntime proActiveRuntime = (ProActiveRuntime) stub;

                            String runtimeUrl = proActiveRuntime.getURL();
                            runtimeUrl = FactoryName.getCompleteUrl(runtimeUrl);

                            if (runtimeObjects.containsKey(runtimeUrl)) {
                                continue;
                            }

                            RuntimeObject runtime = (RuntimeObject) hostObject.getChild(runtimeUrl);

                            if (runtime == null && hostObject.isMonitored()) {
                                final ObjectName oname = FactoryName.createRuntimeObjectName(runtimeUrl);
                                // This runtime is not yet monitored
                                runtime = new RuntimeObject(hostObject, runtimeUrl, oname, hostUrl,
                                    proActiveRuntime.getMBeanServerName());
                            }
                            runtimeObjects.put(runtimeUrl, runtime);
                        }
                    } catch (Exception e) {
                        // the lookup returned an active object, and an active
                        // object is
                        // not a remote object (for now...)
                        e.printStackTrace();
                        console.warn("Could not get remote object at : " + url);
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof ConnectException || e instanceof ConnectIOException) {
                console.debug(e);
            } else {
                console.logException(e);
            }
        }

        if (nbZombieStubs > 0) {
            console.log(nbZombieStubs + " invalid urls in registry at host " + hostObject.getHostName() +
                ":" + hostObject.getPort());
        }
        return runtimeObjects.values();
    }
}
