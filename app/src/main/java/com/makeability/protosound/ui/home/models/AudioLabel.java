package com.makeability.protosound.ui.home.models;

public class AudioLabel {
	public String label;
	public double confidence;
	public String time;
	public String db;
	public String recordTime;

	public AudioLabel(String label, String confidence, String time, String db, String recordTime) {
		this.label = label;
		this.confidence = Double.parseDouble(confidence);
		this.time = time;
		this.db = db;
		this.recordTime = recordTime;
	}

	public String getShortenLabel() {
		// stores each characters to a char array
		char[] charArray = label.toCharArray();
		boolean foundSpace = true;

		for(int i = 0; i < charArray.length; i++) {

			// if the array element is a letter
			if(Character.isLetter(charArray[i])) {

				// check space is present before the letter
				if(foundSpace) {

					// change the letter into uppercase
					charArray[i] = Character.toUpperCase(charArray[i]);
					foundSpace = false;
				}
			}
			else {
				// if the new character is not character
				foundSpace = true;
			}
		}

		// convert the char array to the string
		return String.valueOf(charArray).replaceAll("_", " ");
	}

	public String getTimeAndLabel() {
		return time + "| " + this.getShortenLabel() + " | " + db + " dB";
	}
	public String getTime() {
		return this.time;
	}
}
