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
// (C) 2017 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---

package org.dogtagpki.server.rest;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.dogtagpki.common.CAInfo;
import org.dogtagpki.common.CAInfoResource;
import org.dogtagpki.common.KRAInfo;
import org.dogtagpki.common.KRAInfoClient;
import org.dogtagpki.server.ca.CAConfig;
import org.dogtagpki.server.ca.CAEngine;
import org.dogtagpki.server.ca.CAEngineConfig;
import org.mozilla.jss.crypto.EncryptionAlgorithm;
import org.mozilla.jss.crypto.KeyWrapAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.base.PKIException;
import com.netscape.certsrv.client.ClientConfig;
import com.netscape.certsrv.client.PKIClient;
import com.netscape.certsrv.connector.ConnectorConfig;
import com.netscape.certsrv.connector.ConnectorsConfig;
import com.netscape.certsrv.system.KRAConnectorInfo;
import com.netscape.cms.servlet.admin.KRAConnectorProcessor;
import com.netscape.cms.servlet.base.PKIService;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmsutil.crypto.CryptoUtil;

/**
 * @author Ade Lee
 *
 * This class returns CA info, including KRA-related values the CA
 * clients may need to know (e.g. for generating a CRMF cert request
 * that will cause keys to be archived in KRA).
 *
 * The KRA-related info is read from the KRAInfoService, which is
 * queried according to the KRA Connector configuration.  After
 * the KRAInfoService has been successfully contacted, the recorded
 * KRA-related settings are regarded as authoritative.
 *
 * The KRA is contacted ONLY if the current info is NOT
 * authoritative, otherwise the currently recorded values are used.
 * This means that any change to relevant KRA configuration (which
 * should occur seldom if ever) necessitates restart of the CA
 * subsystem.
 *
 * If this is unsuccessful (e.g. if the KRA is down or the
 * connector is misconfigured) we use the default values, which
 * may be incorrect.
 */
public class CAInfoService extends PKIService implements CAInfoResource {

    private static Logger logger = LoggerFactory.getLogger(CAInfoService.class);

    // is the current KRA-related info authoritative?
    private static boolean kraInfoAuthoritative = false;

    // KRA-related fields (the initial values are only used if we
    // did not yet receive authoritative info from KRA)
    private static String archivalMechanism = CAInfo.KEYWRAP_MECHANISM;
    private static String encryptAlgorithm;
    private static String keyWrapAlgorithm;
    private static String rsaPublicKeyWrapAlgorithm;
    private static String caRsaPublicKeyWrapAlgorithm;

    @Override
    public Response getInfo() throws Exception {

        HttpSession session = servletRequest.getSession();
        logger.debug("CAInfoService.getInfo(): session: " + session.getId());

        CAInfo info = new CAInfo();

        addKRAInfo(info);
        info.setCaRsaPublicKeyWrapAlgorithm(caRsaPublicKeyWrapAlgorithm);

        return createOKResponse(info);
    }

    /**
     * Add KRA fields if KRA is configured, querying the KRA
     * if necessary.
     *
     * Apart from reading 'headers', this method doesn't access
     * any instance data.
     */
    private void addKRAInfo(CAInfo info) throws Exception {

        CAEngine engine = (CAEngine) getCMSEngine();
        KRAConnectorInfo connInfo = null;

        try {
            KRAConnectorProcessor processor = new KRAConnectorProcessor(getLocale(headers));
            processor.setCMSEngine(engine);
            processor.init();

            connInfo = processor.getConnectorInfo();
        } catch (Throwable e) {
            // connInfo remains as null
        }
        boolean kraEnabled =
            connInfo != null
            && "true".equalsIgnoreCase(connInfo.getEnable());

        if (kraEnabled) {
            if (!kraInfoAuthoritative) {
                // KRA is enabled but we are yet to successfully
                // query the KRA-related info.  Do it now.
                queryKRAInfo(connInfo);
            }

            info.setArchivalMechanism(archivalMechanism);
            info.setEncryptAlgorithm(encryptAlgorithm);
            info.setKeyWrapAlgorithm(keyWrapAlgorithm);
            info.setRsaPublicKeyWrapAlgorithm(rsaPublicKeyWrapAlgorithm);
        }
    }

    private static void queryKRAInfo(KRAConnectorInfo connInfo) throws Exception {

        CAEngine engine = CAEngine.getInstance();
        CAEngineConfig cs = engine.getConfig();

        try (PKIClient client = createPKIClient(connInfo)) {

            KRAInfoClient kraInfoClient = new KRAInfoClient(client, "kra");
            KRAInfo kraInfo = kraInfoClient.getInfo();

            archivalMechanism = kraInfo.getArchivalMechanism();
            encryptAlgorithm = kraInfo.getEncryptAlgorithm();
            keyWrapAlgorithm = kraInfo.getWrapAlgorithm();
            rsaPublicKeyWrapAlgorithm = kraInfo.getRsaPublicKeyWrapAlgorithm();
            caRsaPublicKeyWrapAlgorithm =  getCaRsaPublicKeyWrapAlgorithm();

            // mark info as authoritative
            kraInfoAuthoritative = true;
        } catch (PKIException e) {
            if (e.getCode() == 404) {
                // The KRAInfoResource was added in 10.4,
                // so we are talking to a pre-10.4 KRA

                encryptAlgorithm = EncryptionAlgorithm.DES3_CBC_PAD.toString();
                keyWrapAlgorithm = KeyWrapAlgorithm.DES3_CBC_PAD.toString();

                // pre-10.4 KRA does not advertise the archival
                // mechanism; look for the old knob in CA's config
                // or fall back to the default
                boolean encrypt_archival;
                try {
                    encrypt_archival = cs.getBoolean(
                        "kra.allowEncDecrypt.archival", false);
                } catch (EBaseException e1) {
                    encrypt_archival = false;
                }
                archivalMechanism = encrypt_archival ? CAInfo.ENCRYPT_MECHANISM : CAInfo.KEYWRAP_MECHANISM;

                // mark info as authoritative
                kraInfoAuthoritative = true;
            } else {
                logger.warn("Failed to retrieve archive wrapping information from the CA: " + e.getMessage(), e);
            }
        } catch (Throwable e) {
            logger.warn("Failed to retrieve archive wrapping information from the CA: " + e.getMessage(), e);
        }
    }

    /**
     * Construct PKIClient given KRAConnectorInfo
     */
    private static PKIClient createPKIClient(KRAConnectorInfo connInfo) throws Exception {

        CAEngine engine = CAEngine.getInstance();
        CAEngineConfig cs = engine.getConfig();
        CAConfig caConfig = cs.getCAConfig();
        ConnectorsConfig connectorsConfig = caConfig.getConnectorsConfig();
        ConnectorConfig kraConnectorConfig = connectorsConfig.getConnectorConfig("KRA");

        ClientConfig config = new ClientConfig();
        int port = Integer.parseInt(connInfo.getPort());
        config.setServerURL("https", connInfo.getHost(), port);
        config.setNSSDatabase(CMS.getInstanceDir() + "/alias");

        // Use client cert specified in KRA connector
        String nickname = kraConnectorConfig.getString("nickName", null);
        if (nickname == null) {
            // Use subsystem cert as client cert
            nickname = cs.getString("ca.subsystem.nickname");

            String tokenname = cs.getString("ca.subsystem.tokenname", "");
            if (!CryptoUtil.isInternalToken(tokenname)) nickname = tokenname + ":" + nickname;
        }
        config.setCertNickname(nickname);

        return new PKIClient(config);
    }

    private static String getCaRsaPublicKeyWrapAlgorithm() throws EBaseException {

        CAEngine engine = CAEngine.getInstance();
        CAEngineConfig cs = engine.getConfig();

        boolean useOAEP = cs.getUseOAEPKeyWrap();

        return useOAEP ? "RSA_OAEP" : "RSA";
    }


}
