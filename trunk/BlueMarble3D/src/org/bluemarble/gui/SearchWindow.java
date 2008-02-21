package org.bluemarble.gui;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;

import org.bluemarble.util.BM3DUtils;
import org.bluemarble.util.YGeoSearch;
import org.bluemarble.util.YGeoSearch.YResult;
import org.fenggui.Button;
import org.fenggui.Container;
import org.fenggui.FengGUI;
import org.fenggui.List;
import org.fenggui.ScrollContainer;
import org.fenggui.TextEditor;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.mouse.MouseReleasedEvent;
import org.fenggui.layout.FormAttachment;
import org.fenggui.layout.FormData;
import org.fenggui.layout.FormLayout;
import org.fenggui.layout.GridLayout;
import org.fenggui.util.Spacing;


public class SearchWindow extends Window 
{
	private List<Object> list;
	private TextEditor searchText;
	
	// Yahoo search results
	private YResult[] results;
	
	private WorldWindowGLCanvas canvas;
//	private int selectedRow = -1;
	
	/**
	 * Results list
	 * @author Owner
	 *
	 */
	private class ResultList  extends List<Object> {
		public ResultList() {
			super();
			setupTheme(ResultList.class);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void mouseReleased(MouseReleasedEvent mr) 
		{
			List list = (List)mr.getSource();
			flyToListElement(list);
			//selectedRow =list.getMouseOverRow();
		}
	}
	
	/**
	 * Constructor
	 */
	public SearchWindow(WorldWindowGLCanvas canvas) 
	{
    	super(true, false, false, true);
    	this.canvas = canvas;
    	
    	setupTheme(SearchWindow.class);
    	
        setXY(10, getDisplayY() + 30);
        setSize(250, 250);
        setTitle("Places Search");
        		
		getContentContainer().setLayoutManager(new FormLayout());
		
		Container c = FengGUI.createContainer(getContentContainer());
		c.getAppearance().setPadding(new Spacing(5,5));

		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(100,0);
		
		c.setLayoutData(fd);
		c.setLayoutManager(new GridLayout(2,2));
		
		// Search text
		searchText = FengGUI.createTextField(c);
		searchText.getAppearance().setMargin(new Spacing(0, 0, 0, 2));
		searchText.setSize(80, searchText.getMinHeight());
		searchText.setShrinkable(false);
		
		// Go button
		Button goButton = FengGUI.createButton(c, "Go");
		goButton.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				search();
			}
		});	
		
		// Scroll container for the Results list
        ScrollContainer sc = FengGUI.createScrollContainer(getContentContainer());

		fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(c,0);
		fd.bottom = new FormAttachment(1,0);

		sc.setLayoutData(fd);
		sc.getAppearance().setPadding(new Spacing(5,5));

		// Results list
		list = new ResultList();
		
        list.setSize(100, 100);
		sc.addWidget(list);

//		Button flyButton = FengGUI.createButton(getContentContainer(), "Fly to");
//
//		fd = new FormData();
//		fd.left = new FormAttachment(0,0);
//		fd.right = new FormAttachment(100,0);
//		fd.top = new FormAttachment(sc,0);
//		fd.bottom = new FormAttachment(0,0);
//		
//		flyButton.setLayoutData(fd);
//		
//		flyButton.addButtonPressedListener(new IButtonPressedListener()
//		{
//			public void buttonPressed(ButtonPressedEvent e)
//			{
//				flyToListElement(list);
//			}
//		});	

		
		layout();
	}
	
	@SuppressWarnings("unchecked")
	private void flyToListElement (List list) 
	{
		//int row = selectedRow; 
		int row = list.getMouseOverRow();
		
		System.out.println("row=" + row + " res=" + results);
		if ( row < 0 || results == null) return;
		
		YResult Yresult = results[row];
		
		//System.out.println("fly to=" + Yresult);
		
		BM3DUtils.flyTo(canvas, new LatLon(
				Angle.fromDegrees(Double.parseDouble(Yresult.latitude))
				, Angle.fromDegrees(Double.parseDouble(Yresult.longitude))));

	}
	
	/**
	 * Search
	 */
	private void search ()
	{
		final String loc = searchText.getText();
		
		if ( loc == null || loc.length() == 0) return;
		
		list.clear();
		
		try {
			results = new YGeoSearch(loc).getLocations();
			
			if ( results != null ) {
				for (int i = 0; i < results.length; i++) {
					FengGUI.createListItem(list).setText(results[i].toString());
				}
			}
		} 
		catch (Exception e) {
			// Should not happen
			BM3DUtils.MessageBox(getDisplay(), e.getMessage());
		}
	}
	
	
}
