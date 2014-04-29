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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class psleciClass {

    public static final String mServer = "http://psleci.nic.in";

    public static final String script = "default.aspx";

    public static final String script1 = "Gservice.asmx/GetGoogleObject";

    public static final String et = "__EVENTTARGET";

    /* Random string as cookie */
    public static final String mCky = "ASP.NET_SessionId=hsfura55tmr5gd45zovpkejw";


    static BufferedReader getDataFromSite (String scrpt, String postData, String type) throws IOException, Exception {

        URL url = null;
        try {
            url = new URL(mServer + "/" + scrpt);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();

        urlConn.setDoInput (true);
        urlConn.setDoOutput (true);

        urlConn.setRequestProperty("Cookie", mCky);
        urlConn.setRequestProperty ("Content-Type", type);

        urlConn.setRequestMethod("POST");

        urlConn.connect();

        DataOutputStream output = new DataOutputStream(urlConn.getOutputStream());

        // Send the request data.
        output.writeBytes(postData);
        output.flush();
        output.close();

        BufferedReader b = new BufferedReader(new InputStreamReader (urlConn.getInputStream()));

        return b;
    }

    static Document getDocFromSite (String postData) throws IOException, Exception {
        BufferedReader b = getDataFromSite(script, postData, "application/x-www-form-urlencoded");
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
        BufferedReader b = getDataFromSite(script1, "", "application/json");
        StringBuilder br = new StringBuilder();
        String line = null;

        while (null != (line = b.readLine())) {
            br.append(line);
        }

        dumpToFile(file + ".json", br.toString());
    }

    static String buildPostData(Document doc, String target, String sCode, String dCode, String acCode, Boolean submit) {
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        String query = "";

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

        list.add(new BasicNameValuePair ("ddlState", sCode));
        list.add(new BasicNameValuePair ("ddlDistrict", dCode));
        list.add(new BasicNameValuePair ("ddlAC", acCode));
        list.add(new BasicNameValuePair ("ddlPS", "ALL"));
        list.add(new BasicNameValuePair (et, target));

        if (submit) {
            list.add(new BasicNameValuePair ("imgbtnFind.x", "26"));
            list.add(new BasicNameValuePair ("imgbtnFind.y", "17"));
        }
        query = URLEncodedUtils.format(list, "UTF-8");
        return query;
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
        List<NameValuePair> states = new ArrayList<NameValuePair>();
        String acCode = "-1";
        String districtCode = "-1";
        String query = "";
        Document doc = getDocFromSite("");

        states = getKeyDetails(doc, "ddlState");

        for (NameValuePair nvp : states) {
            String stateCode = nvp.getName();
            if (stateCode.equals("S01") || stateCode.equals("S02"))
                continue;
            List<NameValuePair> districts = new ArrayList<NameValuePair>();
            districtCode = "-1";
            acCode = "-1";

            query = buildPostData(doc, "ddlState", stateCode, districtCode, acCode, false);

            doc = getDocFromSite(query);
            districts = getKeyDetails(doc, "ddlDistrict");
            Logger.getAnonymousLogger().log(Level.INFO, districts.toString());

            /* Assembly Constituency */
            acCode = "-1";

            for (NameValuePair nvp1 : districts) {

                districtCode = nvp1.getName();
                List<NameValuePair> acList = new ArrayList<NameValuePair>();

                query = buildPostData(doc, "ddlDistrict", stateCode, districtCode, acCode, false);

                doc = getDocFromSite(query);

                acList = getKeyDetails(doc, "ddlAC");

                for (NameValuePair nvp2 : acList) {
                    acCode = nvp2.getName();

                    query = buildPostData(doc, "ddlAC", stateCode, districtCode, acCode, false);

                    doc = getDocFromSite(query);

                    /* Now Try to get the last Map Data */
                    query = buildPostData(doc, "", stateCode, districtCode, acCode, true);

                    doc = getDocFromSite(query);

                    Logger.getAnonymousLogger().log(Level.INFO, stateCode + "-" + districtCode + "-" + acCode);

                    /* Write the map data */
                    dumpMapData (stateCode + "-" + acCode);
                    Thread.sleep (1000);
                }
                Thread.sleep (2000);
            }
            Thread.sleep(3000);
        }
    }

    public static void main (String [] args) throws Exception {
        getLocation();
        Logger.getAnonymousLogger().log(Level.INFO, "Completed");
    }
}
