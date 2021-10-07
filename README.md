# ScaFi with Reinforcement Learning techniques
This repository contains tests about the integration of Aggregate computing techniques with Reinforcement learning.

## Installation
To run the experiments, you need:
- Java (> 11) installed;
- Python (> 3) installed.

before runs any experiments, types these commands:
```bash
./gradlew build
pip install -r requirements.txt
```

We suggest to used Python virtual env. If you already have a Python > 3 installed, you can create a virtual env as:
```
python -m venv venv
```
Then, to enable the virtual environment you have to type:
```
source venv/bin/activate
```
Finally, in the terminal you should see something like this:
```
(venv)/my/relative/path
```
This means that everything is ok. To deactivate the current virtual environment just type:
```bash
deactivate
```

## Experiment

<!-- rewrite in english -->

In questo branch sono presenti gli esperimenti della combinazione di Aggregate Computing con l'apprendimento per rinforzo multi agente.

Quì è stato utilizzato utilizzato Independent Reinforcement Learning, cioè ogni nodo tenta di apprendere 
la miglior policy concorrentemente con gli altri nodi.

Tra i vari algoritmi di apprendimento per rinforzo, è stato utilizzato Q-Learning, in quanto in letteratura
sembra essere una buona soluzione anche in caso di Collective Adaptive System.

L'applicazione di riferimento in questo caso è l'Hop count, 
cioè un programma aggregato che dato un nodo (o più) destinazione, restituisce 
un campo computazionale in cui ogni nodo contiene il numero minimo di hop rispetto a tale destinazione

Ad esempio, dato una semplice rete:

O-O-O-X

Dove X è il nodo destinazione, il campo computazionale associato è:

3-2-1-0

<!-- describe the simplest algorithm to implement the hop count strategy -->

In questo caso, l'aprendimento per rinforzo è stato applicato per migliorare
i tempi di convergenza della soluzione classica 
<!-- TODO extend this statement -->

cioè:
```scala

```
## How to reproduce the results
