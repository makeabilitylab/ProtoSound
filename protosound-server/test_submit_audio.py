import socketio
from scipy.io.wavfile import read
import numpy as np


sio = socketio.Client()

@sio.event
def connect():
    print('connection established')
    FILES = ['living_cat-meow_10ft_sample1_166_chunk0_001.wav', 'living_dog-bark_10ft_sample1_181_chunk0_001.wav', 'living_doorbell_10ft_sample1_196_chunk0_000.wav']
    for f in FILES:
        sample_rate, data = read('data/' + f)
        data_np = np.array(data, dtype=float)
        sample_rate_np = np.array(sample_rate, dtype=float)
        print('data', data)
        sio.emit('submit_audio', {'file_name': f, 'sample_rate': sample_rate_np.tolist(), 'data': data_np.tolist()})

@sio.event
def my_message(data):
    print('message received with ', data)
    sio.emit('my response', {'response': 'my response'})

@sio.event
def disconnect():
    print('disconnected from server')

sio.connect('http://localhost:5000')

sio.wait()
