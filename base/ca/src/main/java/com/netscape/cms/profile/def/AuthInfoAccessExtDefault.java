// --- BEGIN COPYRIGHT BLOCK ---
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// (C) 2007 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---
package com.netscape.cms.profile.def;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.lang3.StringUtils;
import org.dogtagpki.server.ca.CAEngine;
import org.dogtagpki.server.ca.CAEngineConfig;
import org.mozilla.jss.netscape.security.extensions.AccessDescription;
import org.mozilla.jss.netscape.security.extensions.AuthInfoAccessExtension;
import org.mozilla.jss.netscape.security.util.ObjectIdentifier;
import org.mozilla.jss.netscape.security.x509.GeneralName;
import org.mozilla.jss.netscape.security.x509.GeneralNameInterface;
import org.mozilla.jss.netscape.security.x509.X509CertInfo;

import com.netscape.certsrv.common.NameValuePairs;
import com.netscape.certsrv.profile.EProfileException;
import com.netscape.certsrv.property.Descriptor;
import com.netscape.certsrv.property.EPropertyException;
import com.netscape.certsrv.property.IDescriptor;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.apps.ServerConfig;
import com.netscape.cmscore.base.ConfigStore;
import com.netscape.cmscore.request.Request;

/**
 * This class implements an enrollment default policy
 * that populates Authuority Info Access extension.
 *
 * @version $Revision$, $Date$
 */
public class AuthInfoAccessExtDefault extends EnrollExtDefault {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AuthInfoAccessExtDefault.class);

    public static final String CONFIG_CRITICAL = "authInfoAccessCritical";
    public static final String CONFIG_NUM_ADS = "authInfoAccessNumADs";
    public static final String CONFIG_AD_ENABLE = "authInfoAccessADEnable_";
    public static final String CONFIG_AD_METHOD = "authInfoAccessADMethod_";
    public static final String CONFIG_AD_LOCATIONTYPE = "authInfoAccessADLocationType_";
    public static final String CONFIG_AD_LOCATION = "authInfoAccessADLocation_";

    public static final String VAL_CRITICAL = "authInfoAccessCritical";
    public static final String VAL_GENERAL_NAMES = "authInfoAccessGeneralNames";

    private static final String AD_METHOD = "Method";
    private static final String AD_LOCATION_TYPE = "Location Type";
    private static final String AD_LOCATION = "Location";
    private static final String AD_ENABLE = "Enable";

    private static final int DEF_NUM_AD = 1;
    private static final int MAX_NUM_AD = 100;

    public AuthInfoAccessExtDefault() {
        super();
    }

    protected int getNumAds() {
        int num = DEF_NUM_AD;
        String numAds = getConfig(CONFIG_NUM_ADS);

        if (numAds != null) {
            try {
                num = Integer.parseInt(numAds);
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (num > MAX_NUM_AD) {
            num = DEF_NUM_AD;
        }

        return num;
    }

    @Override
    public void init(CAEngineConfig engineConfig, ConfigStore config) throws EProfileException {
        super.init(engineConfig, config);
        refreshConfigAndValueNames();
    }

    @Override
    public void setConfig(String name, String value)
            throws EPropertyException {
        int num = 0;
        if (name.equals(CONFIG_NUM_ADS)) {
            try {
                num = Integer.parseInt(value);

                if (num >= MAX_NUM_AD || num < 0) {
                    throw new EPropertyException(CMS.getUserMessage(
                            "CMS_INVALID_PROPERTY", CONFIG_NUM_ADS));
                }

            } catch (Exception e) {
                throw new EPropertyException(CMS.getUserMessage(
                            "CMS_INVALID_PROPERTY", CONFIG_NUM_ADS));
            }
        }
        super.setConfig(name, value);
    }

    @Override
    public Enumeration<String> getConfigNames() {
        refreshConfigAndValueNames();
        return super.getConfigNames();
    }

    @Override
    protected void refreshConfigAndValueNames() {
        //refesh our config name list

        super.refreshConfigAndValueNames();
        mConfigNames.removeAllElements();
        addValueName(VAL_CRITICAL);
        addValueName(VAL_GENERAL_NAMES);

        // register configuration names bases on num ads
        addConfigName(CONFIG_CRITICAL);
        int num = getNumAds();

        addConfigName(CONFIG_NUM_ADS);
        for (int i = 0; i < num; i++) {
            addConfigName(CONFIG_AD_METHOD + i);
            addConfigName(CONFIG_AD_LOCATIONTYPE + i);
            addConfigName(CONFIG_AD_LOCATION + i);
            addConfigName(CONFIG_AD_ENABLE + i);
        }
    }

    @Override
    public IDescriptor getConfigDescriptor(Locale locale, String name) {
        if (name.equals(CONFIG_CRITICAL)) {
            return new Descriptor(IDescriptor.BOOLEAN, null,
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_CRITICAL"));
        } else if (name.startsWith(CONFIG_AD_METHOD)) {
            return new Descriptor(IDescriptor.STRING, null,
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_METHOD"));
        } else if (name.startsWith(CONFIG_AD_LOCATIONTYPE)) {
            return new Descriptor(IDescriptor.CHOICE,
                    "RFC822Name,DNSName,DirectoryName,EDIPartyName,URIName,IPAddress,OIDName",
                    "URIName",
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_LOCATIONTYPE"));
        } else if (name.startsWith(CONFIG_AD_LOCATION)) {
            return new Descriptor(IDescriptor.STRING, null,
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_LOCATION"));
        } else if (name.startsWith(CONFIG_AD_ENABLE)) {
            return new Descriptor(IDescriptor.BOOLEAN, null,
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_ENABLE"));
        } else if (name.startsWith(CONFIG_NUM_ADS)) {
            return new Descriptor(IDescriptor.INTEGER, null,
                    "1",
                    CMS.getUserMessage(locale, "CMS_PROFILE_NUM_ADS"));
        }
        return null;
    }

    @Override
    public IDescriptor getValueDescriptor(Locale locale, String name) {
        if (name.equals(VAL_CRITICAL)) {
            return new Descriptor(IDescriptor.BOOLEAN, null,
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_CRITICAL"));
        } else if (name.equals(VAL_GENERAL_NAMES)) {
            return new Descriptor(IDescriptor.STRING_LIST, null,
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_GENERAL_NAMES"));
        } else {
            return null;
        }
    }

    @Override
    public void setValue(String name, Locale locale,
            X509CertInfo info, String value)
            throws EPropertyException {

        logger.info("AuthInfoAccessExtDefault: Setting " + name);

        try {
            AuthInfoAccessExtension ext = null;

            if (name == null) {
                throw new EPropertyException(CMS.getUserMessage(
                            locale, "CMS_INVALID_PROPERTY", name));
            }

            AuthInfoAccessExtension a = new AuthInfoAccessExtension(false);
            ObjectIdentifier oid = a.getExtensionId();

            ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

            if (ext == null) {
                populate(null, info);
            }

            if (name.equals(VAL_CRITICAL)) {

                ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

                if (ext == null) {
                    return;
                }

                boolean val = Boolean.valueOf(value).booleanValue();
                logger.info("AuthInfoAccessExtDefault: - critical: " + val);
                ext.setCritical(val);

            } else if (name.equals(VAL_GENERAL_NAMES)) {

                ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

                if (ext == null) {
                    return;
                }

                boolean critical = ext.isCritical();

                Vector<NameValuePairs> v = parseRecords(value);
                int size = v.size();

                ext = new AuthInfoAccessExtension(critical);
                String method = null;
                String locationType = null;
                String location = null;
                String enable = null;

                for (int i = 0; i < size; i++) {
                    NameValuePairs nvps = v.elementAt(i);

                    for (String name1 : nvps.keySet()) {

                        if (name1.equals(AD_METHOD)) {
                            method = nvps.get(name1);
                            logger.info("AuthInfoAccessExtDefault: - " + name1 + ": " + method);

                        } else if (name1.equals(AD_LOCATION_TYPE)) {
                            locationType = nvps.get(name1);
                            logger.info("AuthInfoAccessExtDefault: - " + name1 + ": " + locationType);

                        } else if (name1.equals(AD_LOCATION)) {
                            location = nvps.get(name1);
                            logger.info("AuthInfoAccessExtDefault: - " + name1 + ": " + location);

                        } else if (name1.equals(AD_ENABLE)) {
                            enable = nvps.get(name1);
                            logger.info("AuthInfoAccessExtDefault: - " + name1 + ": " + enable);
                        }
                    }

                    if (enable != null && enable.equals("true")) {
                        GeneralName gn = null;

                        if (locationType != null || location != null) {
                            GeneralNameInterface interface1 = parseGeneralName(locationType + ":" + location);
                            if (interface1 == null)
                                throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", locationType));
                            gn = new GeneralName(interface1);
                        }

                        if (method != null) {
                            try {
                                ext.addAccessDescription(new ObjectIdentifier(method), gn);
                            } catch (NumberFormatException ee) {
                                logger.error("AuthInfoAccessExtDefault: " + ee.getMessage(), ee);
                                throw new EPropertyException(CMS.getUserMessage(locale, "CMS_PROFILE_DEF_AIA_OID", method), ee);
                            }
                        }
                    }
                }

            } else {
                throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name));
            }

            replaceExtension(ext.getExtensionId().toString(), ext, info);

        } catch (IOException e) {
            logger.error("AuthInfoAccessExtDefault: Unable to set value: " + e.getMessage(), e);
            throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name), e);

        } catch (EProfileException e) {
            logger.error("AuthInfoAccessExtDefault: Unable to set value: " + e.getMessage(), e);
            throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name), e);
        }
    }

    @Override
    public String getValue(String name, Locale locale,
            X509CertInfo info)
            throws EPropertyException {

        logger.info("AuthInfoAccessExtDefault: Getting " + name);

        AuthInfoAccessExtension ext = null;

        if (name == null) {
            throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name));
        }

        AuthInfoAccessExtension a = new AuthInfoAccessExtension(false);
        ObjectIdentifier oid = a.getExtensionId();

        ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

        if (ext == null) {
            try {
                populate(null, info);

            } catch (EProfileException e) {
                logger.error("AuthInfoAccessExtDefault: Unable to get value: " + e.getMessage(), e);
                throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name));
            }

        }

        if (name.equals(VAL_CRITICAL)) {

            ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

            if (ext == null) {
                return null;
            }

            String value = Boolean.toString(ext.isCritical());
            logger.info("AuthInfoAccessExtDefault: - value: " + value);
            return value;

        } else if (name.equals(VAL_GENERAL_NAMES)) {

            ext = (AuthInfoAccessExtension) getExtension(oid.toString(), info);

            if (ext == null) {
                return "";
            }

            int num = getNumAds();
            logger.debug("AuthInfoAccessExtDefault: - access descriptions: " + num);

            Vector<NameValuePairs> recs = new Vector<>();

            for (int i = 0; i < num; i++) {
                NameValuePairs np = new NameValuePairs();
                AccessDescription des = null;

                if (i < ext.numberOfAccessDescription()) {
                    des = ext.getAccessDescription(i);
                }
                if (des == null) {
                    np.put(AD_METHOD, "");
                    np.put(AD_LOCATION_TYPE, "");
                    np.put(AD_LOCATION, "");
                    np.put(AD_ENABLE, "false");
                } else {
                    ObjectIdentifier methodOid = des.getMethod();
                    GeneralName gn = des.getLocation();

                    np.put(AD_METHOD, methodOid.toString());
                    np.put(AD_LOCATION_TYPE, getGeneralNameType(gn));
                    np.put(AD_LOCATION, getGeneralNameValue(gn));
                    np.put(AD_ENABLE, "true");
                }
                recs.addElement(np);
            }

            String value = buildRecords(recs);
            logger.info("AuthInfoAccessExtDefault: - value: " + value);
            return value;

        } else {
            throw new EPropertyException(CMS.getUserMessage(locale, "CMS_INVALID_PROPERTY", name));
        }
    }

    @Override
    public String getText(Locale locale) {
        StringBuffer ads = new StringBuffer();
        int num = getNumAds();

        for (int i = 0; i < num; i++) {
            ads.append("Record #");
            ads.append(i);
            ads.append("{");
            ads.append(AD_METHOD + ":");
            ads.append(getConfig(CONFIG_AD_METHOD + i));
            ads.append(",");
            ads.append(AD_LOCATION_TYPE + ":");
            ads.append(getConfig(CONFIG_AD_LOCATIONTYPE + i));
            ads.append(",");
            ads.append(AD_LOCATION + ":");
            ads.append(getConfig(CONFIG_AD_LOCATION + i));
            ads.append(",");
            ads.append(AD_ENABLE + ":");
            ads.append(getConfig(CONFIG_AD_ENABLE + i));
            ads.append("}");
        }
        return CMS.getUserMessage(locale, "CMS_PROFILE_DEF_AIA_TEXT",
                getConfig(CONFIG_CRITICAL), ads.toString());
    }

    /**
     * Populates the request with this policy default.
     */
    @Override
    public void populate(Request request, X509CertInfo info)
            throws EProfileException {
        AuthInfoAccessExtension ext = createExtension();

        addExtension(ext.getExtensionId().toString(), ext, info);
    }

    public String getBuiltinOCSPPort() throws Exception {

        CAEngine engine = CAEngine.getInstance();
        if (engine != null) {
            // if running inside server, use the port provided by the engine
            return engine.getEENonSSLPort();
        }

        // if running outside of server, use the proxy port if available
        String port = engineConfig.getString("proxy.unsecurePort", null);
        if (!StringUtils.isEmpty(port)) {
            logger.debug("AuthInfoAccessExtDefault: proxy.unsecurePort: " + port);
            return port;
        }

        // otherwise, get the port from server.xml
        String instanceDir = CMS.getInstanceDir();
        String path = instanceDir + File.separator + "conf" + File.separator + "server.xml";

        ServerConfig serverConfig = ServerConfig.load(path);
        port = serverConfig.getUnsecurePort();
        logger.debug("AuthInfoAccessExtDefault: server.xml port: " + port);

        return port;
    }

    public String getDefaultOCSPURL() throws Exception {

        // return the URL specified in ca.defaultOcspUri if available
        String url = engineConfig.getString("ca.defaultOcspUri", null);
        if (url != null) {
            logger.debug("AuthInfoAccessExtDefault: ca.defaultOcspUri: " + url);
            return url;
        }

        // otherwise, return the URL of the built-in OCSP responder
        String hostname = engineConfig.getHostname();
        String port = getBuiltinOCSPPort();

        return "http://" + hostname + ":" + port + "/ca/ocsp";
    }

    public AuthInfoAccessExtension createExtension() {

        AuthInfoAccessExtension ext = null;
        int num = getNumAds();

        try {
            boolean critical = getConfigBoolean(CONFIG_CRITICAL);

            ext = new AuthInfoAccessExtension(critical);
            for (int i = 0; i < num; i++) {
                String enable = getConfig(CONFIG_AD_ENABLE + i);
                if (enable != null && enable.equals("true")) {
                    logger.debug("AuthInfoAccess: createExtension i=" + i);
                    String method = getConfig(CONFIG_AD_METHOD + i);
                    String locationType = getConfig(CONFIG_AD_LOCATIONTYPE + i);
                    if (locationType == null || locationType.length() == 0)
                        locationType = "URIName";
                    String location = getConfig(CONFIG_AD_LOCATION + i);

                    if (location == null || location.equals("")) {
                        if (method.equals("1.3.6.1.5.5.7.48.1")) {
                            location = getDefaultOCSPURL();
                        }
                    }

                    String s = locationType + ":" + location;
                    GeneralNameInterface gn = parseGeneralName(s);
                    if (gn != null) {
                        logger.debug("AuthInfoAccessExtDefault: Adding " + s);
                        ext.addAccessDescription(new ObjectIdentifier(method),
                                new GeneralName(gn));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("AuthInfoAccessExtDefault: createExtension " + e.getMessage(), e);
        }

        return ext;
    }
}
