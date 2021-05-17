"# ProtoSound"
NOTE: Remember to stop recording the microphone before training data again
to avoid app crashing.

Before using the app:
1. Put these folders: "library", "meta-test-data", "meta-test-query",
and these files "model_prediction_time.csv", "training_time.csv", "user_feedback.csv"
into the Protosound's internal storage, located at "data/data/com.makeability.protosound/files/" in Device File Explorer
(code will be implemented to automate this process)

Train data:
1. Enter the location, and choose 5 sounds (Your Choice or Predefined)
2. Click "Record background noise"
3. Click "Train Data"

Predict data:
1. Click the "Record" icon to record query sound. Query is recorded
for 3 seconds (BUFFER_SIZE = 3) and then predicted with T value of 0.75.
(The values to be tuned. Some sounds such as siren, knocking are more accurate
to predict than cat_meow and baby_cry because they are a bit similar)
