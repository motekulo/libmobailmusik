/*
 * Copyright (C) 2014-2017 Denis Crowdy
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class SoundView extends View {

	private static final String APP_NAME = "twotrack SV";
	private static final int WAVE_STRETCH = 2;
	private static final int SAMPLE_SKIP = 4;
	private static final int NEW_BIN_CALCULATED = 3;
	private static final int DATA_LOADED = 1;

    //	private ShapeDrawable mRect;
	private int viewWidth;
	private int viewHeight;
    //private float binMaxVal;
	//private float binMinVal;
	private short[] minVal;
	private short[] maxVal;
	private short[] minValR;
	private short[] maxValR;
	private ShapeDrawable mLine;
    private String soundLabel;


	private int numChannels;
    private File fileToDraw;
    
    /*
     * A SoundView needs to be able to draw a short wave file over a longer
     * period of time, as there might be another track that is longer, and we
     * need to match that overall project length for the shorter track
     */
	private long recordingLength; // The length over which to draw the wave file (in frames)
    private int nudgeFrames;  // number of frames to nudge mono track view by

	//private boolean needToRefreshData;
	private boolean dataFromFileLoaded = false;
	private boolean dataIsLoading = false;
	private boolean isRecording = false; 

	public SoundView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public SoundView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public SoundView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	Handler handler=new Handler() { 
		int i = 0;
		@Override 
		public void handleMessage(Message msg) { 
			
			
			if (msg.what == NEW_BIN_CALCULATED) {
				i++;
				//Log.i(APP_NAME, "Handling new bin..." + i);
				if (i % 50 == 0) {
					//Log.i(APP_NAME, "multiple of 50 bins done ");
					invalidate();
				}
			}
			
			if (msg.what == DATA_LOADED) {
				//Log.i(APP_NAME, "Handling data loaded...");
			
				//invalidate();
			}
			
		} 
	};

    public void setNudgeFrames(int nudgeFrames) {
        this.nudgeFrames = nudgeFrames;
    }

    public void setFileToDraw(File file, long timeLength){
		fileToDraw = file;
		
		this.recordingLength = timeLength;   // actually number of stereo frames
		//getDataFromFile();
		//if (viewWidth > 0) {
		if (fileToDraw.exists()) {
			new Thread(new Runnable() {
				public void run() {
				    int i = 0;
					while (dataFromFileLoaded == false) {
                        Log.i(APP_NAME, "test1: In thread and the viewWidth is " + viewWidth);
                        getDataFromFile();
						int width = viewWidth;
						if (width > 0) {
						    i++;
						    Log.i(APP_NAME, "test1: getDataFromFile call number " + i);
						    adjustForWidth(width);
							//getDataFromFile(width);
						}

					}
					
				}
			}).start();
		}
		
	}

	private void adjustForWidth(int width) {
        int binSize = 5000/width;
        int binNum = 0;
        short maxSoFar = 0;
        short minSoFar = 0;
        for (int i = 0; i < 5000; i++) {
            if (maxSoFar < maxVal[i]) {
                maxSoFar = maxVal[i];
            }
            if (minSoFar < minVal[i]) {
                minSoFar = minVal[i];
            }
            if (i % binSize == 0) {
                maxVal[binNum] = maxSoFar;
                minVal[binNum] = minSoFar;

                Log.i(APP_NAME, "BinNum is " + binNum);
                Message msg = handler.obtainMessage();
                msg.what = NEW_BIN_CALCULATED;
                handler.sendMessage(msg);
            }
        }
    }

	private void getDataFromFile(){
//		while (width == 0) {
//			Log.i(APP_NAME, "In getDataFromFile and the viewWidth is 0...");
//			try {
//				Thread.sleep(100); //FIXME potential infinite loop so bail after a few seconds
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}
		//Log.i(APP_NAME, "test1: in getDataFromFile and viewWidth is " + viewWidth);

        FileInputStream fileStream;
        try {
			fileStream = new FileInputStream(fileToDraw);
		} catch (FileNotFoundException e) {
			//Log.i(APP_NAME, "Bailing as file not found");
			e.printStackTrace();
			return;
		}
		dataIsLoading = true;
		numChannels = WavUtils.getNumChannels(fileToDraw);
		//Log.i(APP_NAME, "in GetDataFromFile and numCh is " + numChannels);
		if (numChannels == 0) numChannels = 1;  // Horrible kludge and shouldn't happen... but better than /0
		//Log.i(APP_NAME, "in GetDataFromFile and numCh is " + numChannels);
        int bitsPerSample = WavUtils.getBitsPerSample(fileToDraw);
		if (bitsPerSample == 0) bitsPerSample = 16;
		int read = 0;

		/*
		 * So we have recordingLength, which is the total length in milliseconds over
		 * which we want to display the file. The file length in milliseconds might
		 * well be less than the total time over which we want to display it - another
		 * track could be longer in other words.
		 * 
		 * So need to convert recordingLength, which is in milliseconds, to a number of samples
		 * 
		 * x samples in   recordingLength milliseconds
		 * 44100 samples in 1000 milliseconds
		 * 
		 * 
		 */

		//recordingLength
		int bytesPerSample = bitsPerSample /8 * numChannels;
		int numsamples = (int)fileToDraw.length()/bytesPerSample;  // this is frames - change code to reflect this
		//int numsamples = (int)recordingLength/1000 * 44100; // FIXME use getRate from WavUtils for this
//		numsamples = (int)recordingLength;   // HANG ON - recordingLength is in frames (so samples - get consistent here); so this works...
		int binSize = (int) recordingLength / 5000; // Arbitrary value (5000) greater than number of width pixels
		byte[] bin = new byte[binSize * bytesPerSample]; //

        /*<------------- viewWidth in pixels ------------------------------>
         * -------------------------------------------------------------
         * Wav file that is shorter ends here |
         * |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
         * <->
         * binSize, which is overall timelength of longest track
         * (expressed in frames/samples, though) divided
         * by the viewWidth in pixels
         */

		ByteBuffer binByteBuffer;
		ShortBuffer binShortsBuffer;
		//FloatBuffer binFloatsBuffer;
		maxVal = new short[5000];
		minVal = new short[5000];
		maxValR = new short[5000];
		minValR = new short[5000];
		int binNum = 0;
		try {
			fileStream.skip(44);   // Don't want to draw the header
			while ((read = fileStream.read(bin, 0, binSize * bytesPerSample)) != -1) {
				binByteBuffer = ByteBuffer.wrap(bin);
				binShortsBuffer = binByteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

				binShortsBuffer.rewind();
				//int n;
				short sample;
               // BufferUnderflowException testError = new BufferUnderflowException();
               // throw testError;
				for (int i=0, n = binShortsBuffer.remaining(); i < n; i = i + (numChannels * SAMPLE_SKIP)) {
					try {

						sample = binShortsBuffer.get();


					} catch (BufferUnderflowException e) {
						Log.i(APP_NAME, "caught buffer underflow exception ");
						Crashlytics.logException(e);
						break;
					}

					if (binNum >= 5000) break; // FIXME - problem just for the final buffer read?
					if (maxVal[binNum] < sample) {
						maxVal[binNum] = sample;
					}
					if (minVal[binNum] > sample) {
						minVal[binNum] = sample;
					}
					if (numChannels == 2) {
						// So we'll use maxVal[] and minVal[] for the left channel
						try {
							sample = binShortsBuffer.get();
						} catch (BufferUnderflowException e) {
							Log.i(APP_NAME, "caught buffer underflow exception ");
							Crashlytics.logException(e);
							break;
						}
						
						//if (binNum == viewWidth) break; // FIXME - problem just for the final buffer read?
						if (maxValR[binNum] < sample) {
							maxValR[binNum] = sample;
						}
						if (minValR[binNum] > sample) {
							minValR[binNum] = sample;
						}
					}
					//Log.i(APP_NAME, "i is " + i);
				}

				binNum ++;	
				if (binNum >= 5000) break;  // FIXME - added during debugging
				//Log.i(APP_NAME, "BinNum is " + binNum);
				//Message msg = handler.obtainMessage();
				//msg.what = NEW_BIN_CALCULATED;
				//handler.sendMessage(msg);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		dataFromFileLoaded = true;
		dataIsLoading = false;
		Message msg = handler.obtainMessage();
		msg.what = DATA_LOADED;
		handler.sendMessage(msg);
	}

	public void setIsRecording(boolean recordingFlag){
		isRecording = recordingFlag;
		invalidate();
	}

	public void setInvalidate(){
		invalidate();
		viewWidth = this.getMeasuredWidth();
		viewHeight = this.getMeasuredHeight();
	}

	public void setRefreshDataFlag(){
		//needToRefreshData = true;
		dataFromFileLoaded = false;
		invalidate();

	}

	public void clearSoundView(){
		/* resets the dataFromFileLoaded flag so that
		 * a fresh (ie no wave data) view is drawn after
		 * a file is deleted or removed
		 * 
		 */

		// Reset the bin data array, otherwise things will be drawn...
		if (maxVal != null){
			for (int i = 0; i < maxVal.length; i ++){
				maxVal[i] = 0;
				minVal[i] = 0;
				maxValR[i] = 0;
				minValR[i] = 0;
			}
		}
		dataFromFileLoaded = false;
		invalidate();

	}

	public void setLabel(String label) {
		soundLabel = label;
	}
	
	public void drawStereoWaveform(Canvas canvas){
		//int mcolor = 0xff0000ff;
		int lcolor = Color.WHITE;
		
		float binLMax;
		float binLMin;
		float binRMax;
		float binRMin;
		//mRect.draw(canvas);

		//Log.i(APP_NAME, "in drawStereoWaveForm");
		mLine = new ShapeDrawable(new RectShape());
		mLine.getPaint().setColor(lcolor);

        ShapeDrawable mRightLine = new ShapeDrawable(new RectShape());
		mRightLine.getPaint().setColor(lcolor);

		if (maxVal != null) {
			int maxlengthL = 0;
			int minlengthL = 0;
			int maxlengthR = 0;
			int minlengthR = 0;
			int leftBaseline = viewHeight /4;
			int rightBaseline = viewHeight /4 * 3;

			for (int i = 0; i < viewWidth; i = i+2){   // i increment speeds things up
				binLMax = ((float)maxVal[i])/0x8000;
				binLMin = ((float)minVal[i])/0x8000;
				binRMax = ((float)maxValR[i]/0x8000);
				binRMin = ((float)minValR[i]/0x8000);

				maxlengthL = (int)(viewHeight * binLMax)/4;
				minlengthL = (int)(viewHeight * binLMin)/4;
				maxlengthR = (int)(viewHeight * binRMax)/4;
				minlengthR = (int)(viewHeight * binRMin)/4;

				mLine.setBounds(i, leftBaseline - (maxlengthL * WAVE_STRETCH), i + 1, leftBaseline - (minlengthL * WAVE_STRETCH));
				mRightLine.setBounds(i, rightBaseline - (maxlengthR * WAVE_STRETCH), i + 1, rightBaseline - (minlengthR * WAVE_STRETCH));

				mLine.draw(canvas);
				mRightLine.draw(canvas);
			}
		}
	}

	public void drawMonoWaveForm(Canvas canvas){
		int mcolor = 0xff0000ff;
		int lcolor = Color.WHITE;
		int x = 0;
		int y = 0;
        ShapeDrawable mRect = new ShapeDrawable(new RectShape());
		mRect.getPaint().setColor(mcolor);
		mRect.setBounds(x, y, x + viewWidth, y + viewHeight);
		float binMax;
		float binMin;
		//mRect.draw(canvas);

		//Log.i(APP_NAME, "in drawMonoWaveForm");
		mLine = new ShapeDrawable(new RectShape());
		mLine.getPaint().setColor(lcolor);

		if (maxVal != null) {
			int maxlength = 0;
			int minlength = 0;
			int baseline = viewHeight /2;

			int nudgePoint = (int)((float) nudgeFrames/recordingLength * viewWidth);
            //Log.i(APP_NAME, "nudgePoint: " + nudgePoint);

			for (int i = 0; i < viewWidth - Math.abs(nudgePoint); i++){

                if (i < nudgePoint) {
                    binMax = 0;
                    binMin = 0;
                } else {
                    binMax = ((float) maxVal[i - nudgePoint]) / 0x8000;
                    binMin = ((float) minVal[i - nudgePoint]) / 0x8000;

                }
				maxlength = (int)(viewHeight * binMax)/2;
				minlength = (int)(viewHeight * binMin)/2;

				mLine.setBounds(i, baseline - (maxlength * WAVE_STRETCH), i + 1, baseline - (minlength * WAVE_STRETCH));
				mLine.draw(canvas);
			}
		}
	}

	public void drawAxes(Canvas canvas) {
		int mcolor = Color.YELLOW;
		ShapeDrawable axesRect; // Use for mono axis, and L channel if stereo
		ShapeDrawable axesRectR;

		if (numChannels == 1) {
			axesRect = new ShapeDrawable(new RectShape());
			axesRect.getPaint().setColor(mcolor);

			axesRect.setBounds(0, viewHeight /2, viewWidth, viewHeight /2 + 2);
			axesRect.draw(canvas);
		}
		if (numChannels ==2) {
			int leftBaseline = viewHeight /4;
			int rightBaseline = viewHeight /4 * 3;
			axesRect = new ShapeDrawable(new RectShape());
			axesRectR = new ShapeDrawable(new RectShape());
			axesRect.getPaint().setColor(mcolor);
			axesRectR.getPaint().setColor(mcolor);
			axesRect.setBounds(0, leftBaseline, viewWidth, leftBaseline + 2);
			axesRectR.setBounds(0, rightBaseline, viewWidth, rightBaseline + 2);
			axesRect.draw(canvas);
			axesRectR.draw(canvas);
		}

	}

	private void drawLabel(Canvas canvas) {
		int mColor = Color.CYAN;
		Paint mPaint = new Paint();
		mPaint.setTextSize(24f);
		mPaint.setColor(mColor);
		canvas.drawText(soundLabel, 10, 20, mPaint);
		
	}
	
	private void displayRedBackground(Canvas canvas) {

		int mcolor = Color.RED;
		ShapeDrawable wholeRect; // Use for mono axis, and L channel if stereo
		//ShapeDrawable axesRectR;

		wholeRect = new ShapeDrawable(new RectShape());
		wholeRect.getPaint().setColor(mcolor);

		wholeRect.setBounds(0, 0, viewWidth, viewHeight);
		wholeRect.draw(canvas);

	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		viewHeight = MeasureSpec.getSize(heightMeasureSpec);
		viewWidth = MeasureSpec.getSize(widthMeasureSpec);

		this.setMeasuredDimension(viewWidth, viewHeight);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		viewWidth = w;
		viewHeight = h;
		//Log.i(APP_NAME, "onSizeChanged called; new viewWidth " + viewWidth);

	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawAxes(canvas);
		
		if (isRecording == true) {
			displayRedBackground(canvas);
		}
		
		if (dataIsLoading == true || dataFromFileLoaded == true) {		
			if (numChannels == 1) drawMonoWaveForm(canvas);
			if (numChannels == 2) drawStereoWaveform(canvas);
			//Log.i(APP_NAME, "numChannels from onDraw is " + numChannels);
		}
		
		if (soundLabel != null){
			drawLabel(canvas);
		}
	}


}
