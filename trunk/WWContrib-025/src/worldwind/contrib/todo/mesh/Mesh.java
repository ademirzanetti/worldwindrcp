package worldwind.contrib.todo.mesh;

import javax.media.opengl.GL;
import com.sun.opengl.util.GLUT;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;

public class Mesh //extends RenderableLayer
	implements Renderable
{
	private int mesh_list = 0;		/* display list for mesh */
	private int normals_list;		/* display list for normals */
	private int legend_list;		/* display list for the legend */
	boolean  draw_normals = false;	/* draw the normals? */
	
	boolean  mesh_fill;				/* filled or wireframe mesh? */
	int     mesh_columnstep = 1;	/* column step factor 1 = all points */
	int     mesh_rowstep = 1;		/* row step factor 1 = all points */
	
	
	float[][] DEFAULT_PALETTE = new float [][] { 
	  {0.5f, 0.0f, 1.0f},			/* violet */
	  {0.0f, 0.0f, 1.0f},			/* blue */
	  {0.0f, 1.0f, 1.0f},			/* cyan */
	  {0.0f, 1.0f, 0.0f},			/* green */
	  {1.0f, 1.0f, 0.0f},			/* yellow */
	  {1.0f, 0.5f, 0.0f},			/* orange */
	  {1.0f, 0.0f, 0.0f},			/* red */
	  {1.0f, 1.0f, 1.0f},			/* white */
	  {0.0f, 0.0f, 0.0f},			/* black */
	};

	int PALETTE_SIZE = DEFAULT_PALETTE.length;

	private GLUT glut = new GLUT();
	private MeshReader reader;
	
	private boolean initialized = false;
	
	public Mesh(MeshReader reader) {
		this.reader = reader;
	}
	
	public void init(DrawContext dc ) 
	{
		System.out.println("Init");
		GL gl = dc.getGL();
		
		createMeshList(dc);
		
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_COLOR_MATERIAL);
		gl.glEnable(GL.GL_DEPTH_TEST);

		// Really Nice Perspective Calculations
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);	
		
		initialized = true;
	}
	
	public void render(DrawContext dc)
	//public void doRender(DrawContext dc)
	{
		GL gl = dc.getGL();

		if ( ! initialized) init(dc); 

        gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT
                | GL.GL_POLYGON_BIT | GL.GL_TEXTURE_BIT | GL.GL_ENABLE_BIT
                | GL.GL_CURRENT_BIT);

        
//		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
//		gl.glPushMatrix();

        DrawAxis(dc, 5000000f);
		
		gl.glColor3f(1.0f, 1.0f, 1.0f);
		gl.glCallList(mesh_list);

		if (draw_normals)
		    gl.glCallList(normals_list);

		gl.glCallList(legend_list);
//		gl.glPopMatrix();
		
	}

    public void DrawAxis(DrawContext dc, float length) {
        GL gl = dc.getGL();
        gl.glBegin(GL.GL_LINES);

        // Draw 3 axis
        gl.glColor3f(0f, 0f, 1f);  // Z Blue
        gl.glVertex3d(0d, 0d, 0d);
        gl.glVertex3d(0d, 0d, length);
        gl.glColor3f(0f, 1f, 0f);  // Y Green
        gl.glVertex3d(0d, 0d, 0d);
        gl.glVertex3d(0d, length, 0d);
        gl.glColor3f(1f, 0f, 0f);  // X Red
        gl.glVertex3d(0d, 0d, 0d);
        gl.glVertex3d(length, 0d, 0d);

        gl.glEnd();
    }
	
	void createMeshList(DrawContext dc)
	{
		int i, j;
		
		GL gl = dc.getGL();

		int offset = 0;

		/* destroy any previous lists */
		if (mesh_list != 0 )
			gl.glDeleteLists(mesh_list, 1);
		if (normals_list != 0)
			gl.glDeleteLists(normals_list, 1);
		if (legend_list != 0)
			gl.glDeleteLists(legend_list, 1);
		
		  /* generate a display list for the mesh major = x axis, minor = z axis*/
		  mesh_list = gl.glGenLists(1);
		  gl.glNewList(mesh_list, GL.GL_COMPILE);

		  int mesh_columns = reader.getCols();
		  int mesh_rows = reader.getRows();
		  
		  float[][][] mesh_vertices = reader.getVertices();
		  float[][][] mesh_normals = reader.getNormals();
		  
		  
		  for (i = 0; i < mesh_columns - mesh_columnstep; i += mesh_columnstep) {

		    gl.glPolygonMode(GL.GL_BACK, GL.GL_LINE);
		    gl.glBegin(GL.GL_TRIANGLE_STRIP);

		    for (j = 0; j < mesh_rows; j += mesh_rowstep) {
		      gl.glColor3fv(DEFAULT_PALETTE[(int)((PALETTE_SIZE-2) * 
						   (mesh_vertices[i+mesh_columnstep][j][1] 
						    + 0.5))], offset);
		      gl.glNormal3fv(mesh_normals[i+mesh_columnstep][j], offset);
		      gl.glVertex3fv(mesh_vertices[i+mesh_columnstep][j],offset);
		      gl.glColor3fv(DEFAULT_PALETTE[(int)((PALETTE_SIZE-2) * 
						   (mesh_vertices[i][j][1] + 0.5))], offset);
		      gl.glNormal3fv(mesh_normals[i][j], offset);
		      gl.glVertex3fv(mesh_vertices[i][j], offset);
		    }

		    gl.glEnd();
		  }
		  gl.glEndList();
		
		  
		  normals_list = gl.glGenLists(1);
		  gl.glNewList(normals_list, GL.GL_COMPILE);
		  
		  /* draw normals */
		  for (i = 0; i < mesh_columns; i += mesh_columnstep) {
		    for (j = 0; j < mesh_rows; j += mesh_rowstep) {
		      gl.glPushMatrix();
		      gl.glTranslatef(mesh_vertices[i][j][0], 
				   mesh_vertices[i][j][1], 
				   mesh_vertices[i][j][2]);
		      gl.glScalef(0.1f, 0.1f, 0.1f);
		      gl.glBegin(GL.GL_LINES);
		      gl.glColor3f(1.0f, 0.0f, 0.0f);
		      gl.glVertex3f(0.0f, 0.0f, 0.0f);
		      gl.glColor3f(0.0f, 1.0f, 0.0f);
		      gl.glVertex3fv(mesh_normals[i][j], offset);
		      gl.glEnd();
		      gl.glScalef(1.0f, 1.0f, 1.0f);
		      gl.glPopMatrix();
		    }
		  }
		  gl.glEndList();
		  
		  legend_list = gl.glGenLists(1);
		  gl.glNewList(legend_list, GL.GL_COMPILE);
		  gl.glPushMatrix();
		  gl.glLoadIdentity();
		  gl.glTranslatef(0.0f, 0.0f, -2.0f);
		  for (i = PALETTE_SIZE; i >= 1; i--) {
		    gl.glColor3fv(DEFAULT_PALETTE[i-1], offset);
		    gl.glPolygonMode(GL.GL_BACK, GL.GL_FILL);
		    gl.glNormal3f(0.577f, 0.577f, 0.577f);
		    gl.glRectf(-0.98f, (float)(i+2) / (float)(PALETTE_SIZE) - 0.98f,
			    -0.78f, (float)(i+1) / (float)(PALETTE_SIZE) - 0.98f);
		  }
		  gl.glColor3f(1.0f, 1.0f, 1.0f);
		  
		  float mesh_maximum = reader.getMax();
		  float mesh_minimum = reader.getMin();
		  
		  String buf = ( mesh_maximum + mesh_minimum) + "";

		  text(dc, -0.98f, (float)(PALETTE_SIZE) / (float)(PALETTE_SIZE-1) - 0.95f, 0.0005f, buf);
		  
		  buf = "" + mesh_minimum;
		  
		  text(dc, -0.98f, -0.95f, 0.0005f, buf );
		  
		  gl.glPopMatrix();
		  gl.glEndList();
		  
	}
	
	/* text: general purpose text routine.  draws a string according to
	   format in a stroke font at x, y after scaling it by scale in all
	   three dimensions. */
	void text(DrawContext dc, float x, float y, float scale, String text)
	{
	  GL gl = dc.getGL();
	  gl.glPushMatrix();
	  gl.glPushAttrib(GL.GL_ENABLE_BIT);

	  gl.glDisable(GL.GL_LIGHTING);
	  gl.glDisable(GL.GL_TEXTURE_2D);
	  gl.glDisable(GL.GL_DEPTH_TEST);
	  gl.glTranslatef(x, y, 0.0f);
	  gl.glScalef(scale, scale, scale);
	  
	  for(int i = 0 ; i < text.length(); i++)
		glut.glutStrokeCharacter(GLUT.STROKE_ROMAN, text.charAt(i));

	  gl.glPopAttrib();
	  gl.glPopMatrix();
	}
	
	public void setRGBPalette (float [][] rgbPalette) {
		DEFAULT_PALETTE = rgbPalette;
		PALETTE_SIZE		= DEFAULT_PALETTE.length;
	}
	
}
