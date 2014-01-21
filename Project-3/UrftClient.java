import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * File name: UrftClient.java
 * Author: Paul Prae
 * Last Edited: March 23rd, 2012
 * Title: Project 3 
 * Course: CSCI 4760 Spring 2012
 * Professor: Professor Roberto Perdisci
 * 
 * Purpose of this file and associated program:
 *
 *	This will be the client side of a project that will demonstrate
 * a reliable file transfer protocol using UDP.
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


public class UrftClient extends UrftProtocol{


	
	//constructor
	public UrftClient(String userInputIP, String userInputPort, String userInputFilename){
		
			super.setDestinationIPAddress(userInputIP);
			super.setDestinationPort(userInputPort);
			super.setFilename(userInputFilename);
			super.setFileObject(new File(userInputFilename));
			super.checkFileQuality();		
			super.setFileSize((int)this.getFileObject().length());
			super.initializeFileByteArray(this.getFileSize());
			super.initializeConnection();
	
	}
	
	public void fileToArrayList(){
		p("Converting file to array list...");
		try {
			FileInputStream inputStream = new FileInputStream(this.getFileObject());
			inputStream.read(this.getFileByteArray());
			inputStream.close();
			//printByteArrayDetails(this.getFileByteArray());
			
			this.setFileArrayList(byteArrayToArrayList(this.getFileByteArray()));
			
			
		} catch (FileNotFoundException e) {
			System.err.println("Caught FileNotFoundException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e){
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();	
		}
		
	}
	
	public double calculateNumberOfPackets(){
		
		return Math.ceil((double)(this.getFileSize() ) / (double)(DATASIZE) );
		
	}
	
	public static void main(String[] args) throws Exception {
		
		checkArgs(args);
		
		//Set-up variables that will be needed
		String stringIPAddress = args[0];
		String stringPort = args[1];
		String stringFilename = args[2];
		p("User has provided the following server IP: " + stringIPAddress);
		p("User has provided the following server port number: " + stringPort);
		p("User has provided the following filename: " + stringFilename);
		
		UrftClient client = new UrftClient(stringIPAddress, stringPort, stringFilename);
		client.printFileDetails();
		
		//Pull in the data from the file
		client.fileToArrayList();
		
		//Calculate the number of packets that will need to be sent
		client.setNumberOfPackets((int)client.calculateNumberOfPackets());
		
		//Create and send one packet at a time as each is ACK'd
		do{
			client.send();
			
		}while(!client.isComplete());
		
		//client.send();
		
		//Close the socket
		client.closeConnection();
		
		//printArrayListDetails(client.getFileArrayList());
		
		p("\nFile sent with a total of " + client.getNumberOfTimeouts() + " timeouts!");
		//printArrayListDetails(client.getFileArrayList());
		System.exit(0);
	}
	
	public static void checkArgs(String[] args){
		
		if(args.length != 3){
			
			System.err.print("Incorrect number of arguments.\n Use: urft-client <server ip> <server port> <file name>");
			System.exit(1);
			
		}
		
	}

}
