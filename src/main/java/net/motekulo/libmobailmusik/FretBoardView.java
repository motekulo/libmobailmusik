package net.motekulo.openguitarchords;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class FretBoardView extends View {

	protected static final String APP_NAME = "OpenGuitarChords";
	private int width;
	private int height;
	private int numberOfStrings;
	private int lowestFret;
	private int highestFret;
	//private int bottomBoundary;
	//private int rightBoundary;
	private int padding;
	private int [][] dotsToDraw; // string, fret pairs of dots to draw

	int stringStart[]; // x position of string

	//GuitarNotePlayer mPlayer;

	ShapeDrawable string; 
	ShapeDrawable fret;
	ShapeDrawable dot;

	public FretBoardView(Context context) {

		super(context);
		initialize(context);


	}

	public FretBoardView(Context context, AttributeSet attrs) {

		super(context, attrs);
		initialize(context);
	}

	public FretBoardView(Context context, AttributeSet attrs, int defStyle) {

		super(context, attrs, defStyle);
		initialize(context);
	}


	public void setNumberOfStrings(int numberOfStrings) {
		this.numberOfStrings = numberOfStrings;
	}

	public void setLowestFret(int lowestFret) {
		this.lowestFret = lowestFret;
	}

	public void setHighestFret(int highestFret) {
		this.highestFret = highestFret;
	}

	public void setDotsToDraw(int[][] dotsToDraw) {
		this.dotsToDraw = dotsToDraw;
		// Get the appropriate samples loaded up in the Guitar note player
		Log.i(APP_NAME, "In setDotsToDraw");
		//mPlayer.loadCurrentVoicingSamples(dotsToDraw);
		invalidate();

	}

	private void initialize(Context context) {
		string = new ShapeDrawable(new RectShape());
		numberOfStrings = 6;
		stringStart = new int[6];
		highestFret = 5;
		lowestFret = 0;
		padding = 36;
		fret = new ShapeDrawable(new RectShape());
		dot = new ShapeDrawable(new OvalShape()); //comment out for debugging and viewing custom class - bloody eclipse!
		//dot = new ShapeDrawable(new RectShape());
		dotsToDraw = new int[6][3];

		//mPlayer = new GuitarNotePlayer(context);


		//testDotDraw();
		setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(final View v, final MotionEvent event) {
				final int pos;
				boolean[] playSwitch;
				playSwitch = new boolean[6];
				pos = (int) (event.getX());
				//Log.i(APP_NAME, "Pos is " + pos);
				int stringGapInPixels = (stringStart[1] - stringStart[0])/4; // the divisor determines the touch size 
				// setPosition(pos);
				
				for (int i=0; i < 6; i++) {
					if (pos < stringStart[i] + stringGapInPixels && pos > stringStart[i] - stringGapInPixels) {
						Log.i(APP_NAME, "String " + i);
						if (dotsToDraw[i][1] >= 0) {
							//mPlayer.selectAndPlayFromNote(i);
						}
					}	
				}
				
				return true;
			}
		});		

	}

	private void testDotDraw() {


		dotsToDraw[0][0] = 6; //string
		dotsToDraw[0][1] = 3; //fret

		dotsToDraw[1][0] = 5; //string
		dotsToDraw[1][1] = 2; //fret

		dotsToDraw[2][0] = 4; //string
		dotsToDraw[2][1] = 0; //fret
		dotsToDraw[3][0] = 3; //string
		dotsToDraw[3][1] = 0; //fret
		dotsToDraw[4][0] = 2; //string
		dotsToDraw[4][1] = 0; //fret
		dotsToDraw[5][0] = 1; //string
		dotsToDraw[5][1] = 3; //fret

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		width = View.MeasureSpec.getSize(widthMeasureSpec);
		height = View.MeasureSpec.getSize(heightMeasureSpec);
		//height = width;
		this.setMeasuredDimension(width, height);
		Log.i(APP_NAME, "Width is " + width);
		Log.i(APP_NAME, "Height is " + height);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		width = w;
		height = h;
		//height = w;

	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawStrings(canvas);
		drawFrets(canvas);
		drawDots(canvas);
		//			string.setBounds();
		//			axesRect.draw(canvas);
	}

	private void drawStrings(Canvas canvas){
		int mcolor = Color.BLACK;


		string.getPaint().setColor(mcolor);

		int mwidth = height/2;
		//	padding = getPadding(mwidth);
		//		int stringEnd;
		int mpadding = (width - mwidth) /2;
		for (int i = 0; i < numberOfStrings; i ++) {
			//stringStart = mpadding + ((mwidth - (padding * 2))/(numberOfStrings-1)) * i;
			stringStart[i] = mpadding + ( mwidth/(numberOfStrings-1)) * i;
			Log.i(APP_NAME, "StringStart " + i + " is " + stringStart[i]);
			string.setBounds(stringStart[i], 0 + padding, stringStart[i] + (2 + Math.abs(5-i)), height);
			string.draw(canvas);
		}
		//canvas.drawTe

	}

	private void drawFrets(Canvas canvas){
		int fretStart;

		int numFrets = highestFret - lowestFret;
		int mwidth = height/2;
		//	padding = getPadding(mwidth);
		int mpadding = (width - mwidth) /2;
		for (int i = 0; i < numFrets; i ++) {
			fretStart = (height - padding)/numFrets * i;
			if (i == 0) {
				fret.setBounds(mpadding, fretStart + padding, mwidth + mpadding, fretStart + padding + 12);
			} else {
				fret.setBounds(mpadding, fretStart + padding, mwidth + mpadding, fretStart + padding + 2);
			}
			fret.draw(canvas);
		}
	}

	private void drawDots(Canvas canvas) {
		int mcolor = Color.BLACK;

		string.getPaint().setColor(mcolor);
		// try drawing something at string 3, fret 3 for starters
		int numFrets = highestFret - lowestFret;
		
		int stringStart;
		int fretStart;
		int mwidth = height/2;
		int mpadding = (width - mwidth) /2;
		
		int fretDistance = (height-padding)/numFrets;
		Log.i(APP_NAME, "fretDistance " + fretDistance);
		int dotWidth = fretDistance/3;
		Log.i(APP_NAME, "dotWidth " + dotWidth);
		
		//	padding = getPadding(mwidth);

		for (int pair[]: dotsToDraw) {
			// [0] is a string value in human terms 
			//int mString = Math.abs(pair[0] - 6);  // convert to the machine view where 0 is string 6 and 5 is string 1
			int mString = pair[0];

			// [1] is the fret to be fretted on that string
			int mFret = pair[1];
			Log.i(APP_NAME, "string is " + mString + " fret is " + mFret);
			stringStart = mpadding + ((mwidth )/(numberOfStrings-1)) * mString;
			fretStart = padding + ((height - padding)/numFrets * mFret);
			Paint imagePaint = new Paint();
			imagePaint.setTextAlign(Align.CENTER);
			if (height > 200) {
				imagePaint.setTextSize(24f);
			} else {
				imagePaint.setTextSize(12f);
				dotWidth = fretDistance/2;
			}
			if (mFret <= 0) {
				// then we should draw an open string indication or X at that point, rather than a black dot

				String indication;
				if (mFret == 0) {
					indication = "0";
				} else {
					indication = "X";
				}
				canvas.drawText(indication, stringStart, 30, imagePaint);
				//canvas.drawText("X", width/4, height/2,imagePaint);
			}
			else {
				dot.setBounds(stringStart - (dotWidth/2), fretStart - dotWidth, stringStart + (dotWidth/2), fretStart);
				dot.draw(canvas);
				int mFingering = pair[2];
				imagePaint.setColor(Color.WHITE);
				canvas.drawText(Integer.toString(mFingering), stringStart, fretStart - (dotWidth/3), imagePaint);
			}

		}

	}

	private int getPadding(int mwidth) {
		padding = (width - mwidth) /2;

		return padding;
	} 
}
