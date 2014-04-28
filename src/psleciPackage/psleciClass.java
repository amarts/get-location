package psleciPackage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class psleciClass {
    
    public static final String mServer = "YOU-KNOW-THE-SITE";
    
    public static final String script = "default.aspx";
    
    public static final String script1 = "Gservice.asmx/GetGoogleObject";
    public static final String et = "__EVENTTARGET";
    public static final String ea = "__EVENTARGUMENT";
    public static final String ev = "__EVENTVALIDATION";
    public static final String lf = "__LASTFOCUS";
    public static final String vs = "__VIEWSTATE";
    
    public static final String ds = "ddlState";
    public static final String dd = "ddlDistrict";
    public static final String dac = "ddlAC";
    public static final String dps = "ddlPS";
    
    /* Keep some random cookie here :-) */
    public static final String cookie = "ASP.NET_SessionId=hsfura55tmr5gd45zovpkejq";
    
    static String getViewState (String line) {
        if (line.contains("name=\"" + vs)) {
            Matcher m = Pattern.compile(".*value=\"(.*)\".*").matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }
    
    static Boolean containsViewState (String line) {
        if (line.contains("name=\"" + vs))
            return true;
        return false;
    }
    
    static String getEventValidation (String line) {
        if (line.contains("name=\"" + ev)) {
            Matcher m = Pattern.compile(".*value=\"(.*)\".*").matcher(line);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }
    

    static Boolean containsEventValidation (String line) {
        if (line.contains("name=\"" + ev))
            return true;
        return false;
    }

    static BufferedReader getDataFromSite (String scpt, String type, String postData) throws IOException {
        URL url = null;
        try {
            url = new URL(mServer + "/" + scpt);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();

        urlConn.setDoInput (true);
        urlConn.setDoOutput (true);
        //urlConn.setUseCaches (false);

        urlConn.setRequestProperty("Cookie", cookie);
        urlConn.setRequestProperty ("Content-Type", type);
        
        try {
            urlConn.setRequestMethod("POST");
        } catch (ProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
                
        urlConn.connect();

        DataOutputStream output = new DataOutputStream(urlConn.getOutputStream());

        // Construct the POST data.
        // Send the request data.
        output.writeBytes(postData);
        output.flush();
        output.close();

        BufferedReader b = new BufferedReader(new InputStreamReader (urlConn.getInputStream()));
        //urlConn.disconnect();
        
        return b;        
    }
  
    static BufferedReader getMapData () throws IOException {
       return getDataFromSite(script1, "application/json", "");
    }
    
    static void dumpToFile (String file, BufferedReader b) throws IOException {
        File newFile = new File(file);
        FileOutputStream fop = new FileOutputStream(newFile);
        
        String line = null;
        while (null != (line = b.readLine())) {
            fop.write(line.getBytes());
        }
        fop.flush();
        fop.close();

    }

    static String buildPostData (BufferedReader b, String etv) throws IOException {
        String query = "";
        String localViewState = null;
        String localEventValidation = null;
        String line = null;
        String dacv = "";
        String ddv = "";
        String dsv = "";
        String extraString = "";
        
        while (null != (line = b.readLine())) {
            if (localViewState == null)
                localViewState = getViewState(line);
            if (localEventValidation == null)
                localEventValidation = getEventValidation(line);
        }

        if (localEventValidation != null && localEventValidation.isEmpty() == false) {
            /* Build the post String */
            Logger.getLogger("test").log(Level.INFO, "Inside");
            if (etv.equals("ddlState")) {
                dacv = "-1";
                ddv = "-1";
                dsv = "S01";
            } else if (etv.equals("ddlDistrict")) {
                dsv = "S01";
                ddv = "1";
                dacv = "-1";
            } else if (etv.equals("ddlAC")) {
                dsv = "S01";
                ddv = "1";
                dacv = "1";
            } else {
                dsv = "S01";
                ddv = "1";
                dacv = "1";
                extraString="&imgbtnFind.x=41&imgbtnFind.y=11";
            }

            List<NameValuePair> params = new ArrayList<NameValuePair>();   
            params.add(new BasicNameValuePair(et, etv));
            params.add(new BasicNameValuePair(ea, ""));
            params.add(new BasicNameValuePair(lf, ""));
            params.add(new BasicNameValuePair(ev, localEventValidation));
            params.add(new BasicNameValuePair(vs, localViewState));
            params.add(new BasicNameValuePair(ds, dsv));
            params.add(new BasicNameValuePair(dd, ddv));
            params.add(new BasicNameValuePair(dac, dacv));
            params.add(new BasicNameValuePair(dps, "ALL"));
            params.add(new BasicNameValuePair("GoogleMapForASPNet1$hidEventName", ""));
            params.add(new BasicNameValuePair("GoogleMapForASPNet1$hidEventValue", ""));
            
            if (extraString.isEmpty() == false) {
                params.add(new BasicNameValuePair("imgbtnFind.x", "41"));
                params.add(new BasicNameValuePair("imgbtnFind.y", "11"));            
            }
            
            query = URLEncodedUtils.format(params, "UTF-8");
        }

        Logger.getAnonymousLogger().log(Level.INFO, query);
        
        return query;
    }
    
    public static void main (String [] args) throws IOException {
        BufferedReader data = null;
        String postData = null;
        BufferedReader mapData = null;
        
        data = getDataFromSite(script, "application/x-www-form-urlencoded", "");

        postData = buildPostData (data, "ddlState");
        data = getDataFromSite(script, "application/x-www-form-urlencoded", postData);

        postData = buildPostData (data, "ddlDistrict");
        data = getDataFromSite(script, "application/x-www-form-urlencoded", postData);

        postData = buildPostData (data, "ddlAC");
        data = getDataFromSite(script, "application/x-www-form-urlencoded", postData);

        postData = buildPostData (data, "");
        data = getDataFromSite(script, "application/x-www-form-urlencoded", postData);

        /* Map Data is output here */
        mapData = getMapData();
        dumpToFile("temp1.json", mapData);
    }
}
