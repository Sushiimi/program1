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

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String filePath = readHTTPRequest(is); // store the GET request for the HTML file
      filePath = filePath.substring(1); // removes the '/' from the filePath string
      writeHTTPHeader(os, "text/html", filePath);
      writeContent(os, filePath);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   // vars
   String line;
   String getLine = "";
   String extractedPath = "";

   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {

         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");

         // Determine if the the line contains GET command
         if( line.length() > 0 )
            getLine = line.substring(0,3); // saving the GET substring for checking below

         // check the current value of getLine for the GET substring
         // save the filepath from the GET line if true
         if( getLine.equals("GET") ){
             extractedPath = line.substring(4); // create substring starting at '/' in path
             extractedPath = extractedPath.substring(0, extractedPath.indexOf(" ")); // removes trailing text from GET command
         }

         if (line.length()==0) break; // break when done reading lines
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return extractedPath;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private File writeHTTPHeader(OutputStream os, String contentType, String filePath) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   File f = new File(filePath);

   // if the file exists, give OK response
   if( f.exists() && !f.isDirectory() )
      os.write("HTTP/1.1 200 OK\n".getBytes());
   // otherwise give 404 Not Found
   else
      os.write("HTTP/1.1 404 Not Found\n".getBytes());


   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   
   return f;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String filePath) throws Exception
{

   int i;
   //byte[] fileBytes = new byte[16384];
   FileInputStream outputFile = new FileInputStream( filePath );
   BufferedReader r = new BufferedReader(new InputStreamReader(outputFile));
   final String dateTag = "<cs371date>";
   final String serverIDTag = "<cs371server>";
   String serverID = "Ian's P1 Server";
   String currentLine, dateOutputString, serverIDOutputString;

   // process the html file for tags
   while( ( currentLine = r.readLine() ) != null ){ // loop continues until no lines left to read

      // checking for the date and serverID tags
      // and replacing them
      if( ( currentLine.contains( dateTag ) ) == true ){
         Date outputDate = new Date();
         DateFormat outputDateFormat = DateFormat.getDateTimeInstance();
         outputDateFormat.setTimeZone( TimeZone.getTimeZone("MST") );
         currentLine = currentLine.replaceAll( dateTag, ( outputDateFormat.format( outputDate ) ) );
      }// end if

      if ( ( currentLine.contains( serverIDTag ) ) == true )
         currentLine = currentLine.replaceAll( serverIDTag, serverID );
      
      os.write( currentLine.getBytes() ); // write currentLine after replacing any tags
   }// end while

   // converting InputStream to a byte array then
   // serving the file
   //while( ( i = outputFile.read(fileBytes) ) > 0 )
       //os.write(fileBytes, 0, i);


}// end writeContent function

} // end class






