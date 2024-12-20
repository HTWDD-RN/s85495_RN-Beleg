import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger ;


public class FileTransfer implements FT {
    private final ARQ mARQ;
    int PacketNummer = 0;
    private Socket socket;
    private String fileName;
    double TotalByteTransfer = 0;
    double TotalTransferTime = 0;

    double sendingDelay = 0;
    long startTime = System.currentTimeMillis();
    Logger logger;

    public FileTransfer(String host, Socket socket, String filename, String arq) {
        this.socket = socket;
        this.fileName = filename;
        mARQ = new SW(socket);
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.log(Level.FINER, "Client-FT " + arq + " new session ID: " + 1);
    }

    public FileTransfer(Socket socket, String dir) {
        this.socket = socket;
        mARQ = new SW(socket);
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.log(Level.FINER, "Server-FT:init");
    }

    @Override
    public boolean file_req() throws IOException {  //client

        double startTime = System.currentTimeMillis();  //time before sending the packet
        double Datenrate, Timediff;

        System.out.println("Check");

        byte[] sendData = ("Start").getBytes();
        mARQ.data_req(sendData, sendData.length, false); //send the data
        double currentTime = System.currentTimeMillis(); //currenttime after sending the data
        double bytesent = sendData.length;
        TotalByteTransfer = TotalByteTransfer + bytesent;
        System.out.println("FT:Byte send: " + bytesent); //printing the size of send data
        Timediff = currentTime - startTime;   //timediff for datenrate
        TotalTransferTime += Timediff;
        Datenrate = ((double) (bytesent / Timediff) * 1000); //warum 0 ??
        System.out.println("Time to transfer the data: " + (Timediff));
        System.out.println("Datenrate : " + Datenrate + "m/s");
        logger.log(Level.FINER, "Client-FT: Start sent -Finished ");


                        //session nummer
        int sessionnummer = getSessionID();
        //Print the session nummer
        System.out.println("Session Nummer is: "+sessionnummer);
        String Packetnummer = String.valueOf(sessionnummer); //converting the session no in Packetnummer
        byte[] sendACK = Packetnummer.getBytes();

        byte[] zeichen=(";;").getBytes();
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String value;
        while ((value = in.readLine()) != null) {

            //sending the packet nummer
            PacketNummer++;
            int moduleValue = PacketNummer % 255;
            Packetnummer = String.valueOf(moduleValue);
            byte[] sendPacket = Packetnummer.getBytes();
            //mARQ.data_req(sendPacket,sendPacket.length,false);


            sendData = value.getBytes();
            startTime = System.currentTimeMillis();
            byte [] sendDataCombined =new byte[sendACK.length+zeichen.length+sendPacket.length+zeichen.length+sendData.length];
            //System.out.println("Error checking1");
            System.arraycopy(sendACK,0,sendDataCombined,0         ,sendACK.length);
            //System.out.println("Error checking1");
            System.arraycopy(zeichen,0,sendDataCombined,sendACK.length ,zeichen.length);//zeichen
            System.arraycopy(sendPacket,0,sendDataCombined,sendACK.length+zeichen.length,sendPacket.length);
            System.arraycopy(zeichen,0,sendDataCombined,sendACK.length+zeichen.length+sendPacket.length ,zeichen.length);//zeichen
            System.arraycopy(sendData,0,sendDataCombined,sendACK.length+zeichen.length+sendPacket.length+zeichen.length,sendData.length);


            System.out.println("Length: "+sendData.length);
           /* System.out.println("Length: "+sendPacket.length);
            System.out.println("Length: "+sendData.length);
            String valuee = new String(sendDataCombined);
            System.out.println("FT: Data send: "+valuee);*/

            boolean a = mARQ.data_req(sendDataCombined, sendDataCombined.length, false);
            if (!a) {
                System.out.println("End of program ");
                return false;
            }
            currentTime = System.currentTimeMillis();
            bytesent = sendData.length;
            System.out.println("Bytesend: " + bytesent + " Transfertime:  " + (currentTime - startTime));
        }

        int moduleValue = PacketNummer % 255;
        Packetnummer = String.valueOf(moduleValue);
        byte[] sendPacket = Packetnummer.getBytes();
        mARQ.data_req(sendPacket, sendPacket.length, false);
        PacketNummer++;

        System.out.println("Total Datenrate: " + (TotalByteTransfer / TotalTransferTime) * 1000 + "m/s");
        //At last  sending the end to notify the server to send the checksum and for the connection end
        byte[] lastPacket = ("End of File Transfer").getBytes();
        mARQ.data_req(lastPacket, lastPacket.length, false);

       /* float CRCfromServer=mARQ.CRCfromServer();
        float CRCfromClient =mARQ.TotalCheckValue();
        if (CRCfromServer==CRCfromClient)
        {
            System.out.println("Hurry\n Hurry \n Data transfer is successful");
        }*/
        return true;
    }


    //*****************************Receiver************************************************

    @Override
    public boolean file_init() throws IOException {  //server
        byte[] data;
        try {

            data = mARQ.data_ind_req();   //call to SW
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        String marker = new String(data, 0, 0);
        //^logger.log(Level.FINER,"Server-FT:Packet "+marker +" received");
        //return marker.equals("Start");

        return true;
    }



    protected int getSessionID() { //creating the 16bit random number

        int min = 0; // Minimum value of range
        int max = 65535; // Maximum value of range

        // Generate random int value from min to max
        int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
        // Printing the generated random numbers
        //System.out.println(random_int);
        return random_int;
        //    return 0;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}