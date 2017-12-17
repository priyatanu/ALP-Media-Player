/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model;


public interface Constants {

    public static final int UPNP_MULTICAST_PORT = 1900;

    public static final String IPV4_UPNP_MULTICAST_GROUP = "239.255.255.250";

    public static final String IPV6_UPNP_LINK_LOCAL_ADDRESS = "FF02::C";
    public static final String IPV6_UPNP_SUBNET_ADDRESS = "FF03::C";
    public static final String IPV6_UPNP_ADMINISTRATIVE_ADDRESS = "FF04::C";
    public static final String IPV6_UPNP_SITE_LOCAL_ADDRESS = "FF05::C";
    public static final String IPV6_UPNP_GLOBAL_ADDRESS = "FF0E::C";

    public static final String PRODUCT_TOKEN_NAME = "Teleal-Cling";
    public static final String PRODUCT_TOKEN_VERSION = "1.0";

    public static final int MIN_ADVERTISEMENT_AGE_SECONDS = 1800;
    public static final int DEFAULT_SUBSCRIPTION_DURATION_SECONDS = 1800;

    // Our URIs are created like so:

    // http://host:port/upnp/device/<udn>/desc.xml
    // http://host:port/upnp/device/<udn>/service/<svcIdNamespace>/<svcId>/desc.xml
    // http://host:port/upnp/device/<udn>/service/<svcIdNamespace>/<svcId>/control
    // http://host:port/upnp/device/<udn>/service/<svcIdNamespace>/<svcId>/events
    // http://host:port/upnp/device/<udn>/embedded/<udn>/embedded/<udn>/service/<svcIdNamespace>/<svcId>/events

    public static final String RESOURCE_DEVICE_PREFIX = "/upnp/device";
    public static final String RESOURCE_EMBEDDED_PREFIX = "/embedded";
    public static final String RESOURCE_SERVICE_PREFIX = "/service";
    public static final String RESOURCE_SERVICE_CONTROL_SUFFIX = "/control";
    public static final String RESOURCE_SERVICE_EVENTS_SUFFIX = "/events";
    public static final String RESOURCE_DESCRIPTOR_FILE = "/desc.xml";
    public static final String RESOURCE_SERVICE_CALLBACK_FILE = "/callback.xml";

    // Parsing rules for: deviceType, serviceType, serviceId (UDA 1.0, section 2.5)

    // TODO: UPNP VIOLATION: Microsoft Windows Media Player Sharing 4.0, X_MS_MediaReceiverRegistrar service has type with periods instead of hyphens in the namespace!
    // UDA 1.0 spec: "Period characters in the vendor domain name MUST be replaced with hyphens in accordance with RFC 2141"
    // TODO: UPNP VIOLATION: Azureus/Vuze 4.2.0.2 sends a URN as a service identifier, so we need to match colons!

    public static final String REGEX_NAMESPACE = "[a-zA-Z0-9\\-\\.]+";
    public static final String REGEX_TYPE = "[a-zA-Z_0-9\\-]{1,64}";
    public static final String REGEX_ID = "[a-zA-Z_0-9\\-:]{1,64}";

    // TODO: I have no idea how to match or what even is a "unicode extender character", neither does the Unicode book
    public static final String REGEX_UDA_NAME = "[a-zA-Z0-9^-_\\p{L}\\p{N}]{1}[a-zA-Z0-9^-_\\\\p{L}\\\\p{N}\\p{Mc}\\p{Sk}]*";

    // Random patentable "inventions" by MSFT
    public static final String SOAP_NS_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String SOAP_URI_ENCODING_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";
    public static final String NS_UPNP_CONTROL_10 = "urn:schemas-upnp-org:control-1-0";
    public static final String NS_UPNP_EVENT_10 = "urn:schemas-upnp-org:event-1-0";

    // State variable prefixes
    public static final String ARG_TYPE_PREFIX = "A_ARG_TYPE_";

}