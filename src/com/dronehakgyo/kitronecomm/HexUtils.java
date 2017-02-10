/**
 * 
 */
package com.dronehakgyo.kitronecomm;

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
}
