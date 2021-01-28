#!/usr/bin/env python
from random import random
from threading import Lock

# Load the necessary python libraries
import numpy as np
import librosa
import os
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from tqdm import tqdm

from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect
from scipy.io.wavfile import write
from main import personalize_model, predict_query

# Set this variable to "threading", "eventlet" or "gevent" to test the
# different async modes, or leave it set to None for the application to choose
# the best option based on installed packages.

async_mode = None

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, async_mode=async_mode)
thread = None
thread_lock = Lock()

RATE = 44100
# VARIABLES
WAYS = 5
SHOTS = 5
LIBRARY_DATA_PATH = 'library'
DATA_PATH = 'meta-test-data'  # DIRECTORY OF 25 SAMPLES
DATA_CSV_PATH = ''
MODEL_PATH = 'model/protosound_10_classes.pt'

# GET DEVICE
if torch.cuda.is_available():
    device = torch.device('cuda:0')
else:
    device = torch.device('cpu')

# GET MODEL
protosound_model = torch.load(MODEL_PATH, map_location=device)
protosound_model = protosound_model.to(device)
classes_prototypes = None  # TODO: Replace this variable once the training is done
support_data = None

"""
    Flask SocketIO functions
"""


def seed():
    return 0.31415926


def background_thread():
    """Example of how to send server generated events to clients."""
    count = 0
    while True:
        socketio.sleep(10)
        count += 1
        socketio.emit('my_response',
                      {'data': 'Server generated event', 'count': count})


def add_background_noise(data, noise, noise_ratio=0.5):
    output = data * (1 - noise_ratio) + noise * noise_ratio
    return output


def generate_csv(data_path_directory, labels, output_path_directory):
    # dirs = sorted([dI for dI in os.listdir(data_path_directory) if os.path.isdir(os.path.join(data_path_directory, dI))])
    categories = labels
    c2i = {}
    i2c = {}
    for i, category in enumerate(categories):
        c2i[category] = i
        i2c[i] = category

    name = []
    fold = []
    category = []
    for ea_dir in labels:
        csv_index = 1
        path = os.path.join(data_path_directory, ea_dir)
        file_list = os.listdir(path)
        random.shuffle(file_list, seed)
        for file_name in file_list:
            try:
                if file_name.endswith('.wav'):
                    file_path = os.path.join(path, file_name)
                    category.append(ea_dir)
                    name.append(file_name)
                    # index = num_class + 1
                    if csv_index == len(labels) + 1:
                        csv_index = 1
                    fold.append(csv_index)
                    csv_index += 1
            #         TODO: Copy filepath to output_path_directory

            except:
                open("exceptions.txt", "a").write("Exception raised for: %s\n" % file_name)
    dict = {'filename': name, 'fold': fold, 'category': category}
    df = pd.DataFrame(dict)
    df.to_csv(output_path_directory + '/user_data.csv')
    print("Processing complete.")


@socketio.on('submit_data')
def submit_audio(json_data):
    print("submit_data->receive request")
    data = []
    for i in range(0, 16):
        data.append(json_data['data_' + i])
    labels = json_data['labels']
    background_noise = np.asarray(data[16], dtype=np.int16)
    for i in range(0, 15):
        # generate new directory for new data, store it into "library" for future usage
        label = labels[i % 5]
        current_dir = os.path.join(LIBRARY_DATA_PATH, label)
        if not os.path.exists(current_dir):
            os.makedirs(current_dir)

        np_data = np.asarray(data[i], dtype=np.int16)
        output = add_background_noise(np_data, background_noise, 0.5)

        filename = os.path.join(current_dir, label + "_user_" + str(i % 5) + '.wav')

        write(filename, RATE, output)
    generate_csv(LIBRARY_DATA_PATH, labels, DATA_PATH)


# sample_rate = int(json_data['sample_rate'])
# data = (np.asarray(json_data['data'])).astype(int)
# file_name = json_data['file_name']
# print('writing_file', sample_rate, type(sample_rate))
# print('data', data)
# write(DATA_PATH + '/' + file_name, sample_rate, data)


@socketio.on('audio_data')
def handle_source(json_data):
    data = np.asarray(json_data['data'], dtype=np.int16)
    PREDICTION_QUERY_FILE_NAME = 'query'
    # Write the prediction query file
    QUERY_FILE = LIBRARY_DATA_PATH + '/' + PREDICTION_QUERY_FILE_NAME + '.wav'
    write(QUERY_FILE, RATE, data)
    # Make prediction
    output = predict_query(protosound_model, QUERY_FILE, classes_prototypes, support_data.i2c, device=device)
    print('Making prediction...')
    socketio.emit('audio_label',
                  {
                      'label': str(output),
                      'accuracy': '1.0',
                      'db': '1.0'
                  })


@app.route('/')
def index():
    return render_template('index.html', async_mode=socketio.async_mode)


@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected', request.sid)


if __name__ == '__main__':
    socketio.run(app, host='128.208.49.41', port=5000, debug=True)
