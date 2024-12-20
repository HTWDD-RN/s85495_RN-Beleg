import java.io.IOException;
import java.util.logging.Logger;

public class FileTransfer implements FT {

    private ARQ myARQ;

    public FileTransfer(String host, Socket socket, String fileName, String arq) {

        myARQ = new SW(socket);
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    }

    public FileTransfer(Socket socket, String dir) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }


    @Override
    public boolean file_req() throws IOException {
        return false;
    }


    //***************************************** SERVER **************//


    @Override
    public boolean file_init() throws IOException {
        return false;
    }
}
