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

from helpers import dbFS
from vggish_input import waveform_to_examples

async_mode = None

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, async_mode=async_mode)
thread = None
thread_lock = Lock()

RATE = 16000

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
    np_wav = np.fromstring(data, dtype=np.int16, sep=',') / \
        32768.0  # Convert to [-1.0, +1.0]
    # Compute RMS and convert to dB
    print('Successfully convert to NP rep', np_wav)
    rms = np.sqrt(np.mean(np_wav**2))
    db = dbFS(rms)
    print('Db...', db)
    # Make predictions
    print('Making prediction...')
    x = waveform_to_examples(np_wav, RATE)
    predictions = []
    if x.shape[0] != 0:
        x = x.reshape(len(x), 96, 64, 1)
    print('Successfully reshape x', x.shape)
    # pred = model.predict(x)
    # predictions.append(pred)

    print('Prediction: Speech (50%)')
    socketio.emit('audio_label',
                  {
                      'label': 'Unrecognized Sound',
                      'accuracy': '1.0'
                  })


@app.route('/')
def index():
    return render_template('index.html', async_mode=socketio.async_mode)


@socketio.event
def my_event(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']})


@socketio.event
def my_broadcast_event(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']},
         broadcast=True)


@socketio.event
def join(message):
    join_room(message['room'])
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': 'In rooms: ' + ', '.join(rooms()),
          'count': session['receive_count']})


@socketio.event
def leave(message):
    leave_room(message['room'])
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': 'In rooms: ' + ', '.join(rooms()),
          'count': session['receive_count']})


@socketio.on('close_room')
def on_close_room(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response', {'data': 'Room ' + message['room'] + ' is closing.',
                         'count': session['receive_count']},
         to=message['room'])
    close_room(message['room'])


@socketio.event
def my_room_event(message):
    session['receive_count'] = session.get('receive_count', 0) + 1
    emit('my_response',
         {'data': message['data'], 'count': session['receive_count']},
         to=message['room'])


@socketio.event
def disconnect_request():
    @copy_current_request_context
    def can_disconnect():
        disconnect()

    session['receive_count'] = session.get('receive_count', 0) + 1
    # for this emit we use a callback function
    # when the callback function is invoked we know that the message has been
    # received and it is safe to disconnect
    emit('my_response',
         {'data': 'Disconnected!', 'count': session['receive_count']},
         callback=can_disconnect)


@socketio.event
def my_ping():
    emit('my_pong')


@socketio.event
def connect():
    global thread
    with thread_lock:
        if thread is None:
            thread = socketio.start_background_task(background_thread)
    emit('my_response', {'data': 'Connected', 'count': 0})


@socketio.on('disconnect')
def test_disconnect():
    print('Client disconnected', request.sid)


if __name__ == '__main__':
    socketio.run(app)
