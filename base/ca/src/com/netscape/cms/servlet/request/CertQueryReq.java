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
package com.netscape.cms.servlet.request;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * Show paged list of certificate requests matching search criteria.
 *
 * @version $Revision$, $Date$
 */
public class CertQueryReq extends QueryReq {

    public CertQueryReq() {
    }

    /**
     * Initialize the servlet. This servlet uses the template file
     * "queryReq.template" to process the response.
     *
     * @param sc servlet configuration, read from the web.xml file
     */
    public void init(ServletConfig sc) throws ServletException {

        super.init(sc);

        String tmp = sc.getInitParameter(PROP_PARSER);

        if (tmp != null) {
            if (tmp.trim().equals("CertReqParser.NODETAIL_PARSER")) {
                mParser = CertReqParser.NODETAIL_PARSER;
            } else if (tmp.trim().equals("CertReqParser.DETAIL_PARSER")) {
                mParser = CertReqParser.DETAIL_PARSER;
            }
        }
    }
}
