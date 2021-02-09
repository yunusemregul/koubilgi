package com.koubilgi.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AssetReader
{
	// https://stackoverflow.com/a/4867192/12734824
	public static String readFileAsString(Context context, String fileName) throws java.io.IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(fileName)));
		String line, results = "";
		while ((line = reader.readLine()) != null)
		{
			results += line;
		}
		reader.close();
		return results;
	}
}