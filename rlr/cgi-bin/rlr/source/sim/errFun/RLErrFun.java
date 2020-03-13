package sim.errFun;
import matrix.*;
import watch.*;
import sim.funApp.*;
import sim.*;
import Random;
import expression.*;
import parse.*;
import sim.mdp.*;
import pointer.*;

/** All RL objects inherit from this class.  RLErrFun is used to allow RL algorithm objects (i.e. QLearning,
  * AdvantageLearning, ValueIteration) to have access to the parameters passed to ReinforcementLearning from the
  * html file.
  *    <p>This code is (c) 1997 Mance E. Harmon
  *    <<a href=mailto:mharmon@acm.org>mharmon@acm.org</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird/java>http://www.cs.cmu.edu/~baird/java</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  *
  * @version 1.03, 21 July 97
  * @author Mance E. Harmon
  */

public abstract class RLErrFun extends ErrFun {

  /** the mdp to control */
  protected MDP mdp=null;
  /** The state of the MDP */
  protected MatrixD state=null;
  /** The state reached after performing an action */
  protected MatrixD newState=null;
  /** An action that can be chosen in a given state */
  protected MatrixD action=null;
  /** The time step size used in transitioning from state x(t) to x(t+1) */
  protected NumExp dt=new NumExp(1);
  /** The weighting factor for the weights of resgrad and direct vectors. */
  protected NumExp phi=new NumExp(1);
  /** The decay factor for the trace of the of resgrad and direct update vectors used to calculate phi. */
  protected NumExp mu=new NumExp(1);
  /** Specifies method (0=residual, 1=resGrad, 2=direct) */
  protected NumExp method=new NumExp(1);
  /** The discount factor */
  protected NumExp gamma=new NumExp(0.9);
  /** Specifies method "residual", "resGrad" or "direct" */
  protected PString methodStr=new PString("resGrad");
  /** The mode of learning: incremental or epoch-wise. */
  protected PBoolean incremental = new PBoolean(true);
  /** The percentage of time a random action is chosen for training. */
  protected NumExp exploration=new NumExp(1);
  /** Used to tell ReinforcementLearning if this algorithm uses states only (as opposed to state/action pairs).
    * If the RL algorithm being implemented uses only states, then this variable must be set to true. */
  protected boolean statesOnly=false;
  /** Are we at an absorbing state? Used doing batch training where the length of a trajectory is the size of a batch. */
  protected PBoolean endTrajectory= new PBoolean(false);
  /** A flag stating whether or not we know for certain the value of a state. */
  protected PBoolean valueKnown=new PBoolean(false);
  /** Should we follow trajectories. */
  protected PBoolean trajectories = new PBoolean(false);

  /** Used to initialize the inputs, state, and action vectors in all RL algorithm objects (not ReinforcementLearning).*/
  public abstract void initVects(MDP mpd,RLErrFun rl);

}