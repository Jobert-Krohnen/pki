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
// (C) 2013 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---
package com.netscape.cms.profile.constraint;

import java.util.Locale;

import org.dogtagpki.server.authentication.AuthToken;
import org.mozilla.jss.netscape.security.x509.CertificateSubjectName;
import org.mozilla.jss.netscape.security.x509.X500Name;
import org.mozilla.jss.netscape.security.x509.X509CertInfo;

import com.netscape.certsrv.profile.ERejectException;
import com.netscape.certsrv.property.IDescriptor;
import com.netscape.cms.profile.def.AuthTokenSubjectNameDefault;
import com.netscape.cms.profile.def.PolicyDefault;
import com.netscape.cmscore.apps.CMS;
import com.netscape.cmscore.request.Request;

/**
 * This class implements the user subject name constraint for cmc requests
 * authenticated by the SharedSecret
 * The resulting cert should match that of the authenticating DN
 *
 * @author cfu
 * @version $Revision$, $Date$
 */
public class CMCSharedTokenSubjectNameConstraint extends EnrollConstraint {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CMCSharedTokenSubjectNameConstraint.class);

    public CMCSharedTokenSubjectNameConstraint() {
    }

    @Override
    public IDescriptor getConfigDescriptor(Locale locale, String name) {
        return null;
    }

    public String getDefaultConfig(String name) {
        return null;
    }

    /**
     * Validates the request. The request is not modified
     * during the validation. User encoded subject name
     * is copied into the certificate template.
     */
    @Override
    public void validate(Request request, X509CertInfo info)
            throws ERejectException {
        String method = "CMCSharedTokenSubjectNameConstraint: ";
        String msg = "";

        CertificateSubjectName infoCertSN = null;
        String authTokenSharedTokenSN = null;

        try {
            infoCertSN = (CertificateSubjectName) info.get(X509CertInfo.SUBJECT);
            if (infoCertSN == null) {
                msg = method + "infoCertSN null";
                logger.error(msg);
                throw new Exception(msg);
            }
            logger.debug(method + "validate user subject=" + infoCertSN);
            X500Name infoCertName = (X500Name) infoCertSN.get(CertificateSubjectName.DN_NAME);
            if (infoCertName == null) {
                msg = method + "infoCertName null";
                logger.error(msg);
                throw new Exception(msg);
            }

            authTokenSharedTokenSN = request.getExtDataInString(AuthToken.TOKEN_SHARED_TOKEN_AUTHENTICATED_CERT_SUBJECT);
            if (authTokenSharedTokenSN == null) {
                msg = method + "authTokenSharedTokenSN null";
                logger.error(msg);
                throw new Exception(msg);
            }
            if (infoCertName.getName().equalsIgnoreCase(authTokenSharedTokenSN)) {
                logger.debug(method + "names matched");
            } else {
                msg = method + "names do not match; authTokenSharedTokenSN =" +
                        authTokenSharedTokenSN;
                logger.error(msg);
                throw new Exception(msg);
            }

        } catch (Exception e) {
            throw new ERejectException(
                    CMS.getUserMessage(getLocale(request),
                        "CMS_PROFILE_SUBJECT_NAME_NOT_MATCHED") + e);
        }
    }

    @Override
    public String getText(Locale locale) {
        return CMS.getUserMessage(locale,
                   "CMS_PROFILE_CONSTRAINT_CMC_SELF_SIGNED_SUBJECT_NAME_TEXT");
    }

    @Override
    public boolean isApplicable(PolicyDefault def) {
        String method = "CMCSharedTokenSubjectNameConstraint: isApplicable: ";
        if (def instanceof AuthTokenSubjectNameDefault) {
            logger.debug(method + "true");
            return true;
        }
        logger.debug(method + "false");
        return false;
    }
}
