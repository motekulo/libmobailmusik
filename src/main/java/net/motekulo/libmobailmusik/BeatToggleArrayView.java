package net.motekulo.beatsandloops;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BeatToggleArrayView extends View {
    public static final int LOAD_BUTTON_ID = 1;
    public static final int REVERSE_BUTTON_ID = 2;
    private static final String APP_NAME = "BeatsAndLoops";
    private BeatToggleArrayView.touchListener mTouchListener;
    private int height;
    private int width;
    private int mcolor = Color.BLACK;
    private int beatColor = Color.DKGRAY;
    private int currentBeatColor = Color.RED;
    private int numPulses;
    private int numBeats = 4;
    private int beatSubdiv = 4;
    private int[] beatToggleState;
    private int beatWidth;
    private int beatYValue;
    private int beatPadding = 4;
    private int loadButtonWidth;
    private int reverseButtonWidth;
    private boolean reverseButtonState = false;
    private ShapeDrawable gridBoundary;
    private ShapeDrawable beatIndicator;
    private ShapeDrawable currentBeatIndicator;
    private ShapeDrawable reverseButton;
    private ShapeDrawable beatOutline;
    private ShapeDrawable subDivOutline;
    private int currentBeat;
    private Paint mPaint; // for the text of the Load button/area
    private Paint mPaintRev; // for the text of the Rev/Fwd button/area

    public BeatToggleArrayView(Context context) {
        super(context);
        initView();

    }

    public BeatToggleArrayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public BeatToggleArrayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        gridBoundary = new ShapeDrawable(new RectShape());
        gridBoundary.getPaint().setColor(mcolor);
        beatIndicator = new ShapeDrawable((new RectShape()));
        beatIndicator.getPaint().setColor(beatColor);

        reverseButton = new ShapeDrawable((new RectShape()));
        reverseButton.getPaint().setColor(Color.GRAY);

        mPaint = new Paint();
        //mPaint.setTextSize(24f);
        mPaint.setTextSize(loadButtonWidth);
        mPaint.setColor(Color.WHITE);

        mPaintRev = new Paint();
        mPaintRev.setTextSize(32f);
        mPaintRev.setColor(Color.WHITE);

        //subDivOutline = new ShapeDrawable(new RectShape());
        //subDivOutline.getPaint().setColor(Color.LTGRAY);
        float outerRadius = 4;
        float[] outerRadii = new float[]{outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius,
                outerRadius, outerRadius};

        RectF inset = new RectF(2, 2, 2, 2);

        loadButtonWidth = 48;
        reverseButtonWidth = 24;

        float[] innerRadii = new float[]{outerRadius, outerRadius, outerRadius, outerRadius, outerRadius, outerRadius,
                outerRadius, outerRadius};

        beatOutline = new ShapeDrawable((new RoundRectShape(outerRadii, null, innerRadii)));
        beatOutline.getPaint().setColor(Color.WHITE);

        subDivOutline = new ShapeDrawable(new RoundRectShape(outerRadii, null, innerRadii));
        subDivOutline.getPaint().setColor(Color.BLUE);

        currentBeatIndicator = new ShapeDrawable((new RectShape()));
        currentBeatIndicator.getPaint().setColor(currentBeatColor);
        currentBeat = -1; // so it doesn't get displayed at first
        numPulses = 16;
        // initialise beat state array
        beatToggleState = new int[16];
        for (int i = 0; i < beatToggleState.length; i++) {
            // for testing
            beatToggleState[i] = 0;

        }

        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(final View v, final MotionEvent event) {
                final int pos;
                pos = (int) event.getX();
                //Log.i(APP_NAME, "pos " + pos);

                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    toggleBeat(pos);
                }
                return true;
            }

        });
    }

    public void setBeatToggleArray(int[] beatArray) {

        if (beatArray == null) {
            for (int i = 0; i < 16; i++) {
                beatToggleState[i] = 0;
            }
            invalidate();
            return;
        }

        if (beatArray.length == 16) {
            beatToggleState = beatArray; // CHECK
            invalidate();
            return;
        }

        // if the incoming array is less than 16 elements, pad accordingly
        int l = beatArray.length;

        for (int i = 0; i < 16; i++) {
            if (i < l) {
                beatToggleState[i] = beatArray[i];
            } else {
                beatToggleState[i] = 0;
            }

        }
        invalidate();
    }

    private void toggleBeat(int pos) {
        // TODO Auto-generated method stub

        if (pos > loadButtonWidth && pos < (width - reverseButtonWidth)) {
            int beat = 0;

            beat = (int)Math.abs(((pos - loadButtonWidth - beatPadding) / (float) (width - (loadButtonWidth + beatPadding) - reverseButtonWidth) * numPulses));

            if (beat < 0) {
                beat = 0;
            }
            if (beat > 15) {
                beat = 15;
            }
            if (beatToggleState[beat] == 0) {
                beatToggleState[beat] = 1;
            } else {
                beatToggleState[beat] = 0;
            }

            //Log.i(APP_NAME, "Beat is " + beat);
            if (mTouchListener != null) {
                mTouchListener.onPositionChange(this, beat, beatToggleState[beat]);
            }
        }

        if (pos < loadButtonWidth) {
            //Log.i(APP_NAME, "Load button pressed");
            if (mTouchListener != null) {
                mTouchListener.onButtonPressed(this, LOAD_BUTTON_ID);
            }
        }
        if (pos > (width - reverseButtonWidth)) {
            //Log.i(APP_NAME, "Reverse button pressed");
            reverseButtonState = !reverseButtonState; // toggle the button state
            if (mTouchListener != null) {
                mTouchListener.onButtonPressed(this, REVERSE_BUTTON_ID);
            }
            invalidate();
        }
    }

    public void setTouchListener(final touchListener listener) {
        mTouchListener = listener;
    }

    public void setBeatSubdiv(int beatSubdiv) {
        this.beatSubdiv = beatSubdiv;
        invalidate();
    }

    public void setNumBeats(int numBeats) {
        this.numBeats = numBeats;
        invalidate();
    }

    public void setCurrentBeat(int beat) {
        currentBeat = beat;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        height = View.MeasureSpec.getSize(heightMeasureSpec);
        width = View.MeasureSpec.getSize(widthMeasureSpec);
        //height = width/6;
        this.setMeasuredDimension(width, height);
        loadButtonWidth = width / 8;
        reverseButtonWidth = width / 8;
        //	Log.i(APP_NAME, "height is " + height);
        //	Log.i(APP_NAME, "width is " + width);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        loadButtonWidth = width / 8;
        reverseButtonWidth = width / 8;

    }

    @Override
    protected void onDraw(final Canvas canvas) {
        numPulses = numBeats * beatSubdiv;
        //		beatWidth = width / numPulses;
//		beatWidth = (width - loadButtonWidth - reverseButtonWidth) / numPulses;
        beatWidth = (width - loadButtonWidth - reverseButtonWidth - (beatPadding * numPulses)) / numPulses;
        //beatYValue = height/2  - (beatWidth/2);
        beatYValue = height / 8;
        //		int rem = 0;
        gridBoundary.setBounds(0, 0, width, height);
        gridBoundary.draw(canvas);
        for (int i = 0; i < numPulses; i++) {
            //rem = i % beatSubdiv;
            //Log.i(APP_NAME, "Remainder is " + rem);
            if ((i % beatSubdiv) == 0) {
                subDivOutline.setBounds(loadButtonWidth + (i * (beatWidth + beatPadding)), beatYValue, loadButtonWidth + (i * (beatWidth + beatPadding)) + beatWidth, (height - beatYValue));
                subDivOutline.draw(canvas);
            } else {
                beatOutline.setBounds(loadButtonWidth + (i * (beatWidth + beatPadding)), beatYValue, loadButtonWidth + (i * (beatWidth + beatPadding)) + beatWidth, height - beatYValue);
                beatOutline.draw(canvas);
            }

            if (beatToggleState[i] == 1) {

                beatIndicator.setBounds(loadButtonWidth + i * (beatWidth + beatPadding), beatYValue, loadButtonWidth + (i * (beatWidth + beatPadding) + beatWidth), height - beatYValue);
                beatIndicator.draw(canvas);
            }
            if (i == currentBeat) {
                currentBeatIndicator.setBounds(loadButtonWidth + (i * (beatWidth + beatPadding)), beatYValue, loadButtonWidth + (i * (beatWidth + beatPadding) + beatWidth), height - beatYValue);
                currentBeatIndicator.draw(canvas);
            }

        }
        //int mColor = MTURQ;
        mPaint.setTextSize(loadButtonWidth / 4);
        canvas.drawText("Load", 4, height - beatYValue, mPaint);

        // colour the reverse button if necessary
        if (reverseButtonState) {
            reverseButton.setBounds(width - reverseButtonWidth, 0, width, height);
            reverseButton.draw(canvas);
            mPaintRev.setColor(Color.BLACK);
            canvas.drawText("Fwd", width - reverseButtonWidth, height - beatYValue, mPaint);

        } else {
            mPaintRev.setColor(Color.WHITE);
            canvas.drawText("Rev", width - reverseButtonWidth, height - beatYValue, mPaint);
        }

    }

    public boolean isReverseButtonState() {
        return reverseButtonState;
    }

    public interface touchListener {
        void onPositionChange(View view, int beatToToggle, int value);

        void onButtonPressed(View view, int buttonId);
    }

}
