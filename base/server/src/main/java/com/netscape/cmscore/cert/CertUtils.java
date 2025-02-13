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
package com.netscape.cmscore.cert;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.dogtag.util.cert.CertUtil;
import org.mozilla.jss.netscape.security.extensions.NSCertTypeExtension;
import org.mozilla.jss.netscape.security.pkcs.PKCS7;
import org.mozilla.jss.netscape.security.util.Cert;
import org.mozilla.jss.netscape.security.util.DerInputStream;
import org.mozilla.jss.netscape.security.util.DerOutputStream;
import org.mozilla.jss.netscape.security.util.ObjectIdentifier;
import org.mozilla.jss.netscape.security.util.PrettyPrintFormat;
import org.mozilla.jss.netscape.security.util.Utils;
import org.mozilla.jss.netscape.security.x509.AlgorithmId;
import org.mozilla.jss.netscape.security.x509.CertificateExtensions;
import org.mozilla.jss.netscape.security.x509.Extension;
import org.mozilla.jss.netscape.security.x509.X500Name;
import org.mozilla.jss.netscape.security.x509.X509CRLImpl;
import org.mozilla.jss.netscape.security.x509.X509CertImpl;
import org.mozilla.jss.netscape.security.x509.X509CertInfo;
import org.mozilla.jss.netscape.security.x509.X509Key;

import com.netscape.certsrv.base.EBaseException;
import com.netscape.cmscore.apps.CMS;

/**
 * Utility class with assorted methods to check for
 * smime pairs, determining the type of cert - signature
 * or encryption ..etc.
 *
 * @author kanda
 * @version $Revision$, $Date$
 */
public class CertUtils {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CertUtils.class);

    public static DerInputStream parseKeyGen(String certreq) throws Exception {
        byte[] data = Utils.base64decode(certreq);
        return new DerInputStream(data);
    }

    public static void setRSAKeyToCertInfo(X509CertInfo info,
            byte encoded[]) throws EBaseException {
        try {
            if (info == null) {
                throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_OPERATION"));
            }
            X509Key key = new X509Key(AlgorithmId.get("RSAEncryption"), encoded);

            info.set(X509CertInfo.KEY, key);
        } catch (Exception e) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_OPERATION"));
        }
    }

    public static void sortCerts(X509CertImpl[] arr) {
        Arrays.sort(arr, new CertDateCompare());
    }

    public static boolean isSigningCert(X509CertImpl cert) {
        boolean[] keyUsage = null;

        try {
            keyUsage = cert.getKeyUsage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (keyUsage == null) ? false : keyUsage[0];
    }

    public static boolean isEncryptionCert(X509CertImpl cert) {
        boolean[] keyUsage = null;

        try {
            keyUsage = cert.getKeyUsage();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (keyUsage == null)
            return false;
        if (keyUsage.length < 3)
            return false;
        else if (keyUsage.length == 3)
            return keyUsage[2];
        else
            return keyUsage[2] || keyUsage[3];
    }

    public static boolean haveSameValidityPeriod(X509CertImpl cert1,
            X509CertImpl cert2) {
        long notBefDiff = 0;
        long notAfterDiff = 0;

        try {
            notBefDiff = Math.abs(cert1.getNotBefore().getTime() -
                        cert2.getNotBefore().getTime());
            notAfterDiff = Math.abs(cert1.getNotAfter().getTime() -
                        cert2.getNotAfter().getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notBefDiff <= 1000 && notAfterDiff <= 1000;
    }

    public static boolean isSmimePair(X509CertImpl cert1, X509CertImpl cert2, boolean matchSubjectDN) {
        // Check for subjectDN equality.
        if (matchSubjectDN) {
            String dn1 = cert1.getSubjectName().toString();
            String dn2 = cert2.getSubjectName().toString();

            if (!sameSubjectDN(dn1, dn2))
                return false;
        }

        // Check for the presence of signing and encryption certs.
        boolean hasSigningCert = isSigningCert(cert1) || isSigningCert(cert2);

        if (!hasSigningCert)
            return false;

        boolean hasEncryptionCert = isEncryptionCert(cert1) || isEncryptionCert(cert2);

        if (!hasEncryptionCert)
            return false;

        // If both certs have signing & encryption usage set, they are
        // not really pairs.
        if ((isSigningCert(cert1) && isEncryptionCert(cert1)) ||
                (isSigningCert(cert2) && isEncryptionCert(cert2)))
            return false;

        // See if the certs have the same validity.
        boolean haveSameValidity =
                haveSameValidityPeriod(cert1, cert2);

        return haveSameValidity;
    }

    public static boolean isNotYetValidCert(X509CertImpl cert) {
        boolean ret = false;

        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
        } catch (CertificateNotYetValidException e) {
            ret = true;
        } catch (Exception e) {
        }
        return ret;
    }

    public static boolean isValidCert(X509CertImpl cert) {
        boolean ret = true;

        try {
            cert.checkValidity();
        } catch (Exception e) {
            ret = false;
        }
        return ret;
    }

    public static boolean isExpiredCert(X509CertImpl cert) {
        boolean ret = false;

        try {
            cert.checkValidity();
        } catch (CertificateExpiredException e) {
            ret = true;
        } catch (Exception e) {
        }
        return ret;
    }

    public static boolean sameSubjectDN(String dn1, String dn2) {
        boolean ret = false;

        // The dn cannot be null.
        if (dn1 == null || dn2 == null)
            return false;
        try {
            X500Name n1 = new X500Name(dn1);
            X500Name n2 = new X500Name(dn2);

            ret = n1.equals(n2);
        } catch (Exception e) {
        }
        return ret;
    }

    public static String getValidCertsDisplayInfo(String cn, X509CertImpl[] validCerts) {
        StringBuffer sb = new StringBuffer(1024);

        sb.append(cn + "'s Currently Valid Certificates\n\n");
        sb.append(getCertsDisplayInfo(validCerts));
        return new String(sb);
    }

    public static String getExpiredCertsDisplayInfo(String cn, X509CertImpl[] expiredCerts) {
        StringBuffer sb = new StringBuffer(1024);

        sb.append(cn + "'s Expired Certificates\n\n");
        sb.append(getCertsDisplayInfo(expiredCerts));
        return new String(sb);
    }

    public static String getRenewedCertsDisplayInfo(String cn,
            X509CertImpl[] validCerts, X509CertImpl[] renewedCerts) {
        StringBuffer sb = new StringBuffer(1024);

        if (validCerts != null) {
            sb.append(cn + "'s Currently Valid Certificates\n\n");
            sb.append(getCertsDisplayInfo(validCerts));
            sb.append("\n\nRenewed Certificates\n\n\n");
        } else
            sb.append(cn + "'s Renewed Certificates\n\n");
        sb.append(getCertsDisplayInfo(renewedCerts));
        return new String(sb);
    }

    public static String getCertsDisplayInfo(X509CertImpl[] validCerts) {
        // We assume that the given pair is a valid S/MIME pair.
        StringBuffer sb = new StringBuffer(1024);

        sb.append("Subject DN: " + validCerts[0].getSubjectName().toString());
        sb.append("\n");
        X509CertImpl signingCert, encryptionCert;

        if (isSigningCert(validCerts[0])) {
            signingCert = validCerts[0];
            encryptionCert = validCerts[1];
        } else {
            signingCert = validCerts[1];
            encryptionCert = validCerts[0];
        }
        sb.append("Signing      Certificate Serial No: " + signingCert.getSerialNumber().toString(16).toUpperCase());
        sb.append("\n");
        sb.append("Encryption Certificate Serial No: " + encryptionCert.getSerialNumber().toString(16).toUpperCase());
        sb.append("\n");
        sb.append("Validity: From: "
                + signingCert.getNotBefore().toString() + "  To: " + signingCert.getNotAfter().toString());
        sb.append("\n");
        return new String(sb);
    }

    /**
     * Returns the index of the given cert in an array of certs.
     *
     * Assumptions: The certs are issued by the same CA
     *
     * @param certArray The array of certs.
     * @param givenCert The certificate we are lokking for in the array.
     * @return -1 if not found or the index of the given cert in the array.
     */
    public static int getCertIndex(X509CertImpl[] certArray, X509CertImpl givenCert) {
        int i = 0;

        for (; i < certArray.length; i++) {
            if (certArray[i].getSerialNumber().equals(
                    givenCert.getSerialNumber())) {
                break;
            }
        }

        return ((i == certArray.length) ? -1 : i);
    }

    /**
     * Returns the most recently issued signing certificate from an
     * an array of certs.
     *
     * Assumptions: The certs are issued by the same CA
     *
     * @param certArray The array of certs.
     * @param currentCert The certificate we are looking for in the array.
     * @return null if there is no recent cert or the most recent cert.
     */
    public static X509CertImpl getRecentSigningCert(X509CertImpl[] certArray,
            X509CertImpl currentCert) {
        if (certArray == null || currentCert == null)
            return null;

        // Sort the certificate array.
        Arrays.sort(certArray, new CertDateCompare());

        // Get the index of the current cert in the array.
        int i = getCertIndex(certArray, currentCert);

        if (i < 0)
            return null;

        X509CertImpl recentCert = currentCert;

        for (; i < certArray.length; i++) {
            // Check if it is a signing cert and has its
            // NotAfter later than the current cert.
            if (isSigningCert(certArray[i]) &&
                    certArray[i].getNotAfter().after(recentCert.getNotAfter()))
                recentCert = certArray[i];
        }
        return ((recentCert == currentCert) ? null : recentCert);
    }

    public static String getCertType(X509CertImpl cert) throws CertificateParsingException, IOException {
        StringBuffer sb = new StringBuffer();

        if (isSigningCert(cert))
            sb.append("signing");
        if (isEncryptionCert(cert)) {
            if (sb.length() > 0)
                sb.append("  ");
            sb.append("encryption");
        }

        // Is is object signing cert?
        CertificateExtensions extns = (CertificateExtensions)
                cert.get(X509CertImpl.NAME + "." +
                        X509CertImpl.INFO + "." +
                        X509CertInfo.EXTENSIONS);

        if (extns != null) {
            NSCertTypeExtension nsExtn = (NSCertTypeExtension)
                    extns.get(NSCertTypeExtension.NAME);

            if (nsExtn != null) {
                String nsType = getNSExtensionInfo(nsExtn);

                if (nsType != null) {
                    if (sb.length() > 0)
                        sb.append("  ");
                    sb.append(nsType);
                }
            }
        }
        return (sb.length() > 0) ? sb.toString() : null;
    }

    public static String getNSExtensionInfo(NSCertTypeExtension nsExtn) {
        StringBuffer sb = new StringBuffer();

        try {
            Boolean res;

            res = (Boolean) nsExtn.get(NSCertTypeExtension.SSL_CLIENT);
            if (res.equals(Boolean.TRUE))
                sb.append("   ssl_client");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.SSL_SERVER);
            if (res.equals(Boolean.TRUE))
                sb.append("   ssl_server");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.EMAIL);
            if (res.equals(Boolean.TRUE))
                sb.append("   email");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.OBJECT_SIGNING);
            if (res.equals(Boolean.TRUE))
                sb.append("   object_signing");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.SSL_CA);
            if (res.equals(Boolean.TRUE))
                sb.append("   ssl_CA");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.EMAIL_CA);
            if (res.equals(Boolean.TRUE))
                sb.append("   email_CA");
            res = (Boolean) nsExtn.get(NSCertTypeExtension.OBJECT_SIGNING_CA);
            if (res.equals(Boolean.TRUE))
                sb.append("   object_signing_CA");
        } catch (Exception e) {
        }

        return (sb.length() > 0) ? sb.toString() : null;
    }

    public static byte[] readFromFile(String fileName)
            throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fileName);
            int available = fin.available();
            byte[] ba = new byte[available];
            int nRead = fin.read(ba);

            if (nRead != available)
                throw new IOException("Error reading data from file: " + fileName);

            return ba;
        } finally {
            if (fin != null)
                fin.close();
        }
    }

    public static void storeInFile(String fileName, byte[] ba)
            throws IOException {
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(fileName);

            fout.write(ba);
        } finally {
            if (fout != null)
                fout.close();
        }
    }

    public static X509Certificate mapCert(String mime64)
            throws IOException {
        mime64 = stripCertBrackets(mime64.trim());
        String newval = normalizeCertStr(mime64);
        byte rawPub[] = Utils.base64decode(newval);
        X509Certificate cert = null;

        try {
            cert = new X509CertImpl(rawPub);
        } catch (CertificateException e) {
        }
        return cert;
    }

    public static X509Certificate[] mapCertFromPKCS7(String mime64)
            throws IOException {
        mime64 = stripCertBrackets(mime64.trim());
        String newval = normalizeCertStr(mime64);
        byte rawPub[] = Utils.base64decode(newval);
        PKCS7 p7 = null;

        try {
            p7 = new PKCS7(rawPub);
            return p7.getCertificates();
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }

    public static X509CRL mapCRL(String mime64)
            throws IOException {
        mime64 = stripCRLBrackets(mime64.trim());
        String newval = normalizeCertStr(mime64);
        byte rawPub[] = Utils.base64decode(newval);
        X509CRL crl = null;

        try {
            crl = new X509CRLImpl(rawPub);
        } catch (Exception e) {
        }
        return crl;
    }

    public static X509CRL mapCRL1(String mime64)
            throws IOException {
        mime64 = stripCRLBrackets(mime64.trim());
        byte rawPub[] = Utils.base64decode(mime64);
        X509CRL crl = null;

        try {
            crl = new X509CRLImpl(rawPub);
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
        return crl;
    }

    public static String normalizeCertStr(String s) {
        StringBuffer val = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                continue;
            } else if (s.charAt(i) == '\r') {
                continue;
            } else if (s.charAt(i) == '"') {
                continue;
            } else if (s.charAt(i) == ' ') {
                continue;
            }
            val.append(s.charAt(i));
        }
        return val.toString();
    }

    public static String stripCRLBrackets(String s) {
        if (s == null) {
            return s;
        }
        if (s.startsWith(CertUtil.CRL_HEADER) && s.endsWith(CertUtil.CRL_FOOTER)) {
            return (s.substring(43, (s.length() - 41)));
        }
        return s;
    }

    /**
     * strips out the begin and end certificate brackets
     *
     * @param s the string potentially bracketed with
     *            "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----"
     * @return string without the brackets
     */
    public static String stripCertBrackets(String s) {
        if (s == null) {
            return s;
        }

        if (s.startsWith(Cert.HEADER) && s.endsWith(Cert.FOOTER)) {
            return (s.substring(27, (s.length() - 25)));
        }

        // To support Thawte's header and footer
        if ((s.startsWith("-----BEGIN PKCS #7 SIGNED DATA-----")) &&
                (s.endsWith("-----END PKCS #7 SIGNED DATA-----"))) {
            return (s.substring(35, (s.length() - 33)));
        }

        return s;
    }

    /**
     * Returns a string that represents a cert's fingerprint.
     * The fingerprint is a MD5 digest of the DER encoded certificate.
     *
     * @param cert Certificate to get the fingerprint of.
     * @return a String that represents the cert's fingerprint.
     */
    public static String getFingerPrint(Certificate cert)
            throws CertificateEncodingException, NoSuchAlgorithmException {
        byte certDer[] = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("MD5");

        md.update(certDer);
        byte digestedCert[] = md.digest();
        PrettyPrintFormat pp = new PrettyPrintFormat(":");
        StringBuffer sb = new StringBuffer();

        sb.append(pp.toHexString(digestedCert, 4, 20));
        return sb.toString();
    }

    /**
     * Returns a string that has the certificate's fingerprint using
     * MD5, MD2 and SHA1 hashes.
     * A certificate's fingerprint is a hash digest of the DER encoded
     * certificate.
     *
     * @param cert Certificate to get the fingerprints of.
     * @return a String with fingerprints using the MD5, MD2 and SHA1 hashes.
     *         For example,
     *
     *         <pre>
     * MD2:   78:7E:D1:F9:3E:AF:50:18:68:A7:29:50:C3:21:1F:71
     *
     * MD5:   0E:89:91:AC:40:50:F7:BE:6E:7B:39:4F:56:73:75:75
     *
     * SHA1:  DC:D9:F7:AF:E2:83:10:B2:F7:0A:77:E8:50:E2:F7:D1:15:9A:9D:00
     * </pre>
     */
    public static String getFingerPrints(Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        byte certDer[] = cert.getEncoded();
        /*
        String[] hashes = new String[] {"MD2", "MD5", "SHA1"};
        String certFingerprints = "";
        PrettyPrintFormat pp = new PrettyPrintFormat(":");

        for (int i = 0; i < hashes.length; i++) {
            MessageDigest md = MessageDigest.getInstance(hashes[i]);

            md.update(certDer);
            certFingerprints += "    " + hashes[i] + ":" +
                    pp.toHexString(md.digest(), 6 - hashes[i].length());
        }
        return certFingerprints;
        */
        return getFingerPrints(certDer);
    }

    /**
     * Returns a string that has the certificate's fingerprint using
     * MD5, MD2 and SHA1 hashes.
     * A certificate's fingerprint is a hash digest of the DER encoded
     * certificate.
     *
     * @param certDer Certificate to get the fingerprints of.
     * @return a String with fingerprints using the MD5, MD2 and SHA1 hashes.
     *         For example,
     *
     *         <pre>
     * MD2:   78:7E:D1:F9:3E:AF:50:18:68:A7:29:50:C3:21:1F:71
     *
     * MD5:   0E:89:91:AC:40:50:F7:BE:6E:7B:39:4F:56:73:75:75
     *
     * SHA1:  DC:D9:F7:AF:E2:83:10:B2:F7:0A:77:E8:50:E2:F7:D1:15:9A:9D:00
     * </pre>
     */
    public static String getFingerPrints(byte[] certDer)
            throws NoSuchAlgorithmException/*, CertificateEncodingException*/{
        //        byte certDer[] = cert.getEncoded();
        String[] hashes = new String[] { "MD2", "MD5", "SHA1", "SHA256", "SHA512" };
        StringBuffer certFingerprints = new StringBuffer();
        PrettyPrintFormat pp = new PrettyPrintFormat(":");

        for (int i = 0; i < hashes.length; i++) {
            MessageDigest md = MessageDigest.getInstance(hashes[i]);

            md.update(certDer);
            certFingerprints.append(hashes[i] + ":\n" +
                    pp.toHexString(md.digest(), 8, 16));
        }
        return certFingerprints.toString();
    }

    /**
     * Check if a object identifier in string form is valid,
     * that is a string in the form n.n.n.n and der encode and decode-able.
     *
     * @param attrName attribute name (from the configuration file)
     * @param value object identifier string.
     */
    public static ObjectIdentifier checkOID(String attrName, String value)
            throws EBaseException {
        String msg = "value must be a object identifier in the form n.n.n.n";
        String msg1 = "not a valid object identifier.";
        ObjectIdentifier oid;

        try {
            oid = ObjectIdentifier.getObjectIdentifier(value);
        } catch (Exception e) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTR_VALUE",
                        attrName, msg));
        }

        // if the OID isn't valid (ex. n.n) the error isn't caught til
        // encoding time leaving a bad request in the request queue.
        DerOutputStream derOut = null;
        try {
            derOut = new DerOutputStream();

            derOut.putOID(oid);
            new ObjectIdentifier(new DerInputStream(derOut.toByteArray()));
        } catch (Exception e) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTR_VALUE",
                    attrName, msg1));
        } finally {
            try {
                derOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return oid;
    }

    public static String trimB64E(String b64e) {
        StringBuffer tmp = new StringBuffer("");
        String line = null;
        StringTokenizer tokens = new StringTokenizer(b64e, "\n");

        while (tokens.hasMoreTokens()) {
            line = tokens.nextToken();
            line = line.trim();
            tmp.append(line.trim());
            if (tokens.hasMoreTokens())
                tmp.append("\n");
        }

        return tmp.toString();
    }

    /*
     * addCTpoisonExt adds the Certificate Transparency V1 poison extension
     * to the Ceritificate Info
     *
     * @param certinfo X509CertInfo where the poison extension is to be added
     *
     * @author cfu
     */
    public static final String CT_POISON_OID = "1.3.6.1.4.1.11129.2.4.3";
    public static final boolean CT_POISON_CRITICAL = true;
    public static final byte CT_POISON_DATA[] =  new byte[] { 0x05, 0x00 };

    public static void addCTv1PoisonExt(X509CertInfo certinfo)
                throws CertificateException, IOException, EBaseException {
        String method = "CryptoUtil:addCTv1PoisonExt: ";
        ObjectIdentifier ct_poison_oid = new ObjectIdentifier(CT_POISON_OID);
        Extension ct_poison_ext = null;
        CertificateExtensions exts =  null;

        exts = (CertificateExtensions)
                certinfo.get(X509CertInfo.EXTENSIONS);
        if (exts == null) {
            logger.debug(method + " X509CertInfo.EXTENSIONS not found inf cetinfo");
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INTERNAL_ERROR", " X509CertInfo.EXTENSIONS not found inf cetinfo"));
        }
        DerOutputStream out = new DerOutputStream();
        out.putOctetString(CT_POISON_DATA);
        ct_poison_ext = new Extension(ct_poison_oid, CT_POISON_CRITICAL, out.toByteArray());
        //System.out.println(method + " ct_poison_ext id = " +
        //        ct_poison_ext.getExtensionId().toString());
        certinfo.set(X509CertInfo.EXTENSIONS, exts);

        exts.set(CT_POISON_OID, ct_poison_ext);
        certinfo.delete(X509CertInfo.EXTENSIONS);
        certinfo.set(X509CertInfo.EXTENSIONS, exts);
    }

    /*
     * for debugging
     */
    public static void printExtensions(CertificateExtensions exts) {

        String method = "CryptoUtil.printExtensions: ";
        System.out.println(method + "begins");
        try {
            if (exts == null)
                return;

            Enumeration<String> e = exts.getNames();
            while (e.hasMoreElements()) {
                String n = e.nextElement();
                Extension ext = (Extension) exts.get(n);

                System.out.println(" ---- " + ext.getExtensionId().toString());
            }
        } catch (Exception e) {
            System.out.println(method + e.toString());
        }
        System.out.println(method + "ends");
    }

    /**
     * Write the int as a big-endian byte[] of fixed width (in bytes).
     */
    public static byte[] intToFixedWidthBytes(int n, int width) {
        byte[] out = new byte[width];
        for (int i = 0; i < width; i++) {
            out[i] = (byte) (n >> ((width - i - 1) * 8));
        }
        return out;
    }

    /*
     * from byte array to hex in String
     */
    public static String bytesToHex(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean certInCertChain(X509Certificate[] certChain, X509Certificate cert) {

        for (X509Certificate c : certChain) {
            if (!cert.equals(c)) continue;
            return true;
        }

        return false;
    }
}
