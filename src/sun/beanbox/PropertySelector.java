
// Support for PropertyEditors that use tags.

package sun.beanbox;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

//import com.paragraph.grinkrug.editors.*;

public // E.G.
class PropertySelector extends Choice implements ItemListener 
{

	public // E.G.
    PropertySelector(PropertyEditor pe) 
	{
		editor = pe;
		String tags[] = editor.getTags();
		for (int i = 0; i < tags.length; i++) 
		{
			addItem(tags[i]);
		}
		select(0);
		// This is a noop if the getAsText is not a tag.
		select(editor.getAsText());
		addItemListener(this);
    }

    public void itemStateChanged(ItemEvent evt) 
	{
		String s = getSelectedItem();
		editor.setAsText(s);
    }

    public void repaint() 
	{
		select(editor.getAsText());
    }

    PropertyEditor editor;    
}
