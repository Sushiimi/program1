/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection.
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring
* the fact that the entirety of the webserver execution might be handling
* other clients, too.
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format).
*
**/

// removed redundancies in imports - Ian Johnson - 1-30-2018
import java.util.*;
import java.io.*;
import java.net.Socket;
import java.lang.Runnable;
import java.text.DateFormat;
import java.lang.Throwable;

public class WebWorker implements Runnable {

   private Socket socket;

   /**
   * Constructor: must have a valid open socket
   **/
   public WebWorker(Socket s) {
      socket = s;
   }

   /**
   * Worker thread starting point. Each worker handles just one HTTP
   * request and then returns, which destroys the thread. This method
   * assumes that whoever created the worker created it with a valid
   * open socket object.
   **/
   public void run() {
      System.err.println("Handling connection...");
      try {
         String contentType = "";
         InputStream  is = socket.getInputStream();
         OutputStream os = socket.getOutputStream();
         String filePath = readHTTPRequest(is); // store the GET request for the HTML file
         filePath = filePath.substring(1); // removes the '/' from the filePath string

         // changing the contentType based on the file extension
         // of the requested file
         // gif, jpeg/jpg, png, html, and ico files are supported
         if ( ( filePath.toLowerCase().contains(".jpeg") ) == true
               || ( filePath.toLowerCase().contains(".jpg") ) == true)
            contentType = "image/jpeg";

         else if ( ( filePath.toLowerCase().contains(".gif") ) == true )
            contentType = "image/gif";

         else if ( ( filePath.toLowerCase().contains(".png") ) == true )
            contentType = "image/png";

         else if ( ( filePath.toLowerCase().contains(".ico") ) == true )
            contentType = "image/x-icon";

         else if ( ( filePath.toLowerCase().contains(".html") ) == true )
            contentType = "text/html";

         writeHTTPHeader(os, contentType, filePath);
         writeContent(os, contentType, filePath);
         os.flush();
         socket.close();
      } catch (Exception e) {
         System.err.println("Output error: " + e);
      }
      System.err.println("Done handling connection.");
      return;
   }

   /**
   * Read the HTTP request header.
   **/
   private String readHTTPRequest(InputStream is) {
      // vars
      String line;
      String getLine = "";
      String extractedPath = "";

      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      while (true) {
         try {

            while (!r.ready()) Thread.sleep(1);
            line = r.readLine();
            System.err.println("Request line: (" + line + ")");

            // Determine if the the line contains GET command
            if ( line.length() > 0 )
               getLine = line.substring(0, 3); // saving the GET substring for checking below

            // check the current value of getLine for the GET substring
            // save the filepath from the GET line if true
            if ( getLine.equals("GET") ) {
               extractedPath = line.substring(4); // create substring starting at '/' in path
               extractedPath = extractedPath.substring(0, extractedPath.indexOf(" ")); // removes trailing text from GET command
            }

            if (line.length() == 0) break; // break when done reading lines
         } catch (Exception e) {
            System.err.println("Request error: " + e);
            break;
         }
      }
      return extractedPath;
   }

   /**
   * Write the HTTP header lines to the client network connection.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   * @param filePath is the file directory for the requested file
   **/
   private void writeHTTPHeader(OutputStream os, String contentType, String filePath) throws Exception {
      Date d = new Date();
      DateFormat df = DateFormat.getDateTimeInstance();
      df.setTimeZone(TimeZone.getTimeZone("MST"));
      File f = new File( filePath );

      // if the file exists, give OK response
      if ( f.exists() && !f.isDirectory() )
         os.write("HTTP/1.1 200 OK\n".getBytes());
      // otherwise give 404 Not Found, print in console for debugging
      else {
         os.write("HTTP/1.1 404 Not Found\n".getBytes());
         System.err.println("Write line: (File: " + filePath + ", Not Found)");
         System.err.println("Write line: (Displaying 404 Page)");
      }

      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Ian's very own server\n".getBytes());
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines

      return;
   }

   /**
   * Write the data content to the client network connection. This MUST
   * be done after the HTTP header has been written out.
   * @param os is the OutputStream object to write to
   * @param contentType is the string MIME content type (e.g. "text/html")
   * @param filePath is the file directory for the requested file
   **/
   private void writeContent(OutputStream os, String contentType, String filePath) throws Exception {
      // vars and objects
      int i;
      File f = new File( filePath ); // needed to check if valid file
      final String dateTag = "<cs371date>";
      final String serverIDTag = "<cs371server>";
      final String error404path = "test/404page.html";
      final String serverID = "Ian's P1 Server";
      String currentLine, dateOutputString, serverIDOutputString;
      byte[] fileBytes = new byte[16384];

      // if the file is html and it exists, print it line by line and replace the tags
      if ( f.exists() && !f.isDirectory() && contentType == "text/html" ) {
         FileInputStream outputFile = new FileInputStream( filePath );
         BufferedReader r = new BufferedReader(new InputStreamReader( outputFile ));

         // process the html file for tags
         while ( ( currentLine = r.readLine() ) != null ) { // loop continues until no lines left to read

            // checking for the date and serverID tags
            // and replacing them
            if ( ( currentLine.contains( dateTag ) ) == true ) {
               Date outputDate = new Date();
               DateFormat outputDateFormat = DateFormat.getDateTimeInstance();
               outputDateFormat.setTimeZone( TimeZone.getTimeZone("MST") );
               currentLine = currentLine.replaceAll( dateTag, ( outputDateFormat.format( outputDate ) ) );
            }// end if

            if ( ( currentLine.contains( serverIDTag ) ) == true )
               currentLine = currentLine.replaceAll( serverIDTag, serverID );

            os.write( currentLine.getBytes() ); // write currentLine after replacing any tags
         }// end while
      }
      // if the file is an image file and it exists, serve it using a byte array
      else if ( f.exists() && !f.isDirectory() && contentType.contains("image") == true ) {
         FileInputStream outputImage = new FileInputStream( filePath );
         while ( ( i = outputImage.read( fileBytes ) ) > 0 )
            os.write( fileBytes, 0, i );
      }
      // assume the file does not exist, serve the 404 page
      else {
         FileInputStream errorPage = new FileInputStream( error404path );
         while ( ( i = errorPage.read( fileBytes ) ) > 0 )
            os.write( fileBytes, 0, i );
      }// end if else statements

   }// end writeContent function

} // end class






