package net.motekulo.phonstrument;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class XYControllerBeatView extends View {
	public interface touchListener {
		void onPositionChange(View view, int mouseX, int mouseY, int value);
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

	private XYControllerBeatView.touchListener mTouchListener;
	private int height;
	private int width;
	private int mcolor = Color.BLACK;
	private ShapeDrawable gridBoundary;
	private ShapeDrawable YDivLine;
	private ShapeDrawable XDivLine;
	private ShapeDrawable beatIndicator;

	private int Xmin;
	private int Xmax;
	private int Ymin;
	private int Ymax;

	private int[][] toggleState;

	private int currentBeat;

	public XYControllerBeatView(Context context) {
		super(context);
		initView();
	}

	public XYControllerBeatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public XYControllerBeatView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	private void initView() {

		float outerRadius = 16;
		float[] outerRadii = new float[] {outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, 
				outerRadius, outerRadius};

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

		Xmin = 0;
		Xmax = 16;
		Ymin = 0;
		Ymax = 4;

		toggleState = new int[Ymax][Xmax]; // rows and columns

		setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				final int Xpos;
				final int Ypos;
				Xpos = (int) event.getX();
				Ypos = (int) event.getY();
				//Log.i(APP_NAME, "Xpos, Ypos " + Xpos + ", " + Ypos);

				if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
					sendMouseValues(Xpos, Ypos);
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

	public void setToggleState (int[][] toggleArray) {

		for (int i = 0; i < Ymax; i++) {

			for (int j = 0; j < Xmax; j++) {
				toggleState[i][j] = toggleArray[i][j];  // FIXME just copy the array over...
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

		if (toggleState[Yval][Xval] == 0) {
			toggleState[Yval][Xval] = 1;
		} else {
			toggleState[Yval][Xval] = 0;
		}

		if (mTouchListener != null) {
			mTouchListener.onPositionChange(this, Yval, Xval, toggleState[Yval][Xval]);
		}
		invalidate();
	}




	public void setTouchListener(final touchListener listener) {
		mTouchListener = listener;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		width = View.MeasureSpec.getSize(widthMeasureSpec);
		height = (width/Xmax) * Ymax + 16;
		this.setMeasuredDimension(width, height);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		width = w;
		height = h;

	}

	@Override
	protected void onDraw(final Canvas canvas) {

		gridBoundary.setBounds(0, 0, width, height);
		gridBoundary.draw(canvas);

		int pixelsPerXDiv;
		int pixelsPerYDiv;

		//pixelsPerYDiv = height/Ymax;
		pixelsPerXDiv = (width - PADDING)/Xmax;
		pixelsPerYDiv = pixelsPerXDiv;

		for (int i = 0; i < Ymax; i++) {
			for (int j = 0; j < Xmax; j++) {

				if (toggleState[i][j] == 0) {
					//if (i % 2 == 0) {
					beatIndicator.getPaint().setColor(MGREY);
					// Draw a coloured rect
				} else {
					beatIndicator.getPaint().setColor(MLIGHTBLUE);
					// Draw another coloured rect (black?)					
				}
				beatIndicator.setBounds(j * pixelsPerXDiv + PADDING, ((Ymax - (i+1)) * pixelsPerYDiv) + PADDING, 
						j * pixelsPerXDiv + pixelsPerXDiv, (Ymax - (i+1)) * pixelsPerYDiv + pixelsPerYDiv);
				beatIndicator.draw(canvas);

			}
		}

		for (int i = 0; i < Ymax; i++) {

			YDivLine.setBounds(0, i * pixelsPerYDiv, width, (i * pixelsPerYDiv) + 4);
			YDivLine.getPaint().setColor(Color.WHITE);
		}

		for (int i = 0; i < Xmax; i++) {
			if (i == currentBeat) {
				XDivLine.getPaint().setColor(Color.RED);
			} else {
				XDivLine.getPaint().setColor(Color.WHITE);
			}
			XDivLine.setBounds(i * pixelsPerXDiv, 0, (i * pixelsPerXDiv) + 4, height);
			
		}




	}

}