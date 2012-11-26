package com.flatturtle.myturtlecontroller.ui;

import android.widget.ImageView;

public class MainButton {
	public String tag;
	public String title;
	public int lines;
	public ImageView icon;
	
	public MainButton(ImageView icon, String tag, String title, int lines) {
		this.icon = icon;
		this.tag = tag;
		this.title = title;
		this.lines = lines;
	}
}
