package psleciPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.apache.http.NameValuePair;
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
        
    public static Map<String,String> mCky = null;
    

    static Document getDocFromSite (Map<String, String> params) throws IOException, Exception {

       // urlConn.setRequestProperty("Cookie", cookie);
       // urlConn.setRequestProperty ("Content-Type", type);
        
        if (params != null)
            Logger.getAnonymousLogger().log(Level.INFO, params.toString());

        Connection.Response res = null;
        if (mCky != null) {
            res = Jsoup.connect(mServer + "/" + script)
                    .data(params)
                    .cookie("ASP.NET_SessionId", "hsfura55tmr5gd45zovpjejq")
                    .method(Method.POST)
                    .execute();
        } else {
            /* first call */
            res = Jsoup.connect(mServer + "/" + script).method(Method.GET).execute();
        }
        Document doc = res.parse();
        if (mCky == null) {
            mCky = res.cookies();
        }
        
        return doc;        
    }

    static void dumpToFile (String file, String data) throws IOException {
        FileOutputStream fop = new FileOutputStream(new File(file));      
        fop.write(data.getBytes());
        fop.flush();
        fop.close();
    }

    static void dumpMapData (String file) throws Exception {
        Connection.Response res = Jsoup.connect(mServer + "/" + script1)
                .cookies(mCky)
                .ignoreContentType(true)
                .method(Method.POST)
                .execute();
        
        dumpToFile(file + ".json", res.body()); 
    }

    static Map<String,String> getInputElements(Document doc) {
        Map<String, String> map = new HashMap<String, String>();
        
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
        
            map.put(elm.attr("name"), elm.attr("value"));            
        }
        return map;
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

        Map<String, String> params = new HashMap<String, String>();
        List<NameValuePair> states = new ArrayList<NameValuePair>();

        Document doc = getDocFromSite(null);

        states = getKeyDetails(doc, "ddlState");

        Logger.getAnonymousLogger().log(Level.INFO, states.toString());
        for (NameValuePair nvp : states) {
            String stateCode = nvp.getName();
            List<NameValuePair> districts = new ArrayList<NameValuePair>();

            params = getInputElements(doc);
            params.put("ddlState", stateCode);
            params.put("ddlDistrict", "-1");
            params.put("ddlAC", "-1");
            params.put("ddlPS", "ALL");
            params.put(et, "ddlState");

            //query = URLEncodedUtils.format(params, "UTF-8");

            Logger.getAnonymousLogger().log(Level.INFO, stateCode);
            doc = getDocFromSite(params);
            Logger.getAnonymousLogger().log(Level.INFO,doc.toString());
            districts = getKeyDetails(doc, "ddlDistrict");
            Logger.getAnonymousLogger().log(Level.INFO, districts.toString());
            
            /* Assembly Constituency */
            for (NameValuePair nvp1 : districts) {
                String districtCode = nvp1.getName();
                List<NameValuePair> acList = new ArrayList<NameValuePair>();

                params = getInputElements(doc);
                params.put("ddlState", stateCode);
                params.put("ddlDistrict", districtCode);
                params.put("ddlAC", "-1");
                params.put("ddlPS", "ALL");
                params.put(et, "ddlDistrict");

                doc = getDocFromSite(params);
            
                acList = getKeyDetails(doc, "ddlAC");

                for (NameValuePair nvp2 : acList) {
                    String acCode = nvp2.getName();

                    params = getInputElements(doc);
                    params.put("ddlState", stateCode);
                    params.put("ddlDistrict", districtCode);
                    params.put("ddlAC", acCode);
                    params.put("ddlPS", "ALL");
                    params.put(et, "ddlAC");
                    doc = getDocFromSite(params);
                    
                    /* Now Try to get the last Map Data */
                    params = null;
                    params = getInputElements(doc);
                    params.put("ddlState", stateCode);
                    params.put("ddlDistrict", districtCode);
                    params.put("ddlAC", acCode);
                    params.put("ddlPS", "ALL");
                    params.put(et, "");
                    params.put("imgbtnFind.x", "26");
                    params.put("imgbtnFind.y", "17");
                    doc = getDocFromSite(params);
                    params = null;
                    /* Write the map data */
                    dumpMapData (stateCode + "-" + acCode);
                    Logger.getAnonymousLogger().log(Level.INFO, stateCode + "-" + acCode);
                    Thread.sleep (500);
                    break;
                }  
                break;
            }
            break;
        }    
    }

    public static void main (String [] args) throws Exception {
        getLocation();
        Logger.getAnonymousLogger().log(Level.INFO, "Done");
    }
}
