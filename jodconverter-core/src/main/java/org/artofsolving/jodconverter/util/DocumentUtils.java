//
// JODConverter - Java OpenDocument Converter
// Copyright 2004-2012 Mirko Nasato and contributors
//
// JODConverter is Open Source software, you can redistribute it and/or
// modify it under either (at your option) of the following licenses
//
// 1. The GNU Lesser General Public License v3 (or later)
//    -> http://www.gnu.org/licenses/lgpl-3.0.txt
// 2. The Apache License, Version 2.0
//    -> http://www.apache.org/licenses/LICENSE-2.0.txt
//
package org.artofsolving.jodconverter.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DocumentUtils
{

    private static final String FLD_SIMPLE = "fldSimple";
    private static final String FLD_RUN = "r";
    private static final String INSTR_ATTR = "w:instr";
    private static final String FLD_FILENAME = "FILENAME";

    private DocumentUtils()
    {
    }

    public static void disableFilenameRecalculation(File inputFile) {
        try {
            Map<String, byte[]> zipEntries = readAndProcessZipEntries(inputFile);
            writeModifiedZipEntries(inputFile, zipEntries);
        } catch (Exception e) {
            // Disabling filename recalculation failed
        }
    }

    private static Map<String, byte[]> readAndProcessZipEntries(File zipFile) throws Exception {
        Map<String, byte[]> zipEntries = new HashMap<>();

        try (ZipFile docxZip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = docxZip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                processZipEntry(docxZip, entry, zipEntries);
            }
        }

        return zipEntries;
    }

    private static void processZipEntry(ZipFile docxZip, ZipEntry entry, Map<String, byte[]> zipEntries)
            throws Exception {
        try (InputStream is = docxZip.getInputStream(entry)) {
            byte[] data = is.readAllBytes();
            String xml = new String(data, StandardCharsets.UTF_8);
            String modifiedXml = removeFldSimple(xml);
            zipEntries.put(entry.getName(), modifiedXml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void writeModifiedZipEntries(File outputFile, Map<String, byte[]> zipEntries)
            throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
            for (Map.Entry<String, byte[]> entry : zipEntries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
    }

    private static String removeFldSimple(String xml) throws ParserConfigurationException, SAXException,
            TransformerException, java.io.IOException {

        Document doc = parseXmlDocument(xml);

        List<Element> fieldsToRemove = processFieldNodes(doc);

        removeFieldElements(fieldsToRemove);

        return documentToString(doc);
    }

    private static Document parseXmlDocument(String xml) throws ParserConfigurationException,
            SAXException, java.io.IOException {
        DocumentBuilderFactory documentBuilderFactory = getDocumentBuilderFactory();
        DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static List<Element> processFieldNodes(Document doc) {
        NodeList fldSimpleNodes = doc.getElementsByTagNameNS("*", FLD_SIMPLE);
        List<Element> fieldsToRemove = new ArrayList<>();

        for (int i = 0; i < fldSimpleNodes.getLength(); i++) {
            Element fldSimple = (Element) fldSimpleNodes.item(i);
            if(fldSimple.getAttribute(INSTR_ATTR).contains(FLD_FILENAME)) {
                preserveFieldContent(fldSimple);
                fieldsToRemove.add(fldSimple);
            }
        }

        return fieldsToRemove;
    }

    private static void preserveFieldContent(Element fieldElement) {
        NodeList runs = fieldElement.getElementsByTagNameNS("*", FLD_RUN);
        for (int j = 0; j < runs.getLength(); j++) {
            Node runNode = runs.item(j).cloneNode(true);
            fieldElement.getParentNode().insertBefore(runNode, fieldElement);
        }
    }

    private static void removeFieldElements(List<Element> fieldsToRemove) {
        for (Element fieldElement : fieldsToRemove) {
            fieldElement.getParentNode().removeChild(fieldElement);
        }
    }

    private static String documentToString(Document doc) throws TransformerException {
        TransformerFactory tf = getTransformerFactory();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private static TransformerFactory getTransformerFactory()
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD","");
        tf.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet","");
        return tf;
    }

    private static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException
    {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory;
    }
}
