
# ProtoSound #

Note: This repository is work-in-progress. Stay tuned for our first release.

![Status](https://img.shields.io/badge/Version-Experimental-brightgreen.svg) [![License](https://img.shields.io/badge/license-MIT-blue)](https://opensource.org/licenses/MIT)

## Introduction ##

ProtoSound is a mobile application for Android smartphones that allows users to customize their sound recognition models and receive private sound feedback in a different contexts.

It adapts prototypical networks, a commonly used algorithm for few-shot image classification, to the sound classification domain while extending the traditional training pipeline to incorporate additional user-centric features for real-world deployment.

The project is inspired by previous sound awareness work with DHH users, our DHH lead author's experiences, and a survey of 472 DHH participants. According to field evaluations with a real-life dataset and an interactive mobile application, ProtoSound can be used to build sound recognition systems that support highly personalized and fine-grained sound categories, can train on-device in real-time, and can handle contextual variations in a variety of real-world contexts using few custom recordings.

The **DeployablePythonCode** folder contains the python-based implementation of ProtoSound which can be run on any python-enabled devices.

The **StandaloneAndroidApp** folder contains the android-based implementation of ProtoSound which can be run on android development platforms that meet the prerequisites specified in the folder.

This repository also contains samples of 10 sound categories preferred by DHH people (e.g, fire alarm, knocking, baby crying), procured from a high-quality online library, FreeSound, and manually cleaned by our research team. Navigate to **StandaloneAndroidApp/app/src/main/assets/library** for the full library of sounds.

[[Website](https://makeabilitylab.cs.washington.edu/project/protosound/)] 
[[Paper PDF](https://homes.cs.washington.edu/~djain/img/portfolio/Jain_ProtoSound_CHI2022.pdf)] 
[[Video](https://homes.cs.washington.edu/~djain/img/portfolio/protosound-video.mp4)]  

## Support ##  

- Developed with [Dhruv Jain](https://homes.cs.washington.edu/~djain/) and collaborators at [MakeabilityLab](https://makeabilitylab.cs.washington.edu/)
- Contact [Khoa Nguyen](https://www.linkedin.com/in/akka/) @MakeabilityLab through email `akhoa99` at cs.washington.edu
- Contact [Quan Dang](https://www.linkedin.com/in/quangary/) @MakeabilityLab through email `quangary` at cs.washington.edu
- Contact [Hung V Ngo](https://www.hungvngo.com) @MakeabilityLab through email `hvn297` at cs.washington.edu

Drop us a note if you are using or plan to use ProtoSound for research purposes. We are also happy to help with any questions or issues.

## Related Work ##  

- [SoundWatch](https://makeabilitylab.cs.washington.edu/project/soundwatch/): SoundWatch: Exploring Smartwatch-based Deep Learning Approaches to Support Sound Awareness for Deaf and Hard of Hearing Users
- [HomeSound](https://makeabilitylab.cs.washington.edu/project/smarthomedhh/): An Iterative Field Deployment of an In-Home Sound Awareness System for Deaf or Hard of Hearing Users
