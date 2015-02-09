/**
 * File name: TracerouteAnalysis.java
 * Author: Paul Prae
 * Last Edited: January 16th, 2012
 * Title: Project 1 
 * Course: CSCI 4760 Spring 2012
 * Professor: Professor Roberto Perdisci
 * 
 * Purpose of this file and associated program:
 *
 *	In this project, my program will take in input a textual tcpdump trace
 * of traffic generated by Traceroute and compute the time between a TCP
 * packet sent by the client and the related ICMP "Time exceeded
 * in-transit" message.
 *
 * Limitations of development resources:
 * 
 *  This code was written as directed by the instructions for Project
 * 1 found at-
 * 	http://www.cs.uga.edu/~perdisci/CSCI4760-S12/Assignments/Project_1.html
 *	This code was written using concepts, ideas, and code presented
 * in class, from the class textbook, and from various web resources. 
 * The textbook used is-
 * 	Computer Networking: A Top-Down Approach Featuring the Internet, 5/e
 * 	James F. Kurose and Keith W. Ross
 * 	Addition Wesley, ISBN: 0-13-607967-9
 * The web resources used are-
 * 	http://docs.oracle.com 
 * 
 * Statement of Academic Honesty:
 * 
 * 	The following code represents my own work. I have neither
 * received nor given inappropriate assistance. I have not copied
 * or modified code from any source other than the those resources
 * mentioned in this document. I recognize that any unauthorized 
 * assistance or plagiarism will be handled in accordance
 * with the University of Georgia's Academic Honesty Policy and the
 * policies of this course.
 *
 * This project was created using Eclipse
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Character;
import java.lang.Double;
import java.text.DecimalFormat;

/**
 * @author Praeducer
 *
 */
public class TracerouteAnalysis {

	static ArrayList<String[]> TCPSentMessages = new ArrayList<String[]>();
	
	static ArrayList<String[]> ICMPResponses = new ArrayList<String[]>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		File traceFile = new File(args[0]);

		if(!traceFile.exists()){
			
			System.err.println("Unable to find file titled: " + args[0] + ".");
			System.err.println("\n" +"Exiting program now.");
			System.exit(1);
			
		}		
		
		if(!traceFile.canRead()){
			
			System.err.println("Unable to read file titled: " + args[0] + ".");
			System.err.println("\n" +"Exiting program now.");
			System.exit(1);
			
		}
		
		FileInputStream traceStream;
		
		try {
			
			//Create a stream from file given at command line
			traceStream = new FileInputStream(traceFile);
			//create a byte array to hold file contents
			byte[] traceBytes = new byte[(int) traceFile.length()];
			//store bytes from file
			traceStream.read(traceBytes);
			traceStream.close();
			//convert bytes to string
			String traceString = new String(traceBytes);
			
			populateTCPMessages(traceString);
			//printMessages();
			analysisOutput();
			
		} catch (FileNotFoundException FNFe1) {
			System.err.println("FileNotFoundException thrown.");
			FNFe1.printStackTrace();
			System.exit(1);
		} catch (IOException IOe1) {
			System.err.println("IOEXception thrown: Unable to close stream.");
			IOe1.printStackTrace();
			System.exit(1);
		}
		
	}

	public static void populateTCPMessages(String trace){
		
		int traceLength = trace.length();
		if(traceLength > 100){
			
			int currentPosition = 0;
			String currentProto = detectProto(trace, currentPosition);
			
			if(currentProto.equals("TCP")){insertTCP(trace, currentPosition);}
			else if(currentProto.equals("ICMP")){insertICMP(trace, currentPosition);}
			
			for(; currentPosition < (traceLength - 1); currentPosition++){
				
				if((trace.charAt(currentPosition) == '\n')
						&& Character.isDigit(trace.charAt(currentPosition + 1))){
					currentPosition++;
					currentProto = detectProto(trace, currentPosition);
					if(currentProto.equals("TCP")){insertTCP(trace, currentPosition);}
					else if(currentProto.equals("ICMP")){insertICMP(trace, currentPosition);}
					
				}
				
			}	
		}
	}
	//TCPMessage[time, ttl, id, path]
	//ICMPResponse[time, responseIP, id, path]
	public static void analysisOutput(){
		
		String currentTTL;
		String currentResponseIP;
		String timeString;
		String roundingPattern = "0.000";
		DecimalFormat decimalFormat = new DecimalFormat(roundingPattern);
		//[TTL, responseIP, Time1, Time2, Time 3...]
		ArrayList<String> outputBuffer = new ArrayList<String>();
		outputBuffer.add("");
		outputBuffer.add("");
		for(int i = 0; i < TCPSentMessages.size(); i ++){
			currentTTL = "TTL " + TCPSentMessages.get(i)[1];
			currentResponseIP = "";
			for(int j = 0; j < ICMPResponses.size(); j++){			
				if((TCPSentMessages.get(i)[2].equals(ICMPResponses.get(j)[2])) && (TCPSentMessages.get(i)[3].equals(ICMPResponses.get(j)[3])))
				{
					currentResponseIP = ICMPResponses.get(j)[1];
			
					double sentTime = new Double(TCPSentMessages.get(i)[0]);
					double receivedTime = new Double(ICMPResponses.get(j)[0]);
					double timeDifference =((receivedTime * 1000) - (sentTime * 1000));
					timeString = decimalFormat.format(timeDifference);
					
					if((currentTTL.equalsIgnoreCase(outputBuffer.get(0)) && (currentResponseIP.equalsIgnoreCase(outputBuffer.get(1))))){
						outputBuffer.add(timeString + " ms");					
					}else if(currentTTL.equalsIgnoreCase(outputBuffer.get(0))){
						outputBuffer.add(currentResponseIP);
						outputBuffer.add(timeString + " ms");
					}else{
						if(outputBuffer.size() > 2) {
							for(int k = 0; k < outputBuffer.size(); k++){			
								System.out.println(outputBuffer.get(k));
							}
						}
						outputBuffer.clear();
						outputBuffer.add(currentTTL);
						outputBuffer.add(currentResponseIP);
						outputBuffer.add(timeString + " ms");
					}		
				}
			}
		}	
		if(outputBuffer.size() > 2) {
			for(int k = 0; k < outputBuffer.size(); k++){			
				System.out.println(outputBuffer.get(k));
			}
		}
	}
	
	public static void printMessages(){

		for(int j = 0; j < TCPSentMessages.size(); j++){
			System.out.println("TCPSentMessages " + j);
			System.out.println("\t" + TCPSentMessages.get(j)[0]);
			System.out.println("\t" + TCPSentMessages.get(j)[1]);
			System.out.println("\t" + TCPSentMessages.get(j)[2]);
			System.out.println("\t" + TCPSentMessages.get(j)[3]);
			System.out.println();
		}
		
		System.out.println("-------------------------");
		System.out.println("-------------------------");
		System.out.println("-------------------------");
		System.out.println("-------------------------");
		System.out.println();
		
		for(int j = 0; j < ICMPResponses.size(); j++){
			System.out.println("ICMPResponse " + j);
			System.out.println("\t" + ICMPResponses.get(j)[0]);
			System.out.println("\t" + ICMPResponses.get(j)[1]);
			System.out.println("\t" + ICMPResponses.get(j)[2]);
			System.out.println("\t" + ICMPResponses.get(j)[3]);
			System.out.println();
		}
		
	}
	
	//Figure out which protocol message is up next
	public static String detectProto(String trace, int currentPosition){
		
		String proto = null;
		
		
		for(int commaCount = 0; commaCount < 6; currentPosition++){
			
			if(trace.charAt(currentPosition) == ',') commaCount++;
			if(commaCount == 5){
				
				if(trace.charAt( (currentPosition + 8) ) == 'T'){proto = "TCP";}
				else if(trace.charAt( (currentPosition + 8) ) == 'I'){proto = "ICMP";}
			}
		}
		
		return proto;
		
	}
	
	//TCPMessage[time, ttl, id, path]
	public static void insertTCP(String trace, int currentPosition){
		
		String[] TCPMessage = new String[4];
		
		TCPMessage[0] = getTime(trace, currentPosition);
		TCPMessage[1] = getTTL(trace, currentPosition);
		TCPMessage[2] = getTCPId(trace, currentPosition);
		TCPMessage[3] = getTCPPath(trace, currentPosition);
		
		TCPSentMessages.add(TCPMessage);
	}
	
	//ICMPResponse[time, responseIP, id, path]
	public static void insertICMP(String trace, int currentPosition){
		
		String[] ICMPMessage = new String[4];
		
		ICMPMessage[0] = getTime(trace, currentPosition);
		ICMPMessage[1] = getResponseIP(trace, currentPosition);	
		ICMPMessage[2] = getICMPId(trace, currentPosition);
		ICMPMessage[3] = getICMPPath(trace, currentPosition);
		
		ICMPResponses.add(ICMPMessage);
		
	}
	
	public static String getTime(String trace, int currentPosition){		
		String time = trace.substring(currentPosition, (currentPosition + 17));
		return time;		
	}
	
	public static String getTTL(String trace, int currentPosition){	
		
		int firstComma = 0, secondComma = 0;
		
		for(int commaCount = 0; (commaCount < 2) && (currentPosition < trace.length()); currentPosition++){
			if(trace.charAt(currentPosition) == ',') commaCount++;
			if(commaCount == 1 && (firstComma == 0)) firstComma = currentPosition;
			if(commaCount == 2) secondComma = currentPosition;
		}
		
		String ttl = trace.substring((firstComma + 6), secondComma);
		return ttl;		
	}
	
	public static String getTCPId(String trace, int currentPosition){	
		
		int nextCommaLoc = 0, nextIdLoc = 0, nextIPLoc = 0;
		nextIPLoc = getNextIPLoc(trace, currentPosition);
		nextIdLoc = getNextIdLoc(trace, nextIPLoc);
		nextCommaLoc = getNextCommaLoc(trace, nextIdLoc);
		
		String TCPId = trace.substring((nextIdLoc + 3), nextCommaLoc);
		return TCPId;		
	}
	
	public static String getTCPPath(String trace, int currentPosition){
		
		int newLineLoc = getNextNewLineLoc(trace, currentPosition);
		int colonLoc = getNextColonLoc(trace, newLineLoc);
		
		String TCPPath = trace.substring((newLineLoc + 5), colonLoc);
		return TCPPath;
	}
	
	public static String getResponseIP(String trace, int currentPosition){
		
		int newLineLoc = getNextNewLineLoc(trace, currentPosition);
		int greaterThanLoc = getNextGreaterThanLoc(trace, newLineLoc);
		String responseIP = trace.substring((newLineLoc + 5), (greaterThanLoc - 1));
		return responseIP;
		
	}
	
	public static String getICMPId(String trace, int currentPosition){
		
		int nextCommaLoc = 0, nextIdLoc = 0, nextIPLoc = 0;
		nextIPLoc = getNextIPLoc(trace, currentPosition);
		currentPosition = getNextIPLoc(trace, (nextIPLoc + 1));
		nextIdLoc = getNextIdLoc(trace, currentPosition);
		nextCommaLoc = getNextCommaLoc(trace, nextIdLoc);
		
		String ICMPId = trace.substring((nextIdLoc + 3), nextCommaLoc);
		return ICMPId;
		
	}	
	
	public static String getICMPPath(String trace, int currentPosition){
		
		int newLineLoc = getNextNewLineLoc(trace, currentPosition);
		currentPosition = getNextNewLineLoc(trace, (newLineLoc + 1));
		currentPosition = getNextNewLineLoc(trace, (currentPosition + 1));
		String ICMPId = getTCPPath(trace, currentPosition);
		return ICMPId;
		
	}	
	
	public static int getNextNewLineLoc(String trace, int currentPosition){
		
		int newLineLoc = 0;		
		for(;(newLineLoc == 0) && (currentPosition < trace.length()); currentPosition++){
			if(trace.charAt(currentPosition) == '\n') newLineLoc = currentPosition;
		}
		return newLineLoc;
		
	}
	
	public static int getNextColonLoc(String trace, int currentPosition){
		
		int colonLoc = 0;		
		for(;(colonLoc == 0) && (currentPosition < trace.length()); currentPosition++){
			if(trace.charAt(currentPosition) == ':') colonLoc = currentPosition;
		}
		return colonLoc;
		
	}
	
	public static int getNextGreaterThanLoc(String trace, int currentPosition){
		
		int greaterThanLoc = 0;		
		for(;(greaterThanLoc == 0) && (currentPosition < trace.length()); currentPosition++){
			if(trace.charAt(currentPosition) == '>') greaterThanLoc = currentPosition;
		}
		return greaterThanLoc;
		
	}
	
	public static int getNextIPLoc(String trace, int currentPosition){
		
		int nextIPLoc = 0;
		
		for(;(nextIPLoc == 0) && ((currentPosition + 1) < trace.length()); currentPosition++){
			
			if( (trace.charAt(currentPosition) == 'I') && (trace.charAt(currentPosition + 1) == 'P') ){
				nextIPLoc = currentPosition;
			}
			
		}
		
		return nextIPLoc;
	}

	public static int getNextIdLoc(String trace, int currentPosition){
		
		int nextIdLoc = 0;
		
		for(;(nextIdLoc == 0) && ((currentPosition + 1) < trace.length()); currentPosition++){
			
			if( (trace.charAt(currentPosition) == 'i') && (trace.charAt(currentPosition + 1) == 'd') ){
				nextIdLoc = currentPosition;
			}
			
		}
		
		return nextIdLoc;
	}
	
	public static int getNextCommaLoc(String trace, int currentPosition){
		
		int nextCommaLoc = 0;
		
		for(;(nextCommaLoc == 0) && ((currentPosition) < trace.length()); currentPosition++){
			
			if(trace.charAt(currentPosition) == ','){
				nextCommaLoc = currentPosition;
			}
			
		}
		
		return nextCommaLoc;
	}
	
}
