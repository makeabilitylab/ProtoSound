package com.makeability.protosound.utils;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class ProtoApp extends Application {
    private static final String TAG = "ProtoApp";
    private Module mModuleEncoder;

    private final int RATE = 44100;
    private final int WAYS = 5;
    private final int SHOTS = 5;
    private final int BUFFER_SIZE = 4;          // originally 4
    private final float THRESHOLD = 0.75f;      // originally 0.6
    private String INTERNAL_STORAGE;
    private String LIBRARY_DATA_PATH;
    private String USER_LIBRARY_PATH;
    private String DATA_CSV_PATH;

    private String USER_LOCATION_FILE;

    private Map<String, Integer> c2i;
    private Map<Integer, String> i2c;
    private float[][] meanSupportEmbeddings;
    private String location;
    private int bufferCounter = 0;
    private float[] bufferArray = new float[WAYS];


    private void generateCSV(Map<String, Integer> labelsDict, int[] predefinedSamples) throws IOException {
        List<String> categories = new ArrayList<>(labelsDict.keySet());
        c2i = new HashMap<>();
        i2c = new HashMap<>();

        int i = 0;
        for (String category : categories) {
            c2i.put(category, i);
            i2c.put(i, category);
            i++;
        }

        List<String> name = new ArrayList<>();
        List<Integer> fold = new ArrayList<>();
        List<String> category = new ArrayList<>();

        boolean useUserData = false;
        String dataPathDir;
        for (String eaDir : categories) {
            if (labelsDict.get(eaDir) == 0) {
                useUserData = true;
                dataPathDir = USER_LIBRARY_PATH + "/" + location;
            } else if (labelsDict.get(eaDir) == 1) {
                dataPathDir = LIBRARY_DATA_PATH;
            } else {
                dataPathDir = USER_LIBRARY_PATH + "/" + location;
            }
            int csvIndex = 1;
            String path = dataPathDir + "/" + eaDir;
            Log.d(TAG, "PATH " + path);
            File dir = new File(path);
            File[] fileList = dir.listFiles();
            for (File file : fileList) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".wav")) {
                    if (useUserData && !fileName.contains("user")) {
                        Log.d(TAG, "skip " + useUserData + " location " + fileName);
                        continue;
                    }
//
//                    if (useUserData && !fileName.contains(location.toLowerCase())) {
//                        Log.d(TAG, "skip " + useUserData + " location " + fileName);
//                        continue;
//                    }

//                    if (!useUserData && !fileName.contains("lib")) {
//                        Log.d(TAG, "skip " + useUserData + " location " + fileName);
//                        continue;
//                    }
                    //Log.d(TAG, "USE_USER_DATA, " + useUserData + " location " + fileName);
                    String filePath = path + "/" + fileName;
                    // CODE FOR NOT EXIST OUTPUT_PATH_DIR

                    category.add(eaDir);
                    name.add(fileName);
                    if (csvIndex == categories.size() + 1) {
                        csvIndex = 1;
                    }
                    fold.add(csvIndex);
                    csvIndex++;
//                    // COPY from library to meta-test-data
//                    File src = new File(filePath);
//                    File dst = new File(DATA_PATH);
//                    FileUtils.copyToDirectory(src, dst);
                }
            }
            useUserData = false;
        }
        // DICT
        // DATAFRAME
        String[] headers = new String[]{"filename", "predefinedType", "category"};
        try (Writer writer = new FileWriter(DATA_CSV_PATH);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers))
        ) {
            for (int j = 0; j < 25; j++) {
                csvPrinter.printRecord(name.get(j), labelsDict.get(category.get(j)), category.get(j));
            }
        }
        Log.d(TAG, "GENERATE CSV COMPLETE");

    }

    public void submitData(JSONObject jsonData, Map<Integer, List<Short>> map) {
        try {
            // Labels
            JSONArray labelsJSON = jsonData.getJSONArray("label");
            String[] labels = new String[labelsJSON.length()];
            for (int i = 0; i < labelsJSON.length(); i++) {
                String label = labelsJSON.getString(i).toLowerCase().replace(" ", "_");
                labels[i] = label;
            }
            // Background noise
            List<Short> backgroundNoiseList = map.get(25);
            double[] backgroundNoise = new double[backgroundNoiseList.size()];
            for (int i = 0; i < backgroundNoise.length; i++) {
                backgroundNoise[i] = backgroundNoiseList.get(i) / 32768.0;
            }

            Log.d(TAG, "BACKGROUNDNOISE " + Arrays.toString(backgroundNoise));

            backgroundNoise = Arrays.copyOfRange(backgroundNoise, 700, backgroundNoise.length);

            // Predefined samples
            JSONArray predefinedSamplesJSON = jsonData.getJSONArray("predefinedSamples");
            int[] predefinedSamples = new int[predefinedSamplesJSON.length()];
            for (int i = 0; i < predefinedSamplesJSON.length(); i++) {
                predefinedSamples[i] = predefinedSamplesJSON.getInt(i);
            }

            Map<String, Integer> labelsDict = new HashMap<>();
            for (int i = 0; i < labels.length; i++) {
                labelsDict.put(labels[i], predefinedSamples[i * 5]);
            }

            String userLocationDir = USER_LIBRARY_PATH + "/" + location;
            // IF CURRENTDIR NOT EXIST MAKE IT
            File dir = new File(userLocationDir);
            if (!dir.exists()) {
                dir.mkdir();
            }

            for (int i = 0; i < 25; i++) {
                if (predefinedSamples[i] == 1 || predefinedSamples[i] == 2) {
                    continue;
                }
                String label = labels[Math.floorDiv(i, 5)];

                String userSoundDir = userLocationDir +  "/" + label;
                // IF CURRENTDIR NOT EXIST MAKE IT
                File dir2 = new File(userSoundDir);
                if (!dir2.exists()) {
                    dir2.mkdir();
                }


                // Data_i
//                Log.d(TAG, "CURRENT DIR " + currentDir);
//                Log.d(TAG, "CURRENT DATA_: " + i);
                double[] data = new double[map.get(i).size()];
                List<Short> dataList = map.get(i);

                for (int j = 0; j < data.length; j++) {
                    data[j] = dataList.get(j) / 32768.0;
                }


                data = Arrays.copyOfRange(data, 700, data.length);
                double[] output = addBackgroundNoise(data, backgroundNoise);

                String filename = userSoundDir + "/" + label + "_user_" + i % 5 + ".wav";

                // Write to WAV file
                try {
                    int numFrames = data.length;
                    WavFile wavFile = WavFile.newWavFile(new File(filename), 1, numFrames, 16, RATE);
                    int framesWritten;
                    do {
                        framesWritten = wavFile.writeFrames(output, numFrames);
                    }
                    while (framesWritten != 0);
                    wavFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // GENERATE CSV
            Log.d(TAG, "Generate CSV file and put 25 samples into one single folder");
            try {
                generateCSV(labelsDict, predefinedSamples);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // LOAD DATA
            Set<String> labels2 = new LinkedHashSet<>();    // LinkedHashSet preserves order
            float[][][][] data = new float[25][1][128][87];
            int i = 0;
            try (Reader reader = new FileReader(DATA_CSV_PATH);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {
                String dataPathDir;
                for (CSVRecord record : csvParser) {
                    if (record.get("predefinedType").equals("1")) {
                        dataPathDir = LIBRARY_DATA_PATH;
                    } else {
                        dataPathDir = USER_LIBRARY_PATH + "/" + location;
                    }
                    //String filePath = DATA_PATH + "/" + record.get("filename");
                    String filePath = dataPathDir + "/" + record.get("category") + "/" + record.get("filename");

                    float[][] specScaled = ML.specToImage(ML.getMelSspectrogramDb(filePath));
                    float[][][] arr = new float[1][128][87];
                    arr[0] = specScaled;

                    data[i] = arr;
                    labels2.add(record.get("category"));
                    i++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            i = 0;
            for (String label : labels2) {
                i2c.put(i, label);
                i++;
                Log.d(TAG, "LABEL " + label);
            }

            //Log.d(TAG, "SHAPE IS " + data.length + " " + data[0].length + " " + data[0][0].length + " " + data[0][0][0].length);

            Tensor inputTensor = Tensor.fromBlob(flatten(data), new long[]{data.length, data[0].length, data[0][0].length, data[0][0][0].length});
            Tensor outputTensor = mModuleEncoder.forward(IValue.from(inputTensor)).toTensor();
            long[] outputShape = outputTensor.shape();
            //Log.d(TAG, "OUTPUT IS " + outputTensor);

            float[] outputArr = outputTensor.getDataAsFloatArray();

            int arrDim = outputShape.length;
            float[][] supportEmbeddings = new float[(int) outputShape[0]][(int) outputShape[1]];
            for (int j = 0; j < supportEmbeddings.length; j++) {
                for (int k = 0; k < supportEmbeddings[0].length; k++) {
                    supportEmbeddings[j][k] = outputArr[(j * supportEmbeddings[0].length) + k];
                }
            }

            meanSupportEmbeddings = new float[WAYS][supportEmbeddings[0].length];
            for (int j = 0; j < meanSupportEmbeddings.length; j++) {
                for (int k = 0; k < meanSupportEmbeddings[0].length; k++) {
                    float sum = 0;
                    for (int l = 0; l < meanSupportEmbeddings.length; l++) {
                        sum += supportEmbeddings[5 * j + l][k];
                    }
                    float mean = sum / meanSupportEmbeddings.length;
                    meanSupportEmbeddings[j][k] = mean;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> handleSource(List<Short> queryData, double db) {
        try {
            if (meanSupportEmbeddings == null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "No training happened! Please train the model first.", Toast.LENGTH_SHORT).show();
                    }
                });

                Log.d(TAG, "NO TRAINING HAPPENED YET");
                return null;
            }
            if (db < 30.0) return null;

            String dbStr = String.valueOf(Math.round(db));
            double[] data = new double[queryData.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = queryData.get(i) / 32768.0;
            }
            data = Arrays.copyOfRange(data, 0, RATE);
            String queryFile = INTERNAL_STORAGE + "/query.wav";

            int numFrames = data.length;
            WavFile wavFile = WavFile.newWavFile(new File(queryFile), 1, numFrames, 16, RATE);
            int framesWritten;
            do {
                framesWritten = wavFile.writeFrames(data, numFrames);

            }
            while (framesWritten != 0);
            wavFile.close();


            if (bufferCounter == 4) {
                Log.d(TAG, "Something went wrong with buffer counter");
                bufferCounter = 0;
                bufferArray = new float[5];
            }
            float[][] specScaled = ML.specToImage(ML.getMelSspectrogramDb(queryFile));

            float[][][][] arr = new float[1][1][128][87];
            arr[0][0] = specScaled;

            Tensor inputTensor = Tensor.fromBlob(flatten(arr), new long[]{arr.length, arr[0].length, arr[0][0].length, arr[0][0][0].length});
            Tensor outputTensor = mModuleEncoder.forward(IValue.from(inputTensor)).toTensor();


            float[] outputArr = outputTensor.getDataAsFloatArray();
            float[][] queryEmbedding = new float[1][outputArr.length];
            queryEmbedding[0] = outputArr;
            float[] confidences = pairwiseDistanceLogits(queryEmbedding, meanSupportEmbeddings);

            for (int i = 0; i < bufferArray.length; i++) {
                bufferArray[i] += confidences[i];
            }
            bufferCounter++;

            // Record the query for BUFFER_SIZE times, then
            // find R = d1 / d2. If R <= T, take it.
            Log.d(TAG, "BUFFER COUNTER " + bufferCounter);
            if (bufferCounter >= BUFFER_SIZE) {
                int bestIndex = 0;
                float bestConfidence = -Float.MAX_VALUE;
                float secondBestConfidence = bestConfidence;
                for (int i = 0; i < bufferArray.length; i++) {
                    if (Float.compare(bufferArray[i], bestConfidence) > 0) {
                        secondBestConfidence = bestConfidence;
                        bestConfidence = bufferArray[i];
                        bestIndex = i;
                    }
                    if (Float.compare(bufferArray[i], bestConfidence) < 0 &&
                            Float.compare(bufferArray[i], secondBestConfidence) > 0
                    ) {
                        secondBestConfidence = bufferArray[i];
                    }
                    Log.d(TAG, "BUFFER ARRAY ELEMENTS at " + i + ": " + bufferArray[i]);
                }

                // Actually no need to divide by BUFFER_SIZE
                float avgBestConfidence = bestConfidence / BUFFER_SIZE;
                float avgSecondBestConfidence = secondBestConfidence / BUFFER_SIZE;
                bufferCounter = 0;
                Log.d(TAG, "BUFFER ARRAY PREDICTION " + Arrays.toString(bufferArray));
                bufferArray = new float[5];
                float ratio = avgBestConfidence / avgSecondBestConfidence;
                Log.d(TAG, "PREDICTION RATIO: " + ratio);
                if (ratio > THRESHOLD) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "No match for query. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Log.d(TAG, "Exit due to R= " + ratio + " which is > threshold of " + THRESHOLD);
                    return null;
                }
                Log.d(TAG, "Making prediction...");
                List<String> result = new ArrayList<>();
                result.add(i2c.get(bestIndex)); // label
                result.add(new DecimalFormat("#").format(Math.round((1 - ratio) * 100)));
                result.add(dbStr);
                Log.d(TAG, "PREDICTION LABEL " + result.get(0));
                //Log.d(TAG, "AVG BEST CONFIDENCE " + result.get(1));
                return result;
            }
            return null;


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // queryEmbedding shape: [1, 1280]
    // meanSupportEmbedding shape: [5, 1280]
    private float[] pairwiseDistanceLogits(float[][] queryEmbedding, float[][] meanSupportEmbeddings) {
        int cols = queryEmbedding[0].length;    // 1280
        float[] logits = new float[meanSupportEmbeddings.length];   // 5
        for (int i = 0; i < logits.length; i++) {
            float sum = 0;
            for (int j = 0; j < cols; j++) {
                sum += Math.pow((queryEmbedding[0][j] - meanSupportEmbeddings[i][j]), 2);
            }
            logits[i] = -sum;
        }
        return logits;
    }

    static void flatten(Object object, List<Float> list) {
        if (object.getClass().isArray())
            for (int i = 0; i < Array.getLength(object); ++i)
                flatten(Array.get(object, i), list);
        else
            list.add((float) object);
    }

    static float[] flatten(Object object) {
        List<Float> list = new ArrayList<>();
        flatten(object, list);
        int size = list.size();
        float[] result = new float[size];
        for (int i = 0; i < size; ++i)
            result[i] = list.get(i);
        return result;
    }


    private double[] addBackgroundNoise(double[] inputData, double[] noiseData) {
        double noiseRatio = 0.25;
        double[] output = new double[inputData.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = inputData[i] * (1 - noiseRatio) + noiseData[i] * noiseRatio;
        }
        return output;
    }

    public void submitLocation(String loc) {
        this.location = loc;
        Log.d(TAG, "Received location " + location);
    }

    public List<String> getUserSoundsForLoc() {
        List<String> sounds = new ArrayList<>();
        String path = USER_LIBRARY_PATH + "/" + location;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
            return sounds;
        }
        String[] userSoundLabels = dir.list();
        assert userSoundLabels != null;
        sounds.addAll(Arrays.asList(userSoundLabels));
        Collections.sort(sounds);
        return sounds;
    }

    public boolean saveLocation(String loc) {
        List<String> savedLocations = getSavedLocations();
        for (int i = 0; i < savedLocations.size(); i++) {
            if (savedLocations.get(i).toString().toLowerCase().equals(loc)) {
                Toast.makeText(this, "Location already exists.", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(USER_LOCATION_FILE, true));
            writer.append(loc + "\n");
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getSavedLocations() {
        List<String> locations = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(USER_LOCATION_FILE));

            String line = reader.readLine();
            while (line != null) {
                locations.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.sort(locations);
        return locations;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ProtoApp initialized.");
        INTERNAL_STORAGE = getApplicationContext().getFilesDir().getAbsolutePath();

        LIBRARY_DATA_PATH = makeDir(INTERNAL_STORAGE + "/library");
        moveAssetToStorage(this, "library");

        USER_LIBRARY_PATH = makeDir(INTERNAL_STORAGE + "/user_library");

        DATA_CSV_PATH = INTERNAL_STORAGE + "/user_data.csv";
        File user_data_csv = new File(DATA_CSV_PATH);
        USER_LOCATION_FILE = INTERNAL_STORAGE + "/user_location.txt";
        File user_location_file = new File(USER_LOCATION_FILE);
        try {
            user_data_csv.createNewFile();
            user_location_file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String moduleFileAbsoluteFilePath = "";
        if (mModuleEncoder == null) {
            moduleFileAbsoluteFilePath = INTERNAL_STORAGE + "/protosound_10_classes_scripted.pt";
            moveAssetToStorage(this, "protosound_10_classes_scripted.pt");
            mModuleEncoder = Module.load(moduleFileAbsoluteFilePath);	// Have a ScriptModule now
        }
    }

    private String makeDir(String dirName) {
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dirName;
    }

    public Module getModule() {
        return mModuleEncoder;
    }

    // If assetName is a folder, this method only accommodates the structure
    // of folder "library" (can use recursion to support other folder structures)
    // Otherwise, it supports files normally
    public void moveAssetToStorage(Context context, String assetName) {
        try {
            String[] numFiles = context.getAssets().list(assetName);   // list out library
            if (numFiles.length > 0) {  // A folder. Only library folder in assets
                List<String> categories = new ArrayList<>();
                for (String file : numFiles) {
                    String path = LIBRARY_DATA_PATH + "/" + file;
                    makeDir(path);
                    String[] wavFiles = context.getAssets().list(assetName + "/" + file);
                    for (String wavFile : wavFiles) {
                        String inFile = assetName + "/" + file + "/" + wavFile;
                        String outFile = path + "/" + wavFile;
                        writeAssetFileToStorage(context, inFile, outFile);
                    }
                }
            } else {    // Otherwise, other csv files and protosound.pt
                writeAssetFileToStorage(context, assetName, INTERNAL_STORAGE + "/" + assetName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeAssetFileToStorage(Context context, String inFile, String outFile){
        File file = new File(outFile);
        // If file already exists in internal storage, skip
        if (file.exists() && file.length() > 0) {
            return;
        }
        try (InputStream is = context.getAssets().open(inFile)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, file + ": " + e.getLocalizedMessage());
        }
    }

}

