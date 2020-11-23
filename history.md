## 16/11/20
Add Independent GQRL concept, following the idea of some distributed swarm reinforcement learning approaches

## 19/11/20
Fixing the time problems, seems that the algorithms reach a stable sub-optimal policy.
 * Initial configuration ![image](result/19-11-20/concentrated_episode-0..png)
 * Final configuration ![image](result/19-11-20/concentrated_episode-20..png)
This result is reached using ConcentratedProgramRL using the task *concentratedGRL*
This is a sub-optimail solution, but finally the aggregate converge to a policy:

 * [*Q-table learn]()
  
## 23/11/20
A robust refactor is need to clarify the library and the usage in the aggregate programming context. 

The results founded in 19-11-20 might have problems; the state evolution is perceived centrally by the server.
But violate the "locality" perception of nodes. In the gradient example, imagine that the source is moved
from a node to another. At a certain moment, the node state 
evaluation returns "NormalState". In the previous moment,
a node sensed a RisingSlowlyState and performs "NoAction". 
Our desired configuration is NormalState. 
So, globally, the central mind sensed that the system was 
in RisingSlowlyState, and performing no action, it became 
NormalState. This makes it seem that "RisingSlowlyState" and 
"NoAction" is a good state/action pair!

If I evaluated the state evolution locally, the system doesn't converge anymore again...