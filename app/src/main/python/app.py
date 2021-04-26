#!/usr/bin/env python


# Load the necessary python libraries
import random
from threading import Lock
from shutil import copy, rmtree
import numpy as np
import os
from os.path import dirname, join
import pandas as pd
import torch
from torch.utils.data import Dataset, DataLoader
import time
import json

from scipy.io.wavfile import write
from main import personalize_model, get_query, predict_query, ProtoSoundDataset

RATE = 44100
# VARIABLES
WAYS = 5
SHOTS = 5
BUFFER_SIZE = 4
THRESHOLD = -7   # ORIGINALLY -7
STORAGE = os.environ["HOME"]
LIBRARY_DATA_PATH = STORAGE + '/library'
DATA_PATH = STORAGE + '/meta-test-data'  # DIRECTORY OF 25 SAMPLES
DATA_CSV_PATH = DATA_PATH + '/user_data.csv'
# DATA_PATH = STORAGE + '/example/meta-test-data'  # DIRECTORY OF 25 SAMPLES
# DATA_CSV_PATH = STORAGE + '/example/meta-test-data/dj_test_data.csv'

MODEL_PATH = STORAGE + '/protosound_10_classes_scripted.pt'
QUERY_PATH = STORAGE + '/meta-test-query/'
#QUERY_FILE = 'meta-test-data/kitchen_hazard-alarm_5ft_sample4_344_chunk0_009.wav'
USER_FEEDBACK_FILE = STORAGE + '/user_feedback.csv'
TRAINING_TIME_FILE = STORAGE + '/training_time.csv'
PREDICTION_TIME_FILE = STORAGE + '/model_prediction_time.csv'
user_prototype_available = False

# GET DEVICE
device = None
if torch.cuda.is_available():
    device = torch.device('cuda:0')
else:
    device = torch.device('cpu')

# GET MODEL

#protosound_model = script_module
classes_prototypes = None  # TODO: Replace this variable once the training is done
support_data = None
buffer_array = np.zeros(WAYS)
buffer_counter = 0
location = ''

""" 
    Perf test constants
"""
SHOULD_RECORD_TRAINING_TIME = True
SHOULD_RECORD_PREDICTION_TIME = True

def seed():
    return 0.31415926

def add_background_noise(input_data, noise_data, noise_ratio=0.5):
    output = input_data * (1 - noise_ratio) + noise_data * noise_ratio
    output = output.astype(np.float32)
    return output



def generate_csv(data_path_directory, labels_dict, output_path_directory):
    # dirs = sorted([dI for dI in os.listdir(data_path_directory) if os.path.isdir(os.path.join(data_path_directory, dI))])
    global location
    categories = list(labels_dict.keys())
    c2i = {}
    i2c = {}
    for i, category in enumerate(categories):
        c2i[category] = i
        i2c[i] = category

    name = []
    fold = []
    category = []
    USE_USER_DATA = False
    print()
    for ea_dir in categories:
        # if user train an existing label, then use their data instead
        if labels_dict[ea_dir] != 1:
            USE_USER_DATA = True
        csv_index = 1
        path = os.path.join(data_path_directory, ea_dir)
        file_list = os.listdir(path)
        random.shuffle(file_list, seed)
        for file_name in file_list:
            try:
                if file_name.endswith('.wav'):
                    # if using user data and the file isn't user generated (pretrained file)
                    # then we ignore this .wav file
                    if (USE_USER_DATA and 'user' not in file_name.lower()):
                        print('skip', USE_USER_DATA, location, file_name)
                        continue

                    # if there is a location string sent by user and the .wav file doesn't have the corresponding
                    # location, then we ignore this .wav file
                    if (USE_USER_DATA and location.lower() not in file_name.lower()):
                        print('skip', USE_USER_DATA, location, file_name)
                        continue

                    if (not USE_USER_DATA and 'lib' not in file_name.lower()):
                        print('skip', USE_USER_DATA, location, file_name)
                        continue
                    print('USE_USER_DATA', USE_USER_DATA, location, file_name)
                    file_path = os.path.join(path, file_name)
                    if not os.path.exists(output_path_directory):
                        os.makedirs(output_path_directory)
                    category.append(ea_dir)
                    name.append(file_name)
                    # index = num_class + 1
                    if csv_index == len(categories) + 1:
                        csv_index = 1
                    fold.append(csv_index)
                    csv_index += 1
                    # Copy filepath to output_path_directory
                    copy(file_path, output_path_directory)
            except Exception as e:
                #open("exceptions.txt", "a").write("Exception raised for: %s\n" % file_name)
                print("Exception in generate_csv", e)
        USE_USER_DATA = False
    dict = {'filename': name, 'fold': fold, 'category': category}
    df = pd.DataFrame(dict)
    df.to_csv(output_path_directory + '/user_data.csv')
    global DATA_CSV_PATH
    DATA_CSV_PATH = output_path_directory + '/user_data.csv'
    print("Generate CSV complete.")

def submit_data(json_data):
    json_data = json.loads(json_data)
    global start_time
    start_time = time.time()
    print("submit_data->receive request")
    labels = json_data['label']
    # submitAudioTime = str(json_data['submitAudioTime'])
    labels = [element.lower().replace(" ", "_") for element in labels]
    background_noise = np.asarray(json_data['data_25'], dtype=np.int16) / 32768.0
    #print(labels)
    background_noise = background_noise[700:]

    predefine_sample = json_data['predefinedSamples']
    #print(predefine_sample)
    labels_dict = {}
    for j in range(len(labels)):
        labels_dict[labels[j]] = predefine_sample[j*5]
    for i in range(0, 25):
        predefine_sample = np.asarray(predefine_sample, dtype=np.int16)
        if predefine_sample[i] == 1:
            continue
        # generate new directory for new data, store it into "library" for future usage
        label = labels[i // 5]
        current_dir = os.path.join(LIBRARY_DATA_PATH, label)
        if not os.path.exists(current_dir):
            os.makedirs(current_dir)
        data = json_data['data_' + str(i)]
        np_data = np.asarray(data, dtype=np.int16) / 32768.0
        #print(len(np_data))
        np_data = np_data[700:]
        output = add_background_noise(np_data, background_noise, 0.25)
        global location
        filename = os.path.join(current_dir, location + '_' + label + "_user_" + str(i % 5) + '.wav')

        write(filename, RATE, output)

    # Generate CSV file and put 25 samples into one single folder
    print('Generate CSV file and put 25 samples into one single folder')
    generate_csv(LIBRARY_DATA_PATH, labels_dict, DATA_PATH)

    # LOAD DATA
    df = pd.read_csv(DATA_CSV_PATH)
    global support_data
    support_data = ProtoSoundDataset(DATA_PATH, df, 'filename', 'category')
    print(support_data.c2i, support_data.categories, support_data.i2c)
    # TRAIN MODEL
    train_loader = DataLoader(support_data, batch_size=WAYS*SHOTS)
    batch = next(iter(train_loader))
    data, label = batch
    data = data.to(device, dtype=torch.float32)
    data = data.tolist()
    print("THE BATCH SHAPE IS", len(data))
    return data

def train_data(output_arr, shape):
    # IMPORTANT: MAKE SURE THAT "classes_prototypes" AND "support_data.i2c"
    # ARE GLOBAL VARIABLEs WHICH NEED TO CHANGE EVERY TIME TRAINING IS DONE
    global classes_prototypes
    global start_time
    classes_prototypes = personalize_model(output_arr, shape, WAYS, SHOTS, device=device)
    print("training complete")
    if (SHOULD_RECORD_TRAINING_TIME):
        elapsed_time = time.time() - start_time
        # Write prediction time to file
        with open(TRAINING_TIME_FILE, 'a') as file:
            file.write(str(elapsed_time) + '\n')
        return str(elapsed_time)

def submit_location(loc):
    print('receive location:', loc)
    global location
    location = loc

def audio_prediction_feedback(json_data):
    json_data = json.loads(json_data)
    predictedLabel = str(json_data['predictedLabel'])
    location = str(json_data['location'])
    isTruePrediction = str(json_data['isTruePrediction'])
    time = str(json_data['time'])
    # Write user feedback to a file
    with open(USER_FEEDBACK_FILE, 'a') as file:
        file.write(time + ',' + location + ',' + predictedLabel + ','  + isTruePrediction + '\n')

def handle_source(json_data):
    if not os.path.exists(QUERY_PATH):
        os.makedirs(QUERY_PATH)
    json_data = json.loads(json_data)
    data = np.asarray(json_data['data'], dtype=np.int16) / 32768.0

    data = data[:44100]
    PREDICTION_QUERY_FILE_NAME = 'query'
    # Write the prediction query file
    QUERY_FILE = QUERY_PATH + '/' + PREDICTION_QUERY_FILE_NAME + '.wav'
    write(QUERY_FILE, RATE, data)
    # Make prediction
    global support_data
    global classes_prototypes
    if support_data is None or classes_prototypes is None:
        print('no training happened yet')
        return
    return get_query(QUERY_FILE, device)

def get_prediction(output_arr, shape, json_data):
    json_data = json.loads(json_data)
    db = json_data['db']
    db = str(round(db))
    confidences = None
    global support_data
    global classes_prototypes
    global buffer_array
    global buffer_counter
    if buffer_counter == 4:
        print("Something went wrong with buffer counter")
        buffer_counter = 0
        buffer_array = np.zeros(5)
    if (SHOULD_RECORD_PREDICTION_TIME):
        start_time = time.time()
        confidences = predict_query(output_arr, shape, classes_prototypes, support_data.i2c)
        elapsed_time = time.time() - start_time
        # Write prediction time to filesub
        with open(PREDICTION_TIME_FILE, 'a') as file:
            file.write(str(elapsed_time) + '\n')
    else:
        confidences = predict_query(output_arr, shape, classes_prototypes, support_data.i2c)
    # add sound to the buffer
    buffer_array += confidences
    buffer_counter += 1
    print(f"Log: Pred: {support_data.i2c[confidences.argmax()]} | Confidence: {confidences.max()}")

    # once the buffer is full, get the best confidence
    print("BUFFER COUNTER", buffer_counter)
    if buffer_counter >= BUFFER_SIZE:
        # get the best confidence and return, also check if it reachs a certain threshold
        best_index, best_confidence = buffer_array.argmax(), buffer_array.max()
        avg_best_confidence = best_confidence / BUFFER_SIZE

        # reset the buffer once it reaches BUFFER_SIZE (currently 4)
        buffer_counter = 0
        buffer_array = np.zeros(WAYS)

        # check threshold
        if (avg_best_confidence < THRESHOLD):
            print("Exit due to confidence <", THRESHOLD,", confidence is", avg_best_confidence)
            return
        print('Making prediction...', support_data.i2c[best_index], 'Confi:', avg_best_confidence)

        return [support_data.i2c[best_index], str(avg_best_confidence), db]

