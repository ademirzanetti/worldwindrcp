package demo.todo.mesh;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class MeshReader 
{
	float[][][] mesh_vertices;		// vertices
	float[][][] mesh_normals;		// normals
	float    mesh_maximum;		/* maximum height value in mesh */
	float    mesh_minimum;		/* minimum height value in mesh */
	
	int mesh_columns;
	int mesh_rows;

	String filePath;
	
	public MeshReader(String filePath) throws IOException 
	{
		this.filePath = filePath;
		
		RandomAccessFile raf = new RandomAccessFile(filePath, "r");
		read(raf);
	}
	
	abstract void read( RandomAccessFile raf) throws IOException;
	
	void normalizedCross(float[] u, float[] v, float[] n)
	{
	  float l;

	  /* compute the cross product (u x v for right-handed [ccw]) */
	  n[0] = u[1] * v[2] - u[2] * v[1];
	  n[1] = u[2] * v[0] - u[0] * v[2];
	  n[2] = u[0] * v[1] - u[1] * v[0];

	  /* normalize */
	  l = (float)Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
	  n[0] /= l;
	  n[1] /= l;
	  n[2] /= l;
	}

	public float getMax() { return mesh_maximum;}
	public float getMin() { return mesh_minimum;}
	public int getRows() { return mesh_rows;}
	public int getCols() { return mesh_columns;}
	
	public float[][][] getNormals () { return mesh_normals; }
	public float[][][] getVertices () { return mesh_vertices; }

	public String getFilePath() { return filePath; }
	
	/**
	 * Some utility functions
	 */
	public static final String LINE_SEP = System.getProperty("line.separator");
	
	/**
	 * Utility functions to compute test Mesh functions
	 * Usage: MeshReader.Sine("c:/tmp/mysine.dat", 100, 100);
	 * @param filePath
	 * @param ncols
	 * @param nrows
	 * @throws IOException
	 */
	public static void Sine (String filePath, int ncols, int nrows) 
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(filePath);
		fos.write( (ncols + " " + nrows + LINE_SEP).getBytes());
	
		for (int j = 0; j < nrows; j++) 
		{
			for (int i = 0; i < ncols; i++) 
			{
				final float F = (float)Math.sin(6.28 * (float)i / (float)ncols) 
						+ (float)Math.sin(6.28 * (float)j / (float)nrows);
				
				fos.write( (F + " ").getBytes() );
			}
			fos.write(LINE_SEP.getBytes());
		}
		fos.close();
	}
	
	/**
	 * Usage: MeshReader.Sine("c:/tmp/mysinc.dat", 100, 100);
	 */
	public static void Sinc (String filePath, int ncols, int nrows) 
		throws IOException
	{
		float x, y;
		
		FileOutputStream fos = new FileOutputStream(filePath);
		fos.write( (ncols + " " + nrows + LINE_SEP).getBytes());
	
		for (int j = 0; j < nrows; j++) {
			for (int i = 0; i < ncols; i++) {
				x = (float)Math.PI * 8f * ((float)(i+1) / (float)ncols) - (float)Math.PI * 4f;
			    y = (float)Math.PI * 8f * ((float)(j+1) / (float)nrows) - (float)Math.PI * 4f;
			    
			    if (x == 0.0)
			    	x = 1e-6f;
			    if (y == 0.0)
			    	y = 1e-6f;
			
			    final float F = (float)(Math.sin(x) / x + Math.sin(y) / y); 
				fos.write( ( F + " ").getBytes() );
			}
			fos.write(LINE_SEP.getBytes());
		}
		fos.close();
	}
	
}
