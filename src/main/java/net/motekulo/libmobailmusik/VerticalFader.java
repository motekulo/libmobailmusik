/*
 * Copyright (C) 2014-2015 Denis Crowdy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.motekulo.libmobailmusik;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Displays a vertically oriented fader
 *  
 * @author dcrowdy
 *
 */

public class VerticalFader extends View{
	public interface VerticalFaderPositionListener {
		void onPositionChange(View view, int newPosition);
	}

	private static final String APP_NAME = "VerticalFader";
	private static final int PADDING = 12;  // pixel width on either side of the fader
	
	private Drawable mSlider;
	private Drawable mKnob;
	private int height;
	private int width;
	private float mKnobPosition;  // Assuming a value between 0 and 100 for the moment
	private VerticalFaderPositionListener mPositionListener;

	
	/**
	 * Constructor method
	 * @param context
	 */
	public VerticalFader(Context context) {
		super(context);
		final Resources res = context.getResources();
		mSlider = res.getDrawable(R.drawable.slider);
		mKnob = res.getDrawable(R.drawable.slider_knob);
			
		mKnobPosition = (float)0.25;
		mPositionListener = null;
		
		setOnTouchListener(new OnTouchListener() {
		      public boolean onTouch(final View v, final MotionEvent event) {
		        final int pos;
		        pos = (int) (event.getX() * 100);
		       // setPosition(pos);
		        setmKnobPosition(pos);
		        return true;
		      }
		    });		
	}

	public VerticalFader(Context context, AttributeSet attrs) {
		super(context, attrs);
		final Resources res = context.getResources();
		mSlider = res.getDrawable(R.drawable.slider);
		mKnob = res.getDrawable(R.drawable.slider_knob);
		
		mKnobPosition = (float)0.25;
		mPositionListener = null;
		
		setOnTouchListener(new OnTouchListener() {
		      public boolean onTouch(final View v, final MotionEvent event) {
		        final int pos;
		       // pos = (int) (event.getX() * 100);
		        // Needs to be a value between 0 and 100, relative to height of view
		        pos = (int) (100 - event.getY()/height * 100);
			       // setPosition(pos);
		        setmKnobPosition(pos);
		        return true;
		      }
		    });
		
	}

	public VerticalFader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		final Resources res = context.getResources();
		mSlider = res.getDrawable(R.drawable.slider);
		mKnob = res.getDrawable(R.drawable.slider_knob);
		
		mKnobPosition = (float)0.25;
		mPositionListener = null;
		
		setOnTouchListener(new OnTouchListener() {
		      public boolean onTouch(final View v, final MotionEvent event) {
		        final int pos;
		        pos = (int) (event.getX() * 100);
		        
		       // setPosition(pos);
		        setmKnobPosition(pos);
		        return true;
		      }
		    });
		
	} 
	/**
	 * Sets the listener for the fader
	 * @param listener
	 */
	public void setPositionListener(final VerticalFaderPositionListener listener) {
	    mPositionListener = listener;
	  }
	
	/**
	 * Gets the current knob position
	 * @return the position of the fader knob as a float
	 */
	public float getmKnobPosition() {
		return mKnobPosition; // not converting back so probably need to relook at this
	}

	/**
	 * Sets the position of the fader knob 
	 * @param mKnobPosition
	 */
	public void setmKnobPosition(int mKnobPosition) {
		// So this expects a value between 0 and 100
		// And our mKnobPosition is a float from 0 to 1
		this.mKnobPosition = (100 - mKnobPosition)/(float)100;
		if (mKnobPosition > 100) this.mKnobPosition = 0;
		if (mKnobPosition < 0) this.mKnobPosition = 1;
		
		invalidate();
		if (mPositionListener != null) {
	        mPositionListener.onPositionChange(this, mKnobPosition);
	      }
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    height = MeasureSpec.getSize(heightMeasureSpec);
	    width = MeasureSpec.getSize(widthMeasureSpec);
	   // height = width * 4;
	    this.setMeasuredDimension(width, height);
	    //Log.i(APP_NAME, "height is " + height);
	    //Log.i(APP_NAME, "width is " + width);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		width = w;
		height = h;

	}
	
	
	@Override
	  protected void onDraw(final Canvas canvas) {
		
		//mSlider.setBounds(30, 0, 50, height);
		// left top, right bottom
		mSlider.setBounds((width/2) - (width/4), 0, (width/2) + (width/4), height);
		
		mKnob.setBounds(0, (int)(height * mKnobPosition), width, (int)(height * mKnobPosition + height/8));
		
		mSlider.draw(canvas);
		mKnob.draw(canvas);
		
	}

}
