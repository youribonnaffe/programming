/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2008 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive@ow2.org
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
 * $$PROACTIVE_INITIAL_DEV$$
 */
package org.objectweb.proactive.benchmarks.timit.result;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.objectweb.proactive.benchmarks.timit.util.basic.BasicTimer;
import org.objectweb.proactive.benchmarks.timit.util.basic.ResultBag;
import org.objectweb.proactive.core.mop.Utils;


/**
 * This class generate final result file from data generated by basic timers.
 * Generated file will be an XML file.
 *
 * @author The ProActive Team
 */
public class BasicResultWriter {
    public static final DecimalFormat df = new DecimalFormat("###.###", new DecimalFormatSymbols(
        java.util.Locale.US));

    /** The xml document */
    private Document document;

    /** The root xml element */
    private Element eTimit;

    /** The name of the output file */
    private String filename;

    /** The default namespace */
    private Namespace defaultNS;

    /**
     * Creates an instance of this class with a given filename and a default namespace.
     * @param filename The name of the xml file
     * @param defaultNamespace An instance of the default namespace
     */
    public BasicResultWriter(String filename, Namespace defaultNamespace) {
        this.eTimit = new Element("timit", defaultNamespace);
        this.defaultNS = defaultNamespace;
        this.document = new Document(this.eTimit);
        this.filename = filename;
    }

    /**
     * Creates an instance of this class with a given filename.
     * @param filename The name of the xml file
     */
    public BasicResultWriter(String filename) {
        this(filename, Namespace.NO_NAMESPACE);
    }

    /**
     * Creates an instance of this class with a given filename and a default namespace uri.
     * @param filename The name of the xml file
     * @param defaultNamespaceURI The default namespace uri
     */
    public BasicResultWriter(String filename, String defaultNamespaceURI) {
        this(filename, Namespace.getNamespace(defaultNamespaceURI));
    }

    /**
     * Creates an instance of this class with advanced parameters in order to specify the schema.
     * @param filename The name of the xml file
     * @param defaultNamespaceURI The default namespace uri
     * @param schemaFilename The filename of the schema and its location
     */
    public BasicResultWriter(String filename, String defaultNamespaceURI, String schemaFilename) {
        this(filename, defaultNamespaceURI);
        // Declare and set the schema location
        Namespace xsiNS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        this.eTimit.addNamespaceDeclaration(xsiNS);
        // Add schema location
        this.eTimit.setAttribute(new Attribute("schemaLocation", defaultNamespaceURI + " " + schemaFilename,
            xsiNS));
    }

    /**
     * Use this method to add timers results to a single file.
     * @param bag A bag of timer results
     */
    public void addTimersElement(ResultBag bag) {
        if (bag == null) {
            return;
        }

        // Create the ao element
        Element aoElement = new Element("ao", defaultNS);
        this.fillTimersResults(aoElement, bag);
        @SuppressWarnings("unchecked")
        Iterator it = aoElement.getDescendants();
        while (it.hasNext()) {
            Element e = (Element) it.next();
            e.removeAttribute("parentId");
        }
        // Attach the ao element as a child to the timit element
        this.eTimit.addContent(aoElement);
    }

    /**
     * Use this method to add some additional information and the version of proActive
     * @param globalInformation Some additional information
     * @param proActiveVersion The current proActive version
     */
    public void addGlobalInformationElement(String globalInformation, String proActiveVersion) {
        if (globalInformation == null) {
            return;
        }

        // Create the globalInformation element
        Element globalInformationElement = new Element("globalInformation", defaultNS);
        globalInformationElement.setAttribute(new Attribute("info", globalInformation));
        // Set the proActiveVersion values as an attribute value
        globalInformationElement.setAttribute(new Attribute("proActiveVersion", proActiveVersion));
        // Attach the globalInformationElement element as a child to the timit element
        this.eTimit.addContent(globalInformationElement);
    }

    /**
     * Returns the root element of the document.
     * @return the instance of the root element
     */
    public Element getETimit() {
        return eTimit;
    }

    /**
     * This method is used to fill a specified element with timers results.
     * @param rootElement The root element that will be filled
     * @param bag A bag of timer results
     */
    private void fillTimersResults(final Element rootElement, final ResultBag bag) {
        String className = bag.getClassName();
        String uniqueID = bag.getUniqueID();
        List<BasicTimer> timersList = bag.getTimersList();
        String otherInformation = bag.getOtherInformation();
        // Set the classname value
        rootElement.setAttribute(new Attribute("className", className));
        // Set the uniqueID value
        rootElement.setAttribute(new Attribute("uniqueID", uniqueID));
        // Set the otherInformation value as an attribute value
        rootElement.setAttribute(new Attribute("otherInformation", otherInformation));
        // Create the timers element
        Element timersElement = new Element("timers", defaultNS);
        rootElement.addContent(timersElement);

        // Finding and adding all roots
        for (int i = 0; i < timersList.size(); i++) {
            // If the current is the root add it to the tree
            BasicTimer currentRoot = timersList.get(i);
            if (currentRoot.getParent() == null) {
                Element createdElement = createTimerElement(currentRoot, defaultNS);
                timersElement.addContent(createdElement);
                timersList.remove(i);
                i--;
                // Find and attach direct children of roots
                for (int j = 0; j < timersList.size(); j++) {
                    BasicTimer b = timersList.get(j);
                    if (b.getParent().equals(currentRoot)) {
                        Element directChildElement = createTimerElement(b, defaultNS);
                        createdElement.addContent(directChildElement);
                        timersList.remove(j);
                        j--;
                    }
                }
            }
        }

        // Build the hierarchical tree
        for (int i = 0; i < timersList.size(); i++) {
            // If the current is the root add it to the tree
            BasicTimer currentTimer = timersList.get(i);
            int indexOfParent = timersList.indexOf(currentTimer.getParent());

            // If the parent is not in the list
            if (indexOfParent == -1) {
                // Then the parent should be in the tree and we can try to add
                // the current to the tree
                if (addTimerToItsParentElement(currentTimer, timersElement) == false) {
                    throw new RuntimeException("The timer " + currentTimer.getName() +
                        " has no parent in the tree and no parent in the list.");
                } else {
                    // Else remove the current from the list
                    timersList.remove(i);
                    i--;
                }

                // If the parent is in the list
            } else {
                // Then try to add the parent to the tree
                if (addTimerToItsParentElement(currentTimer.getParent(), timersElement)) {
                    // Remove the current.parent from the list
                    timersList.remove(indexOfParent);
                    i--;
                }
            }
        }
    }

    /**
     * Finds the parent of the timer then creates the element and
     * attaches it to the tree.
     * @param timerToAdd The timer to add in the xml tree
     * @param rootElement The root element
     * @return true if the parent was found and timer attached else return false
     */
    private boolean addTimerToItsParentElement(BasicTimer timerToAdd, Element rootElement) {
        // If the current element is the parent of the timerToAdd then add it
        Iterator<Element> it = rootElement.getDescendants();
        while (it.hasNext()) {
            Element ee = it.next();
            if (timerToAdd.getParent().getName().equals(ee.getAttributeValue("name")) &&
                (timerToAdd.getParent().getParent().getId() == Integer.valueOf(ee
                        .getAttributeValue("parentId")))) {
                ee.addContent(createTimerElement(timerToAdd, defaultNS));
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an element timer from an instance of a timer
     *
     * @param currentTimer
     *            The timer to create an element of.
     * @return The created element.
     */
    private static final Element createTimerElement(final BasicTimer currentTimer, Namespace namespace) {
        final Element newTimerElement = new Element("timer", namespace);
        // Set the name as an attribute
        newTimerElement.setAttribute(new Attribute("name", currentTimer.getName()));
        double totalTimeValueInMillis = (currentTimer.getTotalTime()) / 1000000d;
        // Set the totalTime in millis of the timer as an attribute
        newTimerElement.setAttribute(new Attribute("totalTimeInMillis", "" + totalTimeValueInMillis));
        // Set the avgTime in millis of the timer as an attribute
        newTimerElement.setAttribute(new Attribute("avgTimeInMillis", "" +
            (totalTimeValueInMillis / currentTimer.getStartStopCoupleCount())));
        // Set the number of startStopCoupleCount value
        newTimerElement
                .setAttribute(new Attribute("invocations", "" + currentTimer.getStartStopCoupleCount()));
        // Set the temporary parent name
        newTimerElement.setAttribute(new Attribute("parentId", ((currentTimer.getParent() == null) ? ""
                : ("" + currentTimer.getParent().getId()))));
        return newTimerElement;
    }

    /**
     * Writes this document to a file
     */
    public void writeToFile() {
        try {
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());

            FileOutputStream fos = new FileOutputStream(createFileWithDirs(this.filename));
            out.output(this.document, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            System.err.println("Unable to write the XML file: " + filename);
            e.printStackTrace();
        }
    }

    public static File createFileWithDirs(String filename) {
        try {
            File file = new File(filename);

            String path = file.getParent();
            if (path != null) {
                new File(path).mkdirs();
            }
            return file;
        } catch (Exception e) {
            System.err.println("Unable to create file: " + filename);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prints the XML tree.
     */
    public void printMe() {
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        try {
            xmlOutputter.output(this.document, System.out);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Checks the best format for a time value.
     * @param timeInNanos The initial time is taken in nanoseconds
     * @return The formatted representation of the time value.
     */
    public static final String checkBestFormat(double t) {
        double timeInNanos = t;
        String format = null;
        double result = 0;

        // Check if nanoseconds is ok
        if ((timeInNanos / 1000000) < 1) {
            format = "nanos";
            result = timeInNanos;
        } else {
            // Set to milliseconds
            double timeInMillis = timeInNanos / 1000000;

            // Check if milliseconds is ok
            if ((timeInMillis / 1000) < 1) {
                format = "millis";
                result = timeInMillis;
            } else {
                double timeInSeconds = timeInMillis / 1000;

                // Check if milliseconds is ok
                if ((timeInSeconds / 60) < 1) {
                    format = "secs";
                    result = timeInSeconds;
                } else {
                    // return time in minutes
                    format = "mins";
                    result = timeInSeconds / 60;
                }
            }
        }
        return new DecimalFormat("##0.000", new DecimalFormatSymbols(java.util.Locale.US)).format(result) +
            " " + format;
    }

    /**
     * This method takes a list of documents of same structure and computes
     * the average.
     * @param documents A list of documents of same structure
     * @return A copy of the first document in the list that contains all average values
     */
    public static Document getAverage(java.util.List<Document> documents) {
        // Check documents list
        if ((documents == null) || (documents.size() == 0)) {
            return null;
        }

        // Use first document to fill avg times in it
        Document result = null;
        try {
            result = (Document) Utils.makeDeepCopy(documents.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If the list contains only one doc return it
        if (documents.size() == 1) {
            return result;
        }

        // Iterate through the root to fill the map
        java.util.Map<String, Element> resultElementsMap = new java.util.HashMap<String, Element>();

        Iterator<Element> itResultElements = result.getRootElement().getDescendants();
        String currentParentName = "";
        while (itResultElements.hasNext()) {
            Object o = itResultElements.next();
            if (o instanceof Element) {
                Element current = (Element) o;
                if (current.getName().equals("ao")) {
                    currentParentName = current.getAttributeValue("className");
                }
                if (current.getName().equals("timer")) {
                    String timerName = current.getAttributeValue("name");
                    resultElementsMap.put(timerName + "_" + currentParentName, current);
                }
            }
        }

        // Iterate through the documents
        Iterator<Document> itDocuments = documents.iterator();

        // Skip first documents since its used for average
        if (itDocuments.hasNext()) {
            itDocuments.next();
        }

        // Since the iterator has been moved to the next document
        // Begin the counter from 1
        int counter = 1;
        int listSize = documents.size();

        while (itDocuments.hasNext()) {
            itResultElements = itDocuments.next().getRootElement().getDescendants();
            currentParentName = "";
            while (itResultElements.hasNext()) {
                Object o = itResultElements.next();

                // If element
                if (o instanceof Element) {
                    Element current = (Element) o;
                    if (current.getName().equals("ao")) {
                        currentParentName = current.getAttributeValue("className");
                    }
                    if (current.getName().equals("timer")) {
                        String timerName = current.getAttributeValue("name");
                        double currentTotalValue = Double.valueOf(current
                                .getAttributeValue("totalTimeInMillis"));
                        double currentAvgValue = Double.valueOf(current.getAttributeValue("avgTimeInMillis"));
                        Element resultElement = resultElementsMap.get(timerName + "_" + currentParentName);
                        if (resultElement != null) {
                            double resultTotalValue = Double.valueOf(resultElement
                                    .getAttributeValue("totalTimeInMillis"));
                            double resultAvgValue = Double.valueOf(resultElement
                                    .getAttributeValue("avgTimeInMillis"));
                            resultTotalValue += currentTotalValue;
                            resultAvgValue += currentAvgValue;
                            // If last doc then add and make average
                            if (counter == (listSize - 1)) {
                                resultTotalValue = resultTotalValue / listSize;
                                resultAvgValue = resultAvgValue / listSize;
                            }
                            resultElement.setAttribute("totalTimeInMillis", "" + resultTotalValue);
                            resultElement.setAttribute("avgTimeInMillis", "" + resultAvgValue);
                        }
                    }
                }
            }
            counter++;
        }

        return result;
    }
}
