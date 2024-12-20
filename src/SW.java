import java.net.DatagramPacket;

public class SW extends ARQAbst {



    public SW(Socket socket) {
        super(socket);
    }

    public SW(Socket socket, int sessionID) {
        super(socket, sessionID);
    }


    @Override
    public boolean data_req(byte[] hlData, int hlSize, boolean lastTransmission) {
        return false;
    }

    @Override
    protected boolean waitForAck(int packetNr) {
        return false;
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
        return 0;
    }

    @Override
    protected byte[] generateDataPacket(byte[] sendData, int dataSize) {
        return new byte[0];
    }

    @Override
    public byte[] data_ind_req(int... values) throws TimeoutException {
        return new byte[0];
    }

    @Override
    byte[] generateAckPacket(int packetNr) {
        return new byte[0];
    }

    @Override
    void sendAck(int nr) {

    }

    @Override
    boolean checkStart(DatagramPacket packet) {
        return false;
    }

    @Override
    public void closeConnection() {

    }
}
