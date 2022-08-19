package com.github.timmy80.mia.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A utility class to create log messages using the Logfmt format.<br>
 * Get more info at <a href="https://brandur.org/logfmt">https://brandur.org/logfmt</a>
 * @author anthony
 *
 */
public class LogFmt {
	
	static Pattern p = Pattern.compile("[^A-Za-z0-9_]+");
	
	StringBuilder strBuilder;
	LinkedHashMap<String, Object> map = new LinkedHashMap<>();

	public LogFmt append(String key, Object value) {
		
		if(strBuilder != null) // already built
			strBuilder = null; // not built anymore
		
		if(value == null)
			map.remove(key);
		else
			map.put(key, value);
		
		return this;
	}
	
	private void build(String key, String value) {
		// append a space before the next key
		if(strBuilder.length() > 0) 
			strBuilder.append(" ");
		
		strBuilder
			.append(key.replaceAll("\\s","_").replaceAll("[^A-Za-z0-9_]+", ""))
			.append("=");
			
		boolean quote = p.matcher(value).find();
		if(quote)
			strBuilder.append('"');
		
		strBuilder.append(value.replace("\"","\\\""));

		if(quote)
			strBuilder.append('"');
	}
	
	@Override
	public String toString() {
		
		if(strBuilder != null) // already built
			return strBuilder.toString();
		else	
			strBuilder = new StringBuilder();
		
		for(Map.Entry<String, Object> entry : map.entrySet()) {
			build(entry.getKey(), entry.getValue().toString());
		}
		
		return strBuilder.toString();
	}
}
