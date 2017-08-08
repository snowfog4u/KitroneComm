/**
 * 
 */
package com.dronehakgyo.kitronecomm;

import android.util.*;

/**
 * @author LBO
 *
 */
public class HexUtils {

	public static String hexToStringPrint( byte[] ba, int offset, int length )
	{
		StringBuilder sb = new StringBuilder();
		
		for( int i = offset; i < offset + length; i++ )
		{
			byte b = ba[i];
			sb.append( String.format("%02x ", (b & 0xFF)) );
		}
		
		return sb.toString();		
	}
	
	public static String hexToStringPrint( byte[] ba )
	{
		return hexToStringPrint( ba, 0, ba.length );
	}
	
	public static String asciiToStringPrint( byte[] ba )
	{
		StringBuilder sb = new StringBuilder( ba.length );
		
		for( int i = 0; i < ba.length; i++ )
		{
			Log.d( "HexUtils", "ba[" + i +"] = " + ba[i] );
			if( (ba[i] & 0xFF) < 0 ) throw new IllegalArgumentException();
			sb.append( (char) ba[i] );
		}
		
		return sb.toString();
	}
}
