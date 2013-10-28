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
package functionalTests.component.interceptor.nfcomponent;

import org.objectweb.proactive.core.component.interception.InterceptedRequest;
import org.objectweb.proactive.core.util.wrapper.StringWrapper;


public class InterceptorImpl extends AbstractInterceptorImpl {
    public static final String COMPONENT_NAME = "interceptor";
    public static final String BEFORE_INTERCEPTION = " - before-interception-" + COMPONENT_NAME + " - ";
    public static final String AFTER_INTERCEPTION = " - after-interception-" + COMPONENT_NAME + " - ";

    @Override
    public InterceptedRequest beforeMethodInvocation(InterceptedRequest interceptedRequest) {
        interceptedRequest.setParameter(interceptedRequest.getParameter(0) + BEFORE_INTERCEPTION, 0);

        setDummyValue(getDummyValue() + BEFORE_INTERCEPTION + interceptedRequest.getInterfaceName() + "-" +
            interceptedRequest.getMethodName() + " - ");

        return interceptedRequest;
    }

    @Override
    public InterceptedRequest afterMethodInvocation(InterceptedRequest interceptedRequest) {
        interceptedRequest.setResult(new StringWrapper(((StringWrapper) interceptedRequest.getResult())
                .getStringValue() +
            AFTER_INTERCEPTION));

        setDummyValue(getDummyValue() + AFTER_INTERCEPTION + interceptedRequest.getInterfaceName() + "-" +
            interceptedRequest.getMethodName() + " - ");

        return interceptedRequest;
    }
}