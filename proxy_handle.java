
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * how to work: http://localhost:3310/http://www.bom.gov.au/
 */


/**
* this class will handle the client request by rewriting the request, then sending it to the server, parsing the message returned by
* the server, doing some modifications on the displayed text which is change the "Canberra" to "beijing". Finally it sends back the revised
* response message to the client.
* example website: http://localhost:3310/http://www.bom.gov.au/
* 
* Framework structure Reference: https://www.cnblogs.com/muphy/p/14210917.html
* 
* @author Yixi Rao
* @version 1.0
* @since 2021-04-27
*/
public class proxy_handle extends Thread {

    private Socket client;                      // the client we serve for
    private String web_name;                    // the website name

    private Socket remote_server = null;        // remote server that the client requested for
    private int remote_port = 80;               // remote server port number

    private BufferedReader client_reader = null; // client input stream, read data from a source
    private DataOutputStream client_writer = null; // client output stream, write data to a place

    private DataInputStream server_reader = null; // server input stream, read data from a source
    private BufferedWriter server_writer = null; // server output stream, write data to a place

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //used to represented the request date
    boolean GET_REQUEST = false;

    public proxy_handle(Socket client, String name) {
        this.web_name = name;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            client_reader = new BufferedReader(new InputStreamReader(client.getInputStream()));            
            client_writer = new DataOutputStream(client.getOutputStream()); 

            if (client_reader != null && client_writer != null) {
                String firstline = client_reader.readLine(); // the first line of the HTTP request e.g. GET /http://www.bom.gov.au/ HTTP/1.1
                firstline        = firstline.substring(0); // delete the space
                String URL_first = firstline.substring(firstline.indexOf("GET /") + 5, firstline.indexOf("HTTP/1.1")); // get the URL of the request
                // we should add the http:// manually
                if (!firstline.contains(web_name)) {
                    URL_first = "http://" + web_name + "/" + URL_first;
                }

                if (firstline.contains("GET")) {
                    GET_REQUEST = true;
                    System.out.println("-------------------------Logs----------------------------------------");
                    System.out.println(firstline);
                    System.out.println("request time: " + df.format(System.currentTimeMillis()));
                }
                // just focus on the http request
                if (URL_first.contains("http://")) {
                    InetAddress addr = InetAddress.getByName(web_name);                                                  // this is the host IP
                    remote_server = new Socket(addr, remote_port);                                                       // using the host ip, we create the remote server
                    server_reader = new DataInputStream(remote_server.getInputStream()); 
                    server_writer = new BufferedWriter(new OutputStreamWriter(remote_server.getOutputStream(), "UTF-8"));

                    if (server_reader != null && server_writer != null && remote_server != null) {
                        // we rewrite the http request in http 1.0
                        server_writer.write("GET " + URL_first + " HTTP/1.0\r\n");
                        server_writer.write("Host: www.bom.gov.au\r\n");
                        server_writer.write("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36\r\n");
                        server_writer.write("\r\n");
                        server_writer.flush();
                        // now get the response, and parse it, modify it
                        get_and_send(server_reader, client_writer);
                        // close all the closable object
                        close_all(server_reader, server_writer, remote_server, client_reader, client_writer, client);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        close_all(server_reader, server_writer, remote_server, client_reader, client_writer, client);
    }

    /**
    * read the data from the remote server, and some modifications are made on the html(change capital name and HTML links), the response
    * data was sent back to the client
    *
    * @param inR BufferedReader: read the server bytes
    * @param outW BufferedWriter: send it back to server
    * @author Yixi Rao
    * @version 1.0
    * @since 2021-04-27
    * @Reference: https://www.cnblogs.com/muphy/p/14210917.html
    */
    private void get_and_send(DataInputStream inR, DataOutputStream outW) {
        String html_string = "";   // all the html string
        String report_string = ""; // all the header string
        byte buf[];
        try {
            boolean token = false; // if we have html file then the token is true
            buf = inR.readAllBytes();
            String buf_string = new String(buf);
            // seperate the html string and the header
            if (buf_string.contains("text/html")) {
                token = true;
                if (buf_string.contains("<!doctype")) {
                    report_string = buf_string.substring(0, buf_string.indexOf("<!doctype"));
                    html_string = buf_string.substring(buf_string.indexOf("<!doctype"));
                } else {
                    report_string = buf_string.substring(0, buf_string.indexOf("<!DOCTYPE"));
                    html_string = buf_string.substring(buf_string.indexOf("<!DOCTYPE"));
                }
            }

            if (GET_REQUEST) {
                System.out.println(buf_string.substring(0, buf_string.indexOf("\r\n")));
            }
            // html modifycation part
            if (token) {
                Document doc   = Jsoup.parse(html_string); // html document
                Element body   = doc.body();               // body part of the html
                Element head   = doc.head();               // head part of the html
                Elements links = body.select("a[href]");   // all the links
                int count      = 0;                        // count the number of name changes
                int link_count = 0;                        // count the number of links changes
                // remove all the javascript from head
                for (Element ele : head.getAllElements()) {
                    if (ele.tagName() == "script") {
                        ele.remove();
                    }
                }
                // rewrite link
                for (Element link : links) {               // e.g. http://localhost:3310/http://www.bom.gov.au/
                    if (!link.attr("href").contains("#")){
                        if (!link.attr("href").contains("http://")) {
                            link.attr("href", "http://localhost:3310/" + "http://" + web_name + link.attr("href"));
                            link_count ++;
                        } else if(link.attr("href").contains("http://www.bom.gov.au/")){
                            link.attr("href", "http://localhost:3310/" + link.attr("href"));
                            link_count ++;
                        }
                    } 
                }
                // rewrite text
                for (Element ele : body.getAllElements()) {
                    // remove all the javascript from body
                    if (ele.tagName() == "script") {
                        ele.remove();
                    }
                    if (ele.childNodeSize() == 1 && ele.tagName() != "body" && (ele.text().contains("Canberra") || ele.text().contains("canberra"))) {
                        count++; // we find canberra in Node!
                        String canberra_text = ele.text();
                        if (canberra_text.contains("Canberra")) {
                            int i_c = canberra_text.indexOf("Canberra");
                            String sub_canberra_text = canberra_text.substring(0, i_c) + "beijing"
                                    + canberra_text.substring(i_c + 8); 
                            ele.text(sub_canberra_text); // change it to beijing
                        } else {
                            int i_c = canberra_text.indexOf("canberra");
                            String sub_canberra_text = canberra_text.substring(0, i_c) + "beijing"
                                    + canberra_text.substring(i_c + 8);
                            ele.text(sub_canberra_text); // change it to beijing
                        }
                    }
                }
                System.out.println("text changes: " + count);
                System.out.println("links changes: " + link_count);
                html_string = doc.toString();
            }

            // return the content to the client
            if (token) {
                outW.write(report_string.getBytes("UTF-8"));
                outW.write(html_string.getBytes("UTF-8"));
                outW.flush();
            } else {
                outW.write(buf);
                outW.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * close all the closable objects 
    * method Reference: https://www.cnblogs.com/muphy/p/14210917.html. I think this is a very helpful method, so I copy it
    *
    * @param Closeable all the closable objects
    */
    public void close_all(Closeable... closeables) {
        if (closeables != null) {
            for (int i = 0; i < closeables.length; i++) {
                if (closeables[i] != null) {
                    try {
                        closeables[i].close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
