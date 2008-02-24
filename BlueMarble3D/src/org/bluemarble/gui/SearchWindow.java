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
import org.fenggui.ListItem;
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
	
	private WorldWindowGLCanvas canvas;
	
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
    	
        setXY(10, getDisplayY() + 50);
        setSize(250, 200);
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
		int row = list.getMouseOverRow();
		
		System.out.println("row=" + row);

		if ( row < 0 || list.size() < row ) return;
		
		YResult Yresult = (YResult)list.getItem(row).getValue(); 
		
		System.out.println("fly to=" + Yresult);
		
		BM3DUtils.flyTo(canvas, new LatLon(
				Angle.fromDegrees(Double.parseDouble(Yresult.latitude))
				, Angle.fromDegrees(Double.parseDouble(Yresult.longitude))));

	}
	
	/**
	 * Search
	 */
	@SuppressWarnings("unchecked")
	private void search ()
	{
		final String loc = searchText.getText();
		
		if ( loc == null || loc.length() == 0) return;
		
		list.clear();
		
		try {
			YResult[] results = new YGeoSearch(loc).getLocations();
			
			if ( results != null ) 
			{
				// Add search results to the list
				for (int i = 0; i < results.length; i++) {
					final ListItem<YResult> item = FengGUI.createListItem(list);
					item.setText(results[i].toString());
					item.setValue(results[i]);
				}
			}
		} 
		catch (Exception e) {
			// Should not happen
			BM3DUtils.MessageBox(getDisplay(), e.getMessage());
		}
	}
	
	
}
