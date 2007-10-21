package worldwind.contrib.parsers;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import worldwind.contrib.Messages;

/**
 * Simple WMS Capabilities object
 * @author Vladimir Silva
 *
 */
public class WMS_Capabilities 
{
	private static final Logger logger = Logger.getLogger(WMS_Capabilities.class);
	
	// used to format iso dates
    public static NumberFormat nf = NumberFormat.getInstance(); 
	
	private Version version;
	private Service service;
	private ArrayList<Layer> layers;

	private int totalLayers;
	
	// WMS Requests: GetMap, GetCapabilities
	private Hashtable<String, Request> requests;
	
	/* service data */
	public static class Service 
	{
		public String Name;
		public String Title;
		public String Abstract;
		

		public Service() {}
		public Service(String name, String title, String description) {
			Name = name;
			Title = title;
			Abstract = description;
		}
		@Override
		public String toString() {
			return "\nName:" + Name + "\nTitle: " + Title + "\nAbs: " + Abstract;
		}
	}
	
	public static class Request {
		public static final String TYPE_GETMAP = "GET_MAP";
		public static final String TYPE_GETCAPS = "GET_CAPS";
		
		public URL Url;
		public Vector<String> formats = new Vector<String>();
		
		@Override
		public String toString() {
			return Url.toString() + " formats: " + formats;
		}
	}
	
	/* Layer info */
	public static class Layer implements Comparable<Layer>
	{
		public String Name;
		public String Title;
		String Abstract;
		public String CRS;
		public BBox bbox;			// Lat/lon box
		public String ISOTimeSpan; 	// Time Span: t1/t2/period | CSV list of t1/t2/period 
		public URL DataURL;			// Docs ur
		public Style style;			// Layer syle (1)
		public int fixedWidth, fixedHeight;	// img wxh
		
		// Capabilities parent
		private WMS_Capabilities root;
		
		public Layer(String name, String title, String CRS, String timeSpan
			, int width, int height) 
		{
			this();
			this.Name = name;
			this.Title = title;
			this.CRS = CRS;
			this.ISOTimeSpan = timeSpan;
			this.fixedWidth = width;
			this.fixedHeight = height;
		}
		
		public Layer() {
			style = new Style();
			bbox = new BBox();
		}

		
		/* WMS_Capabilities for this layer */
		public WMS_Capabilities getCapabilities(){
			return root;
		}
		
		/* build a getmap request url */
		public URL getMapUrl (final String format) 
		{
			Request request = root.getRequests().get(Request.TYPE_GETMAP);
			
			String url 	= request.Url.toString(); 
			
			final String firstFmt = ( request.formats.size() > 0) 
				? "&format=" + request.formats.firstElement()
				: "";
				
			url += ( url.indexOf("?") == -1) 
				? "?" : "";
			url += "service=WMS&request=GetMap&version="
				+ root.getVersion() ;
			
			url += "&layers=" + Name;
			url += "&bbox=" + bbox.west + "," + bbox.south 
				+ "," + bbox.east + "," + bbox.north;
			
			url += "&width=" + fixedWidth;
			url += "&height=" + fixedHeight;
			
			url += (CRS != null) ? "&crs=" + CRS : "";
			
			// Style is required
			url += (style.isValid()) ? "&" + style.asWMS() : "";
			
			url += (format != null) ? "&format=" + format : firstFmt ;
			
			try {
				return new URL(url);
			} catch (Exception e) {
				return null;
			}
		}
		
		@Override
		public String toString() {
			return "Name: " + Name + "\nTitle: " + Title
			+ "\nAbs Len: " + ((Abstract!= null) ? Abstract.length() : 0)
			+ "\nTime Span: " + ISOTimeSpan
			+ "\nCRS: " + CRS
			+ "\nBBox: " + bbox
			+ "\nStyle: " + style
			+ "\nData URL: " + DataURL
			+ "\nGetMap Url: " + getMapUrl(null)
			+ "\nWxH=" + fixedWidth + " " + fixedHeight;
		}
		
		/**
		 * Layer as a HTML <table> fragment
		 * @return
		 */
		public String getDescriptionAsHTML() 
		{
			StringBuffer buf = new StringBuffer(); 
			if ( Title != null) 
				buf.append("<b>" + Title + "</b>" + Messages.NL);
			
			if ( Abstract != null )
				buf.append("<p>" + Abstract+ "</p>" + Messages.NL);
			
			buf.append("<table>");
			
			if ( ISOTimeSpan != null ) 
				buf.append("<tr><td>Time Span</td><td>" 
						+ (ISOTimeSpan.length() < 30 ? ISOTimeSpan : ISOTimeSpan.substring(3, 30) + "...") 
						+ "</td></tr>" + Messages.NL);
			
			buf.append("<tr><td>CRS</td><td>" + CRS + "</td></tr>" + Messages.NL);
			buf.append("<tr><td>BBox</td><td>" + bbox + "</td></tr>" + Messages.NL);
			buf.append("<tr><td>Style</td><td>" + style + "</td></tr>" + Messages.NL);
			
			if ( DataURL != null)
				buf.append("<tr><td>Data URL</td><td><a href=" + DataURL + ">" 
						+ DataURL + "</a>" 
						+ "</td></tr></table>" + Messages.NL);
			
			return buf.toString();
		}
		
		public String toKmlFragment() {
			return "<GroundOverlay><name>" + Name + "</name>" + Messages.NL
				+ "<description>" + getDescriptionAsHTML() 
				+ "</description>" + Messages.NL
				+ "<Icon><href>" + getMapUrl(null) + "</href></Icon>" + Messages.NL
				+ "<LatLonBox><north>" + bbox.north + "</north>"
				+ "<south>" + bbox.south + "</south>"
				+ "<east>" + bbox.east + "</east>"
				+ "<west>" + bbox.west + "</west><LatLonBox>" + Messages.NL
				+ "</GroundOverlay>" + Messages.NL;
		}
		
		/** Compares Layer Titles lexicographically. */
		public int compareTo(Layer that) {
			if ( this.Title == null || that.Title == null) return -1;
			return this.Title.compareTo(that.Title);
		}
	}
	
	/* BBOX */
	public static class BBox {
		public String north, south, east,west;
		
		public BBox() {
		}

		public BBox(String north, String south, String east, String west) 
		{
			this.north = north; this.south = south;
			this.east = east; this.west = west;
		}
		
		public boolean isValid () {
			return north != null && south != null && east != null && west != null;
		}
		
		@Override
		public String toString() {
			return "(w,e,s,n)=" + west + "," + east + "," + south +"," + north;
		}
	}
	
	/* Layer Style */
	public static class Style 
	{
		public String Name;
		public URL LegendURL;

		public Style() {
		}
		public Style(String name, URL legend) {
			Name = name;
			LegendURL = legend;
		}
		@Override
		public String toString() {
			return Name + " Legend=" + LegendURL;
		}
		public boolean isValid () {
			return Name != null;
		}
		String asWMS() {
			return "styles=" + Name;
		}
	}
	

	/* WMS version 1.1.0 - 1.3.0 */
	public static class Version 
	{
		public int hi, mid, low;
		
		public Version() {
		}
		public Version(int i, int j ,int k) {
			this.hi = i;
			this.mid = j;
			this.low = k;
		}
		@Override
		public String toString() {
			return hi + "." + mid + "." + low;
		}
		public boolean isValid() {
			return hi > 0 && mid > 0;
		}
		
	}
	

	public WMS_Capabilities() {
		service =  new WMS_Capabilities.Service();
		version = new WMS_Capabilities.Version();
		layers = new ArrayList<Layer>();
		requests = new Hashtable<String, Request>();
		
		// Add a default GetMap empty Request
		requests.put(Request.TYPE_GETMAP, new Request());
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		for (Layer layer : layers) {
			buf.append(layer + "\n");
		}
		
		return "Service" + service.toString()
			+ "\nVersion: " + version 
			+ "\nGetMap: " + getMapRequest()
			/* layers */
			+ "\n" + layers.size() + " renderable layers."
			+ "\n" + getTotalLayers() + " total layers.";
			//+ "\n" + buf.toString();
	}
	
	public Service getService() { return service; }
	public ArrayList<Layer> getLayers () { return layers; }
	public Version getVersion() { return version; }
	public Hashtable<String, Request> getRequests() { return requests ; }
	public Request getMapRequest() { return requests.get(Request.TYPE_GETMAP); }
	
	public void setVersion( int maj, int mid, int low ) {
		version.hi  = maj;
		version.mid = mid;
		version.low = low;
	}
	
	public void addLayer (Layer layer) {
		layer.root = this;
		layers.add(layer);
	}
	
	/** Sort WMS layers by Title only */
	public void sortLayersByTitle () {
		Collections.sort(layers);
	}
	
	/**************************************************************
	 * WMS 1.3.0 Time extraction utilities
	 **************************************************************/
	
    /**
     * Loop thru a sequence of WMS ISO8601 time ranges of the form ISOTIME1/ISOTIME2/PERIOD
     * to build a CSV list of times used to render frames from a WMS dataset.
     * PERIOD is of the form: P{nn}{Y,M,D,H} -- indicates how the dates are to be generated.
     * Sample ranges:<pre>
     *  1990-01-31
     *  1995-01-01/2006-09-12/P1D
     *  1884-/1994-/P1Y
     *  2005-06-01T00Z/2005-12-12T18Z/PT6H
     *  2005-08-26T22:45Z/2005-08-30T22:45Z/PT5M
     *  2005-08-22T24Z,2005-08-23T05Z,2005-08-23T23Z
     *  2005-08-23T17:45Z/2005-08-30T17:45Z/P1D
     *  2005-08-23T08:45Z/2005-08-24T02:45Z/PT3H,2005-08-24T08:45Z/2005-08-25T02:45Z/PT3H,2005-08-25T08:45Z/2005-08-26T02:45Z/PT3H
     *  2003-06-20T02:44Z/2003-06-22T00:56Z/PT1H39M</pre>
     * @param time ISO string representation of time
     * @return CSV list if times to be sent to the server 
     */
    static public String buildWMSTimeList ( String wmsTime) throws IOException
    {
    	if ( nf.getMinimumIntegerDigits() < 2) {
    		logger.debug("Seeting minimum integer digits to 2");
    		nf.setMinimumIntegerDigits(2);
    	}
    	
    	logger.debug("Building CSV time list for wms time:" + wmsTime);
    	
        if ( wmsTime == null ) return null;

        // simplest case: list of times (commas only)
        if ( wmsTime.indexOf(",") != -1 && wmsTime.indexOf("/") == -1 ) return wmsTime;
        
        // single value (no commas, no slashes)
        if ( wmsTime.indexOf(",") == -1 && wmsTime.indexOf("/") == -1) return wmsTime;
        
        // slashes & commas
        if ( wmsTime.indexOf(",") != -1 && wmsTime.indexOf("/") != -1 ) 
        {
            final String[] tmp = wmsTime.split(",");
            String  data = null;
            
            if ( tmp.length > 0 ) {
                data = buildWMSTimeList(tmp[0]);
                for (int i = 1 ; i < tmp.length ; i++ )  {
                    data += "," + buildWMSTimeList(tmp[i]);
                }
            }
            return data;
        }
        else {
            // slashes only, extract iso times and build a csv list
            int YYmin = 0, MMmin = 0, DDmin = 0, hhmin = 0, mmmin = 0 ,ssmin = 0;
            int YYmax = 0, MMmax = 0, DDmax = 0, hhmax = 0, mmmax = 0 ,ssmax = 0;
            int TLEN = 0; // # of time elems (1=hh, 2=hhmm, 3=hhmmss)
            
            // split tmin, tmax, period. len must be 3
            String[] tmp = wmsTime.split("/");
            
            if ( tmp.length != 3 ) {
                throw new IOException("Error splitting time: " + wmsTime 
                		+ ". The format should be: time1/time2/period.");
            }
            final String tmin = tmp[0];     // min time
            final String tmax = tmp[1];     // max time
            final String period = tmp[2];   // period P1D, P1Y, PT6H (ISO8601)
    
//System.out.println("wms range:" + wmsTime );
    
            // split tmin: YYYY-MM-DD{Thh:mm:ss}{Z}
            int[] iso = splitISOTime(tmin);
            YYmin = iso[0]; MMmin = iso[1]; DDmin = iso[2];
            hhmin = iso[3] ; mmmin = iso[4]; ssmin = iso[5];
            TLEN  = iso[6]; // used to reconstruct times w/ hh/mm=zeros
    
            // split tmax: YYYY-MM-DD{Thh:mm:ss}{Z}
            iso = splitISOTime(tmax);
            YYmax = iso[0]; MMmax = iso[1]; DDmax = iso[2];
            hhmax = iso[3] ; mmmax = iso[4]; ssmax = iso[5];
    
//            System.out.println("buildWMSTimeList tmin:" + tmin + " tmax:" + tmax + " period:" + period);
//            System.out.println("buildWMSTimeList Min(" + YYmin + "," + MMmin + "," + DDmin + " " + hhmin + ":" + mmmin + ":" + ssmin + ")");
//            System.out.println("buildWMSTimeList Max(" + YYmax + "," + MMmax + "," + DDmax + " " + hhmax + ":" + mmmax + ":" + ssmax + ")");

            int[] dummy 	= parseISODatePeriod(period);
            int inc 		= dummy[0];    		// increment value
            char incWhat	= (char)dummy[1];   // increment what? Y=year, M=month, D=day, H=hour, m=minutes, s=seconds

            //System.out.println("buildWMSTimeList increment=" + inc + " Inc what=" + incWhat);
            
            // build a list of iso dates by a given inccrement
            int i = 0;
            StringBuffer buf = new StringBuffer();
            
            switch ( incWhat) 
            {
                case 'Y':   // year increments: Simple replace the year from the start date w/ the new year
                    for (i = YYmin  ; i <= YYmax ; i += inc ) 
                        buf.append(tmin.replaceFirst(String.valueOf(YYmin), String.valueOf(i)) + ",");
                    break;
                    
                case 'M':   // month increments
                    for (i = MMmin  ; (YYmin < YYmax) || (i <= MMmax) ; i += inc ) {
                        if ( i > 12 ) { i -= 12; YYmin++; }
                        
                        buf.append(YYmin + "-" +  nf.format(i)); 
                        
                        if (DDmin > 0)  buf.append("-" + nf.format(DDmin) + ",");
                        else buf.append(",");
                    }
                    break;
    
                case 'D':   // day increments
                    for (i = DDmin  ; (YYmin < YYmax) || (MMmin < MMmax) || (i <= DDmax) ; i += inc ) 
                    {
                    	// Months with 28 & 30 days
                    	// Feb 28 days ( 29 in leap year), Apr, Jun, Sep, Nov = 30 days
                    	if (MMmin == 2 && i > 28) { i-=28; MMmin++;}
                    	if ((MMmin == 4 || MMmin == 6 || MMmin ==9 || MMmin == 11)  && i > 30) { i-=30; MMmin++;}
                    	
                        if ( i > 31 ) { i -= 31; MMmin++; }
                        if ( MMmin > 12 ) { MMmin = 1; YYmin++; }

                        buf.append(YYmin + "-" + nf.format(MMmin) + "-" + nf.format(i));
                        
                        // append time?
                        if ( hhmin != 0 ) buf.append("T" + nf.format(hhmin));
                        if ( hhmin != 0 && mmmin != 0 ) buf.append(":" + nf.format(mmmin) + "Z");
                        if ( hhmin != 0 && mmmin == 0 ) buf.append("Z");
                        buf.append(",");
                        
//System.out.println(YYmin + "-" + nf.format(MMmin) + "-" + nf.format(hi) + ",");
                    }
                    break;
    
                case 'H':   // hour increments
                    for (i = hhmin  ; (YYmin < YYmax) || (MMmin < MMmax) || (DDmin < DDmax) || (i <= hhmax) ; i += inc ) 
                    {
                        //if ( hi >= 24 ) { hi -= 24; DDmin++;}
                        while ( i >= 24 ) { i -= 24; DDmin++;}
                        if ( DDmin > 31 ) { DDmin = 1; MMmin++;}
                        if ( MMmin > 12 ) { MMmin = 1; YYmin++; }

                        buf.append(YYmin + "-" + nf.format(MMmin) + "-" + nf.format(DDmin) + "T" + nf.format(i) ); //+ "Z" + ",");
    
                        // append time? This will preserve the original format TnnZ,... or Tnn:nn:nnZ,...
                        if ( TLEN > 0 ) {
                            buf.append(":" + nf.format(mmmin));
                            buf.append(":" + nf.format(ssmin) + "Z");
                        }
                        else {
                        	if ( mmmin != 0 ) buf.append(":" + nf.format(mmmin) + "Z");
                        	if ( mmmin == 0 ) buf.append("Z");
                        }
                        buf.append(",");
                    }
                    
                    break;
                    
                case 'm':   // minute increments    
                    for (i = mmmin  ; (YYmin < YYmax) || (MMmin < MMmax) || (DDmin < DDmax) || (hhmin < hhmax) || (i <= mmmax) ; i += inc ) 
                    {
                        while ( i >= 60 ) {i -= 60; hhmin++;}
                        if ( hhmin >= 24 ) { hhmin = 1; DDmin++;}
                        if ( DDmin > 31 ) { DDmin = 1; MMmin++;}
                        if ( MMmin > 12 ) { MMmin = 1; YYmin++; }

                        buf.append(YYmin + "-" + nf.format(MMmin) + "-" + nf.format(DDmin) 
                            + "T" + nf.format(hhmin) + ":" + nf.format(i));
                        
                        // append seconds? zeros are ignored except if TLEN==3
                        if ( ssmin != 0 || TLEN == 3 ) buf.append(":" + nf.format(ssmin) + "Z,");
                        else buf.append("Z,");
                    }
                    break;

                case 'S':   // Second increments    
                    for (i = ssmin  ; (YYmin < YYmax) || (MMmin < MMmax) || (DDmin < DDmax) || (hhmin < hhmax) ||  (mmmin < mmmax) || (i <= ssmax) ; i += inc ) 
                    {
                        while ( i >= 60 ) {i -= 60; mmmin++;} 		// increase minutes after 60 secs
                        if ( mmmin >= 60 ) { mmmin = 1; hhmin++;}	// increase hours
                        if ( hhmin >= 24 ) { hhmin = 1; DDmin++;}
                        if ( DDmin > 31 ) { DDmin = 1; MMmin++;}
                        if ( MMmin > 12 ) { MMmin = 1; YYmin++; }
                        
                        // YYYY-MM-DDThh:mm:(hi)
                        buf.append(YYmin + "-" + nf.format(MMmin) + "-" + nf.format(DDmin) 
                            + "T" + nf.format(hhmin) + ":" + nf.format(mmmin) + ":" + nf.format(i) + "Z,");
                        
                    }
                    break;
                    
                default:
                    System.err.println("buildWMSTimeList Invalid date increment: " + incWhat);
            }
            
            String isoDates = buf.toString();
            
            // remove trailing (,)
            if ( isoDates.length() > 0 && isoDates.charAt(isoDates.length() - 1) == ',') 
                isoDates = isoDates.substring(0, isoDates.length() -1 );
            
            return isoDates;
        }
    }

    /**
     * Split an ISO5601 toime of the form: YYYY-MM-DD{Thh:mm:ss}{Z}
     * @return array of int values 0=year, 1=month, 2=day, 3=hh, 4=min, 5 = ss;
     *  6=Time length (used to reconstruct non properly formatted times)
     * @param isoTime ISO8601 time String of the form YYYY-MM-DD{Thh:mm:ss}{Z}
     */
    static public int[] splitISOTime( String isoTime)
    {
        int[] isoArray = new int[7];
        String[] tmp = isoTime.split("-");
        
        // Year-month only YYYY-MM
        if ( tmp.length == 2 ) { 
            isoArray [0] = Integer.parseInt(tmp[0]); // year
            isoArray [1] = Integer.parseInt(tmp[1]); // month
            //isoArray [2] = 1; // days cannot be zeros
        }
        else if ( tmp.length == 3 ) { 
            // YYYY-MM-DD
            isoArray [0] = Integer.parseInt(tmp[0]); // year
            isoArray [1] = Integer.parseInt(tmp[1]); // month
            
            // YYYY-MM-DD
            if ( tmp[2].indexOf("T") == -1 ) 
            	isoArray[2] = Integer.parseInt(tmp[2]); // day
            else { 
                // YYYY-MM-DDThh:mm:ssZ
                // split date/time: DD{Thh:mm:ss}{Z}
                isoArray[2]   = Integer.parseInt(tmp[2].split("T")[0]); // day
                
                String hhmmssZ = tmp[2].split("T")[1];
                
                // {hh:mm:ss}{Z} -- get rid of the Z (Zone) char
                if ( hhmmssZ.indexOf("Z") != -1 ) 
                    hhmmssZ = hhmmssZ.substring(0, hhmmssZ.length() - 1 );
                
                // split time: {hh:mm:ss}{Z}
                if ( tmp[2].indexOf(":") != -1 ) 
                {
                    tmp   = hhmmssZ.split(":");
                    if ( tmp.length > 0 ) {
                        isoArray[3] = Integer.parseInt(tmp [0]); // hours
                        isoArray[4] = Integer.parseInt(tmp [1]); // minutes
                        
                        if ( tmp.length > 2 )
                            isoArray[5] = Integer.parseInt(tmp[2]); // secs
                    }
                    isoArray[6] = tmp.length; // time elements (1=hh,2=hhmm,3=hhmmss)
                }
                else isoArray[3] = Integer.parseInt(hhmmssZ); // hours only
            }
        }
        else 
            isoArray[0] = Integer.parseInt(tmp[0]);  // YYYY only
        
        return isoArray;
    }

    /**
     * Parse an ISO date period: PT6H, PT3M , PT1H39M from a date such as 2005-06-01T00Z/2005-12-12T18Z/PT6H
     * @param period
     * @return
     */
    public static int[] parseISODatePeriod (String period) 
    {
        int inc = 0;    // time increment
        char incWhat;   // increment what? Y=year, M=month, D=day, H=hour
        
        if ( period.indexOf("T") == -1 ) {
            // Date increments
            // P{nn}{Y,M,D}  Y=year, M=month, D=day
            inc     = Integer.parseInt(period.substring(1, period.length() - 1 ));
            incWhat = period.charAt(period.length() - 1 );
        }
        else {
            // time increments, example:  PT6H, PT3M , PT1H39M
            // PT{nn}{H}{nn}{M,S} H=hour, M=minute,S=second
            if ( (period.indexOf("H")) != -1 && (period.indexOf("M") != -1) ) 
            {
                final int h = Integer.parseInt(period.substring(2, period.indexOf("H") ));
                final int m = Integer.parseInt(period.substring(period.indexOf("H") + 1, period.indexOf("M")));
                
                inc     = (h * 60) + m; // hours * 60 + mins
                incWhat = 'M';          // increase minutes
            }
            else {
                inc     = Integer.parseInt(period.substring(2, period.length() - 1 ));
                incWhat = period.charAt(period.length() - 1 );
            }
            
            // M (minutes) conflicts w/ (M) months
            if ( incWhat == 'M' ) incWhat = 'm';    // m = minutes
        }
    	return new int[] {inc, incWhat};
    }

    /**
     * Create a well formed ISO time YYYY-MM-DDThh:mm:ssZ from an array of time vales
     * returned by splitISOTime (0=year, 1=month, 2=day, 3=hh, 4=min, 5 = ss;)
     * @return ISO time in the format: YYYY-MM-DDThh:mm:ssZ
     * @param isoTime array of time values (0=year, 1=month, 2=day, 3=hh, 4=min, 5 = ss;)
     */
    public static String isoTime2String (final int isoTime[] )
    {   
        nf.setMinimumIntegerDigits(2);
        return isoTime[0] + "-" + nf.format(isoTime[1]) + "-" + nf.format(isoTime[2])
            + "T" + nf.format(isoTime[3]) + ":" + nf.format(isoTime[4]) + ":" + nf.format(isoTime[5]) + "Z";
    }

    
	public void setTotalLayers(int totalLayers) {
		this.totalLayers = totalLayers;
	}

	public int getTotalLayers() {
		return totalLayers;
	}
}
