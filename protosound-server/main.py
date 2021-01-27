######################################
# SoundWatch ProtoNet Live Prediction
# Author: Dhruv Jain
# Modified: Jan 2021
######################################


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

###############################
## DATA FUNCTIONS
###############################

# VARIABLES
FILE_SIZE = 1  # Length of each sample in seconds
SAMPLING_RATE = 44100


def get_melspectrogram_db(file_path, sr=SAMPLING_RATE, n_fft=2048, hop_length=512, n_mels=128, fmin=20, fmax=8300,
                          top_db=80):
    wav, sr = librosa.load(file_path, sr=sr)  # Librosa converts audio data to mono by default
    if wav.shape[0] < FILE_SIZE * sr:
        print("Should never reach here")
        wav = np.pad(wav, int(np.ceil((FILE_SIZE * sr - wav.shape[0]) / 2)), mode='reflect')
    else:
        wav = wav[:FILE_SIZE * sr]
    spec = librosa.feature.melspectrogram(wav, sr=sr, n_fft=n_fft,
                                          hop_length=hop_length, n_mels=n_mels, fmin=fmin, fmax=fmax)
    spec_db = librosa.power_to_db(spec, top_db=top_db)
    return spec_db


def spec_to_image(spec, eps=1e-6):
    mean = spec.mean()
    std = spec.std()
    spec_norm = (spec - mean) / (std + eps)
    spec_min, spec_max = spec_norm.min(), spec_norm.max()
    spec_scaled = 255 * (spec_norm - spec_min) / (spec_max - spec_min)
    spec_scaled = spec_scaled.astype(np.uint8)
    return spec_scaled


class SoundWatchData(Dataset):
    def __init__(self, data_path_directory, df, in_col, out_col):
        self.df = df
        self.data = []
        self.labels = []
        self.c2i = {}
        self.i2c = {}
        self.categories = sorted(df[out_col].unique())
        for i, category in enumerate(self.categories):
            self.c2i[category] = i
            self.i2c[i] = category
        for ind in tqdm(range(len(df))):
            row = df.iloc[ind]
            file_path = os.path.join(data_path_directory, row[in_col])
            self.data.append(spec_to_image(get_melspectrogram_db(file_path))[np.newaxis, ...])
            self.labels.append(self.c2i[row['category']])

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        return self.data[idx], self.labels[idx]


###############################
## MODEL FUNCTIONS
###############################

def pairwise_distances_logits(a, b):
    n = a.shape[0]
    m = b.shape[0]
    logits = -((a.unsqueeze(1).expand(n, m, -1) -
                b.unsqueeze(0).expand(n, m, -1)) ** 2).sum(dim=2)
    return logits


def accuracy(predictions, targets):
    predictions = predictions.argmax(dim=1).view(targets.shape)
    return (predictions == targets).sum().float() / targets.size(0)


def personalize_model(model, batch, ways, shot, device=None):
    data, labels = batch
    data = data.to(device, dtype=torch.float32)
    labels = labels.to(device, dtype=torch.long)
    support_embeddings = model(data)
    mean_support_embeddings = support_embeddings.reshape(ways, shot, -1).mean(dim=1)

    return mean_support_embeddings


def predict_query(model, query_file_path, class_prototypes, i2c, device=None):
    query_data = spec_to_image(get_melspectrogram_db(query_file_path))[np.newaxis, ...]
    query_data = torch.from_numpy(query_data).unsqueeze(0)
    query_data = query_data.to(device, dtype=torch.float32)
    query_embedding = model(query_data)

    predictions_vector = pairwise_distances_logits(query_embedding, class_prototypes)
    predictions = predictions_vector.argmax(dim=1)

    labels = [i2c.get(key) for key in predictions.numpy()]
    return labels


'''
#################################
## SAMLPLE APPLICATION
#################################
'''

# VARIABLES
WAYS = 5
SHOTS = 5
DATA_PATH = 'meta-test-data'  # DIRECTORY OF 25 SAMPLES
DATA_CSV_PATH = 'meta-test-data/dj_test_data.csv'  # DESCRIPTION OF 25 SAMPLES
QUERY_FILE = 'meta-test-query/kitchen_microwave_15ft_sample3_143_chunk0_009.wav'  # THE ONE FILE THAT NEEDS TO BE PREDICTED
MODEL_PATH = 'model/protosound_10_classes.pt'

# GET DEVICE
if torch.cuda.is_available():
    device = torch.device('cuda:0')
else:
    device = torch.device('cpu')

# GET MODEL
protosound_model = torch.load(MODEL_PATH, map_location=device)
protosound_model = protosound_model.to(device)

# LOAD DATA
df = pd.read_csv(DATA_CSV_PATH)
support_data = SoundWatchData(DATA_PATH, df, 'filename', 'category')

# TRAIN MODEL
train_loader = DataLoader(support_data, batch_size=WAYS * SHOTS)
batch = next(iter(train_loader))
classes_prototypes = personalize_model(protosound_model, batch, WAYS, SHOTS,
                                       device=device)  ## KHOA: IMPORTANT: MAKE SURE THAT "classes_prototypes" AND "support_data.i2c" ARE GLOBAL VARIABLEs WHICH NEED TO CHANGE EVERY TIME TRAINING IS DONE

# CALCULATE PREDICTION FOR A QUERY (RUNS EVERYTIME A QUERY IS RECEIVED)
output = predict_query(protosound_model, QUERY_FILE, classes_prototypes, support_data.i2c, device=device)
print(output)
