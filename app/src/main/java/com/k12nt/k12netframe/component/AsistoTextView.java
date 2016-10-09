package com.k12nt.k12netframe.component;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class AsistoTextView extends TextView {
	
	public String fontName = "ProximaNova/ProximaNova-Light.otf";
	public String fontNameBold = "ProximaNova/ProximaNova-Semibold.otf";
	public String fontNameItalic = "ProximaNova/ProximaNova-RegularItalic.otf";
	public String fontNameBoldItalic = "ProximaNova/ProximaNova-Semibolditalic.otf";

	public AsistoTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	    init();

	}
	
	public AsistoTextView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init();
	}


	public AsistoTextView(Context context) {
	    super(context);
	    init();
	}


	public AsistoTextView(Context context, int defStyle) {
		super(context, null, defStyle);
	    init();
	}

	private void init() {
		if(isInEditMode() == false) {
		    Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontName); //"fonts/HelveNeuMedCon.ttf"
		    if(getTypeface() != null){
				if (getTypeface().isBold() && getTypeface().isItalic()) {
					 tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontNameBoldItalic); //"fonts/HelveNeuMedCon.ttf"
				}
			    else if(getTypeface().isBold()) {
				    tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontNameBold); //"fonts/HelveNeuMedCon.ttf"
				}
				else if(getTypeface().isItalic()) {
				    tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/"+fontNameItalic); //"fonts/HelveNeuMedCon.ttf"
				}
		    }
		    setTypeface(tf);
		}
	}

	public String getFontName() {
		return fontName;
	}

	public void setFontName(String fontName) {
		this.fontName = fontName;
	}
	
}
