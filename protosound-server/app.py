#!/usr/bin/env python
from threading import Lock
from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect
from scipy.io.wavfile import write
import numpy as np
from main import personalize_model, predict_query

# Set this variable to "threading", "eventlet" or "gevent" to test the
# different async modes, or leave it set to None for the application to choose
# the best option based on installed packages.

# from helpers import dbFS
from helpers import dbFS
from vggish_input import waveform_to_examples

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
classes_prototypes = None # TODO: Replace this variable once the training is done
support_data = None


"""
    Flask SocketIO functions
"""

def background_thread():
    """Example of how to send server generated events to clients."""
    count = 0
    while True:
        socketio.sleep(10)
        count += 1
        socketio.emit('my_response',
                      {'data': 'Server generated event', 'count': count})

@socketio.on('submit_audio')
def submit_audio(json_data):
    print("receive request")
    sample_rate = int(json_data['sample_rate'])
    data = (np.asarray(json_data['data'])).astype(int)
    file_name = json_data['file_name']
    print('writing_file', sample_rate, type(sample_rate))
    print('data', data)
    write(DATA_PATH + '/' + file_name, sample_rate, data)


@socketio.on('audio_data')
def handle_source(json_data):
    sample_rate = int(json_data['sample_rate'])
    data = (np.asarray(json_data['data'])).astype(int)
    PREDICTION_QUERY_FILE_NAME = 'query'
    # Write the prediction query file
    QUERY_FILE = DATA_PATH + '/' + PREDICTION_QUERY_FILE_NAME + '.wav'
    write(DATA_PATH + '/' + PREDICTION_QUERY_FILE_NAME, sample_rate, data)
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
    socketio.run(app, host='10.0.0.114', debug=True)
