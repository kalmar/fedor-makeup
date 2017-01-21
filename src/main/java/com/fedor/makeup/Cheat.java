package com.fedor.makeup;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kalmar on 1/21/17.
 */
public class Cheat {

    private HttpClient client = HttpClientBuilder.create().build();
    private HashMap<String, String> payloadHeaders = new HashMap<>();

    private String formLogin;
    private String formPassword;

    public Cheat(String login, String password) {

        formLogin = login;
        formPassword = password;

        // init payload headers
        payloadHeaders.put("Connection", "keep-alive");
        payloadHeaders.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
        payloadHeaders.put("Upgrade-Insecure-Requests", "1");
        payloadHeaders.put("Accept-Encoding", "gzip, deflate, sdch");
        payloadHeaders.put("Accept-Language", "en-US,en;q=0.8");
        payloadHeaders.put("Host", "do.ngs.ru");
        payloadHeaders.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    }

    private void login() throws Exception {

        HttpGet req1 = new HttpGet("http://do.ngs.ru/lc/login/");

        for (String key : payloadHeaders.keySet()) {
            req1.setHeader(key, payloadHeaders.get(key));
        }

        HttpResponse response = client.execute(req1);

        int ret_code = response.getStatusLine().getStatusCode();
        if (200 != ret_code) {
            throw new Exception("GET login page failed: ret_code = " + ret_code);
        }
        log("ret1_code: " + ret_code);
        for (Header h : response.getAllHeaders()) {
            log(h.getName() + " = " + h.getValue());
        }

        HttpPost req2 = new HttpPost("http://do.ngs.ru/lc/auth/");
        payloadHeaders.put("Origin", "http://do.ngs.ru");
        for (String key : payloadHeaders.keySet()) {
            req2.setHeader(key, payloadHeaders.get(key));
        }
        req2.setHeader("Cache-Control", "max-age=0");

        ArrayList<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("login", formLogin));
        form.add(new BasicNameValuePair("pass", formPassword));

        req2.setEntity(new UrlEncodedFormEntity(form));

        response = client.execute(req2);

        ret_code = response.getStatusLine().getStatusCode();
        if (302 != ret_code) {
            throw new Exception("POST login form failed: ret_code = " + ret_code);
        }
        log("\n---\n");
        log("ret2_code: " + ret_code);
        for (Header h : response.getAllHeaders()) {
            log(h.getName() + " = " + h.getValue());
        }

        log("--length = " + response.getEntity().getContentLength());
        BufferedReader br = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        while (null != br.readLine()) {}
        br.close();
    }

    private void action() throws Exception {
        HttpPost req = new HttpPost("http://do.ngs.ru/ajax/lc_goodlist/");
        for (String key : payloadHeaders.keySet()) {
            req.setHeader(key, payloadHeaders.get(key));
        }
        req.setHeader("Referer", "http://do.ngs.ru/lc/catalog/");
        req.setHeader("X-Requested-With", "XMLHttpRequest");
        req.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");

        StringEntity entity = new StringEntity("{\"method\":\"updateAllAdverts\",\"params\":[],\"id\":1}");
        entity.setContentType("application/json");
        req.setEntity(entity);

        HttpResponse response = client.execute(req);

        int ret_code = response.getStatusLine().getStatusCode();
        if (200 != ret_code) {
            throw new Exception("POST xhr request failed: ret_code = " + ret_code);
        }
        log("\n---\n");
        log("ret3_code: " + ret_code);
        for (Header h : response.getAllHeaders()) {
            log(h.getName() + " = " + h.getValue());
        }

        log("--length = " + response.getEntity().getContentLength());
        BufferedReader br = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));

        int good = 0, bad = 0;
        JsonParser parser = new JsonParser();
        JsonObject res = parser.parse(br).getAsJsonObject();
        for (Map.Entry<String, JsonElement> k : res.getAsJsonObject("result").entrySet()) {
            JsonObject adv = k.getValue().getAsJsonObject();
            if (adv.has("success") && adv.get("success").getAsBoolean()) {
                good++;
            } else {
                bad++;
            }
        }
        br.close();

        msg("" + new Date() + " # good/bad = " + good + "/" + bad);
        if (0 < bad) {
            msg(res.toString());
        }
    }

    public void execute() throws Exception {
        login();
        action();
    }

    public static void main(String[] args) {

        if (1 != args.length) {
            msg("uasge: java -jar fedor-makeup.jar conf.json");
            return;
        }

        try {
            JsonReader reader = new JsonReader(new FileReader(args[0]));
            JsonParser parser = new JsonParser();
            JsonObject conf = parser.parse(reader).getAsJsonObject();
            reader.close();

            String login = conf.get("login").getAsString();
            String password = conf.get("password").getAsString();

            JsonArray times = conf.getAsJsonArray("times");

            while (true) {

                // sleep a while
                Thread.sleep(60 * 1000);

                String current = new SimpleDateFormat("HH:mm").format(new Date());
                for (JsonElement t : times) {
                    String tm = t.getAsString();

                    if (current.equals(tm)) {
                        try {
                            new Cheat(login, password).execute();
                        } catch (Exception ex) {
                            msg("EX: " + ex.getMessage());
                        } finally {
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            msg("EX: " + ex.getMessage());
        }
    }

    private static void log(String msg) {
        // System.out.println(msg);
    }

    private static void msg(String m) {
        System.out.println(m);
    }
}
