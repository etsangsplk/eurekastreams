/*
 * Copyright (c) 2010 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.service.utility.http;

import java.io.IOException;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Gets a URL resource and returns it as an XML document.
 */
public interface HttpDocumentFetcher
{
    /**
     * Retrieves an XML document from a given URL.
     *
     * @param url
     *            The URL from which to get the document.
     * @param httpHeaders
     *            HTTP headers to add to the request.
     * @param proxyHost
     *            host name to use (if desired) for proxying http requests.
     * @param proxyPort
     *            port for http proxy server.
     * @param timeout
     *            the timeout period to wait for the request to return (in ms).
     * @param domFactory
     *            Factory for creating document builders.
     * @return The document.
     * @throws IOException
     *             On error.
     * @throws ParserConfigurationException
     *             On error.
     * @throws SAXException
     *             On error.
     */
    Document fetchDocument(final String url, final Map<String, String> httpHeaders, final String proxyHost,
            final String proxyPort, final int timeout, final DocumentBuilderFactory domFactory) throws IOException,
            ParserConfigurationException, SAXException;
}
