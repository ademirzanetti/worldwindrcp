package worldwind.contrib.parsers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class SimpleHTTPClient 
{
	public static final String CT_KMZ = "application/vnd.google-earth.kmz";
	public static final String CT_KML = "application/vnd.google-earth.kml+xml";
	
	private Map<String, List<String>> headers;
	private URL url;
    private int status;
    private HttpURLConnection uc;
    
	public SimpleHTTPClient(String url) throws MalformedURLException {
		this.url = new URL(url);
	}
	public SimpleHTTPClient(URL url) throws MalformedURLException {
		this.url = url;
	}
	
    /**
     * Simple HTTP Get for URLs that return text data
     * @param os stream where the output will be written
     * @throws MalformedURLException
     * @throws IOException
     */
    public void doGet (OutputStream os)
		throws MalformedURLException, IOException
	{
	    uc = (HttpURLConnection)url.openConnection();
	    
	    InputStream buffer  = new BufferedInputStream(uc.getInputStream());   
	    
	    int c;
	    
	    while ((c = buffer.read()) != -1) 
	    {
	      os.write(c);
	    } 
	    os.close();
	    headers = uc.getHeaderFields();
	    status = uc.getResponseCode();
	}

    /**
     * Simple HTTP get request
     * @return HTTP response text
     * @throws MalformedURLException
     * @throws IOException
     */
    public String doGet ()
		throws MalformedURLException, IOException
	{
	    uc = (HttpURLConnection)url.openConnection();
	    
	    BufferedInputStream buffer  = new BufferedInputStream(uc.getInputStream());   
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    
	    int c;
	    while ((c = buffer.read()) != -1) 
	    {
	      bos.write(c);
	    } 
	    bos.close();
	    
	    headers = uc.getHeaderFields();
	    status = uc.getResponseCode();
	    
	    return bos.toString();
	}
    
    public InputStream getInputStream() throws IOException {
	    HttpURLConnection uc = (HttpURLConnection)url.openConnection();
	    return uc.getInputStream();
    }
    
	public Map<String, List<String>> getHeaders(){
		return headers;
	}
	
	public int getStatus (){
		return status;
	}
	
	public String getContentType() {
		return headers.get("Content-Type").get(0);
	}
	
	public boolean isContentTypeKML () {
		return getContentType().indexOf(CT_KML) != -1;
	}

	public boolean isContentTypeKMZ () {
		return getContentType().indexOf(CT_KMZ) != -1;
	}

	public void close () {
		uc.disconnect();
		uc = null;
		headers = null;
		url = null;
	}
	
}
