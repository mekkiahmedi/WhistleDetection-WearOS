/*
 * Copyright (C) 2012 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * musicg api in Google Code: http://code.google.com/p/musicg/
 * Android Application in Google Play: https://play.google.com/store/apps/details?id=com.whistleapp
 * 
 */

package model.helper;

//import static android.support.v4.content.ContextCompat.getSystemService;

import java.util.LinkedList;

import com.musicg.api.ClapApi;
import com.musicg.api.DetectionApi;
import com.musicg.api.WhistleApi;
import com.musicg.wave.WaveHeader;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import static model.constant.Constant.vibrationPattern;
import static model.constant.Constant.vibrationRepeat;

public class DetectorThread extends Thread {


    private DetectorType mType;
    private RecorderThread recorder;
    private WaveHeader waveHeader;
    private DetectionApi mDetectionApi;
    private Thread _thread;

    private LinkedList<Boolean> whistleResultList = new LinkedList<Boolean>();
    private int numWhistles;
    private int totalWhistlesDetected = 0;
    private int whistleCheckLength = 3;
    private int whistlePassScore = 3;
    private boolean isSound;

    Vibrator vibrator;

    private final DetectorCallback detectorCallback;

    private final String TAG = "DetectorTAG";
    public DetectorThread(RecorderThread recorder, DetectorType type,  DetectorCallback detectorCallback) {
        //this.vibrator=vibrator;
        this.detectorCallback = detectorCallback;
        ;
        mType = type;
        this.recorder = recorder;
        AudioRecord audioRecord = recorder.getAudioRecord();

        int bitsPerSample = 0;
        if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
            bitsPerSample = 16;
        } else if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT) {
            bitsPerSample = 8;
        }

        int channel = 0;

        if (audioRecord.getChannelConfiguration() == AudioFormat.CHANNEL_IN_MONO) {
            channel = 1;
        }

        waveHeader = new WaveHeader();
        waveHeader.setChannels(channel);
        waveHeader.setBitsPerSample(bitsPerSample);
        waveHeader.setSampleRate(audioRecord.getSampleRate());

        switch (type) {
            case CLAP:
                mDetectionApi = new ClapApi(waveHeader);
                break;
            case WHISTLE:
                mDetectionApi = new WhistleApi(waveHeader);
                break;
        }
    }

    private void initBuffer() {
        numWhistles = 0;
        whistleResultList.clear();

        // init the first frames
        for (int i = 0; i < whistleCheckLength; i++) {
            whistleResultList.add(false);
        }
        // end init the first frames
    }

    public void start() {
        _thread = new Thread(this);
        _thread.start();
    }

    public void stopDetection() {
        _thread = null;
    }

    @Override
    public void run() {
        Log.e(TAG, "DetectorThread started...");
         boolean isGWhistle=false;
        try {
            byte[] buffer;
            initBuffer();

            Thread thisThread = Thread.currentThread();
            while (_thread == thisThread) {
                // detect sound
                buffer = recorder.getFrameBytes();

                Log.d(TAG, "recorder.getFrameBytes() " + buffer);

                // audio analyst
                if (buffer != null) {
                    // sound detected
                    // MainActivity.whistleValue = numWhistles;

                    // whistle detection
                    // System.out.println("*Whistle:");

                    try {
                        /*
                        switch (mType) {
                            case CLAP:

                                boolean isClap = ((ClapApi) mDetectionApi).isClap(buffer);
                                isSound = isClap;

                                Log.e(TAG, "isClap : " + isClap + " "+ buffer.length);

                                break;
                            case WHISTLE:
                                boolean isWhistle = ((WhistleApi) mDetectionApi).isWhistle(buffer);
                                isSound = isWhistle;
                                isGWhistle= isWhistle;
                                Log.e(TAG, "isWhistle : " + isWhistle + ",  buffer:"+ buffer.length);
                                break;
                        }
                        */
                        boolean isWhistle = ((WhistleApi) mDetectionApi).isWhistle(buffer);
                        isSound = isWhistle;
                        isGWhistle= isWhistle;
                        Log.e(TAG, "isWhistle : " + isWhistle + ",  buffer:"+ buffer.length);

                        if(isGWhistle){
                           // triggerVibration();
                           detectorCallback.onWhistleDetected();
                        }
                        if (whistleResultList.getFirst()) {
                            numWhistles--;
                        }

                        whistleResultList.removeFirst();
                        whistleResultList.add(isSound);

                        if (isSound) {
                            numWhistles++;
                        }

                        Log.e(TAG, "numWhistles : " + numWhistles);

                        if (numWhistles >= whistlePassScore) {
                            // clear buffer
                            initBuffer();
                            totalWhistlesDetected++;

                            Log.e(TAG, "totalWhistlesDetected : "
                                    + totalWhistlesDetected);
                            /*
                            if (onSoundListener != null) {
                                onSoundListener.onSound(mType);
                            }

                             */
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG, "error " + e.getMessage()+" Cause :: " + e.getCause());
                    }
                    // end whistle detection
                } else {
                    // Debug.e("", "no sound detected");
                    // no sound detected
                    if (whistleResultList.getFirst()) {
                        numWhistles--;
                    }
                    whistleResultList.removeFirst();
                    whistleResultList.add(false);

                    // MainActivity.whistleValue = numWhistles;
                }
                // end audio analyst
            }

            Log.e(TAG, "Terminating detector thread...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
/*
    private OnSoundListener onSoundListener;

    public void setOnSoundListener(OnSoundListener onSoundListener) {
        this.onSoundListener = onSoundListener;
    }

    public interface OnSoundListener {
        void onSound(DetectorType type);
    }
*/
    public int getTotalWhistlesDetected() {
        return totalWhistlesDetected;
    }

    private void triggerVibration() {

        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Repeat starting at the first vibrate
                VibrationEffect effect = VibrationEffect.createWaveform(vibrationPattern, vibrationRepeat);
                vibrator.vibrate(effect);
            } else {

                // Repeat starting at index 1
                vibrator.vibrate(vibrationPattern, vibrationRepeat);
            }
        } else {
            Log.w(TAG, "Device does not support vibration.");
        }
    }
}