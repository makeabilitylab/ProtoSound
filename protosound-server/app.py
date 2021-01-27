#!/usr/bin/env python
from threading import Lock
from flask import Flask, render_template, session, request, \
    copy_current_request_context
from flask_socketio import SocketIO, emit, join_room, leave_room, \
    close_room, rooms, disconnect
from scipy.io.wavfile import write
import numpy as np

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
    write(file_name, sample_rate, data)


@socketio.on('audio_data')
def handle_source(json_data):
    data = str(json_data['data'])
    data = data[1:-1]
    global graph
    np_wav = np.fromstring(data, dtype=np.int16, sep=',')
    # Make predictions
    print('Making prediction...')
    # pred = model.predict(x)
    # Send a hard code prediction for now
    print('Prediction: Speech (50%)')
    socketio.emit('audio_label',
                  {
                      'label': 'Unrecognized Sound',
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
