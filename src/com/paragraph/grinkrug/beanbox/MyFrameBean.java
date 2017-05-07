package com.paragraph.grinkrug.beanbox;

import java.awt.*;
import java.awt.event.*; 

public class MyFrameBean 
	//extends
	//	java.awt.Frame
{
	public MyFrameBean()
	{
		myFrame = new java.awt.Frame();
		myFrame.setTitle("MyFrame");

		myFrame.addWindowListener(
			new WindowListener(){
			public void windowActivated(WindowEvent we){}
			public void windowClosed(WindowEvent we){}
			public void windowClosing(WindowEvent we)
			{
				((Frame) we.getSource ()).dispose ();
				System.exit(0);
			}
			public void windowDeactivated(WindowEvent we){}
			public void windowDeiconified(WindowEvent we){}
			public void windowIconified(WindowEvent we){}
			public void windowOpened(WindowEvent we){}
				}
			);

		MyBeanBox beanbox = new MyBeanBox();
		myFrame.setLayout(new BorderLayout());
		myFrame.add(beanbox, "Center");
		myFrame.pack();
		myFrame.show();
	}

	java.awt.Frame myFrame;

}

