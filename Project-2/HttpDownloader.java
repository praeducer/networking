/**
 * File name: HttpDownloader.java
 * Author: Paul Prae
 * Last Edited: February 16th, 2012
 * Title: Project 1 
 * Course: CSCI 4760 Spring 2012
 * Professor: Professor Roberto Perdisci
 * 
 * Purpose of this file and associated program:
 *
 *	In this project, my program will 
 *
 * Limitations of development resources:
 * 
 *  This code was written as directed by the instructions for Project
 * 2 found at-
 * 	http://www.cs.uga.edu/~perdisci/CSCI4760-S12/Assignments/Project2.html
 *	This code was written using concepts, ideas, and code presented
 * in class, from the class textbook, and from various web resources. 
 * The textbook used is-
 * 	Computer Networking: A Top-Down Approach Featuring the Internet, 5/e
 * 	James F. Kurose and Keith W. Ross
 * 	Addition Wesley, ISBN: 0-13-607967-9
 * The web resources used are-
 * 	http://docs.oracle.com 
 * 	http://docs.oracle.com/javase/tutorial/networking/sockets/
 * 	http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.35
 * 	
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpDownloader {

	public static String objectUrlString = new String();
	public static String objectHost = new String();
	public static String objectLocation = new String();
	public static int numberOfThreads;
	public static int port = 80;

	public static int contentLength;
	public static int commonPartSize;

	public static String dashedLine = "------------------------------";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// Get the arguments from the command line
		verifyArgs(args);

		// Find out who is hosting the file and where it is located
		parseUrl();

		// Get the size of the file.
		parseForContentLength();

		// Partition the download
		divideAndConquer();

		// Put it back together
		buildDownload();

	}

	public static boolean verifyArgs(String[] args) {

		// Get the url or the object from the command line

		if ((args.length < 1) || args[0].equals(null)) {
			objectUrlString = "http://www.cs.uga.edu/~perdisci/CSCI4760-S12/Project2-TestFiles/Uga-VII.jpg";
			System.out
					.println("\nYou did not provide a URL.\n\tThe default URL is: "
							+ objectUrlString);
		} else {
			objectUrlString = args[0];
			System.out.println("\nThe URL you provided is: " + objectUrlString);
		}

		// Get the number of connections from the command line
		if ((args.length < 2) || args[1].equals(null)) {
			numberOfThreads = 1;
			System.out
					.println("You did not provide a number of connections.\n\tThe default number of connections is: "
							+ numberOfThreads);
		} else {
			numberOfThreads = Integer.parseInt(args[1]);
			if (numberOfThreads == 0) {
				numberOfThreads = 1;
			}
			System.out
					.println("You have requested the following for the number of connections: "
							+ numberOfThreads);
		}

		return true;

	}

	public static boolean parseUrl() {

		System.out.println("\nParsing URL...");

		int indexAfterProtocol = objectUrlString.indexOf("://");
		int indexStartOfHost = indexAfterProtocol + 3;
		String restAfterProtocol = objectUrlString.substring(indexStartOfHost);
		int indexEndOfHost = indexStartOfHost + restAfterProtocol.indexOf("/");
		objectHost = objectUrlString
				.substring(indexStartOfHost, indexEndOfHost);
		objectLocation = objectUrlString.substring(indexEndOfHost);

		System.out.println("The host is: " + objectHost + ".");
		System.out.println("The file is located at: " + objectLocation + ".");

		return true;

	}

	public static boolean parseForContentLength() {

		String head = headRequest();
		int startOfContentLength = head.indexOf("Content-Length");
		int endOfContentLength = head.indexOf("\r\n", startOfContentLength);
		String stringValueOfContentLength = head.substring(
				startOfContentLength + 16, endOfContentLength);

		// Update the user
		System.out.println("\nThe length of the requested object is "
				+ stringValueOfContentLength + ".");

		contentLength = Integer.parseInt(stringValueOfContentLength);
		commonPartSize = contentLength / numberOfThreads;
		
		return true;

	}

	public static String headRequest() {

		String giveMeHead = new String();
		String iGotHead = new String();

		// Open up a socket and request the information
		try {

			Socket initialSocket = new Socket(objectHost, 80);

			// Setup IO
			BufferedReader buffReader = new BufferedReader(
					new InputStreamReader(initialSocket.getInputStream()));
			BufferedWriter buffWriter = new BufferedWriter(
					new OutputStreamWriter(initialSocket.getOutputStream()));

			// Request something
			giveMeHead = "HEAD " + objectLocation + " HTTP/1.1\r\n" + "Host: "
					+ objectHost + "\r\n" + "Connection: close\r\n"
					+ "\r\n\r\n";

			// Give User update
			System.out.println("\nSending the following request...\n"
					+ dashedLine + dashedLine);
			System.out.println("\n" + giveMeHead + dashedLine + dashedLine);

			// Send request
			buffWriter.write(giveMeHead);
			buffWriter.flush();

			String currentLine = "";
			while ((currentLine = buffReader.readLine()) != null) {

				iGotHead = iGotHead + currentLine + "\r\n";

			}

			buffReader.close();
			buffWriter.close();
			System.out.println("\nRecieved the following response...\n"
					+ dashedLine + dashedLine);
			System.out.println("\n" + iGotHead + dashedLine + dashedLine);
			return iGotHead;

		} catch (UnknownHostException e) {
			System.err.println("Host Unknown: " + objectHost + ".");
			System.err.println("\n" + "Exiting program now.");
			e.printStackTrace();
			System.exit(1);

		} catch (IOException e) {
			System.err.println("IOException thrown.");
			System.err.println("\n" + "Exiting program now.");
			e.printStackTrace();
			System.exit(1);
		}

		return null;

	}

	public static byte[] getRequest(int partLength, String headerFields) {

		String giveMeGet = new String();
		// byte[] iGotGet = new byte[contentLength];

		// Open up a socket and request the information
		try {

			Socket initialSocket = new Socket(objectHost, 80);

			// Setup IO
			DataInputStream dataInputReader = new DataInputStream(
					initialSocket.getInputStream());
			PrintWriter printWriterWriter = new PrintWriter(
					initialSocket.getOutputStream());

			// Request something
			giveMeGet = "GET " + objectLocation + " HTTP/1.1\r\n" + "Host: "
					+ objectHost + "\r\n" + headerFields
					+ "Connection: close\r\n" + "\r\n\r\n";

			// Give User update
			System.out.println("\nSending the following request...\n"
					+ dashedLine + dashedLine);
			System.out.println("\n" + giveMeGet + dashedLine + dashedLine);

			// Send request
			printWriterWriter.write(giveMeGet);
			printWriterWriter.flush();

			byte[] partContents = new byte[partLength];

			for (;;) {
				if (dataInputReader.readByte() == '\r'
						&& dataInputReader.readByte() == '\n'
						&& dataInputReader.readByte() == '\r'
						&& dataInputReader.readByte() == '\n')
					break;
			}
			int bb;
			for (int ii = 0; (bb = dataInputReader.read()) >= 0; ++ii) {
				partContents[ii] = (byte) bb;
			}

			return partContents;

		} catch (UnknownHostException e) {
			System.err.println("Host Unknown: " + objectHost + ".");
			System.err.println("\n" + "Exiting program now.");
			e.printStackTrace();
			System.exit(1);

		} catch (IOException e) {
			System.err.println("IOException thrown.");
			System.err.println("\n" + "Exiting program now.");
			e.printStackTrace();
			System.exit(1);
		}

		return null;

	}
	
	public static boolean divideAndConquer() {

		//This works too
		/*ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		
		for (int i = 0; i < numberOfThreads; i++) {

			final int threadId = i;
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					savePart(threadId);					
				}
			});

			
		}// end for

		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;*/
		
		Thread[] threads = new Thread[numberOfThreads];
		
		for (int i = 0; i < numberOfThreads; i++) {
			final int threadId = i;
			threads[i] = new Thread()
			{
				 @Override
				public void run() {
					 savePart(threadId);
				 }
			};
			threads[i].start();
		}
		
		for (int i = 0; i < numberOfThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return true;

	}

	public static boolean savePart(int partNumber) {

		String partNameString = "part_" + Integer.toString(partNumber + 1);
		int lowerBound = calculateLowerBound(partNumber);
		int upperBound = calculateUpperBound(partNumber);
		int partLength = upperBound - lowerBound + 1;

		String rangeHeader = "Range: bytes=" + lowerBound + "-" + upperBound
				+ "\r\n";
		byte[] rangeResponse = getRequest(partLength, rangeHeader);

		System.out.println("\nSaving " + partNameString + " of length "
				+ partLength + "...");
		writeToFile(partNameString, rangeResponse);

		return true;
	}

	public static int calculateLowerBound(int partNumber) {

		return partNumber * commonPartSize;

	}

	public static int calculateUpperBound(int partNumber) {

		int upperBound = 0;

		if ((partNumber + 1) == numberOfThreads) {

			upperBound = contentLength - 1;

		} else {

			upperBound = ((partNumber + 1) * commonPartSize) - 1;

		}
		return upperBound;
	}

	public static boolean writeToFile(String fileName, byte[] contents) {

		try {

			FileOutputStream outStream = new FileOutputStream(fileName);
			outStream.write(contents);
			outStream.close();

		} catch (Exception e) {
			System.err.println("\nUnable to write " + fileName + " to file.");
		}

		return true;
	}

	public static boolean buildDownload() {

		byte[] bytesOfAllParts = new byte[contentLength];
		String fileName = parseForFileName();
		int fred = 0;
		System.out
				.println("\n\nFinally, combining the parts and saving the complete file as "
						+ fileName + ".");

		for (int i = 0;i < numberOfThreads; i++) {
			try {

				FileInputStream fileStream = new FileInputStream("part_" + (i+1));
				fred += fileStream.read(bytesOfAllParts, fred, fileStream.available());
				fileStream.close();

			} catch (Exception e) {

				System.err.println("\nError during the copying of " + "part_"
						+ i + " to the complete file.");

				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}// end outer for

		writeToFile(fileName, bytesOfAllParts);

		return true;
	}

	public static String parseForFileName() {

		int startOfFilename = objectUrlString.lastIndexOf("/");
		return objectUrlString.substring((startOfFilename + 1));
	}
}
