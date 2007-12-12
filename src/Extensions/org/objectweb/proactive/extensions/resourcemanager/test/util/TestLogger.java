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
package org.objectweb.proactive.extensions.resourcemanager.test.util;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


public class TestLogger {
    // voir la classe org.objectweb.proactive.core.util.log.Loggers 
    /*-----------------------------------------------------------------------
    //InfrastructureManager loggers
    static final public String RM                      = "RM";
    static final public String IMTEST                = RM+".IMTest";
     *-----------------------------------------------------------------------
     */

    // et le fichier im-log4j  
    public static void main(String[] args) {
        Logger logger = ProActiveLogger.getLogger(Loggers.RM);
        Logger loggerTest = ProActiveLogger.getLogger(Loggers.RM_TEST);

        System.out.println("Logger name : " + logger.getName());
        System.out.println("Logger test name : " + loggerTest.getName());

        if (logger.isInfoEnabled()) {
            logger.info("Info mode actif");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Debug mode actif");
        }

        if (loggerTest.isInfoEnabled()) {
            loggerTest.info("Test Info mode actif");
        }

        if (loggerTest.isDebugEnabled()) {
            loggerTest.debug("Test Debug mode actif");
        }

        try {
        } catch (Exception e) {
            logger.fatal("Fatal mode actif");
            logger.fatal("Fatal mode actif", e);
        }
    }
}