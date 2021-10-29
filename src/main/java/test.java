import java.net.InetAddress;
import java.net.UnknownHostException;

public class test
{
    public static void main(String[] args) throws UnknownHostException {
        System.out.println("System.getProperty(\"os.name\") = " + System.getProperty("os.name"));

        System.out.println("InetAddress.getLocalHost().getHostName() = " + InetAddress.getLocalHost().getHostAddress());

    }
}
