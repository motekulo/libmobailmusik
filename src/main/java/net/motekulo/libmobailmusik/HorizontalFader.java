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

package net.motekulo.beatsandloops;

//import net.motekulo.phonstrument.VerticalFader.VerticalFaderPositionListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class HorizontalFader extends View {
	interface HorizontalFaderPositionListener {
		void onPositionChange(View view, int newPosition);
	}

	private static final String APP_NAME = "VerticalFader";

	private static final int MGREY = 0xff4c494f;
	private static final int MOFFWHITE = 0xffd7d7db;
	private static final int MLIGHTBLUE = 0xff00d4d9;
	private static final int MTURQ = 0xff039599;
	private static final int MBLUE = 0xff003d4e;
	private static final int MRED = 0xffc8001a;

	private static final int PADDING = 16;  // pixel width on either side of the fader

	//private Drawable mSlider;
	//private Drawable mKnob;
	private int height;
	private int width;
	private float mKnobPosition;  // Assuming a value between 0 and 100 for the moment
	private HorizontalFaderPositionListener mPositionListener;
	private ShapeDrawable faderOutline;

	private ShapeDrawable faderKnob;

	public HorizontalFader(Context context) {
		super(context);
		initView(context);
	}

	public HorizontalFader(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView(context);
	}

	public HorizontalFader(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context);
	} 

	private void initView(Context context) {
		final Resources res = context.getResources();

		mKnobPosition = (float)0.5;

		mPositionListener = null;

		float outerRadius = 8;
		float[] outerRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		faderOutline = new ShapeDrawable((new RoundRectShape(outerRadii, null, null)));
		faderOutline.getPaint().setColor(MBLUE);

		outerRadius = 8;
		float[] knobRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		faderKnob = new ShapeDrawable((new RoundRectShape(knobRadii, null, null)));
		faderKnob.getPaint().setColor(MGREY);

		setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				int pos;
				pos = (int) (100 - event.getX()/width * 100);
				if (pos <0) {
					pos = 0;
				}
				if (pos > 100) {
					pos = 100;
				}
				setmKnobPosition(pos);
				return true;
			}
		});	
	}

	@Override
	public Parcelable onSaveInstanceState() {

		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putFloat("knobPosition", this.mKnobPosition);

		return bundle;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {

		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			this.mKnobPosition = bundle.getFloat("knobPosition");
			super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
			return;
		}

		super.onRestoreInstanceState(state);
	}


	/**
	 * Sets the listener for the fader
	 * @param horFaderChanged
	 */
	public void setPositionListener(final HorizontalFaderPositionListener horFaderChanged) {
		mPositionListener = horFaderChanged;
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
		height = View.MeasureSpec.getSize(heightMeasureSpec);
		width = View.MeasureSpec.getSize(widthMeasureSpec);
		
		this.setMeasuredDimension(width, height);
		//Log.i(APP_NAME, "height is " + height);
		//Log.i(APP_NAME, "width is " + width);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		width = w;
		height = h;
		
		float outerRadius = width/16;
		float[] outerRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		faderOutline = new ShapeDrawable((new RoundRectShape(outerRadii, null, null)));
		faderOutline.getPaint().setColor(MBLUE);

		outerRadius = outerRadius/2;
		float[] knobRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		faderKnob = new ShapeDrawable((new RoundRectShape(knobRadii, null, null)));
		faderKnob.getPaint().setColor(MGREY);

	}


	@Override
	protected void onDraw(final Canvas canvas) {

		faderOutline.setBounds(0, 0, width, height);
		faderOutline.draw(canvas);
		faderKnob.setBounds(width/2 - width/4, (int)(height * mKnobPosition), 
				width/2 + width/4, (int)(height * mKnobPosition + height/8));
		faderKnob.setBounds((int)(width * mKnobPosition), 0, (int)(width * mKnobPosition + width/6), height);
		
		faderKnob.draw(canvas);

	}

}
