package net.motekulo.openguitarchords;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.util.Log;

public class GuitarNotePlayer {
	private static final String APP_NAME = "OpenGuitarChords";
	private SoundPool soundPool;
	private int[] soundID;
	boolean loaded = false;
	Context mcontext;

	GuitarNotePlayer(Context context){
		mcontext = context;
		// this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// Load the sound
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				loaded = true;
				Log.i(APP_NAME, "sampleId " + sampleId + " status: " + status);
			}
		});
		//soundID = new int[6];
		//  soundID = soundPool.load(this, R.raw.g6, 1);
		//Context mContext = getBaseContext();
//		soundID[0] = soundPool.load(context, R.raw.g6, 1);
//		soundID[1] = soundPool.load(context, R.raw.b5, 1);
//		soundID[2] = soundPool.load(context, R.raw.d4, 1);
//		soundID[3] = soundPool.load(context, R.raw.g3, 1);
//		soundID[4] = soundPool.load(context, R.raw.b2, 1);
//		soundID[5] = soundPool.load(context, R.raw.e1, 1);
	}

	
	public void loadCurrentVoicingSamples(int[][] notesToPlay) {
//		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
//		soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
//			@Override
//			public void onLoadComplete(SoundPool soundPool, int sampleId,
//					int status) {
//				loaded = true;
//				Log.i(APP_NAME, "sampleId " + sampleId + " status: " + status);
//			}
//		});
		soundID = new int[notesToPlay.length];
		String fileName;
		String stringNumber;
		String fretNumber;
		for (int i = 0; i < notesToPlay.length; i ++) {
			if (notesToPlay[i][1] != -1) {
				stringNumber = Integer.toString(notesToPlay[i][0]);
				// With the current system, data frim FretBoardView has pairs with string 0 = string 6, through to string 5 as 1
				// My files are organised as expected however (s60 is string 6 fret 0)
				// so convert:
				stringNumber = Integer.toString(Math.abs(notesToPlay[i][0] - 6));
				
				fretNumber = Integer.toString(notesToPlay[i][1]);
				fileName = "s" + stringNumber + fretNumber;
				Log.i(APP_NAME, "We will play file " + fileName);
				int mResId = mcontext.getResources().getIdentifier(fileName, "raw", "net.motekulo.openguitarchords");
				soundID[i] = soundPool.load(mcontext, mResId, 1);
				//Log.i(APP_NAME, "We will play file " + fileName);
			}
			
			//soundID[i] = soundPool.load(mcontext, fileName, 1);
			//soundPool.loa
		}
		
		//Context mContext = getBaseContext();
		
		
	}
	
	public void playTestSound(){
		AudioManager audioManager = (AudioManager) mcontext.getSystemService(OpenGuitarChords.AUDIO_SERVICE);
		float actualVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		if (loaded) {
			soundPool.play(soundID[0], volume, volume, 1, 0, 1f);
			Log.i(APP_NAME, "Played sound");
		}
	}
	
	public void selectAndPlayFromNote(int note) {
		playNote(note);
	}
	
	private void playNote(int note) {
		
		AudioManager audioManager = (AudioManager) mcontext.getSystemService(OpenGuitarChords.AUDIO_SERVICE);
		float actualVolume = (float) audioManager
				.getStreamVolume(AudioManager.STREAM_MUSIC);
		float maxVolume = (float) audioManager
				.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume = actualVolume / maxVolume;
		if (loaded) {
			//soundPool.
			
			soundPool.play(soundID[note], volume, volume, 1, 0, 1f);
			Log.i(APP_NAME, "Played sound");
		}
	}

}
