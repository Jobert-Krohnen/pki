//
// Copyright Red Hat, Inc.
//
// SPDX-License-Identifier: GPL-2.0-or-later
//
package org.dogtagpki.server.ca;

import org.dogtagpki.legacy.ca.CAPolicyConfig;

import com.netscape.ca.CRLConfig;
import com.netscape.ca.CertificateAuthority;
import com.netscape.certsrv.connector.ConnectorsConfig;
import com.netscape.certsrv.security.SigningUnitConfig;
import com.netscape.cms.servlet.cert.scep.SCEPConfig;
import com.netscape.cmscore.base.ConfigStorage;
import com.netscape.cmscore.base.ConfigStore;
import com.netscape.cmscore.base.SimpleProperties;
import com.netscape.cmscore.ldap.PublishingConfig;

/**
 * Provides ca.* parameters.
 */
public class CAConfig extends ConfigStore {

    public CAConfig(ConfigStorage storage) {
        super(storage);
    }

    public CAConfig(String name, SimpleProperties source) {
        super(name, source);
    }

    /**
     * Returns ca.publish.* parameters.
     */
    public PublishingConfig getPublishingConfig() {
        return getSubStore("publish", PublishingConfig.class);
    }

    /**
     * Returns ca.signing.* parameters.
     */
    public SigningUnitConfig getSigningUnitConfig() {
        return getSubStore("signing", SigningUnitConfig.class);
    }

    /**
     * Returns ca.ocsp_signing.* parameters.
     */
    public SigningUnitConfig getOCSPSigningUnitConfig() {
        return getSubStore("ocsp_signing", SigningUnitConfig.class);
    }

    /**
     * Returns ca.crl_signing.* parameters.
     */
    public SigningUnitConfig getCRLSigningUnitConfig() {
        return getSubStore("crl_signing", SigningUnitConfig.class);
    }

    /**
     * Returns ca.crl.* parameters.
     */
    public CRLConfig getCRLConfig() {
        return getSubStore("crl", CRLConfig.class);
    }

    /**
     * Returns ca.connector.* parameters.
     */
    public ConnectorsConfig getConnectorsConfig() {
        return getSubStore("connector", ConnectorsConfig.class);
    }

    /**
     * Returns ca.Policy.* parameters.
     */
    public CAPolicyConfig getPolicyConfig() {
        return getSubStore(CertificateAuthority.PROP_POLICY, CAPolicyConfig.class);
    }

    /**
     * Returns ca.scep.* parameters.
     */
    public SCEPConfig getSCEPConfig() {
        return getSubStore("scep", SCEPConfig.class);
    }
}
