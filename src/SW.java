import java.io.*;
import java.net.DatagramPacket;
import java.util.logging.Level;
import java.util.zip.CRC32;

public class SW extends ARQAbst {
    int ack = 0;
    String part1;
    String part2;
    String part3;
    int ReceivedPacket = 0;
    int firstpacket = 0;
    int packetNummer = 0;

    float TotalCheckValue;
    float TotalCheckValueServer;
    int i = 1;

    File tmpDir = new File("resources/Belegtransferdata");
    File tmpFir = new File(tmpDir, "file1.txt");

    public SW(Socket socket) {
        super(socket);
    }

    public SW(Socket socket, int sessionID) {
        super(socket, sessionID);
    }

    @Override
    public void closeConnection() {
    }

    @Override
    public boolean data_req(byte[] hlData, int hlSize, boolean lastTransmission) {
        int MAX_RETRIES = 10;
        int retries = 0;

        packetNummer++;

        if (new String(hlData).equals("End of File Transfer")) {
            System.out.println("TotalCheckvalueClient: " + TotalCheckValue);
            byte[] sendData = generateDataPacket(hlData, hlSize);
            socket.sendPacket(sendData);
            float value = waitForCRC();
            System.out.println("Value of CRC from Server is: " + value);
            return false;
        }

        while (retries < MAX_RETRIES) {
            System.out.println("Retry count: " + retries);
            byte[] sendData = generateDataPacket(hlData, hlSize);
            socket.sendPacket(sendData);

            String senddata = new String(sendData);
            System.out.println("SW: Data send: " + senddata);

            float CheckSum = CalculateCRC(senddata);
            TotalCheckValue += CheckSum;

            if (waitForAck(ack)) {
                return true;
            } else {
                retries++;
                System.out.println("ACK not received. Retrying... " + retries);
            }
        }

        return false;
    }

    @Override
    protected boolean waitForAck(int packetNr) {
        socket.setTimeout(2000);
        DatagramPacket ackPacket;
        try {
            ackPacket = socket.receivePacket();
        } catch (TimeoutException e) {
            System.out.println("Timeout waiting for ACK");
            return false;
        }
        String marker = new String(ackPacket.getData(), 0, ackPacket.getLength());
        return marker.equals("ACK" + packetNr);
    }

    protected float waitForCRC() {
        socket.setTimeout(2000);
        DatagramPacket ackPacket;
        try {
            ackPacket = socket.receivePacket();
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        String marker = new String(ackPacket.getData(), 0, ackPacket.getLength());
        return Float.parseFloat(marker);
    }

    @Override
    protected int getPacketNr(DatagramPacket packet) {
        return 0;
    }

    @Override
    protected void getAckData(DatagramPacket packet) {
    }

    @Override
    protected int getSessionID(DatagramPacket packet) {
        return (int) (Math.random() * 65536);
    }

    @Override
    protected byte[] generateDataPacket(byte[] sendData, int dataSize) {
        return sendData;
    }

    @Override
    public byte[] data_ind_req(int... values) throws TimeoutException {
        DatagramPacket dataPacket;
        ReceivedPacket++;

        try {
            dataPacket = socket.receivePacket();
            String Start = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println("Received packet: " + Start);

            if (firstpacket == 0 && Start.equals("Start")) {
                firstpacket++;
                sendAck(ack++);
                return dataPacket.getData();
            }

            if (Start.equals("End of File Transfer")) {
                sendCRC(TotalCheckValueServer);
                return dataPacket.getData();
            }

            String[] parts = Start.split(";;");
            if (parts.length >= 3) {
                part1 = parts[0];
                part2 = parts[1];
                part3 = parts[2];
            } else {
                part3 = "\n";
            }

            // Ensure directory exists
            if (!tmpDir.exists() && !tmpDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + tmpDir.getAbsolutePath());
            }

            // Handle file creation with unique names
            while (tmpFir.exists()) {
                i++;
                tmpFir = new File(tmpDir, "file" + i + ".txt");
            }

            // Write data to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFir, true))) {
                writer.write(part3);
                writer.newLine();
            }

            System.out.println("Data written to file: " + tmpFir.getAbsolutePath());
            sendAck(ack++);

        } catch (TimeoutException e) {
            System.out.println("Timeout while receiving data");
            throw new TimeoutException("Server-SW: receive time out");
        } catch (IOException e) {
            throw new RuntimeException("IOException in data_ind_req: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    byte[] generateAckPacket(int packetNr) {
        return ("ACK" + packetNr).getBytes();
    }

    @Override
    void sendAck(int nr) {
        socket.sendPacket(generateAckPacket(nr));
        System.out.println("Server-SW: ACK sent " + nr);
    }

    void sendCRC(float nr) {
        String nummer = String.valueOf(nr);
        byte[] CRN = nummer.getBytes();
        socket.sendPacket(CRN);
    }

    @Override
    boolean checkStart(DatagramPacket packet) {
        return false;
    }

    float CalculateCRC(String data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes());
        long checksumValue = crc32.getValue();
        System.out.println("CRC32 Checksum: " + checksumValue);
        return checksumValue;
    }
}
