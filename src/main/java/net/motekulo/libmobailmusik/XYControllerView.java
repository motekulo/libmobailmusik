/**
 * Copyright Denis Crowdy 2013
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 * 
 */


package net.motekulo.phonstrument;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class XYControllerView extends View {
	public interface touchListener {
		void onPositionChange(View view, int mouseX, int mouseY);
	}

	private static final String APP_NAME = "Phonstrument";

	// Motekulo swatch colours:
	private static final int MGREY = 0xff4c494f;
	private static final int MOFFWHITE = 0xffd7d7db;
	private static final int MLIGHTBLUE = 0xff00d4d9;
	private static final int MTURQ = 0xff039599;
	private static final int MBLUE = 0xff003d4e;
	private static final int MRED = 0xffc8001a;

	private static final int PADDING = 8;

	private XYControllerView.touchListener mTouchListener;
	private int height;
	private int width;
	private int mcolor = Color.BLACK;
	private ShapeDrawable gridBoundary;
	private ShapeDrawable YDivLine;
	private ShapeDrawable XDivLine;
	private ShapeDrawable beatIndicator;
	private ShapeDrawable blankBeatIndicator;

	private int Xmin;
	private int Xmax;
	private int Ymin;
	private int Ymax;

	private int[] toggleState;

	private int currentBeat;

	public XYControllerView(Context context) {
		super(context);
		initView();

	}

	public XYControllerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public XYControllerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {

		float outerRadius = 16;
		float[] outerRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		//RectF inset = new RectF(2, 2, 2, 2);

		float[] innerRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		gridBoundary = new ShapeDrawable((new RoundRectShape(outerRadii, null, innerRadii)));
		gridBoundary.getPaint().setColor(MBLUE);

		XDivLine = new ShapeDrawable((new RectShape()));
		XDivLine.getPaint().setColor(Color.WHITE);
		YDivLine = new ShapeDrawable((new RectShape()));
		YDivLine.getPaint().setColor(Color.WHITE);

		outerRadius = 8;
		float[] beatOuterRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

		beatIndicator = new ShapeDrawable((new RoundRectShape(beatOuterRadii, null, null)));
		beatIndicator.getPaint().setColor(MLIGHTBLUE);

		blankBeatIndicator = new ShapeDrawable((new RoundRectShape(beatOuterRadii, null, null)));
		blankBeatIndicator.getPaint().setColor(MGREY);

		Xmin = 0;
		Xmax = 16;
		Ymin = 0;
		Ymax = 8;

		toggleState = new int[Xmax];

		setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				final int Xpos;
				final int Ypos;
				Xpos = (int) event.getX();
				Ypos = (int) event.getY();
				sendMouseValues(Xpos, Ypos);
				if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
					//toggleBeat(pos);
				}
				return true;
			}

		});	
	}

	public void setCurrentBeat(int beat) {
		currentBeat = beat;
		invalidate();
	}

	public void setToggleState (int[] toggleArray) {

		if (toggleArray.length >= 16) {
			for (int i = 0; i < 16; i++) {
				toggleState[i] = toggleArray[i];
			}
		}
		invalidate();
	}

	private void sendMouseValues(int Xpos, int Ypos){
		// convert to values useful for Pd
		int Xval;
		int Yval;
		//Yval = (int) (()/height) * Ymax);
		float Xproportion;
		float Yproportion;

		Xproportion = (Xpos/(float)width);
		Xval = (int)(Xproportion * Xmax);

		Yproportion = (height - Ypos)/(float)height;		
		Yval = (int)(Yproportion * Ymax);

		if (Xval < 0) Xval = 0;
		if (Xval >= Xmax) Xval = Xmax - 1;
		if (Yval < 0) Yval = 0;
		if (Yval >= Ymax) Yval = Ymax - 1; // index to be sent

		toggleState[Xval] = Yval;

		if (mTouchListener != null) {
			mTouchListener.onPositionChange(this, Xval, Yval);
		}
		invalidate();
	}


	public void setTouchListener(final touchListener listener) {
		mTouchListener = listener;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		height = View.MeasureSpec.getSize(heightMeasureSpec);
		width = View.MeasureSpec.getSize(widthMeasureSpec);
		this.setMeasuredDimension(width, height);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		width = w;
		height = h;

	}

	@Override
	protected void onDraw(final Canvas canvas) {

		int pixelsPerDiv;
		int pixelsPerXDiv;
		int pixelsPerYDiv;
		pixelsPerDiv = height/Ymax;
		pixelsPerYDiv = height/Ymax;


		gridBoundary.setBounds(0, 0, width, height);
		gridBoundary.draw(canvas);

		gridBoundary.setBounds(0, 0, width, height);
		gridBoundary.draw(canvas);

		pixelsPerDiv = (width - PADDING)/Xmax;
		pixelsPerXDiv = (width - PADDING)/Xmax;

		for (int j = 1; j < Ymax; j++) {

			for (int i = 0; i < Xmax; i++) {
				if (i == currentBeat) {
					XDivLine.getPaint().setColor(Color.RED);
				} else {
					XDivLine.getPaint().setColor(Color.WHITE);
				}
				XDivLine.setBounds(i * pixelsPerDiv, 0, (i * pixelsPerDiv) + 4, height);

				blankBeatIndicator.setBounds(i * pixelsPerXDiv + PADDING, (Ymax - (j+1)) * pixelsPerYDiv + PADDING, 
						i * pixelsPerXDiv + pixelsPerXDiv, (Ymax - (j+1)) * pixelsPerYDiv + pixelsPerYDiv);
				blankBeatIndicator.draw(canvas);

				if (toggleState[i] != 0) {
					beatIndicator.setBounds(i * pixelsPerXDiv + PADDING, (Ymax - (toggleState[i]+1)) * pixelsPerYDiv + PADDING, 
							i * pixelsPerXDiv + pixelsPerXDiv, (Ymax - (toggleState[i]+1)) * pixelsPerYDiv + pixelsPerYDiv);

					beatIndicator.draw(canvas);
				}
			}
			int mColor = MTURQ;
			Paint mPaint = new Paint();
			mPaint.setTextSize(48f);
			mPaint.setTextSize(pixelsPerYDiv);
			mPaint.setColor(mColor);
			canvas.drawText("Clear -->", PADDING , Ymax * pixelsPerYDiv, mPaint);
			
		}

	}

}
