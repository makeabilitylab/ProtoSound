package com.makeability.protosound.utils;

import android.util.Log;

import org.apache.commons.math3.util.Precision;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.stream.Collectors;

public class ML {


    private static final String TAG = "ML";

    public static double[][] getMelSspectrogramDb(String filePath) throws Exception{
        WavFile readWavFile = WavFile.openWavFile(new File(filePath));
        final int BUF_SIZE = (int) readWavFile.getNumFrames();
        float[] wav = new float[BUF_SIZE * 1];
        int framesRead;
        do
        {
            framesRead = readWavFile.readFrames(wav, BUF_SIZE);

        }
        while (framesRead != 0);

        readWavFile.close();

        if (wav.length < 44100) {
            wav = pad_reflect(wav, (int) Math.ceil((44100 - wav.length)/ 2.0));
        } else {
            wav = Arrays.copyOfRange(wav, 0, 44100);
        }

        AudioFeatureExtraction mfccConvert = new AudioFeatureExtraction();
        mfccConvert.setSampleRate(44100);
        mfccConvert.setN_fft(2048);
        mfccConvert.setN_mels(128);
        mfccConvert.setHop_length(512);
        mfccConvert.setfMin(20);
        mfccConvert.setfMax(8300);
        float[][] spec2 = mfccConvert.melSpectrogramWithComplexValueProcessing(wav);

        double[][] spec = new double[spec2.length][spec2[0].length];
        for (int i = 0; i < spec2.length; i++) {
            for (int j = 0; j < spec2[0].length; j++) {
                spec[i][j] =  spec2[i][j];
            }
        }

        spec = mfccConvert.powerToDb(spec);
        return spec;
    }

    static float[] pad_reflect(float[] array, int pad_width) {
        float[] ret = new float[array.length + pad_width * 2];

        if (array.length == 0) {
            throw new IllegalArgumentException("can't extend empty axis 0 using modes other than 'constant' or 'empty'");
        }

        //Exception if only one element exists
        if (array.length == 1) {
            Arrays.fill(ret, array[0]);
            return ret;
        }

        //Left_Pad
        int pos = 0;
        int dis = -1;
        for (int i = 0; i < pad_width; i++) {
            if (pos == array.length - 1 || pos == 0) {
                dis = -dis;
            }
            pos += dis;
            ret[pad_width - i - 1] = array[pos];
        }

        System.arraycopy(array, 0, ret, pad_width, array.length);

        //Right_Pad
        pos = array.length - 1;
        dis = 1;
        for (int i = 0; i < pad_width; i++) {
            if (pos == array.length - 1 || pos == 0) {
                dis = -dis;
            }
            pos += dis;
            ret[pad_width + array.length + i] = array[pos];
        }
        return ret;
    }


    public static float[][] specToImage(double[][] spec) {
        int n = spec.length;
        int m = spec[0].length;
        float avg = 0.0f;
        float variance = 0.0f;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                avg += (float) spec[i][j];
        avg /= n * m;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                variance += Math.pow((spec[i][j] - avg), 2);
        variance /= n * m;
        float stdev = (float) Math.sqrt(variance);

        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                spec[i][j] =  ((spec[i][j] - avg) / (stdev + 1E-6));

        double max =  Arrays.stream(spec).flatMapToDouble(Arrays::stream).max().getAsDouble();
        double min =  Arrays.stream(spec).flatMapToDouble(Arrays::stream).min().getAsDouble();

        float[][] res = new float[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++) {
                int val = (int) (255f * ((float)spec[i][j] - (float)min) / (max - min));
                res[i][j] = (float) val;
            }

        return res;
    }
}
