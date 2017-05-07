
// Support for drawing a property value in a Canvas.

package sun.beanbox;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

public // E.G.
class PropertyCanvas extends Canvas implements MouseListener 
{
    public // E.G.
	//PropertyCanvas(Frame frame, PropertyEditor pe) 
	PropertyCanvas(PropertyEditor pe) 
	{
		//this.frame = frame;
		editor = pe;
		addMouseListener(this);
    }

    public void paint(Graphics g) 
	{
		Rectangle box = new Rectangle(2, 2, getSize().width - 4, getSize().height - 4);
		editor.paintValue(g, box);
    }

    private static boolean ignoreClick = false;

	// E.G.(26.04.2001):
	private Frame getFrame()
	{
		Component c = this;
		while((c = c.getParent()) != null)
			if(c instanceof Frame)
				return (Frame)c;
		return null;
	}
	// end (26.04.2001).

    public void mouseClicked(MouseEvent evt) 
	{
		// E.G.(26.04.2001):
		if(frame == null)
		{
			frame = getFrame();
		}
		// end (26.04.2001).

		if (! ignoreClick) 
		{
			try 
			{
				ignoreClick = true;
				int x = frame.getLocation().x - 30;
				int y = frame.getLocation().y + 50;
				new PropertyDialog(frame, editor, x, y);
			}
			finally 
			{
				ignoreClick = false;
			}
		}
    }

    public void mousePressed(MouseEvent evt) 
	{
    }

    public void mouseReleased(MouseEvent evt) 
	{
    }

    public void mouseEntered(MouseEvent evt) 
	{
    }

    public void mouseExited(MouseEvent evt) 
	{
    }

    private Frame frame;
    private PropertyEditor editor;
}
