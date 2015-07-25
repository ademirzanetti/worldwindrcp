# World Wind Geo: The Eclipse RCP Geobrowser #
Welcome to World Wind Geo: The Eclipse Geo browser. The Geospatial Web or Geoweb is a relatively new term that implies the merging of geographical (location-based) information with the abstract information that currently dominates the Internet. World Wind Geo is an experimental geo browser built on top of:

  * World Wind Java SDK - http://worldwind.arc.nasa.gov/java/ : Allows developers to embed World Wind technology in their own applications.
  * Eclipse - http://www.eclipse.org/ : Eclipse is an open source community whose projects are focused on building an open development platform comprised of extensible frameworks, tools and runtimes.

Our goal is to close the gap between GIS & SIS (Scientific Information Systems) by incorporating low/med/hi resolution GIS datasets from multiple sources: NASA, MS Virtual Earth, and others with the java NetCDF library for plotting/subsetting capabilities of scientific data.

World Wind Geo has the following fetaures:

  * Interactive 3D navigation.
  * Full support for WMS 1.1.x and 1.3.x using wizards.
  * **Over 500 real time weather data sets** from NOAA and the Navy Research Labs.
  * Limited support for KML: **Ground overlays, screen overlays, and placemarks only**.
  * _Support for animated ground overlays to visualize events over time._
  * **Scientific Data Plot/Subset capabilities with NetCDF**.
  * Open source: built on top of the World Wind Java SDK and Eclipse frameworks.
  * Highly extensible: It uses the eclipse plugin framework to extend the user interface.
  * Multi platform: built on proven standards: Java and Eclipse.

# Automatic Updates with Eclipse! #

You can keep WWGeo up to date by using the eclipse update system:

  * Under "Help", select "Software Updates/Find and Install"
  * Click "Search for new features to install", then click next
  * Click the "New Remote Site" button on the right and enter the update information
    * Name: World Wind Geo Update Site
    * Url : http://playerx.sf.net/wwgeo/
  * Click Finish, and follow the easy installation instructions.

# Datasets #
WW Geo bundles the following datasets and features:
# NASA #
http://www.worldwindcentral.com/wiki/World_Wind_Data_Sources

  * Blue Marble (1-km/pixel resolution) - http://earthobservatory.nasa.gov/Newsroom/BlueMarble/
  * i-cubed Landsat 7 (15-meter/pixel resolution) - Global Land Cover Facility. University of Maryland, Institute for Advanced Computer Studies
  * Elevation data (SRTM30Plus/SRTMv2/USGS NED derived dataset). NASA Jet Propulsion Laboratory. http://www2.jpl.nasa.gov/srtm/
  * USGS Topographic, B&W Ortho, and Color Urban Area USGS and Microsoft Research. http://www.terraserver-usa.com/
  * United States Placenames. USGS Geographic Names Information System.http://geonames.usgs.gov/
  * World Placenames. National Geospatial-Intelligence Agency. http://earth-info.nga.mil/gns/html

# Microsoft #
http://www.microsoft.com/virtualearth/
  * Virtual Earth Roads.
  * Virtual Earth Satellite.
  * Virtual Earth Hybrid (Satellite with place names).

# The NetCDF Viewer #
NetCDF (network Common Data Form) is a set of software libraries and machine-independent data formats that support the creation, access, and sharing of array-oriented scientific data. http://www.unidata.ucar.edu/software/netcdf/ . WW Geo bundles a NetCDF viewer for the following conventions:

  * NetCDF File types: http://www.unidata.ucar.edu/software/netcdf-java/
  * NetCDF conventions: COARDS, Models3, WRF, CF, and others (see link above).
  * **To use the viewer** select "File/Open Dataset" or simply point the built-in web browser to any OpeNDAP/THREDDS data server such as http://www.unidata.ucar.edu/projects/THREDDS/currentwork.html then click any OpeNDAP dataset link (The data will be displayed automatically on the NetCDF View).

# Others #
  * Full support for WMS 1.1.x layers and 1.3.x animated or static layers.
  * Limited KML support: Temporal/static Ground, Screen overlays and placemarks.


# Requirements #
  * Graphics card with updated drivers. See http://worldwind.arc.nasa.gov/java/
  * Java 1.5

# WW Geo Main GUI Screen Shot #

Here is a screen shot of the main user interface. The earth, moon, and mars are shown, the earth displays a sea surface temperature ground overlay  obtained from a scientific NetCDF OpeNDAP dataset.

![http://worldwindrcp.googlecode.com/files/wwgeo-main-ss.jpg](http://worldwindrcp.googlecode.com/files/wwgeo-main-ss.jpg)


# Known Issues #
  * OSX/Linux: The built-in web browser doesn't work yet. A patch using Mozilla/XULRunner is on the way.
  * OSX: The WMS wizard server selection combo doesn't recognize built in server URLs.