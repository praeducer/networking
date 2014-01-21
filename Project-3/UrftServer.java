import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * File name: UrftServer.java
 * Author: Paul Prae
 * Last Edited: March 23rd, 2012
 * Title: Project 3 
 * Course: CSCI 4760 Spring 2012
 * Professor: Professor Roberto Perdisci
 * 
 * Purpose of this file and associated program:
 *
 *	This will be the server side of a project that will demonstrate
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


public class UrftServer extends UrftProtocol{
	
	//constructor
	public UrftServer(String userInputPort){
	
		super.setSourcePort(userInputPort);
		super.initializeConnection(getSourcePort());
	}
	
	public void addPacketDataToFileArrayList(){
	
		for(int i = HEADERSIZE; i < this.getReceivePacket().getData().length; i++){
			
			this.getFileArrayList().add(this.getFilePointer(), this.getReceivePacket().getData()[i]);
			this.incrementFilePointer();
		}
		
		//printArrayListDetails(this.getFileArrayList());
		//p("File Pointer = " + this.getFilePointer() + ", Sequence Number = " + this.getSeqNumber() + ", ACK Number = " + this.getACKNumber());

	}
	
	public void addRemainingDataToFileArrayList(){
		
		
		int restBytes = (this.getFileSize() - this.getFilePointer());
		//p("DATASIZE = " + DATASIZE + ", ACK = " + this.getACKNumber() + ", fileSize = " + this.getFileSize() + ", restBytes = " + restBytes);
		for(int i = HEADERSIZE; i < (HEADERSIZE + restBytes); i++){
			
			//p("i = " + i + ", FileArrayList size = " + this.getFileArrayList().size() + ", filePointer = " + this.getFilePointer());
			this.getFileArrayList().add(this.getFilePointer(), this.getReceivePacket().getData()[i]);
			
			this.incrementFilePointer();
		}
		
		//printArrayListDetails(this.getFileArrayList());

	}
	
	public void saveFileArrayList(){
		
		//printArrayListDetails(this.getFileArrayList());
		
		this.initializeFileByteArray(this.getFileArrayList().size());
		this.setFileByteArray(arrayListToByteArray(this.getFileArrayList()));
		this.saveByteArray();
	}
	
	public void saveByteArray(){
		
		try {
			
			FileOutputStream outputStream = new FileOutputStream(NAMEOFSAVEFILE);
			outputStream.write(this.getFileByteArray());
			outputStream.close();
		
			p("\nFile saved!");
			//printByteArrayDetails(this.getFileByteArray());
			
		}	catch (FileNotFoundException e) {
			System.err.println("Caught FileNotFoundException: " + e.getMessage());
			e.printStackTrace();
		}catch (IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}	
	
	public static void main(String[] args) throws Exception {
		
		checkArgs(args);
		String stringPort = args[0];
		p("User has provided the following server port number: " + stringPort);
		
		p("Server Started.");
		UrftServer server = new UrftServer(args[0]);
		do{
			
			server.receive();
			if((server.getSeqNumber() < server.getFilePointer())){
				p("***Ignored packet. Data already received.");
			}else if((server.getSeqNumber() > server.getFilePointer())){	
				p("***Ignored packet. Prior data not yet received.");
			}else{
				if(server.isLastPacket()){
					server.addRemainingDataToFileArrayList();					
				}else{
					server.addPacketDataToFileArrayList();
				}
			}
			server.sendACK();
			
		}while(!server.isComplete());
		
		server.saveFileArrayList();		
		server.closeConnection();
		
		//printArrayListDetails(server.getFileArrayList());
		
		System.exit(0);
		
	}
	
	public static void checkArgs(String[] args){
		
		if(args.length != 1){
			
			System.err.print("Incorrect number of arguments.\n Use: UrftServer <server port>");
			System.exit(1);
			
		}
		
	}
	
}
