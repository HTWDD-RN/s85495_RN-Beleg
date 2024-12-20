import javax.sound.midi.SysexMessage;
import java.io.*;
import java.net.DatagramPacket;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.nio.ByteBuffer;


public class SW extends ARQAbst {
    int ack=0;
    String part1;
    String part2;
    String part3;
    int ReceivedPacket=0;
    int Packetnumber=-1;
    int firstpacket=0;
    int packetNummer=0;
    int ackno;
    int ACKnumber;


    float TotalCheckValue;
    float TotalCheckValueServer;
    int i=1;
    int j=0;
    PrintWriter out=null;  //to write data in the file
    File tmpFir = new File("Resources/file.txt");
    boolean exists =tmpFir.exists();

    File tmpDir = new File("Resources");
    boolean dirExists = tmpDir.exists();


    long transfertime;
    public SW(Socket socket) {
        super(socket);
    }

    public SW(Socket socket, int sessionID) {
        super(socket, sessionID);
    }

    @Override
    public void closeConnection() {  }

    @Override
    public boolean data_req(byte[] hlData, int hlSize, boolean lastTransmission) {  //client
        int MAX_RETRIES = 10;
        int retries = 0;

        packetNummer++;//1 ,2

        if ( new String(hlData).equals("End of File Transfer") ) {
            System.out.println("TotalCheckvalueClient: "+TotalCheckValue);
            byte [] sendData = generateDataPacket(hlData, hlSize);
            socket.sendPacket(sendData);
            float value = waitForCRC();
            System.out.println("Value of CRC from Server is: "+value);
            return false;
        }

        if(packetNummer!=1) {
            //System.out.println("Check in SW");
            String inputString = new String(hlData);
            //System.out.println("Check in SW1");
            // Split the input string based on the '-' delimiter
            String[] parts = inputString.split(";;");

            //System.out.println("Check in SW2");
            // Extract individual parts

            part1 = parts[0]; //
            //System.out.println("part1");
            part2 = parts[1]; //
            //System.out.println("part1");
            // if(part3.equals("\n")){}
            //else
            //part3 = parts[2];}  //
            //System.out.println("Check in SW3");
            ACKnumber = Integer.valueOf(part2);
            //System.out.println("Check in SW3");
// Use the extracted parts as needed
            // System.out.println("Part 1: " + part1);
            //System.out.println("Part 2: " + part2);
            // System.out.println("Part 3: " + part3);
        }
        //coverting the string ack no to int

        //  System.out.println("Check3");


        while (retries < MAX_RETRIES) {

            System.out.println("check in retires");
            byte[] sendData = generateDataPacket(hlData, hlSize);
            long timeBeforeSend = System.currentTimeMillis(); //time
            socket.sendPacket(sendData);  //only send the data
            //System.out.println("Check3");
            //converting the byte data into string and calculating the CRC value
            String senddata = new String(sendData);
            System.out.println("SW: Data send: "+senddata);


            float CheckSum = CalculateCRC(senddata); //return the Checksum
            TotalCheckValue += CheckSum;      //check value einsetzen


            transfertime = System.currentTimeMillis() - timeBeforeSend ;
            logger.log(Level.FINER, "Transfertime  " + transfertime + "\n");
            //System.out.println(new String(sendData));  //print the data to be
            logger.log(Level.FINER, "Client-SW: Packet sent  + wait for ACK");

            Packetnumber++;  //Start ma 0 then 1
            System.out.println("ACKNummer is: "+ACKnumber);
            System.out.println("\n \n");
            if(waitForAck(ACKnumber)) {return true; } //if packet is received then return true

            else {
                //if instead of start other string are send  at first then it  return 256ack and that results in sending the packet again
                if(waitForAck(256)) {
                    sendData=("Start").getBytes();
                    data_req(sendData,sendData.length,false);
                }
                retries++;
                logger.log(Level.WARNING, "ACK not received. Retrying packet, Retry: " + retries);
                if (retries==10) return false;
            }
        }
        return false;
    }


    @Override
    protected boolean waitForAck(int packetNr) {  //client
        socket.setTimeout(1000); // Tbd: Timeout
        DatagramPacket ackPacket;
        try {
            ackPacket = socket.receivePacket();      //receive packet

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        String marker = new String(ackPacket.getData(),0, ackPacket.getLength()); //frage
        //System.out.println("Value of marker is "+marker);
        logger.log(Level.FINER, "Client-SW: Packet " + marker + " received");
        //System.out.println ("Print the value i need to know "+marker.equals("ACK"));
        return marker.equals("ACK"+packetNr);  //false
    }


    protected float waitForCRC() {  //client
        socket.setTimeout(1000); // Tbd: Timeout
        DatagramPacket ackPacket;
        try {
            ackPacket = socket.receivePacket();      //receive packet

        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        String marker = new String(ackPacket.getData(), 0, 15); //frage
        logger.log(Level.FINER, "Client-SW: Packet " + marker + " received");
        //System.out.println ("Print the value i need to know "+marker.equals("ACK"));

        return Float.parseFloat(marker);
    }



    @Override
    protected int getPacketNr(DatagramPacket packet) {return 0;
    } //Server should receive the PacketNr

    @Override
    protected void getAckData(DatagramPacket packet) { //Client should receive the AckData from Server
    }


    @Override
    protected int getSessionID(DatagramPacket packet) { //creating the 16bit random number

        int min = 0; // Minimum value of range
        int max = 65535; // Maximum value of range
        // Print the min and max
        System.out.println("Random value in int from "+ min + " to " + max + ":");
        // Generate random int value from min to max
        int random_int = (int)Math.floor(Math.random() * (max - min + 1) + min);
        // Printing the generated random numbers
        System.out.println(random_int);
        return random_int;
        //    return 0;
    }

    @Override
    protected byte[] generateDataPacket(byte[] sendData, int dataSize) {
        return sendData;
    }

    // ******************************** Receiver *****************************************************
    @Override
    public byte[] data_ind_req(int... values) throws TimeoutException {         //server
        DatagramPacket dataPacket;
        int check=10;
        ReceivedPacket++; // 0 declare so 1

        try {
            dataPacket = socket.receivePacket(); //receive data return garxa
            String Start = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println("Start::" +Start+"\n\n\n");
            if (firstpacket==0)
            {
                //System.out.println("Start packet is "+Start);
                if (Start.equals("Start")) {
                    //System.out.println("Start packet is "+Start);
                    firstpacket++;
                    logger.log(Level.FINER, "Server-SW: Data packet received");
                    sendAck(ack);
                    ack++;
                    return dataPacket.getData();
                }
                if ((!Start.equals("Start")))
                {
                    logger.log(Level.FINER, "Server-SW: Wrong packet received");
                    sendAck(256);
                    return dataPacket.getData();
                }
            }




            if (Start.equals("End of File Transfer")) {
                //TotalCheckValueServer
                // sendAck(0);
                System.out.println("End of File Trasfer is called ");
                sendCRC(TotalCheckValueServer);
                return dataPacket.getData();
            }

            String[] parts = Start.split(";;");

            // Extract individual parts
            if (parts.length >=3)
            {
                part1 = parts[0]; // "58705"
                part2 = parts[1]; // "0"
                part3 = parts[2]; }//
            else
            {
                part3= "\n" ;
            }

            //System.out.println("Server packet value");
            //System.out.println(part1);
            //System.out.println(part2);
            //System.out.println(part3);

            if (!dirExists) {
                //System.out.println("Directory is created");
                dirExists = tmpDir.mkdirs(); // create the directory
            }

            if (dirExists) {
                //System.out.println("Directory exist");
            } else {
                // System.out.println("Directory does not exist, and could not be created.");
            }

            if(!exists & j==0 )   //if file doesnt exist
            {
                exists = tmpFir.createNewFile();
                //System.out.println("Check Check");
                j++;
            }
            if(exists & j==0) { //file exist
                tmpFir = new File("Resources/Belegtransferdata/file" + i+".txt");
                boolean success = tmpFir.createNewFile();
                //System.out.println("Check Check exists");
            }
            j=j+1;

            //String sentence = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println("Received data is: " + part3);  // printing the data
            float CheckValueServer=CalculateCRC(part3);
            TotalCheckValueServer +=CheckValueServer;


            //Writing in the file
            BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFir, true));
            writer.write(part3); // Write the received data to the file
            writer.newLine(); // Create a new line after each packet
            writer.write('\n');
            writer.close();

            logger.log(Level.FINER, "Server-SW: Data packet received");
        } catch (TimeoutException e) {
            logger.log(Level.FINER, "Data packet receive timed out");
            throw new TimeoutException("Server-SW: receive time out");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendAck(ack);
        ack++;
        return dataPacket.getData();
    }

    @Override
    byte[] generateAckPacket(int packetNr) {
        return  ("ACK" + packetNr).getBytes();
    }

    @Override
    void sendAck(int nr) {  //send the ack called by data_init_request
        socket.sendPacket(generateAckPacket(nr));
        logger.log(Level.FINER, "Server SW: ACK sent"+nr);
    }



    void sendCRC(float nr) {  //for sending the CRC
        //int Nummer =(int) nr;
        String nummer=String.valueOf(nr);
        byte[] CRN= nummer.getBytes();
        socket.sendPacket((CRN));
    }

    @Override
    boolean checkStart(DatagramPacket packet) {return false;  }


    float CalculateCRC(String data) {

        // Create CRC32 instance
        CRC32 crc32 = new CRC32();

        // Update CRC32 with bytes from the data
        crc32.update(data.getBytes());

        // Get the computed CRC32 checksum value
        long checksumValue = crc32.getValue();

        // Display the computed CRC32 value
        System.out.println("CRC32 Checksum: " + checksumValue);

        return checksumValue;
    }
    public float TotalCheckValue (){
        return TotalCheckValue;
    }

  /*  public float CRCfromServer () throws  Exception{
        return waitForCRC();
    }

*/
}