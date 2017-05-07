
package sun.beanbox;

import java.beans.*;
import java.lang.reflect.*;
import java.awt.*;
import java.util.Hashtable;
import java.util.Vector;

//import com.paragraph.grinkrug.editors.*;

public class PropertySheet extends Frame 
{
    private PropertySheetPanel panel;
    private boolean started;

    PropertySheet(Wrapper target, int x, int y) 
	{
		super("Properties - <initializing...>");
		setLayout(null);
		setBackground(Color.lightGray);	
		setBounds(x,y, 100, 100);

		panel = new PropertySheetPanel(this);

		show();
		panel.setTarget(target);
		setTitle("Properties - " + target.getBeanLabel());

		started = true;
    }

    void setTarget(Wrapper targ) 
	{
		Object bean = targ.getBean();
		String displayName = targ.getBeanLabel();
		panel.setTarget(targ);
		setTitle("Properties - " + displayName);
    }

    public void doLayout() 
	{
		// Normally we get called when propertySheetPanel.setTarget
		// has changed the size of the ScrollPane and of us.
		if (!started) 
		{
			return;
		}
		panel.stretch();
    }

    void setCustomizer(Customizer c) 
	{
		panel.setCustomizer(c);
    }

    void wasModified(PropertyChangeEvent evt) 
	{
//System.err.println("wasModified(source = "+evt.getSource()+", prop = "+evt.getPropertyName()+")");
		panel.wasModified(evt);
    }
}

class PropertySheetPanel extends Panel 
{

    PropertySheetPanel(PropertySheet frame) 
	{
		this.frame = frame;
		setLayout(null);
    }

    synchronized void setTarget(Wrapper targ)
	{
		frame.removeAll();	
		removeAll();

		// We make the panel invisivle during the reconfiguration
		// to try to reduce screen flicker.

		// As a workaround for #4056424, we avoid maling the panel
		// invisible first time though, during startup.
		if (target != null) 
		{
			setVisible(false);
		}
		targetWrapper = targ;
		target = targ.getBean();

		//debug:
		//*
		Object[] allParentsBeans = targetWrapper.getAllParentsBeans();
		//for(int i = 0; i < allParentsBeans.length; ++i)
		//{
		//	System.err.println("container bean["+i+"] = "+allParentsBeans[i].toString());
		//}
		//System.err.println("-------------------------------");
		//*/

		try 
		{
			BeanInfo bi = Introspector.getBeanInfo(target.getClass());
			properties = bi.getPropertyDescriptors();
		}
		catch (IntrospectionException ex) 
		{
			error("PropertySheet: Couldn't introspect", ex);
			return;
		}

		editors = new PropertyEditor[properties.length];
		values = new Object[properties.length];
		views = new Component[properties.length];
		labels = new Label[properties.length];

		// Create an event adaptor.
		EditedAdaptor adaptor = new EditedAdaptor(frame);

		for (int i = 0; i < properties.length; i++) 
		{
//System.err.println("propertyDescriptor("+properties[i].getName()+") -> "+properties[i]);
			// Don't display hidden or expert properties.
			if (properties[i].isHidden() || properties[i].isExpert()) 
			{
				continue;
			}

			String name = properties[i].getDisplayName();
			Class type = properties[i].getPropertyType();
if(type == null)
{
System.err.println("name = "+name+",  property type == null.");	
//throw new RuntimeException("MY DEBUG.");
}
			Method getter = properties[i].getReadMethod();
			Method setter = properties[i].getWriteMethod();

			// Only display read/write properties.
//			if (getter == null || setter == null) 
//			{
//				continue;
//			}
			// E.G.(28.05.2001):
//			if (getter == null) 
//			{
//				continue;
//			}

			// E.G.(27.09.2001):
			if (getter == null)
			{
			// test for indexed property (05.07.2001):
				Method indexedGetter = 
				(properties[i] instanceof IndexedPropertyDescriptor)?
					((IndexedPropertyDescriptor)properties[i]).getIndexedReadMethod()
					:
					null;
				if((getter == null) && (indexedGetter == null)) 
				{
					continue;
				}
System.err.println(" ONLY HAVE indexedGetter = "+indexedGetter);
			}
		
			Component view = null;

			try 
			{
				//(27.09.2001) commented by E.G.
//				Object args[] = { };
//System.err.println("calling getter.invoke(...): target = "+target+", getter = "+getter);
//				Object value = getter.invoke(target, args);
//System.err.println("OK.");
//				values[i] = value;

				//my changed variant:
				Object value = getPropertyValueByPropertyDescriptor(properties[i]);
				values[i] = value;

				PropertyEditor editor = null;
				Class pec = properties[i].getPropertyEditorClass();
				if (pec != null) 
				{
					try 
					{
						editor = (PropertyEditor)pec.newInstance();
					}
					catch (Exception ex) 
					{
						// Drop through.
					}
				}
				//if (editor == null) 
				if (editor == null && type != null) 
				{
					//System.err.println("calling findEditor for type -> "+type.getName());
					editor = PropertyEditorManager.findEditor(type);
				}
				editors[i] = editor;

				//if(editors[i] != null)
				//System.err.println("editor[i].getClass().getName() -> "+editors[i].getClass().getName());

				// If we can't edit this component, skip it.
				if (editor == null) 
				{
					// If it's a user-defined property we give a warning.
					//String getterClass = properties[i].getReadMethod().getDeclaringClass().getName();

					//if (getterClass.indexOf("java.") != 0) 
					//{
					//	System.err.println("Warning: Can't find public property editor for property \""
					//	+ name + "\". of type "+type.getName()+" Skipping.");
						//System.err.println("properties["+i+"] -> "+properties[i].getClass().getName());
						//if(type.isArray())
					//}
					if(properties[i] instanceof IndexedPropertyDescriptor)
					{
						System.err.println("property["+i+"] type is Array");
                        if(type != null)
						    System.err.println("property["+i+"] type is "+type.getName());
						//System.err.println("property["+i+"] is IndexedPropertyDescriptor.");
//==============================================================================================================================
						//editors[i] = new IndexedPropertyEditor
						//(
						//	targetWrapper, 
						//	(IndexedPropertyDescriptor)properties[i],
						//	frame
						//);
//==============================================================================================================================
					}
					//else
					//{
					//	System.err.println("property["+i+"] is NOT IndexedPropertyDescriptor.");
					//}

					if(editors[i] == null) // E.G.
					{
						//if(getterClass.startsWith("java."))
						//	continue;
						//else

//						System.err.println("Warning: Can't find public property editor for property \""
//						+ name
//                        + "\". of type "
//                        +((type != null)? type.getName() : "null")
//                        +" Skipping.");
						continue;
					}
					else
					{
						editor = editors[i];
					}
				}
	/*
	// COMMENTED BY E.G.(20.07.2000):
			// Don't try to set null values:
			if (value == null) {
				// If it's a user-defined property we give a warning.
				String getterClass = properties[i].getReadMethod().getDeclaringClass().getName();
				if (getterClass.indexOf("java.") != 0) {
					System.err.println("Warning: Property \"" + name 
					+ "\" has null initial value.  Skipping.");	
					//System.err.println("properties[i].getClass().getName() -> "+properties[i].getClass().getName());//EMG (18.07.2000)
				}
				//continue; // Commented by E.G.(19.07.2000)
				continue;
			}
	//*/
	//------------------ E.G. (02.08.2000) -------------------
	// to have an ability to init editor with context related info...
	// (extract public void initContextInfo(Object info) method and call it):
				Class editorClass = editor.getClass();
				Method initMethod = null;
				Class[] parameterTypes = {Object.class};
				try
				{
					initMethod = editorClass.getMethod("initContextInfo", parameterTypes);
					if(initMethod != null)
					{
//System.err.println("calling initContextInfo()...");
						//Object[] initArgs = {allParentsBeans};
						Object[] initArgs = new Object[allParentsBeans.length + 2];
						initArgs[0] = ""+targetWrapper.getBeanLabel()+" @" + Integer.toHexString(target.hashCode());
						initArgs[1] = properties[i];
						System.arraycopy(allParentsBeans, 0, initArgs, 2, allParentsBeans.length);
						Object[] invokeParam = {initArgs};
						initMethod.invoke(editor, invokeParam);
					}
				}
				catch(Exception e)
				{
					//System.err.println(editorClass.getName()+": Cannot get initContextInfo(...) method");
					//System.err.println(e.toString());
				}
	//----------------------------------------------------------

				editor.setValue(value);
				editor.addPropertyChangeListener(adaptor);

				// Now figure out how to display it...
				if (editor.isPaintable() && editor.supportsCustomEditor()) 
				{
					//view = new PropertyCanvas(frame, editor);
					view = new PropertyCanvas(editor);
				} 
				else 
				if (editor.getTags() != null) 
				{
					view = new PropertySelector(editor);
				} 
				else 
				if (editor.getAsText() != null) 
				{
					String init = editor.getAsText();
					view = new PropertyText(editor);
				}
				else 
				{
					System.err.println("Warning: Property \"" + name 
						+ "\" has non-displayabale editor.  Skipping.");
					continue;
				}

			}
			catch (InvocationTargetException ex) 
			{
				System.err.println("Skipping property " + name + " ; exception on target: " + ex.getTargetException());
				ex.getTargetException().printStackTrace();
				continue;
			}
			catch (Exception ex) 
			{
				System.err.println("Skipping property " + name + " ; exception: " + ex);
				ex.printStackTrace();
				continue;
			}

			labels[i] = new Label(name, Label.RIGHT);
			add(labels[i]);

			//-------------(09.08.2001): (E.G.) -------------------
			if(setter == null)
				view.setBackground(Color.gray);
			//-----------------------------------------------------
			views[i] = view;

			add(views[i]);
		}

		frame.add(this);
		doLayout(true);

		processEvents = true;

		Insets ins = frame.getInsets();
		
		int frameWidth = getSize().width + ins.left + ins.right;
		int frameHeight = getSize().height + ins.top + ins.bottom;

		// Do we need to add a scrollpane ?
		boolean needPane = false;
		if (frameWidth > maxWidth) 
		{
			needPane = true;
			frameWidth = maxWidth;
		}
		if (frameHeight > maxHeight) 
		{
			needPane = true;
			frameHeight = maxHeight;
		}

		if (needPane) 
		{
			// Put us in a ScrollPane.

			// Note that the exact ordering of this code is
			// very important in order to get correct behaviour
			// on win32.  Don't modify unless you have a lot of
			// spare time to test/debug it.

			frame.remove(this);

			frameWidth = frameWidth + 30;
			if (frameWidth > maxWidth) 
			{
				frameWidth = maxWidth;
			}
			frameHeight = frameHeight + 30;
			if (frameHeight > maxHeight) 
			{
				frameHeight = maxHeight;
			}

			ScrollPane pane = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
			pane.setBounds(ins.left, ins.top,
					frameWidth - (ins.left + ins.right),
					frameHeight - (ins.top + ins.bottom));

			frame.setSize(frameWidth,frameHeight);	
			pane.add(this);
			frame.add(pane);

			pane.doLayout();

		}
		else 
		{
			frame.setSize(frameWidth,frameHeight);	
			//We don't need a ScrollPane.
			setLocation(ins.left, ins.top);
			frame.add(this);
		}

		setVisible(true);
    }

    void stretch() 
	{
		// This gets called when a user explicitly resizes the frame.
		Component child = null;
		try 
		{
			child = frame.getComponent(0);
		}
		catch (Exception ex) 
		{
			// frame has no active children;
			return;
		}
		Dimension childSize = child.getSize();
		Dimension frameSize = frame.getSize();
		Insets ins = frame.getInsets();
		int vpad = ins.top + ins.bottom;
		int hpad = ins.left + ins.right;

		// If the frame size hasn't changed, do nothing.
		if (frameSize.width == (childSize.width + hpad) &&
			frameSize.height == (childSize.height + vpad)) 
		{
	    return;
		}

		// We treat the new frame sizes as a future maximum for our own
		// voluntary size changes.
		maxHeight = frameSize.height;
		maxWidth = frameSize.width;

		// If we've gotten smaller, force new layout.
		if (frameSize.width < (childSize.width + hpad) ||
			frameSize.height < (childSize.height + vpad)) 
		{
			// frame has shrunk in at least one dimension.
			setTarget(targetWrapper);
		}
		else 
		{
			// Simply resize the contents.  Note that this won't make
			// any ScrollPane go away, that will happen on the next
			// focus change.
 			child.setSize(frameSize.width - hpad, frameSize.height - vpad);
		}
    }

    private void doLayout(boolean doSetSize) 
	{
		if (views == null || labels == null) 
		{
			return;
		}

		// First figure out the size of the columns.
		int labelWidth = 92;
		int viewWidth = 120;

		for (int i = 0; i < properties.length; i++) 
		{
			if (labels[i] == null || views[i] == null)
			{
				continue;
			}
			int w = labels[i].getPreferredSize().width;
			if (w > labelWidth) 
			{
				labelWidth = w;
			}
			w = views[i].getPreferredSize().width;
			if (w > viewWidth) 
			{
				viewWidth = w;
			}
		}
		int width = 3*hPad + labelWidth + viewWidth;

		// Now position all the components.
		int y = 10;
		for (int i = 0; i < properties.length; i++) 
		{
			if (labels[i] == null || views[i] == null) 
			{
			continue;
			}
			labels[i].setBounds(hPad, y+5, labelWidth, 25);

			Dimension viewSize = views[i].getPreferredSize();
			int h = viewSize.height;
			if (h < 30) 
			{
			h = 30;
			}
			views[i].setBounds(labelWidth + 2*hPad, y, viewWidth, h);
			y += (h + vPad);
		}

		y += vPad;

		if (doSetSize) 
		{
			setSize(width, y);
		}
    }


    public void doLayout() 
	{
		doLayout(false);
    }


    synchronized void setCustomizer(Customizer c) 
	{
		if (c != null) 
		{
			c.addPropertyChangeListener(new EditedAdaptor(frame));
		}
    }

    synchronized void wasModified(PropertyChangeEvent evt)
	{
		if(!processEvents) 
		{
			return;
		}

//System.err.println(getClass().getName()+".wasModified("+evt+").");

		if(evt.getSource() instanceof PropertyEditor) 
		{
			PropertyEditor editor = (PropertyEditor) evt.getSource();
			for(int i = 0 ; i < editors.length; i++) 
			{
				if (editors[i] == editor) 
				{
					PropertyDescriptor property = properties[i];
//System.err.println("got event from editor for ["+property.getName()+"]...");
					Object value = editor.getValue();

// my debug:
//if(editor instanceof IndexedPropertyEditor)
//System.err.println("editor = "+editor+"; value = "+value);

					values[i] = value;


					Method setter = property.getWriteMethod();

					//(22.06.2001):
					if(setter != null)
					{
						try 
						{
							Object args[] = { value };
	// my debug:
	//if(editor instanceof IndexedPropertyEditor)
	//System.err.println("args = "+args+"; args.length = "+args.length);

							//args[0] = value;

	//System.err.println("propertyName = "+property.getName()+" target = "+target+" args[0] = "+args[0]+" type = "+args[0].getClass());
	//System.err.println("setter : propertyType = "+property.getPropertyType());
							setter.invoke(target, args);
	//System.err.println("setter.invoke(...) DONE.");
					
							// We add the changed property to the targets wrapper
							// so that we know precisely what bean properties have
							// changed for the target bean and we're able to
							// generate initialization statements for only those
							// modified properties at code generation time. 
							targetWrapper.getChangedProperties().addElement(properties[i]);

						}
						catch (InvocationTargetException ex) 
						{
							if(ex.getTargetException() instanceof PropertyVetoException) 
							{
								//warning("Vetoed; reason is: " 
								//        + ex.getTargetException().getMessage());
								// temp dealock fix...I need to remove the deadlock.
								System.err.println("WARNING: Vetoed; reason is: " 
											+ ex.getTargetException().getMessage());
							}
							else
								//error("InvocationTargetException while updating " 
								//			+ property.getName(), ex.getTargetException());
								error("InvocationTargetException while updating property [" 
											+ property.getName()
											+"]", ex.getTargetException());
						}
						catch (Exception ex) 
						{
							error("Unexpected exception while updating " 
											+ property.getName(), ex);
						}

					}// end of (22.06.2001).

					if(views[i] != null && views[i] instanceof PropertyCanvas) 
					{
						views[i].repaint();
					}

					break;//?????????????????? (31.10.2001-E.G.)
				}

				//break;
			}
		}

		// Now re-read all the properties and update the editors
		// for any other properties that have changed.
		for(int i = 0; i < properties.length; i++) 
		{
			// (27.09.2001) commented by E.G.:
//			Object o;
//			try 
//			{
//				Method getter = properties[i].getReadMethod();
//				Object args[] = { };
//				o = getter.invoke(target, args);
//			}
//			catch(Exception ex) 
//			{
//				o = null;
//			}
//String propertyName = properties[i].getName();

			// new variant:
			Object o;
			try
			{
//System.err.println("reading value from "+target+"; property = "+propertyName);
				o = getPropertyValueByPropertyDescriptor(properties[i]);
			}
			catch(Exception ex)
			{
//System.err.println("got ex -> "+ex);
				o = null;
			}

// my debug:
//if(editors[i] instanceof IndexedPropertyEditor)
//	System.err.println(getClass().getName()+".wasModified(...): o = "+ o);
// myDebug:

			if(properties[i] instanceof IndexedPropertyDescriptor)
			{
				//if(o == values[i] || (o != null && o.equals(values[i]))) 
				if(o == values[i] || areArraysEqual((Object[])values[i], (Object[])o)) 
				{
					// The property is equal to its old value.
//System.err.println(" property ["+propertyName+"] IS NOT CHANGED.");
					continue;
				}
			}
			else
			{
				if(o == values[i] || (o != null && o.equals(values[i]))) 
				{
					// The property is equal to its old value.
//System.err.println(" property ["+propertyName+"] IS NOT CHANGED.");
					continue;
				}
			}

//System.err.println(" property ["+propertyName+"] IS CHANGED.");
//System.err.println(" propertySheet  value = "+values[i]);
//System.err.println(" property(bean) value = "+o);

			//values[i] = o;//???????????????????

			// Make sure we have an editor for this property...
			if (editors[i] == null) 
			{
//System.err.println("No editor for propoerty -> "+propertyName);
				continue;
			}
			// The property has changed!  Update the editor.
//System.err.println(getClass().getName()+".wasModified(...): Update the editor.");
			editors[i].setValue(o);

			values[i] = o; ///????????????????????

			if(views[i] != null) 
			{
				views[i].repaint();
			}
		}

		// Make sure the target bean gets repainted.
		if(Beans.isInstanceOf(target, Component.class)) 
		{
			((Component)(Beans.getInstanceOf(target, Component.class))).repaint();
		}
//System.err.println("--------------------------------------------");
    }

    private void warning(String s) 
	{
		new ErrorDialog(frame, "Warning: " + s);
    }

    //----------------------------------------------------------------------
    // Log an error.

    private void error(String message, Throwable th) 
	{
		String mess = message + ":\n" + th;
		//System.err.println(message);
		//th.printStackTrace();
		// Popup an ErrorDialog with the given error message.
		new ErrorDialog(frame, mess);
    }
//================= (27.09.2001 E.G.) ======================================
	// my extension (NON STANDARD !!!)
	private Object getPropertyValueByPropertyDescriptor(PropertyDescriptor pd)
		throws InvocationTargetException, IllegalAccessException
	{
		//return null;//stub
		if(
			(pd instanceof IndexedPropertyDescriptor)
			&&
			(((IndexedPropertyDescriptor)pd).getReadMethod() == null)
		  )
        {
		   return getPropertyValueByIndexedGetter((IndexedPropertyDescriptor)pd);
        }
		else
		{
			Method getter = pd.getReadMethod();
			Object args[] = {};
			return getter.invoke(target, args);
		}
	}
	private Object getPropertyValueByIndexedGetter(IndexedPropertyDescriptor ipd)
		throws InvocationTargetException, IllegalAccessException
	{
//System.err.println("getPropertyValueByIndexedGetter("+ipd.getName()+") invoked.");
		//return null;//stub
		Method indexedGetter = ipd.getIndexedReadMethod();// method surely exists!
		java.util.Vector tmp = null;//new java.util.Vector();
		int index = 0;
		try
		{
			while(true)
			{
				//int index = 0;
				Object[] args = {new Integer(index)};
				Object tmpObject = indexedGetter.invoke(target, args);
//System.err.println("got indexed property value["+index+"] = "+tmpObject);
				if(tmp == null)
					tmp = new java.util.Vector();
				tmp.addElement(tmpObject);
				index++;
			}
		}
		catch(InvocationTargetException ex)
		{
//System.err.println("getting indexed property value["+index+"] => ex = "+ex);
				Throwable targetException = ((InvocationTargetException)ex).getTargetException();
//System.err.println("targetException = "+targetException);
				if(targetException instanceof NullPointerException)
				{
//System.err.println("returning null.");
					return null;
				}
				else
				//if(targetException instanceof ArrayIndexOutOfBoundsException)
				//if(targetException instanceof IndexOutOfBoundsException)
				if(targetException instanceof RuntimeException)
				{
					if(tmp == null)
					{
//System.err.println("returning null.");
						return null;
					}
					int count = tmp.size();
					Object[] result = (Object[])Array.newInstance(ipd.getIndexedPropertyType(), count);
					tmp.copyInto(result);
//System.err.println("returning result array of size = "+result.length);
//for(int _i = 0; _i < result.length; _i++)
//System.err.println("-> result["+_i+"] = "+result[_i]);
					return result;
				}
				else
				{
//System.err.println("value["+index+"] => rethrow ex = "+ex);
					throw ex;
				}
		}
	}
	//============== static methods ====================
	public static final boolean areArraysEqual(Object[] a1, Object[] a2)
	{
		if(a1 == null)
		{
			if(a2 == null)
				return true;
			else
				return false;
		}
		else
		{ // a1 != null
			if(a2 == null)
				return false;
			else
			{ // a2 != null
				if(a1.length != a2.length)
					return false;
				else
				{
					for(int i = a1.length - 1; i >= 0; i--)
					{
						if(a1[i] == null)
						{
							if(a2[i] == null)
								continue;
							else
								return false;
						}
						else
						{
							if(a1[i].equals(a2[i]))
								continue;
							else
								return false;
						}
					}
					return true;
				}
			
			}
		}
	}


//================= (27.09.2001 E.G.) ======================================
    //----------------------------------------------------------------------
    private PropertySheet frame;

    // We need to cache the targets' wrapper so we can annoate it with
    // information about what target properties have changed during design
    // time.
    private Wrapper targetWrapper;   
    private Object target;
    private PropertyDescriptor properties[];
    private PropertyEditor editors[];
    private Object values[];
    private Component views[];
    private Label labels[];

    private boolean processEvents;
    private static int hPad = 4;
    private static int vPad = 4;
    private int maxHeight = 500;
    private int maxWidth = 300;
}
