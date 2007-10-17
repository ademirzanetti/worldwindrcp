package worldwind.contrib.todo.mesh;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MeshDATReader extends MeshReader 
{
	
	public MeshDATReader(String fname) 
		throws FileNotFoundException, IOException
	{
		super(fname);
	}
	
	void read( RandomAccessFile raf) throws IOException
	{
		float[][] mesh_data; 			// mesh data
		float[][][] mesh_facetnormals;

		float[] u = new float[3], v = new float[3], n = new float[3];
		float l;
		
		int i = 0, j = 0;
		String line;
		String[] split;
		
		// 1st line : cols rows
		split = raf.readLine().split(" ");
		
		mesh_columns = Integer.parseInt(split[0]);
		mesh_rows = Integer.parseInt(split[1]);
		
		// allocate space
		mesh_data 		= new float [mesh_columns] [mesh_rows];
		mesh_vertices	= new float [mesh_columns] [mesh_rows] [3];
		mesh_normals	= new float [mesh_columns] [mesh_rows] [3];
		mesh_facetnormals	= new float [mesh_columns] [mesh_rows] [3];

		// get data: ASCII file must have mesh_rows lines each w/ mesh_cols values
		mesh_maximum = mesh_minimum = 0;
		int row = 0;
		do {
			// read line, values are space sep
			line = raf.readLine();

			if ( line == null ) break;
			
			// must be of mesh_cols size
			split = line.split(" "); 
			
			if ( split.length != mesh_columns)
				throw new IOException("Invalid # of columns " 
						+ split.length + " for " + getFilePath() 
						+ " row " + row);
			
			for (i = 0; i < mesh_columns; i++) 
			{
				//System.out.println("(" + row + "," + i + ")=" + Float.parseFloat(split[i]) );
				mesh_data[i][row] = Float.parseFloat(split[i]);
				
				// get max/min
				if (mesh_maximum < mesh_data[i][row]) mesh_maximum = mesh_data[i][row];
				if (mesh_minimum > mesh_data[i][row]) mesh_minimum = mesh_data[i][row];
			}
			row++;
		}
		while (line != null);

		//System.out.println("max=" + mesh_maximum + " min=" + mesh_minimum);
		
		/* set mesh_maximum to reflect the maximum extent of the height of
 			the mesh. */
		mesh_maximum -= mesh_minimum;

		/* bump the mesh_data value up to the zero point by adding the
 			minimum to it. (normalize to + side of numberline). */
		for ( i = 0; i < mesh_columns; i++) {
			for (j = 0; j < mesh_rows; j++) {
				mesh_data[i][j] += -mesh_minimum;
			}
		}
		
		  /* fill out the vertex array and calculate facet normals for the
	     mesh. */
	  for (i = 0; i < mesh_columns - 1; i++) {
	    for (j = 0; j < mesh_rows - 1; j++) {
	      /* assign the data to vertices.  some of the vertices will be
	         overwritten in subsequent iterations of the loop, but this is
	         okay, since they will be identical. */
	      mesh_vertices[i][j][0] = ((float)i / (float)mesh_columns) - 0.5f; 
	      mesh_vertices[i][j][1] = (mesh_data[i][j] / mesh_maximum) - 0.5f;
	      mesh_vertices[i][j][2] = ((float)j / (float)mesh_rows) - 0.5f;
	      
	      mesh_vertices[i][j+1][0] = mesh_vertices[i][j][0];
	      mesh_vertices[i][j+1][1] = (mesh_data[i][j+1] / mesh_maximum) - 0.5f;
	      mesh_vertices[i][j+1][2] = ((float)(j+1) / (float)mesh_rows) - 0.5f;
	      
	      mesh_vertices[i+1][j][0] = ((float)(i+1) / (float)mesh_columns)- 0.5f;
	      mesh_vertices[i+1][j][1] = (mesh_data[i+1][j] / mesh_maximum) - 0.5f;
	      mesh_vertices[i+1][j][2] = mesh_vertices[i][j][2];
	       
	      /* get two vectors to cross */
	      u[0] = mesh_vertices[i][j+1][0] - mesh_vertices[i][j][0];
	      u[1] = mesh_vertices[i][j+1][1] - mesh_vertices[i][j][1];
	      u[2] = mesh_vertices[i][j+1][2] - mesh_vertices[i][j][2];

	      v[0] = mesh_vertices[i+1][j][0] - mesh_vertices[i][j][0];
	      v[1] = mesh_vertices[i+1][j][1] - mesh_vertices[i][j][1];
	      v[2] = mesh_vertices[i+1][j][2] - mesh_vertices[i][j][2];

	      /* get the normalized cross product */
	      normalizedCross(u, v, n);
	      
	      /* put the facet normal in the i, j position for later averaging
	         with other normals. */
	      mesh_facetnormals[i][j][0] = n[0];
	      mesh_facetnormals[i][j][1] = n[1];
	      mesh_facetnormals[i][j][2] = n[2];
	    }
	  }
		
	  /* fill in the last vertex & it's facet normal */
	  mesh_vertices[i][j][0] = ((float)i / (float)mesh_columns) - 0.5f; 
	  mesh_vertices[i][j][1] = (mesh_data[i][j] / mesh_maximum) - 0.5f;
	  mesh_vertices[i][j][2] = ((float)j / (float)mesh_rows) - 0.5f;

	  mesh_facetnormals[i][j][0] = n[0];
	  mesh_facetnormals[i][j][1] = n[1];
	  mesh_facetnormals[i][j][2] = n[2];
	  
	  /* calculate normals for the mesh */
	  for (i = 1; i < mesh_columns - 1; i++) {
	    for (j = 1; j < mesh_rows - 1; j++) {
	      /* average all the neighboring normals. */
	      n[0] = mesh_facetnormals[i-1][j-1][0];
	      n[1] = mesh_facetnormals[i-1][j-1][1];
	      n[2] = mesh_facetnormals[i-1][j-1][2];

	      n[0] += mesh_facetnormals[i][j-1][0];
	      n[1] += mesh_facetnormals[i][j-1][1];
	      n[2] += mesh_facetnormals[i][j-1][2];

	      n[0] += mesh_facetnormals[i-1][j][0];
	      n[1] += mesh_facetnormals[i-1][j][1];
	      n[2] += mesh_facetnormals[i-1][j][2];

	      n[0] += mesh_facetnormals[i][j][0];
	      n[1] += mesh_facetnormals[i][j][1];
	      n[2] += mesh_facetnormals[i][j][2];
	      
	      l = (float)Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
	      mesh_normals[i][j][0] = n[0] /= l;
	      mesh_normals[i][j][1] = n[1] /= l;
	      mesh_normals[i][j][2] = n[2] /= l;
	    }
	  }

	  /* fill in the normals on the top/bottom edge of the mesh (simply
	     copy the one below/above it). */
	  for (i = 0; i < mesh_columns; i++) {
	    mesh_normals[i][0][0] = mesh_normals[i][1][0];
	    mesh_normals[i][0][1] = mesh_normals[i][1][1];
	    mesh_normals[i][0][2] = mesh_normals[i][1][2];

	    mesh_normals[i][mesh_rows-1][0] = mesh_normals[i][mesh_rows-2][0];
	    mesh_normals[i][mesh_rows-1][1] = mesh_normals[i][mesh_rows-2][1];
	    mesh_normals[i][mesh_rows-1][2] = mesh_normals[i][mesh_rows-2][2];
	  }

	  /* fill in the normals on the left/right edge of the mesh (simply
	     copy the one right/left of it). */
	  for (j = 0; j < mesh_rows; j++) {
	    mesh_normals[0][j][0] = mesh_normals[1][j][0];
	    mesh_normals[0][j][1] = mesh_normals[1][j][1];
	    mesh_normals[0][j][2] = mesh_normals[1][j][2];

	    mesh_normals[mesh_columns-1][j][0] = mesh_normals[mesh_columns-2][j][0];
	    mesh_normals[mesh_columns-1][j][1] = mesh_normals[mesh_columns-2][j][1];
	    mesh_normals[mesh_columns-1][j][2] = mesh_normals[mesh_columns-2][j][2];
	  }
	  
	  System.out.println("(r,c, max)=" +  mesh_columns + "," + mesh_rows + "," + mesh_maximum);
	  
	  	// cleanup
	  	mesh_facetnormals = null;
	  	mesh_data = null;
	  	
		raf.close();
	}

}
