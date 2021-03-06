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
package org.objectweb.proactive.core.body.message;

import org.objectweb.proactive.core.UniqueID;
import org.objectweb.proactive.core.body.tags.MessageTags;


/**
 * <p>
 * A class implementing this interface is an object encapsulating a reified method call
 * either the sending of the call with the arguments or the reply of the call with
 * the result.
 * </p><p>
 * A <code>Message</code> clearly identifies a sender and a receiver of the message. Each message
 * is associated with a unique sequence number.
 * </p>
 *
 * @author The ProActive Team
 * @version 1.0,  2001/10/23
 * @since   ProActive 0.9
 *
 */
public interface Message {

    /**
     * Returns the id of the body source of this message
     * @return the id of the body source of this message
     */
    public UniqueID getSourceBodyID();

    /**
     * Returns the method name of the method call packed in this message
     * @return the method name of the method call packed in this message
     */
    public String getMethodName();

    /**
     * Returns a unique sequence number of this message
     * @return a unique sequence number of this message
     */
    public long getSequenceNumber();

    /**
     * Returns true if the message will not generate a response message
     * @return true if the message will not generate a response message
     */
    public boolean isOneWay();

    /**
     * Returns the time this message was created or deserialized
     * @return the time this message was created or deserialized
     */
    public long getTimeStamp();

    /**
     * Return the MessageTags attached to this message
     * @return the MessageTags attached to this message
     */
    public MessageTags getTags();

}
