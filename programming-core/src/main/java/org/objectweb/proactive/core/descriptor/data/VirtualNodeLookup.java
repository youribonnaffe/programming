/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.objectweb.proactive.core.descriptor.data;

import java.util.List;

import javax.management.Notification;

import org.objectweb.proactive.api.PADeployment;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveRuntimeException;
import org.objectweb.proactive.core.filetransfer.RemoteFile;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.process.ExternalProcess;
import org.objectweb.proactive.core.runtime.ProActiveRuntimeImpl;
import org.objectweb.proactive.core.util.URIBuilder;


/**
 * A <code>VirtualNode</code> represents a conceptual entity. After activation
 * a <code>VirtualNode</code> represents one or several nodes.
 *
 * This class represents a remote VirtualNode resulting from a lookup in some registry such as RMIRegistry
 *
 * Objects of this class will be created when in an XML descriptor a VirtualNode is declared
 * in the virtualNodesAcquisition tag and defined with
 * <pre>
 * lookup virtualNode="Dispatcher" host="hostname" protocol="rmi or http"
 * </pre>
 * @author The ProActive Team
 * @version 1.0,  2003/04/01
 * @since   ProActive 1.0.2
 */

public class VirtualNodeLookup extends RuntimeDeploymentProperties implements VirtualNodeInternal {
    private VirtualNodeInternal virtualNode;
    private String name;
    private String urlForLookup;
    private String lookupProtocol;
    private String lookupHost;
    private boolean isActivated = false;

    //we use 1099 as default port
    private int portForLookup = 1099;
    private String message = "########## Calling this method on a VirtualNodeLookup has no sense, since such VirtualNode object references a remote VirtualNode ##########";
    private String notActivatedMessage = "This VirtualNode lookup is not yet activated. Activate it first";
    protected String runtimeHostForLookup = "LOOKUP_HOST";
    protected String runtimePortForLookup = "LOOKUP_PORT";
    private int fileBlockSize;
    private int overlapping;

    public VirtualNodeLookup(String name) {
        this.name = name;
        ProActiveRuntimeImpl proActiveRuntimeImpl = ProActiveRuntimeImpl.getProActiveRuntime();
        proActiveRuntimeImpl.registerLocalVirtualNode(this, this.name);
        fileBlockSize = org.objectweb.proactive.core.filetransfer.FileBlock.DEFAULT_BLOCK_SIZE;
        overlapping = org.objectweb.proactive.core.filetransfer.FileTransferService.DEFAULT_MAX_SIMULTANEOUS_BLOCKS;
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getProperty()
     */
    public String getProperty() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getProperty();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getTimeout()
     */
    public long getTimeout() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getTimeout();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#addVirtualMachine(VirtualMachine)
     */
    public void addVirtualMachine(VirtualMachine virtualMachine) {
        vnLogger.warn(message);
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getVirtualMachine()
     */
    public VirtualMachine getVirtualMachine() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getVirtualMachine();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#activate()
     */
    public void activate() {
        if (!isActivated) {
            if (isWaitingForProperties()) {
                return;
            }
            try {
                this.urlForLookup = URIBuilder.buildURI(this.lookupHost, this.name, this.lookupProtocol,
                        this.portForLookup).toString();
                //		this.remoteProActiveRuntime = RuntimeFactory.getRuntime(urlForLookup,lookupProtocol);
                //		this.virtualNode = remoteProActiveRuntime.getVirtualNode(this.name);
                this.virtualNode = PADeployment.lookupVirtualNode(urlForLookup).getVirtualNodeInternal();
                isActivated = true;
            } catch (ProActiveException e) {
                e.printStackTrace();
                throw new ProActiveRuntimeException(e);
            }
        } else {
            vnLogger.debug("VirtualNode " + this.name + " already activated");
        }
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNbMappedNodes()
     */
    public int getNbMappedNodes() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getNbMappedNodes();
    }

    /**
     * @deprecated use {@link #getNumberOfCurrentlyCreatedNodes()} or {@link #getNumberOfCreatedNodesAfterDeployment()} instead
     */
    @Deprecated
    public int createdNodeCount() {
        throw new RuntimeException(
            "This method is deprecated, use getNumberOfCurrentlyCreatedNodes() or getNumberOfCreatedNodesAfterDeployment()");
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNumberOfCurrentlyCreatedNodes()
     */
    public int getNumberOfCurrentlyCreatedNodes() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getNumberOfCurrentlyCreatedNodes();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNode()
     */
    public Node getNode() throws NodeException {
        if (!isActivated) {
            throw new NodeException(notActivatedMessage);
        }
        try {
            checkActivation();
        } catch (ProActiveException pae) {
            throw new NodeException(pae);
        }
        return virtualNode.getNode();
    }

    /*
     *  (non-Javadoc)
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNode#getNumberOfCreatedNodesAfterDeployment()
     */
    public int getNumberOfCreatedNodesAfterDeployment() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getNumberOfCreatedNodesAfterDeployment();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNode(int)
     */
    @Deprecated
    public Node getNode(int index) throws NodeException {
        if (!isActivated) {
            throw new NodeException(notActivatedMessage);
        }
        try {
            checkActivation();
        } catch (ProActiveException pae) {
            throw new NodeException(pae);
        }
        return virtualNode.getNode(index);
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNodesURL()
     */
    public String[] getNodesURL() throws NodeException {
        if (!isActivated) {
            throw new NodeException(notActivatedMessage);
        }
        try {
            checkActivation();
        } catch (ProActiveException pae) {
            throw new NodeException(pae);
        }
        return virtualNode.getNodesURL();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNodes()
     */
    public Node[] getNodes() throws NodeException {
        if (!isActivated) {
            throw new NodeException(notActivatedMessage);
        }
        try {
            checkActivation();
        } catch (ProActiveException pae) {
            throw new NodeException(pae);
        }
        return virtualNode.getNodes();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getNode(String)
     */
    public Node getNode(String url) throws NodeException {
        if (!isActivated) {
            throw new NodeException(notActivatedMessage);
        }
        try {
            checkActivation();
        } catch (ProActiveException pae) {
            throw new NodeException(pae);
        }
        return virtualNode.getNode(url);
    }

    public void killAll(boolean softly) {
        vnLogger.warn(message);
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#createNodeOnCurrentJvm(String)
     */
    public void createNodeOnCurrentJvm(String protocol) {
        vnLogger.warn(message);
    }

    public Object getUniqueAO() throws ProActiveException {
        if (!isActivated) {
            throw new ProActiveException(notActivatedMessage);
        }
        checkActivation();
        return virtualNode.getUniqueAO();
    }

    public boolean isActivated() {
        return isActivated;
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#isLookup()
     */
    public boolean isLookup() {
        return true;
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#setRuntimeInformations(String,String)
     * At the moment the only property that can be set at runtime is LOOKUP_HOST.
     */
    public void setRuntimeInformations(String information, String value) throws ProActiveException {
        try {
            checkProperty(information);
        } catch (ProActiveException e) {
            throw new ProActiveException("only " + runtimeHostForLookup + " and" + runtimePortForLookup +
                " property can be set at runtime", e);
        }
        performTask(information, value);
    }

    //	//
    //	//-----------------------implements DeploymentPropertiesEventListener ----------
    //	//
    //
    //	public void  lookForProperty(DeploymentPropertiesEvent event){
    //
    //	}
    public void setLookupInformations(String host, String protocol, String port) {
        //this.urlForLookup = url;
        this.lookupProtocol = protocol;
        if (host.indexOf("*") > -1) {
            runtimeProperties.add(runtimeHostForLookup);
        } else {
            this.lookupHost = host;
        }
        if (port.indexOf("*") > -1) {
            runtimeProperties.add(runtimePortForLookup);
        } else {
            this.portForLookup = new Integer(port).intValue();
        }
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#getMinNumberOfNodes()
     */
    public int getMinNumberOfNodes() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.getMinNumberOfNodes();
    }

    /**
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal#isMultiple()
     */
    public boolean isMultiple() {
        if (!isActivated) {
            vnLogger.error(notActivatedMessage);
        }
        return virtualNode.isMultiple();
    }

    //
    //-------------------PRIVATE METHODS---------------------------------
    //
    private boolean isWaitingForProperties() {
        return (runtimeProperties.size() >= 1);
    }

    private void performTask(String information, String value) {
        if (information.equals(runtimeHostForLookup)) {
            this.lookupHost = value;
        } else {
            this.portForLookup = new Integer(value).intValue();
        }
        runtimeProperties.remove(information);
        if (!isWaitingForProperties()) {
            this.urlForLookup = URIBuilder.buildURI(this.lookupHost, this.name, this.lookupProtocol,
                    this.portForLookup).toString();
            activate();
        }
    }

    private void checkActivation() throws ProActiveException {
        if (isWaitingForProperties()) {
            String exceptionMessage = "This VirtualNode has not yet been activated since, it is waiting for runtime properties ";
            for (int i = 0; i < runtimeProperties.size(); i++) {
                exceptionMessage = exceptionMessage.concat(runtimeProperties.get(i) + " ");
            }
            throw new ProActiveException(exceptionMessage);
        }
    }

    public int startMPI() {
        throw new RuntimeException(" ERROR: No MPI process attached with the virtual node !");
    }

    /* (non-Javadoc)
     * @see org.objectweb.proactive.core.descriptor.data.VirtualNode#fileTransferRetrieve()
     */
    public List<RemoteFile> fileTransferRetrieve() throws ProActiveException {
        throw new ProActiveException("No File Transfer Retrieve support from VirtualNodeLookup");
    }

    public ExternalProcess getMPIProcess() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean hasMPIProcess() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setFileTransferParams(int fileBlockSize, int overlapping) {
        this.fileBlockSize = fileBlockSize;
        this.overlapping = overlapping;
    }

    public VirtualNodeInternal getVirtualNodeInternal() {
        return this;
    }

    public void handleNotification(Notification notification, Object handback) {

        /**
         * @TODO VirtualNodeLookup can be remotely notified of node arrival via JMX
         */
    }
}
