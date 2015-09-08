/*
 * Copyright (C) 2014 Denis Crowdy
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

import android.os.Handler;
import android.os.Message;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


public class WavMixer extends Thread{
	
	private static final int BUFFER_SIZE = 1024 * 8;

	Handler mHandler;
	File file1;
	File file2;
	File rawoutfile;
	float volL;
	float volR;
	boolean file1longer = false;
	int progress = 0;

	private long latencyAdjust;

	private boolean userHasCancelled;

	WavMixer(Handler h) {
		mHandler = h;
		latencyAdjust = 0;
	}

	public void setFilesToMix(File fileA, File fileB) {
		/* Class assumes (so bad...) that file1 is stereo (track1 for twotrack activity
	     * and file2 is mono (track 2 for twotrack activity)
	     */	
		file1 = fileA;
		file2 = fileB;

		String bkpFile1Name = file1.getAbsolutePath() + ".bkp";		
		File bkpFile1 = new File(bkpFile1Name);
		
		String bkpFile2Name = file2.getAbsolutePath() + ".bkp";		
		File bkpFile2 = new File(bkpFile2Name);
		
		//Log.i("WavMixer", "bkpFile1Name is " + bkpFile1Name);
		//Log.i("WavMixer", "bkpFile2Name is " + bkpFile2Name);
		
		File recordedFile1 = new File(file1.getAbsolutePath());
		File recordedFile2 = new File(file2.getAbsolutePath());
		try {

			FileUtils.copyFile(recordedFile1, bkpFile1);
			FileUtils.copyFile(recordedFile2, bkpFile2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
	}

	public void setRawOutputFile(File outputfile){
		rawoutfile = outputfile;
	}

	public void setPanPos(float volLeft, float volRight){
		volL = volLeft;
		volR = volRight;

	}

	public void run(){

		FileInputStream File1Stream = null;
		try {
			File1Stream = new FileInputStream(file1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileInputStream File2Stream = null;
		try {
			File2Stream = new FileInputStream(file2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//int bounceFileLength = (int) Math.max(file1.length(), file2.length());
//		int bounceFileLength = (int) (Math.max(file1.length(), file2.length() * 2));
		if (file1.length() > file2.length()) {
			file1longer = true;
			}
		else {
			file1longer = false;
			}

		//////////// Now get a BufferedInput and Output stream /////////////
		
		BufferedInputStream in1 = new BufferedInputStream(File1Stream, BUFFER_SIZE);
		BufferedInputStream in2 = new BufferedInputStream(File2Stream, BUFFER_SIZE);
		FileOutputStream outstream = null;
		try {
			outstream = new FileOutputStream(rawoutfile);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        BufferedOutputStream out = new BufferedOutputStream(outstream, BUFFER_SIZE);
        
        byte[] file2buffer = new byte[BUFFER_SIZE];
        byte[] file1buffer = new byte[BUFFER_SIZE * 2];
       
        float chL;
		float chR;
		float mixedL;
		float mixedR;
		float file2sampletomix;
		float file1sampleL;
		float file1sampleR;
        
        int count = 0, n = 0;
        int o = 0;
        try {
        	in2.skip(44);   // Move past the header
        	in1.skip(44);   
			while ((n = in2.read(file2buffer, 0, BUFFER_SIZE)) != -1) {
				
				// Read in a file1buffer as well
				//Log.i("Mixer", "Buffer size, n is " + BUFFER_SIZE + ", " + n);
				o = in1.read(file1buffer, 0, BUFFER_SIZE * 2);			
				//Log.i("WavMixer", "first o is " + o);
				// Convert it to a shortbuffer
				ShortBuffer shortbuffFile1 = ByteBuffer.wrap(file1buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				ShortBuffer shortbuffFile2 = ByteBuffer.wrap(file2buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
				short[] file1Shorts = new short[shortbuffFile1.capacity()];
				short[] file2Shorts = new short[shortbuffFile2.capacity()];
				shortbuffFile1.get(file1Shorts);
				shortbuffFile2.get(file2Shorts);
					
				for (int i=0; i < file2Shorts.length; i ++) {   // we only need to change values overlapped by file2 (the overdub)
				
					
				 /* If n/2 (because n is bytes and we are working in shorts) is less than Buffer, then write rest of buffer with 0s.
				  * So if we are at the end of the file, and the bytes read is less than the buffer size, then pad the rest with 0s
				  * Or not... just leave the file1 values as they were, as they aren't overlapped by file2 for mixing anyway? THey have
				  * content in from the previous file 1, so why overwrite that??
				  */
									
					file1sampleL = ((float)file1Shorts[i*2])/0x8000;
					file1sampleR = ((float)file1Shorts[i*2+1])/0x8000;
					if (i < n/2) {
						//Processing as floats so convert first:
						file2sampletomix = ((float)file2Shorts[i])/0x8000;
						

						// Get the panning for the mono file sorted
						chL = file2sampletomix * volL;				
						chR = file2sampletomix * volR;

						// Add to the existing stereo samples
						if (o != -1) {

							mixedL = file1sampleL + chL;
							mixedR = file1sampleR + chR;
						} else {
							mixedL = chL;
							mixedR = chR;
						}
						// Scale back up and convert to shorts - should probably dither here?
						
					}
					else {
						mixedL = file1sampleL;
						mixedR = file1sampleR;
					}
					
						// Doubtful about this... leave alone I suspect
						//file1Shorts[i*2] = (short) file1sampleL;
						//file1Shorts[i*2+1] = (short) file1sampleR;
					file1Shorts[i*2] = (short) (mixedL * 32767);
					file1Shorts[i*2+1] = (short) (mixedR * 32767);	 
				}
				
				// Update progress bar - maybe should be at end of loop? FIXME
				progress = (int)(count/(double)file2.length() * 100) -1 ; // this won't work if file1 is longer, of course FIXME
				Message msg = mHandler.obtainMessage();
				msg.arg1 = progress;
				mHandler.sendMessage(msg);
				
				// convert back to byte array 
				byte[] filearray1a = new byte[file1buffer.length];
				//ByteBuffer outBuffer;
				ByteBuffer.wrap(filearray1a).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(file1Shorts);

				//byte[] filearray2a = new byte[file2buffer.length];
				//ByteBuffer.wrap(filearray2a).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(file2Shorts);
				
				// Write it out 	
				
				try {
					out.write(filearray1a, 0, BUFFER_SIZE * 2);
					//out.write(filearray1a, 0, n * 2);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				count += n;
				if (userHasCancelled == true) {
					return;
				}
			}
			
			// Here we can deal with the situation where file1 is longer so we still have data to write
			
			
			
			if (file1longer == true) {
				
				while ((o = in1.read(file1buffer, 0, BUFFER_SIZE *2)) != -1) {
					//Log.i("WavMixer", "o is " + o);
					out.write(file1buffer, 0, o);				
				
				}
			}
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
      /*  try {
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} */
        
		//int finalcount = count;
	//	finalcount += 1;
        
		copyWaveFile(rawoutfile,file1);
		//if we get here, then we should make sure the progress dialog quits... ROUGH FIXME
		Message msg = mHandler.obtainMessage();
		msg.arg1 = 100;
		mHandler.sendMessage(msg);

	}


	public void setUserHasCancelled(boolean userHasCancelled) {
		this.userHasCancelled = userHasCancelled;
	}

	private void WriteWaveFileHeader(
			FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels,
			long byteRate) throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R';  // RIFF/WAVE header
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
		header[12] = 'f';  // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1;  // format = 1
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
		header[32] = (byte) (2 * 16 / 8);  // block align
		header[33] = 0;
		header[34] = 16;  // bits per sample
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

	private void copyWaveFile(File inFile,File outFile){
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = 44100;
		int channels = 2;
		long byteRate = 16 * 44100 * channels/8;
	//	int bufferSize = 1024 * 512;
		int bufferSize = 1024 * 16;

		byte[] data = new byte[bufferSize];
		//ByteBuffer dataBuffer = ByteBuffer.wrap(array);

		try {
			in = new FileInputStream(inFile);
			out = new FileOutputStream(outFile);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			in.skip(latencyAdjust);
			while(in.read(data) != -1){
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
