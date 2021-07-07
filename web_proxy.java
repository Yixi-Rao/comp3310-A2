import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;



/**
* this is the web proxy class, it will use the web name provided by the user(defauly is www.bom.gov.au) to generate a
* listening web proxy(port number is 3310), once a client want connect to this proxy, it will create a thread and go to another class called proxy_handle
* to handle the client requests.
*
* proxy reference: https://www.cnblogs.com/muphy/p/14210917.html
*/
public class web_proxy extends Thread{
    private final ServerSocket proxy_server; // this is the proxy server with port nuumber 3310
    private final int proxy_port;            // port nuumber of the proxy: 3310
    String name;                             // the website name

    public web_proxy(String webName) throws IOException {
        this.name       = webName;
        this.proxy_port = 3310;
        proxy_server    = new ServerSocket(proxy_port);
        System.out.println("web_proxy on port: " + this.proxy_port);
    }

    @Override
    public void run() {
        while (true) {
            try { // once have a connection then handle it in another class, and the proxy keeps listenning
                Socket client = proxy_server.accept(); // a client have a request and connect the proxy
                new proxy_handle(client, name).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String web_name = "www.bom.gov.au"; // default website host name
        if (args != null && args.length > 0 && args[0].matches("\\d+")) {
            web_name = args[0];
        }
        new web_proxy(web_name).start();
    }

}
