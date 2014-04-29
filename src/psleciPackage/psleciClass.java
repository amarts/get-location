package psleciPackage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.http.NameValuePair;
import org.apache.http.ProtocolException;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class psleciClass {
    
    public static final String mServer = "http://psleci.nic.in";
    
    public static final String script = "default.aspx";
    
    public static final String script1 = "Gservice.asmx/GetGoogleObject";

    public static final String et = "__EVENTTARGET";
    public static final String dps = "ddlPS";
        
    public static final String mCky = "ASP.NET_SessionId=hsfura55tmr5gd45zovpkejq";
    

    static Document getDocFromSite (String postData) throws IOException, Exception {

       // urlConn.setRequestProperty("Cookie", cookie);
       // urlConn.setRequestProperty ("Content-Type", type);
        
        URL url = null;
        try {
            url = new URL(mServer + "/" + script);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();

        urlConn.setDoInput (true);
        urlConn.setDoOutput (true);
        //urlConn.setUseCaches (false);

        urlConn.setRequestProperty("Cookie", mCky);
        urlConn.setRequestProperty ("Content-Type", "application/x-www-form-urlencoded");
        
        urlConn.setRequestMethod("POST");
                
        urlConn.connect();

        DataOutputStream output = new DataOutputStream(urlConn.getOutputStream());

        // Construct the POST data.
        // Send the request data.
        output.writeBytes(postData);
        output.flush();
        output.close();

        BufferedReader b = new BufferedReader(new InputStreamReader (urlConn.getInputStream()));
        //urlConn.disconnect();
        StringBuilder br = new StringBuilder();
        String line = null;
        
        while (null != (line = b.readLine())) {
            br.append(line);
        }
        Document doc = Jsoup.parse(br.toString());
        
        return doc;        
    }

    static void dumpToFile (String file, String data) throws IOException {
        FileOutputStream fop = new FileOutputStream(new File(file));      
        fop.write(data.getBytes());
        fop.flush();
        fop.close();
    }

    static void dumpMapData (String file) throws Exception {
        URL url = null;
        try {
            url = new URL(mServer + "/" + script1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();

        urlConn.setDoInput (true);
        urlConn.setDoOutput (true);
        //urlConn.setUseCaches (false);

        urlConn.setRequestProperty("Cookie", mCky);
        urlConn.setRequestProperty ("Content-Type", "application/json");
        
        urlConn.setRequestMethod("POST");
                
        urlConn.connect();

        DataOutputStream output = new DataOutputStream(urlConn.getOutputStream());

        // Construct the POST data.
        // Send the request data.
        output.writeBytes("");
        output.flush();
        output.close();

        BufferedReader b = new BufferedReader(new InputStreamReader (urlConn.getInputStream()));
        //urlConn.disconnect();
        StringBuilder br = new StringBuilder();
        String line = null;
        
        while (null != (line = b.readLine())) {
            br.append(line);
        }
        
        dumpToFile(file + ".json", br.toString()); 
    }

    static List<NameValuePair> getInputElements(Document doc) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();

        /* Collect all the 'input' fields */            
        Elements inputs = doc.select("input");
    
        for (Element elm : inputs) {
            if (elm.attr("name").equals(et)) {
                /* set it later */
                continue;
            }
            if (elm.attr("name").equals("imgbtnFind")) {
                /* only required while submitting */
                continue;
            }
        
            list.add(new BasicNameValuePair(elm.attr("name"), elm.attr("value")));            
        }
        return list;
    }


    static List<NameValuePair> getKeyDetails (Document doc, String key) throws Exception {
        List<NameValuePair> list = new ArrayList<NameValuePair>();

        /* First time */
        Elements selects = doc.select("select");

        /* for states */
        for (Element elm : selects) {
            if (list.isEmpty() && elm.attr("name").equals(key)) {
                for (Element option: elm.children()) {
                    if (option.attr("value").equals("-1") == true) {
                        continue;
                    }
                    list.add(new BasicNameValuePair(option.attr("value"), option.val()));
                }
            }
        }
        return list;
    }
    
    static void getLocation() throws Exception {
        
        List<NameValuePair> params = new ArrayList<NameValuePair>();

        List<NameValuePair> states = new ArrayList<NameValuePair>();

        Document doc = getDocFromSite("");

        states = getKeyDetails(doc, "ddlState");

        Logger.getAnonymousLogger().log(Level.INFO, states.toString());
        for (NameValuePair nvp : states) {
            String stateCode = nvp.getName();
            List<NameValuePair> districts = new ArrayList<NameValuePair>();

            params = getInputElements(doc);
            params.add(new BasicNameValuePair ("ddlState", stateCode));
            params.add(new BasicNameValuePair ("ddlDistrict", "-1"));
            params.add(new BasicNameValuePair ("ddlAC", "-1"));
            params.add(new BasicNameValuePair ("ddlPS", "ALL"));
            params.add(new BasicNameValuePair (et, "ddlState"));

            //query = URLEncodedUtils.format(params, "UTF-8");

            Logger.getAnonymousLogger().log(Level.INFO, stateCode);
            doc = getDocFromSite(URLEncodedUtils.format(params, "UTF-8"));
            districts = getKeyDetails(doc, "ddlDistrict");
            Logger.getAnonymousLogger().log(Level.INFO, districts.toString());
            
            /* Assembly Constituency */
            for (NameValuePair nvp1 : districts) {
                String districtCode = nvp1.getName();
                List<NameValuePair> acList = new ArrayList<NameValuePair>();
                params = null;  
                params = getInputElements(doc);
                params.add(new BasicNameValuePair ("ddlState", stateCode));
                params.add(new BasicNameValuePair ("ddlDistrict", districtCode));
                params.add(new BasicNameValuePair ("ddlAC", "-1"));
                params.add(new BasicNameValuePair ("ddlPS", "ALL"));
                params.add(new BasicNameValuePair (et, "ddlDistrict"));

                doc = getDocFromSite(URLEncodedUtils.format(params, "UTF-8"));
            
                acList = getKeyDetails(doc, "ddlAC");
                Logger.getAnonymousLogger().log(Level.INFO, acList.toString());

                for (NameValuePair nvp2 : acList) {
                    String acCode = nvp2.getName();

                    params = getInputElements(doc);
                    params.add(new BasicNameValuePair ("ddlState", stateCode));
                    params.add(new BasicNameValuePair ("ddlDistrict", districtCode));
                    params.add(new BasicNameValuePair ("ddlAC", acCode));
                    params.add(new BasicNameValuePair ("ddlPS", "ALL"));
                    params.add(new BasicNameValuePair (et, "ddlAC"));
                    doc = getDocFromSite(URLEncodedUtils.format(params, "UTF-8"));
                    
                    /* Now Try to get the last Map Data */
                    params = null;
                    params = getInputElements(doc);
                    params.add(new BasicNameValuePair ("ddlState", stateCode));
                    params.add(new BasicNameValuePair ("ddlDistrict", districtCode));
                    params.add(new BasicNameValuePair ("ddlAC", acCode));
                    params.add(new BasicNameValuePair ("ddlPS", "ALL"));
                    params.add(new BasicNameValuePair (et, ""));
                    params.add(new BasicNameValuePair ("imgbtnFind.x", "26"));
                    params.add(new BasicNameValuePair ("imgbtnFind.y", "17"));
                    doc = getDocFromSite(URLEncodedUtils.format(params, "UTF-8"));
                    params = null;
                    /* Write the map data */
                    dumpMapData (stateCode + "-" + acCode);
                    Logger.getAnonymousLogger().log(Level.INFO, stateCode + "-" + acCode);
                    Thread.sleep (1000);
                }  
                Thread.sleep (2000);
            }
            break;
        }    
    }

    public static void main (String [] args) throws Exception {
        getLocation();
        Logger.getAnonymousLogger().log(Level.INFO, "Done");
    }
}
