//
// Copyright Red Hat, Inc.
//
// SPDX-License-Identifier: GPL-2.0-or-later
//
package org.dogtagpki.server.ca.cli;

import java.io.File;
import java.io.FileInputStream;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.dogtagpki.cli.CLI;
import org.dogtagpki.cli.CommandCLI;
import org.dogtagpki.server.ca.CAEngineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.base.IConfigStore;
import com.netscape.certsrv.registry.IPluginInfo;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.apps.EngineConfig;
import com.netscape.cmscore.apps.SubsystemConfig;
import com.netscape.cmscore.apps.SubsystemsConfig;
import com.netscape.cmscore.base.ConfigStorage;
import com.netscape.cmscore.base.LDAPConfigStore;
import com.netscape.cmscore.base.PropConfigStore;
import com.netscape.cmscore.ldapconn.LDAPConfig;
import com.netscape.cmscore.ldapconn.LDAPConnectionConfig;
import com.netscape.cmscore.ldapconn.LdapAuthInfo;
import com.netscape.cmscore.ldapconn.LdapBoundConnection;
import com.netscape.cmscore.ldapconn.LdapConnInfo;
import com.netscape.cmscore.ldapconn.PKISocketConfig;
import com.netscape.cmscore.ldapconn.PKISocketFactory;
import com.netscape.cmscore.profile.LDAPProfileSubsystem;
import com.netscape.cmscore.registry.PluginRegistry;
import com.netscape.cmsutil.password.IPasswordStore;
import com.netscape.cmsutil.password.PasswordStoreConfig;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPConnection;

/**
 * @author Endi S. Dewata
 */
public class CAProfileImportCLI extends CommandCLI {

    public static Logger logger = LoggerFactory.getLogger(CAProfileImportCLI.class);

    public CAProfileImportCLI(CLI parent) {
        super("import", "Import CA profiles", parent);
    }


    @Override
    public void createOptions() {
        options.addOption(null, "input-folder", true, "Input folder");

        options.addOption("v", "verbose", false, "Run in verbose mode.");
        options.addOption(null, "debug", false, "Run in debug mode.");
        options.addOption(null, "help", false, "Show help message.");
    }

    @Override
    public void execute(CommandLine cmd) throws Exception {

        String inputFolder = cmd.getOptionValue("input-folder", "/usr/share/pki/ca/profiles/ca");

        String catalinaBase = System.getProperty("catalina.base");

        initializeTomcatJSS();

        String subsystem = parent.getParent().getName();
        String configFile = catalinaBase + File.separator + subsystem + File.separator +
                "conf" + File.separator + CMS.CONFIG_FILE;

        logger.info("Loading " + configFile);
        CAEngineConfig cs = (CAEngineConfig) getEngineConfig(subsystem);

        String pluginRegistryFile = catalinaBase + "/conf/" + subsystem + "/registry.cfg";
        logger.info("Loading " + pluginRegistryFile);

        IConfigStore pluginRegistryConfig = cs.getSubStore(PluginRegistry.ID);
        PluginRegistry pluginRegistry = new PluginRegistry();
        pluginRegistry.init(pluginRegistryConfig, pluginRegistryFile);
        pluginRegistry.startup();

        PasswordStoreConfig psc = cs.getPasswordStoreConfig();
        IPasswordStore passwordStore = IPasswordStore.create(psc);

        LDAPConfig ldapConfig = cs.getInternalDBConfig();
        String baseDN = ldapConfig.getBaseDN();

        LDAPConnectionConfig connConfig = ldapConfig.getConnectionConfig();

        LdapConnInfo connInfo = new LdapConnInfo(connConfig);
        LdapAuthInfo authInfo = getAuthInfo(passwordStore, connInfo, ldapConfig);

        PKISocketConfig socketConfig = cs.getSocketConfig();

        PKISocketFactory socketFactory;
        if (authInfo.getAuthType() == LdapAuthInfo.LDAP_AUTHTYPE_SSLCLIENTAUTH) {
            socketFactory = new PKISocketFactory(authInfo.getClientCertNickname());
        } else {
            socketFactory = new PKISocketFactory(connInfo.getSecure());
        }
        socketFactory.init(socketConfig);

        LdapBoundConnection conn = new LdapBoundConnection(socketFactory, connInfo, authInfo);

        try {
            SubsystemsConfig subsystemsConfig = cs.getSubsystemsConfig();
            for (String subsystemNumber : subsystemsConfig.getSubsystemNames()) {
                SubsystemConfig subsystemConfig = subsystemsConfig.getSubsystemConfig(subsystemNumber);

                String className = subsystemConfig.getClassName();
                Class<?> clazz = Class.forName(className);
                if (! LDAPProfileSubsystem.class.isAssignableFrom(clazz)) continue;

                logger.info("Importing profiles into LDAP");
                importProfiles(cs, pluginRegistry, conn, baseDN, inputFolder);
            }

        } finally {
            conn.disconnect();
            pluginRegistry.shutdown();
        }
    }

    /**
     * Import profiles from the filesystem into the database.
     */
    public void importProfiles(
            EngineConfig cs,
            PluginRegistry pluginRegistry,
            LDAPConnection conn,
            String baseDN,
            String inputFolder) throws Exception {

        IConfigStore profileCfg = cs.getSubStore("profile");
        String profileIds = profileCfg.getString("list", "");
        StringTokenizer st = new StringTokenizer(profileIds, ",");

        while (st.hasMoreTokens()) {
            String profileID = st.nextToken();
            IConfigStore profileConfig = profileCfg.getSubStore(profileID);
            String classID = profileConfig.getString("class_id", "");

            try {
                IPluginInfo info = pluginRegistry.getPluginInfo("profile", classID);
                if (info == null) {
                    throw new EBaseException("Invalid profile class ID: " + classID);
                }

                String profilePath = inputFolder + "/" + profileID + ".cfg";
                logger.info("Importing " + profilePath);

                importProfile(conn, baseDN, classID, profileID, profilePath);

            } catch (EBaseException e) {
                logger.warn("Unable to import profile " + profileID + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Import one profile from the filesystem into the database.
     *
     * @param conn LDAP connection.
     * @param classID The profile class of the profile to import.
     * @param profileID The ID of the profile to import.
     * @param profilePath Path to the on-disk profile configuration.
     */
    public void importProfile(
            LDAPConnection conn,
            String baseDN,
            String classID,
            String profileID,
            String profilePath)
            throws Exception {

        String dn = "cn=" + profileID + ",ou=certificateProfiles,ou=ca," + baseDN;

        String[] objectClasses = { "top", "certProfile" };
        LDAPAttribute[] createAttrs = {
                new LDAPAttribute("objectclass", objectClasses),
                new LDAPAttribute("cn", profileID),
                new LDAPAttribute("classId", classID)
        };

        ConfigStorage storage = new LDAPConfigStore(conn, dn, createAttrs, "certProfileConfig");
        IConfigStore configStore = new PropConfigStore(storage);

        try (FileInputStream input = new FileInputStream(profilePath)) {
            configStore.load(input);
        }

        configStore.commit(false /* no backup */);
    }
}
