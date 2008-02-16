/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * Portions Copyright Apache Software Foundation.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.apache.catalina.ssi;


import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import org.apache.catalina.util.DateTool;
import org.apache.catalina.util.Strftime;
import org.apache.catalina.util.URLEncoder;
/**
 * Allows the different SSICommand implementations to share data/talk to each
 * other
 * 
 * @author Bip Thelin
 * @author Amy Roh
 * @author Paul Speed
 * @author Dan Sandberg
 * @author David Becker
 * @version $Revision: 1.5 $, $Date: 2007/05/05 05:32:20 $
 */
public class SSIMediator {
    protected final static String DEFAULT_CONFIG_ERR_MSG = "[an error occurred while processing this directive]";
    protected final static String DEFAULT_CONFIG_TIME_FMT = "%A, %d-%b-%Y %T %Z";
    protected final static String DEFAULT_CONFIG_SIZE_FMT = "abbrev";
    protected static URLEncoder urlEncoder;
    protected String configErrMsg = DEFAULT_CONFIG_ERR_MSG;
    protected String configTimeFmt = DEFAULT_CONFIG_TIME_FMT;
    protected String configSizeFmt = DEFAULT_CONFIG_SIZE_FMT;
    protected String className = getClass().getName();
    protected SSIExternalResolver ssiExternalResolver;
    protected long lastModifiedDate;
    protected int debug;
    protected Strftime strftime;
    protected SSIConditionalState conditionalState = new SSIConditionalState();
    static {
        //We try to encode only the same characters that apache does
        urlEncoder = new URLEncoder();
        urlEncoder.addSafeCharacter(',');
        urlEncoder.addSafeCharacter(':');
        urlEncoder.addSafeCharacter('-');
        urlEncoder.addSafeCharacter('_');
        urlEncoder.addSafeCharacter('.');
        urlEncoder.addSafeCharacter('*');
        urlEncoder.addSafeCharacter('/');
        urlEncoder.addSafeCharacter('!');
        urlEncoder.addSafeCharacter('~');
        urlEncoder.addSafeCharacter('\'');
        urlEncoder.addSafeCharacter('(');
        urlEncoder.addSafeCharacter(')');
    }


    public SSIMediator(SSIExternalResolver ssiExternalResolver,
            long lastModifiedDate, int debug) {
        this.ssiExternalResolver = ssiExternalResolver;
        this.lastModifiedDate = lastModifiedDate;
        this.debug = debug;
        setConfigTimeFmt(DEFAULT_CONFIG_TIME_FMT, true);
    }


    public void setConfigErrMsg(String configErrMsg) {
        this.configErrMsg = configErrMsg;
    }


    public void setConfigTimeFmt(String configTimeFmt) {
        setConfigTimeFmt(configTimeFmt, false);
    }


    public void setConfigTimeFmt(String configTimeFmt, boolean fromConstructor) {
        this.configTimeFmt = configTimeFmt;
        //What's the story here with DateTool.LOCALE_US?? Why??
        this.strftime = new Strftime(configTimeFmt, DateTool.LOCALE_US);
        //Variables like DATE_LOCAL, DATE_GMT, and LAST_MODIFIED need to be
        // updated when
        //the timefmt changes. This is what Apache SSI does.
        setDateVariables(fromConstructor);
    }


    public void setConfigSizeFmt(String configSizeFmt) {
        this.configSizeFmt = configSizeFmt;
    }


    public String getConfigErrMsg() {
        return configErrMsg;
    }


    public String getConfigTimeFmt() {
        return configTimeFmt;
    }


    public String getConfigSizeFmt() {
        return configSizeFmt;
    }


    public SSIConditionalState getConditionalState() {
        return conditionalState;
    }


    public Collection getVariableNames() {
        Set variableNames = new HashSet();
        //These built-in variables are supplied by the mediator ( if not
        // over-written by
        // the user ) and always exist
        variableNames.add("DATE_GMT");
        variableNames.add("DATE_LOCAL");
        variableNames.add("LAST_MODIFIED");
        ssiExternalResolver.addVariableNames(variableNames);
        //Remove any variables that are reserved by this class
        Iterator iter = variableNames.iterator();
        while (iter.hasNext()) {
            String name = (String)iter.next();
            if (isNameReserved(name)) {
                iter.remove();
            }
        }
        return variableNames;
    }


    public long getFileSize(String path, boolean virtual) throws IOException {
        return ssiExternalResolver.getFileSize(path, virtual);
    }


    public long getFileLastModified(String path, boolean virtual)
            throws IOException {
        return ssiExternalResolver.getFileLastModified(path, virtual);
    }


    public String getFileText(String path, boolean virtual) throws IOException {
        return ssiExternalResolver.getFileText(path, virtual);
    }


    protected boolean isNameReserved(String name) {
        return name.startsWith(className + ".");
    }


    public String getVariableValue(String variableName) {
        return getVariableValue(variableName, "none");
    }


    public void setVariableValue(String variableName, String variableValue) {
        if (!isNameReserved(variableName)) {
            ssiExternalResolver.setVariableValue(variableName, variableValue);
        }
    }


    public String getVariableValue(String variableName, String encoding) {
        String lowerCaseVariableName = variableName.toLowerCase();
        String variableValue = null;
        if (!isNameReserved(lowerCaseVariableName)) {
            //Try getting it externally first, if it fails, try getting the
            // 'built-in'
            // value
            variableValue = ssiExternalResolver.getVariableValue(variableName);
            if (variableValue == null) {
                variableName = variableName.toUpperCase();
                variableValue = (String)ssiExternalResolver
                        .getVariableValue(className + "." + variableName);
            }
            if (variableValue != null) {
                variableValue = encode(variableValue, encoding);
            }
        }
        return variableValue;
    }


    /**
     * Applies variable substitution to the specified String and returns the
     * new resolved string.
     */
    public String substituteVariables(String val) {
        // If it has no variable references then no work
        // need to be done
        if (val.indexOf('$') < 0) return val;
        StringBuffer sb = new StringBuffer(val);
        for (int i = 0; i < sb.length();) {
            // Find the next $
            for (; i < sb.length(); i++) {
                if (sb.charAt(i) == '$') {
                    i++;
                    break;
                }
            }
            if (i == sb.length()) break;
            // Check to see if the $ is escaped
            if (i > 1 && sb.charAt(i - 2) == '\\') {
                sb.deleteCharAt(i - 2);
                i--;
                continue;
            }
            int nameStart = i;
            int start = i - 1;
            int end = -1;
            int nameEnd = -1;
            char endChar = ' ';
            // Check for {} wrapped var
            if (sb.charAt(i) == '{') {
                nameStart++;
                endChar = '}';
            }
            // Find the end of the var reference
            for (; i < sb.length(); i++) {
                if (sb.charAt(i) == endChar) break;
            }
            end = i;
            nameEnd = end;
            if (endChar == '}') end++;
            // We should now have enough to extract the var name
            String varName = sb.substring(nameStart, nameEnd);
            String value = getVariableValue(varName);
            if (value == null) value = "";
            // Replace the var name with its value
            sb.replace(start, end, value);
            // Start searching for the next $ after the value
            // that was just substituted.
            i = start + value.length();
        }
        return sb.toString();
    }


    protected String formatDate(Date date, TimeZone timeZone) {
        String retVal;
        if (timeZone != null) {
            //we temporarily change strftime. Since SSIMediator is inherently
            // single-threaded, this
            //isn't a problem
            TimeZone oldTimeZone = strftime.getTimeZone();
            strftime.setTimeZone(timeZone);
            retVal = strftime.format(date);
            strftime.setTimeZone(oldTimeZone);
        } else {
            retVal = strftime.format(date);
        }
        return retVal;
    }


    protected String encode(String value, String encoding) {
        String retVal = null;
        if (encoding.equalsIgnoreCase("url")) {
            retVal = urlEncoder.encode(value);
        } else if (encoding.equalsIgnoreCase("none")) {
            retVal = value;
        } else if (encoding.equalsIgnoreCase("entity")) {
            //Not sure how this is really different than none
            retVal = value;
        } else {
            //This shouldn't be possible
            throw new IllegalArgumentException("Unknown encoding: " + encoding);
        }
        return retVal;
    }


    public void log(String message) {
        ssiExternalResolver.log(message, null);
    }


    public void log(String message, Throwable throwable) {
        ssiExternalResolver.log(message, throwable);
    }


    protected void setDateVariables(boolean fromConstructor) {
        boolean alreadySet = ssiExternalResolver.getVariableValue(className
                + ".alreadyset") != null;
        //skip this if we are being called from the constructor, and this has
        // already
        // been set
        if (!(fromConstructor && alreadySet)) {
            ssiExternalResolver.setVariableValue(className + ".alreadyset",
                    "true");
            Date date = new Date();
            TimeZone timeZone = TimeZone.getTimeZone("GMT");
            String retVal = formatDate(date, timeZone);
            //If we are setting on of the date variables, we want to remove
            // them from the
            // user
            //defined list of variables, because this is what Apache does
            setVariableValue("DATE_GMT", null);
            ssiExternalResolver.setVariableValue(className + ".DATE_GMT",
                    retVal);
            retVal = formatDate(date, null);
            setVariableValue("DATE_LOCAL", null);
            ssiExternalResolver.setVariableValue(className + ".DATE_LOCAL",
                    retVal);
            retVal = formatDate(new Date(lastModifiedDate), null);
            setVariableValue("LAST_MODIFIED", null);
            ssiExternalResolver.setVariableValue(className + ".LAST_MODIFIED",
                    retVal);
        }
    }
}
