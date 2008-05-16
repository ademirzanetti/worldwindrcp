package demo;

import worldwind.contrib.layers.PlacemarkLayer;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.examples.BasicDragger;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.view.FlyToOrbitViewStateIterator;
import gov.nasa.worldwind.view.OrbitView;

/**
 * Selection listener
 * @author Owner
 *
 */
public class GlobeSelectionListener implements SelectListener 
{
    private WWIcon lastToolTipIcon = null;
    private BasicDragger dragger = null;
    private WorldWindowGLCanvas world;
    private WWIcon lastPickedIcon;
	
	public GlobeSelectionListener(WorldWindowGLCanvas canvas) {
		dragger = new BasicDragger(canvas);
		world = canvas;
	}
	

    private void highlight(Object o)
    {
        if (this.lastPickedIcon == o)
            return; // same thing selected

        if (this.lastPickedIcon != null)
        {
            this.lastPickedIcon.setHighlighted(false);
            this.lastPickedIcon = null;
        }

        if (o != null && o instanceof WWIcon)
        {
            this.lastPickedIcon = (WWIcon) o;
            this.lastPickedIcon.setHighlighted(true);
        }
    }

    /**
     * Select
     */
    public void selected(SelectEvent event)
    {
        if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
        {
            if (event.hasObjects())
            {
                System.out.println("Single clicked " + event.getTopObject()
                		+ " of class:" + event.getTopObject().getClass().getName());
                
                // Annotation clicked
                if (event.getTopObject() instanceof Annotation)
                {
                    // Check for text or url
                    PickedObject po = event.getTopPickedObject();
                    
                    if(po.getValue(AVKey.TEXT) != null)
                    {
                        System.out.println("Text: \"" + po.getValue(AVKey.TEXT) + "\" Hyperlink: "  + po.getValue(AVKey.URL));
                    }
                }
                else if (event.getTopObject() instanceof WorldMapLayer)
                {
                    // Left click on World Map : iterate view to target position
                    Position targetPos = event.getTopPickedObject().getPosition();
                    OrbitView view = (OrbitView)world.getView();
                    Globe globe = world.getModel().getGlobe();
                    // Use a PanToIterator
                    view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
                            view
                            , globe
                            // The elevation component of 'targetPos' here is not the surface elevation,
                            // so we ignore it when specifying the view center position.
                            , new Position( new LatLon(targetPos.getLatitude(), targetPos.getLongitude()), 0)
                            , Angle.ZERO
                            , Angle.ZERO
                            , targetPos.getElevation()));
                }
                else if  (event.getTopObject() instanceof WWIcon ) 
                {
                	// toggle icon annotation
                	PlacemarkLayer.PlacemarkIcon icon = (PlacemarkLayer.PlacemarkIcon)event.getTopObject();
                	
                	icon.getAnnotation().getAttributes().setVisible(
                			! icon.getAnnotation().getAttributes().isVisible());
                	
                }
            }
            else
                System.out.println("Single clicked " + "no object");
        }
        else if (event.getEventAction().equals(SelectEvent.HOVER))
        {
            if (lastToolTipIcon != null)
            {
                lastToolTipIcon.setShowToolTip(false);
                this.lastToolTipIcon = null;
                world.repaint();
            }

            if (event.hasObjects() && !this.dragger.isDragging())
            {
                if (event.getTopObject() instanceof WWIcon)
                {
                    this.lastToolTipIcon = (WWIcon) event.getTopObject();
                    lastToolTipIcon.setShowToolTip(true);
                    world.repaint();
                }
            }
        }
        else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !this.dragger.isDragging())
        {
            highlight(event.getTopObject());
        }
        else if (event.getEventAction().equals(SelectEvent.DRAG_END)
            || event.getEventAction().equals(SelectEvent.DRAG))
        {
            // Delegate dragging computations to a dragger.
            this.dragger.selected(event);
            if (event.getEventAction().equals(SelectEvent.DRAG_END))
            {
                PickedObjectList pol = world.getObjectsAtCurrentPosition();
                if (pol != null)
                    highlight(pol.getTopObject());
            }
        }
    }

}
