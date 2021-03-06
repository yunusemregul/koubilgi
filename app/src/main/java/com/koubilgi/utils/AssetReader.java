package com.koubilgi.utils;

import com.koubilgi.MainApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Uygulamanın içindeki assetlere kolayca ulaşmayı sağlayan sınıf
 */

public class AssetReader {
    // https://stackoverflow.com/a/4867192/12734824
    public static String readFileAsString(String fileName) throws java.io.IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(MainApplication.getAppContext().getAssets().open(fileName)));
        String line;
        String results = "";
        while ((line = reader.readLine()) != null) {
            results += line;
        }
        reader.close();
        return results;
    }
}
