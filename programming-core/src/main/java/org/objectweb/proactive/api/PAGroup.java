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
package org.objectweb.proactive.api;

import java.util.Iterator;
import java.util.List;

import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.annotation.PublicAPI;
import org.objectweb.proactive.core.body.future.FutureProxy;
import org.objectweb.proactive.core.group.DispatchMode;
import org.objectweb.proactive.core.group.Group;
import org.objectweb.proactive.core.group.ProxyForGroup;
import org.objectweb.proactive.core.mop.ClassNotReifiableException;
import org.objectweb.proactive.core.mop.ConstructionOfProxyObjectFailedException;
import org.objectweb.proactive.core.mop.ConstructionOfReifiedObjectFailedException;
import org.objectweb.proactive.core.mop.InvalidProxyClassException;
import org.objectweb.proactive.core.mop.MOP;
import org.objectweb.proactive.core.mop.Proxy;
import org.objectweb.proactive.core.mop.StubObject;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.apache.log4j.Logger;


/**
 * This class is the entry point to groups in ProActive.
 * It allows to create a typed group by several different ways.
 * It also allows to control the behavior of a group, such as parameters scattering.
 *
 * @author The ProActive Team
 * @since ProActive 3.9 (December 2007)
 */
@PublicAPI
public class PAGroup {

    /** The logger for the Class */
    protected static final Logger logger = ProActiveLogger.getLogger(Loggers.GROUPS);

    /** The name of the default proxy for group communication */
    public static final Class<?> DEFAULT_PROXYFORGROUP_CLASS = org.objectweb.proactive.core.group.ProxyForGroup.class;

    /** The name of the default proxy for group communication */
    public static final String DEFAULT_PROXYFORGROUP_CLASS_NAME = "org.objectweb.proactive.core.group.ProxyForGroup";

    /**
     * Returns the <code>Group</code> for typed group <code>o</code>. Returns null if <code>o</code> is not a typed group.
     * @param o - the typed group.
     * @return the <code>Group</code> corresponding to <code>o</code>. <code>null</code> if <code>o</code> is not a typed group.
     */
    public static <E> Group<E> getGroup(E o) {
        return findProxyForGroup(o);
    }

    public static ProxyForGroup<Object> getGroupProxy(Object group) {
        ProxyForGroup<Object> pfg;

        try {
            pfg = (ProxyForGroup<Object>) PAGroup.getGroup(group);
        } catch (ClassCastException cce) {
            pfg = null;
        }

        if (pfg == null) {
            throw new IllegalArgumentException("The argument must be a group");
        }

        return pfg;
    }

    /**
     * Returns the name class of the typed group.
     * If the parameter is not a typed group, returns the name of Class<?> of the parameter.
     * @param o the typed group for which we want the name of the type (Class<?>).
     * @return the name class of the typed group
     */
    public static String getType(Object o) {
        ProxyForGroup<?> tmp = findProxyForGroup(o);
        if (tmp != null) {
            return tmp.getTypeName();
        } else {
            return o.getClass().getName();
        }
    }

    /**
     * Returns the ProxyForGroup of the typed group <code>ogroup</code>.
     * @param ogroup the typed group.
     * @return the <code>ProxyForGroup</code> of the typed group <code>ogroup</code>.
     * <code>null</code> if <code>ogroup</code> does not represent a Group.
     */
    @SuppressWarnings("unchecked")
    public static <E> ProxyForGroup<E> findProxyForGroup(Object ogroup) {
        if (!(MOP.isReifiedObject(ogroup))) {
            return null;
        } else {
            Proxy tmp = ((StubObject) ogroup).getProxy();

            // obj is an object representing a Group (and not a future)
            if (tmp instanceof org.objectweb.proactive.core.group.ProxyForGroup) {
                return (org.objectweb.proactive.core.group.ProxyForGroup<E>) tmp;
            }

            // obj is a future ... but may be a future-Group
            while (tmp instanceof org.objectweb.proactive.core.body.future.FutureProxy)
                // future of future ...
                if (MOP.isReifiedObject(((FutureProxy) tmp).getResult())) {
                    tmp = ((StubObject) ((FutureProxy) tmp).getResult()).getProxy();
                }
                // future of standard object (or group proxy in case of multicast interfaces)
                else {
                    if (((FutureProxy) tmp).getResult() instanceof ProxyForGroup) {
                        return (ProxyForGroup<E>) ((FutureProxy) tmp).getResult();
                    }
                    return null;
                }

            // future-Group
            if (tmp instanceof org.objectweb.proactive.core.group.ProxyForGroup) {
                return (org.objectweb.proactive.core.group.ProxyForGroup<E>) tmp;
            }
            // future of an active object
            else {
                return null;
            }
        }
    }

    /**
     * Returns the member at the specified index of the object representing a Group.
     * Returns <code>null</code> if <code>obj</code> doesn't represent a Group.
     * @param o a typed group.
     * @param n the rank of the wanted member.
     * @return the member of the typed group at the rank <code>n</code>
     */
    public static Object get(Object o, int n) {
        ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);
        if (theProxy == null) {
            return null;
        } else {
            return theProxy.get(n);
        }
    }

    /**
     * Returns the number of members of the object representing a Group.
     * Throws an IllegalArgumentException if <code>o</code> doesn't represent a Group.
     * @param o a typed group.
     * @return the number of member of the typed group <code>o</code>.
     * @throws IllegalArgumentException if the parameter doesn't represent a group
     */
    public static int size(Object o) {
        ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);
        if (theProxy == null) {
            throw new java.lang.IllegalArgumentException("Parameter doesn't represent a group");
        } else {
            return theProxy.size();
        }
    }

    /**
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's members.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className) throws ClassNotFoundException, ClassNotReifiableException {
        return PAGroup.newGroup(className, (Class<?>[]) null);
    }

    /**
     * Creates an object representing an empty group specifying the upper class of members.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @return an empty group of type <code>className</code>.
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     */
    public static Object newGroup(String className, Class<?>[] genericParameters)
            throws ClassNotFoundException, ClassNotReifiableException {
        MOP.checkClassIsReifiable(MOP.forName(className));

        Object result = null;

        try {
            result = MOP.newInstance(className, genericParameters, null,
                    PAGroup.DEFAULT_PROXYFORGROUP_CLASS_NAME, null);

            ProxyForGroup<?> proxy = (ProxyForGroup<?>) ((StubObject) result).getProxy();
            proxy.setClassName(className);
            proxy.setStub((StubObject) result);
        } catch (ClassNotReifiableException e) {
            PAGroup.logger.error("**** ClassNotReifiableException ****");
        } catch (InvalidProxyClassException e) {
            PAGroup.logger.error("**** InvalidProxyClassException ****");
        } catch (ConstructionOfProxyObjectFailedException e) {
            PAGroup.logger.error("**** ConstructionOfProxyObjectFailedException ****");
        } catch (ConstructionOfReifiedObjectFailedException e) {
            PAGroup.logger.error("**** ConstructionOfReifiedObjectFailedException ****");
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with the same params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the parameters used to build all the group's members.
     * @param node the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[] params, Node node)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = node;
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the parameters used to build all the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[] params,
            Node[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = PAGroup.newGroup(className, genericParameters);
        Group<Object> g = getGroup(result);

        if (params != null) {
            for (int i = 0; i < nodeList.length; i++) {
                g.add(PAActiveObject.newActive(className, genericParameters, params, nodeList[i]));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the parameters used to build all the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[] params,
            List<Node> nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = PAGroup.newGroup(className, genericParameters);
        Group<Object> g = getGroup(result);

        if (params != null) {
            Iterator<Node> it = nodeList.iterator();
            while (it.hasNext()) {
                Node node = it.next();
                g.add(PAActiveObject.newActive(className, genericParameters, params, node));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with the same params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the parameters used to build all the group's members.
     * @param nodeName the name of the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[] params,
            String nodeName) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getNode(nodeName);
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the parameters used to build all the group's members.
     * @param nodeListString the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[] params,
            String[] nodeListString) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeList = new Node[nodeListString.length];
        for (int i = 0; i < nodeListString.length; i++)
            nodeList[i] = NodeFactory.getNode(nodeListString[i]);
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getDefaultNode();

        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * @param node the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params, Node node)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = node;
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params,
            Node[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = newGroup(className, genericParameters);
        Group<Object> g = getGroup(result);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                g.add(PAActiveObject.newActive(className, params[i], nodeList[i % nodeList.length]));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params,
            List<Node> nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = newGroup(className, genericParameters);
        Group<Object> g = getGroup(result);

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                g.add(PAActiveObject.newActive(className, params[i], nodeList.get(i % nodeList.size())));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeName the name (String) of the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params,
            String nodeName) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getNode(nodeName);
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param genericParameters genericParameters parameterizing types
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeListString the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Class<?>[] genericParameters, Object[][] params,
            String[] nodeListString) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeList = new Node[nodeListString.length];
        for (int i = 0; i < nodeListString.length; i++)
            nodeList[i] = NodeFactory.getNode(nodeListString[i]);
        return PAGroup.newGroup(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with the same params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * @param node the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[] params, Node node)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = node;
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Object result = newGroup(className, (Class<?>[]) null);
        Group<Object> g = getGroup(result);

        if (params != null) {
            for (int i = 0; i < nodeList.length; i++) {
                g.add(PAActiveObject.newActive(className, params, nodeList[i]));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[] params, List<Node> nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Object result = newGroup(className, (Class<?>[]) null);
        Group<Object> g = getGroup(result);

        if (params != null) {
            Iterator<Node> it = nodeList.iterator();
            while (it.hasNext()) {
                Node node = it.next();
                g.add(PAActiveObject.newActive(className, params, node));
            }
        }

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with the same params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * @param nodeName the name of the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[] params, String nodeName)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getNode(nodeName);
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * @param nodeListString the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[] params, String[] nodeListString)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[nodeListString.length];
        for (int i = 0; i < nodeListString.length; i++)
            nodeList[i] = NodeFactory.getNode(nodeListString[i]);
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group. When <code>params</code> is <code>null</code>
     * signature of <code>public static Object newGroup(String className, Class<?>[] ParametgenericParametersers)</code> and
     * this method conflict. So you have to perform the following cast:<code>((Object[][])null)</code>
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params) throws ClassNotFoundException,
            ClassNotReifiableException, ActiveObjectCreationException, NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getDefaultNode();

        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * @param node the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params, Node node)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = node;
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroup(className, (Class<?>[]) null, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params, List<Node> nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroup(className, (Class<?>[]) null, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates all members with params on the node.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeName the name (String) of the node where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params, String nodeName)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getNode(nodeName);
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * @param className the name of the (upper) class of the group's members.
     * @param params the array that contain the parameters used to build the group's members.
     * @param nodeListString the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroup(String className, Object[][] params, String[] nodeListString)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[nodeListString.length];
        for (int i = 0; i < nodeListString.length; i++)
            nodeList[i] = NodeFactory.getNode(nodeListString[i]);
        return PAGroup.newGroup(className, params, nodeList);
    }

    /**
     * @deprecated use newGroupInParallel
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the parameters used to build all the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    @Deprecated
    public static Object newGroupBuiltWithMultithreading(String className, Object[] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Object result = newGroup(className, (Class<?>[]) null);
        ProxyForGroup<Object> proxy = (org.objectweb.proactive.core.group.ProxyForGroup<Object>) getGroup(result);

        proxy.createMemberWithMultithread(className, null, params, nodeList);

        return result;
    }

    /**
     * @deprecated use newGroupInParallel
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    @Deprecated
    public static Object newGroupBuiltWithMultithreading(String className, Object[] params, String[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeListString = new Node[nodeList.length];
        for (int i = 0; i < nodeList.length; i++)
            nodeListString[i] = NodeFactory.getNode(nodeList[i]);
        return PAGroup.newGroupBuiltWithMultithreading(className, params, nodeListString);
    }

    /**
     * @deprecated use newGroupInParallel
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    @Deprecated
    public static Object newGroupBuiltWithMultithreading(String className, Object[][] params)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getDefaultNode();

        return PAGroup.newGroupBuiltWithMultithreading(className, params, nodeList);
    }

    /**
     * @deprecated use newGroupInParallel
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    @Deprecated
    public static Object newGroupBuiltWithMultithreading(String className, Object[][] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Object result = newGroup(className, (Class<?>[]) null);
        ProxyForGroup<Object> proxy = (org.objectweb.proactive.core.group.ProxyForGroup<Object>) getGroup(result);

        proxy.createMemberWithMultithread(className, null, params, nodeList);

        return result;
    }

    /**
     * @deprecated use newGroupInParallel
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    @Deprecated
    public static Object newGroupBuiltWithMultithreading(String className, Object[][] params,
            String[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeListString = new Node[nodeList.length];
        for (int i = 0; i < nodeList.length; i++)
            nodeListString[i] = NodeFactory.getNode(nodeList[i]);
        return PAGroup.newGroupBuiltWithMultithreading(className, params, nodeListString);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the parameters used to build all the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Class<?>[] genericParameters, Object[] params,
            Node[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = newGroup(className, genericParameters);
        ProxyForGroup<Object> proxy = (org.objectweb.proactive.core.group.ProxyForGroup<Object>) getGroup(result);

        proxy.createMemberWithMultithread(className, genericParameters, params, nodeList);

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Class<?>[] genericParameters, Object[] params,
            String[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeListString = new Node[nodeList.length];
        for (int i = 0; i < nodeList.length; i++)
            nodeListString[i] = NodeFactory.getNode(nodeList[i]);
        return PAGroup.newGroupInParallel(className, genericParameters, params, nodeListString);
    }

    //// generic methods
    /**
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Class<?>[] genericParameters, Object[][] params)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        Node[] nodeList = new Node[1];
        nodeList[0] = NodeFactory.getDefaultNode();

        return PAGroup.newGroupInParallel(className, genericParameters, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Class<?>[] genericParameters,
            Object[][] params, Node[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Object result = newGroup(className, genericParameters);
        ProxyForGroup<Object> proxy = (org.objectweb.proactive.core.group.ProxyForGroup<Object>) getGroup(result);

        proxy.createMemberWithMultithread(className, genericParameters, params, nodeList);

        return result;
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Class<?>[] genericParameters,
            Object[][] params, String[] nodeList) throws ClassNotFoundException, ClassNotReifiableException,
            ActiveObjectCreationException, NodeException {
        Node[] nodeListString = new Node[nodeList.length];
        for (int i = 0; i < nodeList.length; i++)
            nodeListString[i] = NodeFactory.getNode(nodeList[i]);
        return PAGroup.newGroupInParallel(className, genericParameters, params, nodeListString);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the parameters used to build all the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Object[] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroupInParallel(className, null, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with the same params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's members.
     * @param params the parameters used to build all the group's members.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Object[] params, String[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroupInParallel(className, null, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members on the default node.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Object[][] params)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroupInParallel(className, null, params);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * @param nodeList the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Object[][] params, Node[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroupInParallel(className, null, params, nodeList);
    }

    /**
     * Creates an object representing a group (a typed group) and creates members with params cycling on nodeList.
     * Threads are used to build the group's members. This methods returns when all members were created.
     * @param className the name of the (upper) class of the group's member.
     * @param params the array that contain the parameters used to build the group's member.
     * If <code>params</code> is <code>null</code>, builds an empty group.
     * @param nodeList the names of the nodes where the members are created.
     * @return a typed group with its members.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created
     */
    public static Object newGroupInParallel(String className, Object[][] params, String[] nodeList)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return newGroupInParallel(className, null, params, nodeList);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a default
     * node in the local JVM.
     * @param ogroup the typed group to turn active.
     * @return a reference on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created.
     */
    public static Object turnActiveGroup(Object ogroup) throws ClassNotFoundException,
            ClassNotReifiableException, ActiveObjectCreationException, NodeException {
        return PAActiveObject.turnActive(ogroup, null, getType(ogroup), (Node) null, null, null);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a default
     * node in the local JVM.
     * @param ogroup the typed group to turn active.
     * @return a reference on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the node was null and that the DefaultNode cannot be created.
     */
    public static Object turnActiveGroup(Object ogroup, Class<?>[] genericParameters)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return PAActiveObject.turnActive(ogroup, genericParameters, getType(ogroup), (Node) null, null, null);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a specified node.
     * @param ogroup the typed group to turn active.
     * @param node the node where to create the active object on. If <code>null</code>,
     * the active object is created localy on a default node
     * @return a reference (possibly remote) on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the specified node can not be reached.
     */
    public static Object turnActiveGroup(Object ogroup, Class<?>[] genericParameters, Node node)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return PAActiveObject.turnActive(ogroup, genericParameters, getType(ogroup), node, null, null);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a specified node.
     * @param ogroup the typed group to turn active.
     * @param nodeName the name of the node where to create the active object on.
     * @return a reference (possibly remote) on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the specified node can not be reached.
     */
    public static Object turnActiveGroup(Object ogroup, Class<?>[] genericParameters, String nodeName)
            throws ClassNotFoundException, ClassNotReifiableException, ActiveObjectCreationException,
            NodeException {
        return PAActiveObject.turnActive(ogroup, genericParameters, getType(ogroup), NodeFactory
                .getNode(nodeName), null, null);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a specified node.
     * @param ogroup the typed group to turn active.
     * @param node the node where to create the active object on. If <code>null</code>,
     * the active object is created localy on a default node
     * @return a reference (possibly remote) on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the specified node can not be reached.
     */
    public static Object turnActiveGroup(Object ogroup, Node node) throws ClassNotFoundException,
            ClassNotReifiableException, ActiveObjectCreationException, NodeException {
        return PAActiveObject.turnActive(ogroup, null, getType(ogroup), node, null, null);
    }

    /**
     * Turns the target object (a typed group) into an ActiveObject (an active typed group) attached to a specified node.
     * @param ogroup the typed group to turn active.
     * @param nodeName the name of the node where to create the active object on.
     * @return a reference (possibly remote) on the active object produced.
     * @throws ActiveObjectCreationException if a problem occur while creating the stub or the body
     * @throws ClassNotFoundException if the Class<?> corresponding to <code>className</code> can't be found.
     * @throws ClassNotReifiableException if the Class<?> corresponding to <code>className</code> can't be reify.
     * @throws NodeException if the specified node can not be reached.
     */
    public static Object turnActiveGroup(Object ogroup, String nodeName) throws ClassNotFoundException,
            ClassNotReifiableException, ActiveObjectCreationException, NodeException {
        return PAActiveObject.turnActive(ogroup, null, getType(ogroup), NodeFactory.getNode(nodeName), null,
                null);
    }

    /**
     * Gives a view of the group
     * @param ogroup - a typed group
     * @return a typed group, the view of the group
     */
    public static Object captureView(Object ogroup) {
        Object result = null;

        try {
            result = newGroup(getType(ogroup), (Class<?>[]) null);
        } catch (ClassNotReifiableException e) {
            PAGroup.logger.error("**** ClassNotReifiableException ****");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            PAGroup.logger.error("**** ClassNotFoundException ****");
            e.printStackTrace();
        }

        Group<Object> go = getGroup(ogroup);
        Group<Object> gr = getGroup(result);

        Iterator<Object> it = go.iterator();
        while (it.hasNext()) {
            gr.add(it.next());
        }

        return result;
    }

    /**
     * Allows the typed group to dispatch parameters
     * @param ogroup the typed group who will change his semantic of communication.
     */
    public static void setScatterGroup(Object ogroup) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp != null) {
            proxytmp.setDispatchingOn();
        }
    }

    /**
     * Allows the typed group to broadcast parameters
     * @param ogroup the typed group who will change his semantic of communication.
     */
    public static void unsetScatterGroup(Object ogroup) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp != null) {
            proxytmp.setDispatchingOff();
        }
    }

    /**
     * Checks the semantic of communication of the typed group <code>ogroup</code>.
     * @param ogroup a typed group.
     * @return <code>true</code> if the "scatter option" is enabled for the typed group <code>ogroup</code>.
     */
    public static boolean isScatterGroupOn(Object ogroup) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp != null) {
            return proxytmp.isDispatchingOn();
        } else {
            return false;
        }
    }

    /**
     * Allows the typed group to make an unique serialization of parameters when a broadcast call occurs.
     * @param ogroup the typed group who will change his semantic of communication.
     */
    public static void setUniqueSerialization(Object ogroup) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp != null) {
            proxytmp.setUniqueSerializationOn();
        }
    }

    /**
     * Removes the ability of a typed group to make an unique serialization
     * @param ogroup the typed group who will change his semantic of communication.
     */
    public static void unsetUniqueSerialization(Object ogroup) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp != null) {
            proxytmp.setUniqueSerializationOff();
        }
    }

    /**
     * Checks if the object <code>o</code> is an object representing a Group (future or not).
     * @param o the Object to check.
     * @return <code>true</code> if <code>o</code> is a typed group.
     */
    public static boolean isGroup(Object o) {
        return (PAGroup.findProxyForGroup(o) != null);
    }

    public static void setDispatchMode(Object group, DispatchMode balancingMode, int bufferSize) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(group);
        if (proxytmp != null) {
            proxytmp.setBalancingMode(balancingMode, bufferSize);
        }
    }

    /**
     * By default, when a rendez-vous fails an exception is thrown. Instead,
     * when the automatic purge is enabled, failing objects are removed from
     * the group
     * @param ogroup the typed group having its behavior changed
     * @param autoPurge the new behavior
     */
    public static void setAutomaticPurge(Object ogroup, boolean autoPurge) {
        ProxyForGroup<?> proxytmp = PAGroup.findProxyForGroup(ogroup);
        if (proxytmp == null) {
            throw new IllegalArgumentException("argument " + ogroup.getClass().getName() + " is not a group");
        }
        proxytmp.setAutomaticPurge(autoPurge);
    }

    /**
     * Waits for all the futures are arrived.
     * @param o a typed group.
     */
    public static void waitAll(Object o) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                theProxy.waitAll();
            }

            // Else the "standard waitFor" method has been used in the findProxyForGroup method
        }
    }

    /**
     * Waits for one future is arrived and get it.
     * @param o a typed group.
     * @return a member of <code>o</code>.
     */
    public static Object waitAndGetOne(Object o) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.waitAndGetOne();
            }
            // Else the "standard waitFor" method has been used in the findProxyForGroup method so the future is arrived, just return it
            else {
                return o;
            }
        }
        // if o is not a reified object just return it
        else {
            return o;
        }
    }

    /**
     * Waits for one future is arrived and returns it (removes it from the typed group).
     * @param o a typed group.
     * @return a member of <code>o</code>. (<code>o</code> is removed from the typed group)
     */
    public static Object waitAndGetOneThenRemoveIt(Object o) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.waitAndGetOneThenRemoveIt();
            }
            // Else the "standard waitFor" method has been used in the findProxyForGroup method so the future is arrived, just return it
            else {
                return o;
            }
        }
        // if o is not a reified object just return it
        else {
            return o;
        }
    }

    /**
     * Waits for the N-th future is arrived and get it.
     * @param o a typed group.
     * @param n the rank of the awaited member.
     * @return the <code>n</code>-th member of th typed group <code>o</code>.
     */
    public static Object waitAndGetTheNth(Object o, int n) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.waitAndGetTheNth(n);
            }
            // Else the "standard waitFor" method has been used in the findProxyForGroup method so the future is arrived, just return it
            else {
                return o;
            }
        }
        // if o is not a reified object just return it
        else {
            return o;
        }
    }

    /**
     * Waits n futures are arrived.
     * @param o a typed group.
     * @param n the number of awaited members.
     */
    public static void waitN(Object o, int n) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                theProxy.waitN(n);
            }

            // Else the "standard waitFor" method has been used in the findProxyForGroup method
        }
    }

    /**
     * Waits for (at least) one future is arrived.
     * @param o a typed group.
     */
    public static void waitOne(Object o) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                theProxy.waitOne();
            }

            // Else the "standard waitFor" method has been used in the findProxyForGroup method
        }
    }

    /**
     * Waits that at least one member is arrived and returns its index.
     * @param o a typed group.
     * @return the index of a non-awaited member of the Group, -1 if <code>o</code> is not a reified object.
     */
    public static int waitOneAndGetIndex(Object o) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.waitOneAndGetIndex();
            }
            // Else return 0
            else {
                return 0;
            }
        }
        // if o is not a reified object, return -1
        else {
            return -1;
        }
    }

    /**
     * Waitd for the N-th future in the list is arrived.
     * @param o a typed group.
     */
    public static void waitTheNth(Object o, int n) {
        if (MOP.isReifiedObject(o)) {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                theProxy.waitTheNth(n);
            }

            // Else the "standard waitFor" method has been used in the findProxyForGroup method
        }
    }

    /**
     * Tests if all the member of the object <code>o</code> representing a group are arrived or not.
     * Always returns <code>true</code> if <code>o</code> is not a reified object (future or group).
     * @param o a typed group.
     * @return <code>true</code> if all the members of <code>o</code> are arrived.
     */
    public static boolean allArrived(Object o) {
        // If the object is not reified, it cannot be a future (or a group of future)
        if (!(MOP.isReifiedObject(o))) {
            return true;
        } else {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.allArrived();
            }
            // Else the "standard waitFor" method has been used in the findProxyForGroup method so the future is arrived
            else {
                return true;
            }
        }
    }

    /**
     * Tests if all the members of the object <code>o</code> representing a group are awaited or not.
     * Always returns <code>false</code> if <code>o</code> is not a reified object (future or group).
     * @param o a typed group.
     * @return <code>true</code> if all the members of <code>o</code> are awaited.
     */
    public static boolean allAwaited(Object o) {
        // If the object is not reified, it cannot be a future (or a group of future)
        if (!(MOP.isReifiedObject(o))) {
            return false;
        } else {
            ProxyForGroup<?> theProxy = PAGroup.findProxyForGroup(o);

            // If the object represents a group, we use the proxyForGroup's method
            if (theProxy != null) {
                return theProxy.allAwaited();
            }
            // Else the "standard waitFor" method has been used in the findProxyForGroup method so the future is arrived
            else {
                return false;
            }
        }
    }
}
