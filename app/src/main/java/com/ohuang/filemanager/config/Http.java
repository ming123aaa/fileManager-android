package com.ohuang.filemanager.config;

import android.text.TextUtils;

public class Http {
    public static final String BaseDefaultUrl = "http://192.168.8.5:8080";

    private static String BaseUrl = "";

    public static String getBaseUrl() {
        if (TextUtils.isEmpty(BaseUrl)) {
            return BaseDefaultUrl;
        }
        return BaseUrl;
    }

    public static void setBaseUrl(String s) {
        BaseUrl = s;
    }

    public static class Test {
        private static final String Parent = "/test";

        public static String Connect() {
            return getBaseUrl() + Parent + "/connect";
        }
    }

    public static class Main {
        private static final String Parent = "/main";

        public static String FileUpload() {
            return getBaseUrl() + Parent + "/fileUpload";
        }
        public static String index() {
            return getBaseUrl() + Parent + "/index";
        }

        public static String MultifileUpload() {
            return getBaseUrl() + Parent + "/multifileUpload";
        }

        public static String Download() {
            return getBaseUrl() + Parent + "/download";
        }

        /**
         * RequestParam("name")
         * @return
         */
        public static String Get() {
            return getBaseUrl() + Parent + "/get";
        }

        /**
         * @RequestParam("path")
         * @return
         */
        public static String GetAllFile() {
            return getBaseUrl() + Parent + "/getAllFile";
        }

        /**
         * @RequestParam("txt") String txt
         * @return
         */
        public static String WriteText(){ return getBaseUrl() + Parent+"/writeText"; }

        public static String ReadText(){ return getBaseUrl()+Parent+"/readText";}
    }
}
