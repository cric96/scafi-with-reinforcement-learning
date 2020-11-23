# Scafi integration with Reinforcement Learning techniques

This repository contains tests about the integration of Aggregate computing techniques with machine learning.
In particular, we are interested in the integration of Reinforcement learning techniques in the Large Scale Comple Adaptive System (CAS) scenario.

## How to use

Each branch contains a particular case study (e.g. learning with Monte Carlo, distributed Q-Learning,..).

To run a simulation, we use gradle tasks. In each case study, there might be multiple defined gradle tasks.
See the *build.gradle.kts* for choosing what task you want to run and then type:

``./gradlew taskName``

## How to plot data
Each simulation produces some relevant application data. We use python script to plot these data for having a visual feedback about the learning process.
Some additional data might be produced, see the comments in each gradle task.

In .python-version there is the correct python version for run these scripts. The library dependencies are store in
.python-lib. If you use pip, you can run :

`` pip install -r .python-version``

### Progress description

Each branch has own experiment description (in a history.md file) that explain what are the progress done, what are problems, and the solution found.
