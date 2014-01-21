/**
 * File name: UrftProtocol.java
 * Author: Paul Prae
 * Last Edited: March 23rd, 2012
 * Title: Project 3 
 * Course: CSCI 4760 Spring 2012
 * Professor: Professor Roberto Perdisci
 * 
 * Purpose of this file and associated program:
 *
 *	This will contain the common code for the Urft Client
 * and Server classes. It will follow a reliable UDP 
 * stop and wait protocol.
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



//Classes for input and output streams
import java.io.*;
//Provides classes for network support
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
public class UrftProtocol {

	//FIELDS
	//Constants
	public static final String NAMEOFSAVEFILE = "test";
	public static final short MAXDATAGRAMSIZE = 512;
	public static final short HEADERSIZE = 16;
	public static final short DATAGRAMSIZE = (100 + HEADERSIZE);
	public static final short DATASIZE = (DATAGRAMSIZE - HEADERSIZE);
	public static final short MAXTIMEOUT = 1000;
	public static final short TIMEOUT = 500;
	public static final short PROTOCOL = 17;
	
	//Provided by user
	private InetAddress destinationIPAddress;
	private InetAddress sourceIPAddress;
	private int sourcePort = 0;
	private int destinationPort = 0;
	private String filename = "none";
	
	//Create byte arrays to store the data to be sent and that is received
	private byte[] sendData = new byte[DATAGRAMSIZE];
	private byte[] receiveData = new byte[DATAGRAMSIZE];
	private ArrayList<Byte> fileArrayList = new ArrayList<Byte>(DATASIZE);
	private byte[] fileByteArray;
	private byte[] headerByteArray = new byte[HEADERSIZE];
	private byte[] nextPacket = new byte[DATAGRAMSIZE];
	
	//Details of the packets and file
	private int fileSize = 0;
	private File fileObject;
	private int numberOfPackets = 0;
	private int filePointer = 0;
	private int packetCount = 0;
	private int ACKCount = 0;
	private short checksum = 0;
	private int ACKNumber = 0;
	private int seqNumber = 0;
	
	//Sending and receiving 
	private DatagramSocket suckit;
	private DatagramPacket receivePacket;
	private DatagramPacket sendPacket;
	private boolean isHeaderStored = false;
	private int numberOfTimeouts = 0;
	
	//METHODS
	//Packeting
	
	public void send(){
		
		this.sendPacket();
		this.receiveACK();
		this.fillHeaderByteArray();
		this.setACKNumber(this.getACKFromHeader());
		this.assignHeaderFieldsToProtocolObject();
		//this.printHeaderDetails();
		
	}
	
	public void receive(){
		
		this.receivePacket();
		this.fillHeaderByteArray();
		this.setSeqNumber(this.getSeqFromHeader());
		this.assignHeaderFieldsToProtocolObject();
		//this.printHeaderDetails();
		
	}
	
	public void sendPacket(){
		
		
		
		
		this.buildNextPacket();
		this.setSendData(this.getNextPacket());
		this.initializeSendDatagram();
		p("Sending Packet Sequence Number " + this.getACKNumber());
		try {
			this.suckit.send(this.sendPacket);
			this.incrementPacketCount();
			//p("packetCount = "+ this.getPacketCount() );
			//this.printHeaderDetails();
		} catch (IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}

	public void sendACK(){
		
		
		this.setDestinationIPAddress(this.getReceivePacket().getAddress());
		this.setDestinationPort(this.getReceivePacket().getPort());
		this.buildHeader(PROTOCOL, this.getFileSize(), (short)0, this.getSeqNumber(), this.getFilePointer());
		//this.printHeaderDetails();
		p("Sending ACK Number " + this.getFilePointer());
		this.setSendData(this.addHeaderToByteArray());
		this.initializeSendDatagram();
		try {
			this.suckit.send(this.sendPacket);
			this.incrementACKCount();
			//p("ACKCount = "+ this.getACKCount());
		} catch (IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void receivePacket(){
		
		p("Receiving Packet...");
		try{
			
			//Bring in the next packet
			this.initializeReceiveDatagram();
			this.suckit.receive(this.receivePacket);
			this.incrementPacketCount();
			//p("packetCount = "+ this.getPacketCount());	
			
		} catch (IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void receiveACK(){
	
		p("Receiving ACK...");
		
		try{
			
			//Bring in the next packet
			this.initializeReceiveDatagram();
			this.suckit.setSoTimeout(TIMEOUT);
			this.suckit.receive(this.receivePacket);
			
			this.incrementACKCount();
			//p("ACKCount = "+ this.getACKCount());	
			
		}catch (SocketTimeoutException e){
			
			p("***Timeout occurred.");
			this.incrementNumberOfTimeouts();
			p("Resending packet with sequence number " + this.getSeqFromHeader());
			this.send();
			
		}
		catch (IOException e) {
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void closeConnection(){
		
		this.suckit.close();
		
	}
	
	public void buildNextPacket(){
		//TODO think about it...
		this.setFilePointer(this.getACKNumber());
		//printArrayListDetails(this.getFileArrayList());
		this.buildHeader(PROTOCOL, this.getFileSize(), (short)0, this.getFilePointer(), this.getACKNumber());
		
		this.setNextPacket(this.addHeaderToByteArray());
		
		//Put in data
		for(int i = 0;(i < DATASIZE)  && (this.getFilePointer() < this.getFileSize()); i++){
			
			//p("Data size: " + DATASIZE + ", i: " + i + ", filePointer: " + this.getFilePointer() + ", fileSize: " + this.getFileSize());
			this.getNextPacket()[i + HEADERSIZE] = this.getFileArrayList().get(this.getFilePointer());
			//p("Packet " + i + "=" + nextPacket[i]);
			
			this.incrementFilePointer();
		}
		
		//printByteArrayDetails(this.getNextPacket());
		
		
	}

	//DatagramPacket will take care of: entire length of UDP protocol via getLength() and it will get the IP or Port of the machine to 
	// which it is sent or received via getAddress() or getPort(). The rest of the header information will be included below
	//byte[0]-[1] is protocol, [2]-[5] is fileSize, [6]-[7] is checksum, [8]-[11] is seq, [12]-[15] is ACK
	public void buildHeader(short protocol, int fileSize, short checksum, int seq, int ACK ){
		
		ArrayList<Byte> tempList = new ArrayList<Byte>(HEADERSIZE);
		//Which protocol is being used
		byte[] protocolBytes = shortToByteArray(protocol);
		tempList = addByteArrayToArrayList(protocolBytes, tempList);
		//The total length of the file that is being sent
		byte[] fileSizeBytes = intToByteArray(fileSize);
		tempList = addByteArrayToArrayList(fileSizeBytes, tempList);
		//The checksum for error detection
		byte[] checksumBytes = shortToByteArray(checksum);
		tempList = addByteArrayToArrayList(checksumBytes, tempList);
		//The sequence number
		byte[] seqBytes = intToByteArray(seq);
		tempList = addByteArrayToArrayList(seqBytes, tempList);
		//The ACK number
		byte[] ACKBytes = intToByteArray(ACK);
		tempList = addByteArrayToArrayList(ACKBytes, tempList);
		
		this.headerByteArray = arrayListToByteArray(tempList);
		this.setIsHeaderStored(true);
	}
	
	public byte[] addHeaderToByteArray(){
		
		byte[] byteArray = new byte[DATAGRAMSIZE];
		
		for(int i = 0; i < HEADERSIZE; i++){
			
			byteArray[i] = this.getHeaderByteArray()[i];
			
		}
		
		return byteArray;
	}
	
	public void fillHeaderByteArray(){
		
		for(int i = 0; i < HEADERSIZE; i++){
		
			this.getHeaderByteArray()[i] = this.getReceivePacket().getData()[i];
			
		}
	}
	
	public void assignHeaderFieldsToProtocolObject(){
		
			this.setFileSize(this.getFileSizeFromHeader());
			this.setChecksum(this.getChecksumFromHeader());
		
	}

	
	public boolean isComplete(){
		
		if((this.getFilePointer()) >= this.getFileSize()){return true;}
		else{return false;}
	}
	
	public boolean isLastPacket(){
		
		if((this.getFilePointer() + DATASIZE) >= this.getFileSize()){return true;}
		else{return false;}
	}
	
	//Incrementations
	public void incrementPacketCount(){
		
		this.packetCount++;
	}
	
	public void incrementACKCount(){
		
		this.ACKCount++;
		
	}
	
	public void incrementFilePointer(){
		
		this.filePointer++;
		
	}
	
	public void incrementNumberOfTimeouts(){
		
		this.numberOfTimeouts++;
		
	}
	
	//Verifications and initializations
	public void checkFileQuality(){

		if(!this.getFileObject().exists()){

			System.err.print("\nEXITING: File does not exist.\n");
			System.exit(1);
		}
		
		if(!this.getFileObject().isFile()){

			System.err.print("\nWARNING: File is not normal.\n");
		
		}
		
		if(!(this.getFileObject().length() > 0)){

			System.err.print("\nWARNING: File size is not greater than zero.\n");
		
		}
		
	}
	
	public void initializeConnection(){
		
		try {
			this.suckit = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("Caught SocketException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public void initializeConnection(int port){
		
		try {
			this.suckit = new DatagramSocket(port);
		} catch (SocketException e) {
			System.err.println("Caught SocketException: " + e.getMessage());
			e.printStackTrace();
		}
		
	}	
	
	public void initializeFileByteArray(long size){
		
		this.setFileByteArray(new byte[(int)size]);
		
	}

	public void initializeFileByteArray(int size){
		
		this.setFileByteArray(new byte[size]);
		
	}
	
	public void initializeReceiveDatagram(){
		
		this.receivePacket = new DatagramPacket(this.receiveData, this.receiveData.length);
		
	}
	
	public void initializeSendDatagram()
	{
		
		this.sendPacket = new DatagramPacket(this.getSendData(), this.getSendData().length, this.getDestinationIPAddress(), this.getDestinationPort());
		
	}
	
	public void initializeSendPacket(){
		
		this.sendPacket = new DatagramPacket(this.sendData, this.sendData.length, this.getDestinationIPAddress(), this.getDestinationPort());
		
	}

	
	//SETTERS
	//Set IPAddress given a String
	public void setDestinationIPAddress(String IP){
		
		try {
			//Convert String version of IPAddress to InetAddress
			this.destinationIPAddress = InetAddress.getByName(IP);
			if(!this.destinationIPAddress.isReachable(1000)){System.out.println("Destination IP Address currently Unreachable.");}
		} catch (UnknownHostException e) {
			System.err.println("Caught UnknownHostException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e){
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();	
		}
		
	}//end setIpAddress

	//Set IPAddress given a String
	public void setDestinationIPAddress(InetAddress IP){
		
		try {
			//Convert String version of IPAddress to InetAddress
			this.destinationIPAddress = IP;
			if(!this.destinationIPAddress.isReachable(1000)){System.out.println("Destination IP Address currently Unreachable.");}
		} catch (UnknownHostException e) {
			System.err.println("Caught UnknownHostException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e){
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();	
		}
		
	}//end setIpAddress
	
	//Set IPAddress from local
	public void setSourceIPAddress(){
		
		try {
			this.sourceIPAddress = InetAddress.getLocalHost();
			if(!this.sourceIPAddress.isReachable(1000)){System.out.println("Local IP Address currently Unreachable.");}
		} catch (UnknownHostException e) {
			System.err.println("Caught UnknownHostException: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e){
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();	
		}
		
	}
	
	public void setDestinationPort(String stringPort){
		
		this.destinationPort = new Integer(stringPort);
		
	}
	
	public void setDestinationPort(int intPort){
		
		this.destinationPort = intPort;
		
	}

	public void setSourcePort(String stringPort){
		
		this.sourcePort = new Integer(stringPort);
		
	}
	
	public void setSourcePort(int intPort){
		
		this.sourcePort = intPort;
		
	}
	
	public void setFilename(String stringFilename){
		
		this.filename = stringFilename;
		
	}

	
	public void setFileByteArray(byte[] array){
		
		this.fileByteArray = array;
	}
	
	public void setSendData(byte[] data){
		
		this.sendData = data;
		
	}
	
	public void setReceiveData(byte[] ack){
		
		this.receiveData = ack;
		
	}
	
	public void setFileArrayList(ArrayList<Byte> file){
		
		this.fileArrayList = file;
		
	}
	
	public void setFileSize(int size){
		
		this.fileSize = size;
		
	}
	
	public void setNumberOfPackets(int number){
		
		this.numberOfPackets = number;
		p("Total number of packets to send: " + number);
		
	}
	
	public void setFileObject(File newFile){
		
		this.fileObject = newFile;
		
	}
	
	public void setIsHeaderStored(boolean value){
		
		this.isHeaderStored = value;
		
	}
	
	public void setChecksum(short sum){
		
		this.checksum = sum;
		
	}
	
	public void setACKNumber(int number){
		
		this.ACKNumber = number;
		
	}
	
	public void setSeqNumber(int number){
		
		this.seqNumber = number;
		
	}
	
	public void setNextPacket(byte[] byteArray){
		
		this.nextPacket = byteArray;
		
	}
			
	public void setFilePointer(int pointer){
		
		this.filePointer = pointer;
		
	}
	
	//GETTERS
	public InetAddress getDestinationIPAddress(){
		
		return this.destinationIPAddress;
		
	}
	
	public InetAddress getSourceIPAddress(){
		
		return this.sourceIPAddress;
		
	}
	
	public int getDestinationPort(){
		
		return this.destinationPort;
	}

	public int getSourcePort(){
		
		return this.sourcePort;
	}
	
	public String getFilename(){
		
		return this.filename;
		
	}
	
	public byte[] getSendData(){
		
		return this.sendData;
		
	}
	
	public byte[] getReceiveData(){
		
		return this.receiveData;
		
	}
	
	public ArrayList<Byte> getFileArrayList(){
		
		return this.fileArrayList;
		
	}
	
	public byte[] getFileByteArray(){
		
		return this.fileByteArray;
		
	}
	
	public int getFileSize(){
		
		return this.fileSize;
		
	}

	
	public int getNumberOfPackets(){
		
		return this.numberOfPackets;
		
	}
	
	public File getFileObject(){
		
		return this.fileObject;
		
	}
	
	public DatagramSocket getSuckit(){
		
		return this.suckit;
		
	}
	
	public DatagramPacket getReceivePacket(){
		
		return this.receivePacket;
		
	}
	
	public int getFilePointer(){
		
		return this.filePointer;
		
	}
	
	public int getPacketCount(){
		
		return this.packetCount;
		
	}
	
	public int getACKCount(){
		
		return this.ACKCount;
		
	}
	
	public byte[] getHeaderByteArray(){
		
		return this.headerByteArray;
		
	}
	
	public short getProtocolFromHeader(){
		
		return byteArrayToShort(Arrays.copyOfRange(this.getHeaderByteArray(), 0, 2));
		
	}
	
	public int getFileSizeFromHeader(){
		
		return byteArrayToInt(Arrays.copyOfRange(this.getHeaderByteArray(), 2, 6));
		
	}
	
	public short getChecksumFromHeader(){
		
		return byteArrayToShort(Arrays.copyOfRange(this.getHeaderByteArray(), 6, 8));
		
	}
	
	public int getSeqFromHeader(){
		
		return byteArrayToInt(Arrays.copyOfRange(this.getHeaderByteArray(), 8, 12));
		
	}
	
	public int getACKFromHeader(){
		
		return byteArrayToInt(Arrays.copyOfRange(this.getHeaderByteArray(), 12, 16));
		
	}
	
	public boolean getIsHeaderStored(){
		
		return this.isHeaderStored;
		
	}
	
	public short getChecksum(){
		
		return this.checksum;
		
	}
	
	public int getSeqNumber(){
		
		return this.seqNumber;
		
	}
	
	public int getACKNumber(){
	
		return this.ACKNumber;
		
	}
	
	public byte[] getNextPacket(){
		
		return this.nextPacket;
		
	}
	
	public int getNumberOfTimeouts(){
		
		return this.numberOfTimeouts;
		
	}
	
	//Printing
	public static void p(String string){
		
		System.out.println(string);
		
	}
	
	public void printFileDetails(){
		
		p("File details: \n\tName= "+ this.getFileObject().getName());
		p("\tPath= " + this.getFileObject().getAbsolutePath());
		p("\tSize= " + this.getFileSize());
		
	}
	
	public static void printByteArrayDetails(byte[] byteArray){
		
		p("Byte array details: ");
		p("\tSize= " + byteArray.length);
		if(byteArray.length >0){p("\tByte[0]= " + byteArray[0]);}
		if(byteArray.length >1){p("\tByte[1]= " + byteArray[1]);}
		if(byteArray.length >2){p("\tByte[2]= " + byteArray[2]);}
		if(byteArray.length >3){
			p("\tByte[3]= " + byteArray[3]);
			p("\tByte[length - 1]= " + byteArray[byteArray.length - 1]);
			p("\tByte[length - 2]= " + byteArray[byteArray.length - 2]);
			p("\tByte[length - 3]= " + byteArray[byteArray.length - 3]);
			p("\tByte[length - 4]= " + byteArray[byteArray.length - 4]);
		
		}		
		
	}
	
	public static void printArrayListDetails(ArrayList<Byte> arrayList){
		
		p("ArrayList details: ");
		p("\tSize= " + arrayList.size());
		if(arrayList.size() >0){p("\tArrayList[0]= " + arrayList.get(0));}
		if(arrayList.size() >1){p("\tArrayList[1]= " + arrayList.get(1));}
		if(arrayList.size() >2){p("\tArrayList[2]= " + arrayList.get(2));}
		if(arrayList.size() >3){
			p("\tArrayList[3]= " + arrayList.get(3));
			p("\tArrayList[length - 1]= " + arrayList.get(arrayList.size() - 1));
			p("\tArrayList[length - 2]= " + arrayList.get(arrayList.size() - 2));
			p("\tArrayList[length - 3]= " + arrayList.get(arrayList.size() - 3));
			p("\tArrayList[length - 4]= " + arrayList.get(arrayList.size() - 4));
		
		}		
		
	}
	
	public void printByteArray(byte[] byteArray){
		try{
		p("Printing file...");
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		output.write(byteArray);
		String fileString = output.toString();
		p(fileString);
		} catch(IOException e){
			System.err.println("Caught IOException: " + e.getMessage());
			e.printStackTrace();	
		}
	}
	
	public void printHeaderDetails(){
		p("Header details:");
		p("\tProtocol number = " + this.getProtocolFromHeader());
		p("\tTotal size of file being transmitted = " + this.getFileSizeFromHeader());
		p("\tValue of the checksum = " + this.getChecksumFromHeader());
		p("\tCurrent sequence number = " + this.getSeqFromHeader());
		p("\tCurrent ACK number = " + this.getACKFromHeader());
		
	}
	
	//STATICS
	
	public static byte[] arrayListToByteArray(ArrayList<Byte> list){
		
		byte[] byteArray = new byte[list.size()];
		
		for(int i = 0; i < list.size(); i++){
			
			byteArray[i] = list.get(i);
			
		}
		
		return byteArray;
		
	}
	
	public static ArrayList<Byte> byteArrayToArrayList(byte[] byteArray){
		
		ArrayList<Byte> arrayList = new ArrayList<Byte>(byteArray.length);
		
		for(int i = 0; i < byteArray.length; i++){
			
			arrayList.add(byteArray[i]);
			
		}
		
		return arrayList;
		
	}
	
	//http://snippets.dzone.com/posts/show/93
	public static byte[] intToByteArray(int money){
		
		return new byte[]{
				(byte)(money >>> 24),
				(byte)(money >>> 16),
				(byte)(money >>> 8),
				(byte)money};
		
	}
	
	public static int byteArrayToInt(byte[] byteArray){
		
		return (byteArray[0] << 24)
				+ ((byteArray[1] & 0xFF) << 16)
				+ ((byteArray[2] & 0xFF) << 8)
				+ (byteArray[3] & 0xFF);
		
	}
	
	public static byte[] shortToByteArray(int money){
		
		return new byte[]{
				(byte)(money >>> 8),
				(byte)money};
		
	}
	
	public static short byteArrayToShort(byte[] byteArray){
		
		return (short) ((short) (byteArray[0] << 8)
				+ (byteArray[1] & 0xFF));
		
	}
	
	public static ArrayList<Byte> addByteArrayToArrayList(byte[] byteArray, ArrayList<Byte> arrayList){
		
		for(int i = 0; i < byteArray.length; i++){
			
			arrayList.add(byteArray[i]);
			
		}
		
		return arrayList;
		
	}
	
}
