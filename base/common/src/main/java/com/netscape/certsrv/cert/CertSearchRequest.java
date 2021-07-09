//--- BEGIN COPYRIGHT BLOCK ---
//This program is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; version 2 of the License.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License along
//with this program; if not, write to the Free Software Foundation, Inc.,
//51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//(C) 2011 Red Hat, Inc.
//All rights reserved.
//--- END COPYRIGHT BLOCK ---

// TODO: This class is brute force. Come up with a way to divide these search filter entities into
// smaller classes
package com.netscape.certsrv.cert;

import java.util.Objects;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.netscape.certsrv.util.JSONSerializer;

/**
 * @author jmagne
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class CertSearchRequest implements JSONSerializer {

    protected String issuerDN;

    //Serial Number
    protected boolean serialNumberRangeInUse;

    protected String serialTo;

    protected String serialFrom;

    //Subject Name
    protected boolean subjectInUse;

    protected String eMail;

    protected String commonName;

    protected String userID;

    protected String orgUnit;

    protected String org;

    protected String locality;

    protected String state;

    protected String country;

    protected boolean matchExactly;

    //Status
    protected String status;

    //Revoked By
    protected String revokedBy;

    //Revoked On
    protected String revokedOnFrom;

    protected String revokedOnTo;

    //Revocation Reason
    protected String revocationReason;

    //Issued By
    protected String issuedBy;

    //Issued On
    protected String issuedOnFrom;

    protected String issuedOnTo;

    //Valid Not Before
    protected String validNotBeforeFrom;

    protected String validNotBeforeTo;

    //Valid Not After
    protected String validNotAfterFrom;

    protected String validNotAfterTo;

    //Validity Length
    protected String validityOperation;

    protected Integer validityCount;

    protected Long validityUnit;

    // Cert Type
    protected String certTypeSubEmailCA;

    protected String certTypeSubSSLCA;

    protected String certTypeSecureEmail;

    protected String certTypeSSLClient;

    protected String certTypeSSLServer;

    //Revoked By
    protected boolean revokedByInUse;

    //Revoked On
    protected boolean revokedOnInUse;

    protected boolean revocationReasonInUse;

    protected boolean issuedByInUse;

    protected boolean issuedOnInUse;

    protected boolean validNotBeforeInUse;

    protected boolean validNotAfterInUse;

    protected boolean validityLengthInUse;

    protected boolean certTypeInUse;

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    //Boolean values
    public boolean getSerialNumberRangeInUse() {
        return serialNumberRangeInUse;
    }

    public void setSerialNumberRangeInUse(boolean serialNumberRangeInUse) {
        this.serialNumberRangeInUse = serialNumberRangeInUse;
    }

    public boolean getSubjectInUse() {
        return subjectInUse;
    }

    public void setSubjectInUse(boolean subjectInUse) {
        this.subjectInUse = subjectInUse;
    }

    public boolean getRevokedByInUse() {
        return revokedByInUse;
    }

    public void setRevokedByInUse(boolean revokedByInUse) {
        this.revokedByInUse = revokedByInUse;
    }

    public boolean getRevokedOnInUse() {
        return revokedOnInUse;
    }

    public void setRevokedOnInUse(boolean revokedOnInUse) {
        this.revokedOnInUse = revokedOnInUse;
    }

    public void setRevocationReasonInUse(boolean revocationReasonInUse) {
        this.revocationReasonInUse = revocationReasonInUse;
    }

    public boolean getRevocationReasonInUse() {
        return revocationReasonInUse;
    }

    public void setIssuedByInUse(boolean issuedByInUse) {
        this.issuedByInUse = issuedByInUse;
    }

    public boolean getIssuedByInUse() {
        return issuedByInUse;
    }

    public void setIssuedOnInUse(boolean issuedOnInUse) {
        this.issuedOnInUse = issuedOnInUse;
    }

    public boolean getIssuedOnInUse() {
        return issuedOnInUse;
    }

    public void setValidNotBeforeInUse(boolean validNotBeforeInUse) {
        this.validNotBeforeInUse = validNotBeforeInUse;
    }

    public boolean getValidNotBeforeInUse() {
        return validNotBeforeInUse;
    }

    public void setValidNotAfterInUse(boolean validNotAfterInUse) {
        this.validNotAfterInUse = validNotAfterInUse;
    }

    public boolean getValidNotAfterInUse() {
        return validNotAfterInUse;
    }

    public void setValidityLengthInUse(boolean validityLengthInUse) {
        this.validityLengthInUse = validityLengthInUse;
    }

    public boolean getValidityLengthInUse() {
        return validityLengthInUse;
    }

    public void setCertTypeInUse(boolean certTypeInUse) {
        this.certTypeInUse = certTypeInUse;
    }

    public boolean getCertTypeInUse() {
        return certTypeInUse;
    }

    //Actual Values

    public String getSerialTo() {
        return serialTo;
    }

    public void setSerialTo(String serialTo) {
        this.serialTo = serialTo;
    }

    public String getSerialFrom() {
        return serialFrom;
    }

    public void setSerialFrom(String serialFrom) {
        this.serialFrom = serialFrom;
    }

    //Subject Name

    public String getEmail() {
        return eMail;
    }

    public void setEmail(String email) {
        this.eMail = email;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean getMatchExactly() {
        return matchExactly;
    }

    public void setMatchExactly(boolean matchExactly) {
        this.matchExactly = matchExactly;
    }

    //Status

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    //Revoked On

    public String getRevokedOnTo() {
        return revokedOnTo;
    }

    public void setRevokedOnTo(String revokedOnTo) {
        this.revokedOnTo = revokedOnTo;
    }

    public String getRevokedOnFrom() {
        return revokedOnFrom;
    }

    public void setRevokedOnFrom(String revokedOnFrom) {
        this.revokedOnFrom = revokedOnFrom;
    }

    //Revoked By

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }

    //Revocation Reason

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
        this.revocationReason = revocationReason;
    }

    //Issued By

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    //Issued On

    public String getIssuedOnFrom() {
        return issuedOnFrom;
    }

    public void setIssuedOnFrom(String issuedOnFrom) {
        this.issuedOnFrom = issuedOnFrom;
    }

    public String getIssuedOnTo() {
        return issuedOnTo;
    }

    public void setIssuedOnTo(String issuedOnTo) {
        this.issuedOnTo = issuedOnTo;
    }

    //Valid Not After

    public String getValidNotAfterFrom() {
        return validNotAfterFrom;
    }

    public void setValidNotAfterFrom(String validNotAfterFrom) {
        this.validNotAfterFrom = validNotAfterFrom;
    }

    public String getValidNotAfterTo() {
        return validNotAfterTo;
    }

    public void setValidNotAfterTo(String validNotAfterTo) {
        this.validNotAfterTo = validNotAfterTo;
    }

    //Valid Not Before

    public String getValidNotBeforeFrom() {
        return validNotBeforeFrom;
    }

    public void setValidNotBeforeFrom(String validNotBeforeFrom) {
        this.validNotBeforeFrom = validNotBeforeFrom;
    }

    public String getValidNotBeforeTo() {
        return validNotBeforeTo;
    }

    public void setValidNotBeforeTo(String validNotBeforeTo) {
        this.validNotBeforeTo = validNotBeforeTo;
    }

    //Validity Length

    public String getValidityOperation() {
        return validityOperation;
    }

    public void setValidityOperation(String validityOperation) {
        this.validityOperation = validityOperation;
    }

    public Long getValidityUnit() {
        return validityUnit;
    }

    public void setValidityUnit(Long validityUnit) {
        this.validityUnit = validityUnit;
    }

    public Integer getValidityCount() {
        return validityCount;
    }

    public void setValidityCount(Integer validityCount) {
        this.validityCount = validityCount;
    }

    //Cert Type

    public String getCertTypeSubEmailCA() {
        return certTypeSubEmailCA;
    }

    public void setCertTypeSubEmailCA(String certTypeSubEmailCA) {
        this.certTypeSubEmailCA = certTypeSubEmailCA;
    }

    public String getCertTypeSubSSLCA() {
        return certTypeSubSSLCA;
    }

    public void setCertTypeSubSSLCA(String certTypeSubSSLCA) {
        this.certTypeSubSSLCA = certTypeSubSSLCA;
    }

    public String getCertTypeSecureEmail() {
        return certTypeSecureEmail;
    }

    public void setCertTypeSecureEmail(String certTypeSecureEmail) {
        this.certTypeSecureEmail = certTypeSecureEmail;
    }

    public String getCertTypeSSLClient() {
        return certTypeSSLClient;
    }

    public void setCertTypeSSLClient(String SSLClient) {
        this.certTypeSSLClient = SSLClient;
    }

    public String getCertTypeSSLServer() {
        return certTypeSSLServer;
    }

    public void setCertTypeSSLServer(String SSLServer) {
        this.certTypeSSLServer = SSLServer;
    }

    public CertSearchRequest() {
        // required for JAXB (defaults)
    }

    public CertSearchRequest(MultivaluedMap<String, String> form) {
    }

    @Override
    public int hashCode() {
        return Objects.hash(certTypeInUse, certTypeSSLClient, certTypeSSLServer, certTypeSecureEmail,
                certTypeSubEmailCA, certTypeSubSSLCA, commonName, country, eMail, issuedBy, issuedByInUse, issuedOnFrom,
                issuedOnInUse, issuedOnTo, issuerDN, locality, matchExactly, org, orgUnit, revocationReason,
                revocationReasonInUse, revokedBy, revokedByInUse, revokedOnFrom, revokedOnInUse, revokedOnTo,
                serialFrom, serialNumberRangeInUse, serialTo, state, status, subjectInUse, userID, validNotAfterFrom,
                validNotAfterInUse, validNotAfterTo, validNotBeforeFrom, validNotBeforeInUse, validNotBeforeTo,
                validityCount, validityLengthInUse, validityOperation, validityUnit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CertSearchRequest other = (CertSearchRequest) obj;
        return certTypeInUse == other.certTypeInUse && Objects.equals(certTypeSSLClient, other.certTypeSSLClient)
                && Objects.equals(certTypeSSLServer, other.certTypeSSLServer)
                && Objects.equals(certTypeSecureEmail, other.certTypeSecureEmail)
                && Objects.equals(certTypeSubEmailCA, other.certTypeSubEmailCA)
                && Objects.equals(certTypeSubSSLCA, other.certTypeSubSSLCA)
                && Objects.equals(commonName, other.commonName) && Objects.equals(country, other.country)
                && Objects.equals(eMail, other.eMail) && Objects.equals(issuedBy, other.issuedBy)
                && issuedByInUse == other.issuedByInUse && Objects.equals(issuedOnFrom, other.issuedOnFrom)
                && issuedOnInUse == other.issuedOnInUse && Objects.equals(issuedOnTo, other.issuedOnTo)
                && Objects.equals(issuerDN, other.issuerDN) && Objects.equals(locality, other.locality)
                && matchExactly == other.matchExactly && Objects.equals(org, other.org)
                && Objects.equals(orgUnit, other.orgUnit) && Objects.equals(revocationReason, other.revocationReason)
                && revocationReasonInUse == other.revocationReasonInUse && Objects.equals(revokedBy, other.revokedBy)
                && revokedByInUse == other.revokedByInUse && Objects.equals(revokedOnFrom, other.revokedOnFrom)
                && revokedOnInUse == other.revokedOnInUse && Objects.equals(revokedOnTo, other.revokedOnTo)
                && Objects.equals(serialFrom, other.serialFrom)
                && serialNumberRangeInUse == other.serialNumberRangeInUse && Objects.equals(serialTo, other.serialTo)
                && Objects.equals(state, other.state) && Objects.equals(status, other.status)
                && subjectInUse == other.subjectInUse && Objects.equals(userID, other.userID)
                && Objects.equals(validNotAfterFrom, other.validNotAfterFrom)
                && validNotAfterInUse == other.validNotAfterInUse
                && Objects.equals(validNotAfterTo, other.validNotAfterTo)
                && Objects.equals(validNotBeforeFrom, other.validNotBeforeFrom)
                && validNotBeforeInUse == other.validNotBeforeInUse
                && Objects.equals(validNotBeforeTo, other.validNotBeforeTo)
                && Objects.equals(validityCount, other.validityCount)
                && validityLengthInUse == other.validityLengthInUse
                && Objects.equals(validityOperation, other.validityOperation)
                && Objects.equals(validityUnit, other.validityUnit);
    }
}
