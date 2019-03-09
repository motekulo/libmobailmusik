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

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WavRecordService extends Service implements Runnable{

	private static final String APP_NAME = "WavRecordService";
	//private static final int TWOTRACKREC_ID = 1;
	private static final int NUM_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;	
	private static final int SAMPLERATE = 44100;
	private static final String AUDIO_FOLDER = "twotrack";
	private static final String TEMP_FILENAME = "record_temp.raw";
	private static final int BPP = 16;
	//private static final int TIMER_INTERVAL = 120;
	//private static final int COPY_CHUNKSIZE = 8192;
	private static final int COPY_CHUNKSIZE = 4096;
	
	private Handler recServiceHandler;
	
	private File filetorecord;
	private int bufferSize = 0;
	private AudioRecord recorder = null;
	private byte[] data;
	//private short[] shortdata;
	private FileOutputStream os;
	private Thread recordingThread;
	private int bufferMultiplier = 1;

    private int framePeriod; // number of frames written to file each read
	//private final MicLevelInput mListener;
	private boolean isRecording = false;

	private long recStartTime;
	Bundle bundle = new Bundle();
	
	// Binder given to clients
	private final IBinder mBinder = new RecLocalBinder();

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class RecLocalBinder extends Binder {	
		
		public WavRecordService getService() {
			// Return this instance of LocalService so clients can call public methods
			return WavRecordService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onDestroy() {
		
		//Debug.stopMethodTracing();
	}
	
	public void setHandler (Handler h) {
		recServiceHandler = h;   
		
	}
	
	private int initialize() {

        int sampleRate = SAMPLERATE;
		bufferSize = AudioRecord.getMinBufferSize(sampleRate, NUM_CHANNELS, AUDIO_ENCODING);
		
		if (bufferSize < 0) {
			// Error so will need to bail
			Log.i(APP_NAME, "Bailing, as buffer size of wavrecord is " + bufferSize);
			return -1;
		}
		
		bufferSize = bufferSize * bufferMultiplier;
		
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
				SAMPLERATE, NUM_CHANNELS,AUDIO_ENCODING, bufferSize);
		Log.i(APP_NAME, "Buffer size of wavrecord is " + bufferSize);
		
		if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {

			Log.i(APP_NAME, "AudioRecord initialization failed");
			//throw new Exception("AudioRecord initialization failed");
			return -1;

		}

		data = new byte[bufferSize/2];
	//	shortdata = new short[bufferSize/2];
		//buffer = new byte[framePeriod*bSamples/8*nChannels];
		//data = new byte[framePeriod*BPP/8];

		String filename = getTempFilename();
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
            return -1;
		}
		recordingThread = new Thread(this);
		//Log.i(APP_NAME, "Rec priority is " + recordingThread.getPriority());
		
		return 0;
	}

	private String getTempFilename(){
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath,AUDIO_FOLDER);

		if(!file.exists()){
			file.mkdirs();
		}

		File tempFile = new File(filepath,TEMP_FILENAME);

		if(tempFile.exists())
			tempFile.delete();

		return (file.getAbsolutePath() + "/" + TEMP_FILENAME);
	}



	public int setFileToRecord(File file) {
		//Log.i(APP_NAME, "Setting name of file to record: " + file.getAbsolutePath());
		
		int rec_result;
		filetorecord = file;

		rec_result = initialize();   // should return some sort of value - if an error occurs, then we can pass that back
        if (filetorecord == null) {
            rec_result = -1;
        }
		return rec_result;
		
	}



	@Override
	public void run() {
		//recStartTime = java.lang.System.currentTimeMillis();
		//Log.i(APP_NAME, "Sys time is " + recStartTime);


		if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
			recorder.startRecording();
			isRecording = true;
			//Log.i(APP_NAME, "Running");
			//recStartTime = java.lang.System.currentTimeMillis();
			//Log.i(APP_NAME, "Sys time is " + recStartTime);
			
			writeAudioDataToFile();
		}
	}


	/**
	 * The actual recording part of the class
	 * All of the preparation has been moved out of 
	 * this method to try and deal with latency (or more
	 * specifically unpredictable latency) issues
	 */	
	private void writeAudioDataToFile(){			
		//ByteBuffer calcBuffer;
		//recorder.setRecordPositionUpdateListener(updateListener);
		recorder.setPositionNotificationPeriod(framePeriod);

		int read = 0;

		if(null != os){
			recStartTime = System.currentTimeMillis();
			//Log.i(APP_NAME, "Sys time is " + recStartTime);
			while(isRecording){
				recorder.read(data, 0, data.length);

                try {
                    //Log.i(APP_NAME, "Writing...");

                    os.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //calcBuffer = ByteBuffer.wrap(data);
                //ShortBuffer shortbuff = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                //calcLevel(calcBuffer);
            }

			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void calcLevel(ByteBuffer buffer) {
		//recorder.read(shortdata, 0, data.length);
		//if (shortdata != null) {
			double rms = 0;
			short sample = 0;
			int i = 0;

//			for (i=0; i < shortdata.length; i++) {
//				rms += shortdata[i] * shortdata[i];
//			}
			while (buffer.remaining() > 0) {
				sample = buffer.getShort();
				rms += sample * sample;
				i++;
			}
			rms = Math.sqrt(rms/i);
			// mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
			Message msg = recServiceHandler.obtainMessage();
			msg.what = 	5;
			//	msg.setData(rms);
			//				msg.arg1 = rms;
			bundle.putDouble("test", rms);
			msg.setData(bundle);
			recServiceHandler.sendMessage(msg);
		//}
	}

	private AudioRecord.OnRecordPositionUpdateListener updateListener =new AudioRecord.OnRecordPositionUpdateListener() {

		@Override
		public void onPeriodicNotification(AudioRecord recorder) {

			//recorder.read(data, 0, data.length); // Fill buffer
			int read = 0;
			//Log.i(APP_NAME, "reading new buffer");
			if(null != os){

				//while(isRecording){
					read = recorder.read(data, 0, data.length);

					if(AudioRecord.ERROR_INVALID_OPERATION != read){
//						try {
//							os.write(data);
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
					//writeData(data);
					}
			//	}
			}
		}

		@Override
		public void onMarkerReached(AudioRecord recorder) {


		}

	};

	private String getFilename(){
		//Log.i(APP_NAME, filetorecord.getAbsolutePath());

        return (filetorecord.getAbsolutePath());

	}

	public int checkFileExists(){
		if (filetorecord.exists()) {
			return 0;
		} else {
			return -1;
		}
	}

	public void setBufferMultiplier(int mbufferMultiplier) {
		bufferMultiplier = mbufferMultiplier;
	}

	public void stopRecording(Handler handler, long latencyAdjustment) {
		Log.i(APP_NAME, "Stopping recording");
		if (filetorecord == null) {
			/* It's possible to get here and there is no file recorded yet; bug

			 */
			Log.i(APP_NAME, "filetorecord is null so bailing");
			Message msg = recServiceHandler.obtainMessage();
			msg.what = 	2;

			recServiceHandler.sendMessage(msg);

		} else {

			String recordedFileName = getFilename();
			if (recorder != null && recorder.getState() != AudioRecord.STATE_UNINITIALIZED) {

				isRecording = false;
				//Log.i(APP_NAME, "Set isRecording to false");

				recorder.stop();
				recorder.release();
				//Log.i(APP_NAME, "recorder stopped and released");

			}

			//First make a backup of the previously recorded take:
			String bkpFileName = recordedFileName + ".bkp";
			File bkpFile = new File(bkpFileName);

			File recordedFile = new File(recordedFileName);
			if (recordedFile.exists()) {    // might be stopping after a bounce operation, so no 2.wav yet
				try {

					FileUtils.copyFile(recordedFile, bkpFile);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}

			}
			copyWaveFile(handler, getTempFilename(), recordedFileName, latencyAdjustment);
			deleteTempFile();
		}
		//handler.sendMessage(handler.obtainMessage());


		//Debug.stopMethodTracing();

	}

	public void recordAudio() {
		//Log.i(APP_NAME, "Recording now...");
		recordingThread.start();

//		// Post a notification in case the user moves to another app
//		String ns = Context.NOTIFICATION_SERVICE;
//		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
//		int icon = R.drawable.record;
//		CharSequence tickerText = "Recording";
//		CharSequence contentTitle = "Twotrack";
//		CharSequence contentText = "Recording in progress...";
//
//		Intent notificationIntent = new Intent(this, TwotrackActivity.class);
//		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//		Notification notification = new Notification(icon, tickerText, System.currentTimeMillis()); //FIXME use Notification.Builder
//		notification.setLatestEventInfo(this, contentTitle, contentText, contentIntent);
//
//		mNotificationManager.notify(TWOTRACKREC_ID, notification);

	}

	/* Copies the pcm data currently in the temp file (.raw) to
	 * a wav file
	 * 
	 * @param inFilename
	 * @param outFilename
	 */	
	private void copyWaveFile(Handler handler, String inFilename, String outFilename, long latencyAdjustmentTime){
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = SAMPLERATE;
		int channels = 1;
		long byteRate = BPP * SAMPLERATE * channels/8;
		byte[] latencyAdjustmentSize = null;

		//byte[] data = new byte[bufferSize];
		if (bufferSize > 0) {
			byte[] data = new byte[bufferSize];
		} else {
			byte[] data = new byte[COPY_CHUNKSIZE];
		}
		long latencyAdjustmentFrames = (long)(latencyAdjustmentTime * 44.1);
		Log.i(APP_NAME, "latencyAdjustmentFrames is " + latencyAdjustmentFrames);

		// latencyAdjustment is in milliseconds, so need to convert to frames, then bytes
		// Frames are really number of samples here
		// Latency adjustment time might be negative:
		int arraySize = (int)Math.abs(latencyAdjustmentFrames) * 2;
		
		if (arraySize > 0) {
			latencyAdjustmentSize = new byte[arraySize];
		} else {
			latencyAdjustmentSize = new byte[2];  // FIXME kludge kludge kludge
		}
		if (latencyAdjustmentTime > 0) {
			int i;
			for (i = 0; i < latencyAdjustmentSize.length; i++) {
				
				latencyAdjustmentSize[i] = 0;
				
			}
		}

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			
			if (latencyAdjustmentTime > 0){
				out.write(latencyAdjustmentSize); // output a play buffer length to make up for latency
			}

			else {
				in.skip(latencyAdjustmentSize.length);
			}

			int read = 0;
			while (read != -1) {
				read = in.read(data);
				if (read < data.length && read > 0) {
					// Then fill the rest of the byte array with zeroes
					for (int i = read; i < data.length; i ++){
						data[i] = 0;
					}
				}
				out.write(data);
			}
//			while(in.read(data) != -1){
//				out.write(data);
//			}

			in.close();
			out.close();
			handler.sendMessage(handler.obtainMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(
			FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels,
			long byteRate) throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R';  
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';  
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;  
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; 
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8);  
		header[33] = 0;
		header[34] = BPP;  
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}

	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}

	public long getRecStartTime() {
		return recStartTime;
	}

}