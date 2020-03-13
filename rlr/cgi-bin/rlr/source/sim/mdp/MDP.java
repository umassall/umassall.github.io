package sim.mdp;
import watch.*;
import parse.*;
import matrix.*;
import pointer.*;
import sim.funApp.*;
import expression.*;
import Random;

/** a Markov Decision Process or Markov Game that takes a state and action
  * and returns a new state and a reinforcement.  It can be either
  * deterministic or nondeterministic.  If the next state is
  * fed back in as the state, it can run a simulation.  If the
  * state is repeatedly randomized, it can be used for learning
  * with random transitions.  If an MDP class is written for which
  * an optimal policy and value function are known, then
  * findAction() and findValue() will return them, otherwise
  * they just return null and zero respectively.
  * Revision 1.01 added the state parameter to the findValAct method
  *    <p>This code is (c) 1996 Leemon Baird and Mance Harmon
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.11, 22 July 97
  * @author Leemon Baird
  * @author Mance Harmon
  */
public abstract class MDP implements Watchable, Parsable {
  //Version 1.11 22 July 97: Added Random as parameter to methods that might use a number generator - MH
  //Version 1.10 17 June 97: Added explorationFactor to findValue().  This is needed when training on
  //trajectories and exploring.

  //Version 1.09 18 November 96: added state as a parameter to initialAction() and getAction().

  //Version 1.08 13 November 96: added the action parameter to findValue().  This allows findValue()
  //to pass back the action associated with the value of a given state.

  //Version 1.07 6 November 96: changed dt from double to PDouble as a parameter.  Changed findValue()
  //and findValAct() so that the function does not have to be evaluated as a last step before returning
  //from method.

  //Version 1.06 31 October 96: added a demo() method for viewing the policy.

  //Version 1.05 24 October 96: gamma is added as a parameter passed into findValue(). - Mance Harmon

  //Version 1.04 23 October 96: Changed randomAction() to also be passed in the state.  The random
  //action possible could be dependent upon the state.

  //Version 1.03 22 October 96: Added the findValue() abstract method to address the needs
  //of value iteration. - Mance Harmon

  //Version 1.02 11 October 96  Changed all PFloats to PDoubles - Mance Harmon

  //Version 1.01 2 October 96

  /** the WatchManager that variables here may be registered with*/
  protected WatchManager watchManager=null;
  /** the prefix string for the name of every watched variable (passed in to setWatchManager) */
  protected String wmName=null;
  /** a state vector (created in parse())*/
  protected MatrixD state=null;
  /** an action vector (created in parse())*/
  protected MatrixD action=null;
  /** the state vector resulting from doing action in state (created in parse())*/
  protected MatrixD nextState=null;

  /** Register all variables with this WatchManager.
    * Override this if there are internal variables that
    * should be registered here.
    */
  public void setWatchManager(WatchManager wm,String name) {
    watchManager=wm;
    wmName=name;
    wm.registerParameters(this,name);
  }

  /** Return the variable "name" that was passed into setWatchManager */
  public String getName() {
    return wmName;
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** Return the number of states in the given MDP. If the number of states is infinite, then
    * a parameter to the MDP should be defined that is the sample size of a pseudo-epoch.  In
    * other words, an artificial epoch size should be passed as a parameter to the MDP object
    * that defines the length of an epoch.  If the number of states is finite, then the number
    * of states is a function of the time step size dt.  For this reason a step size dt is passed
    * into this object. */
  public abstract int numStates(PDouble dt);

  /** Return the number of elements in the state vector. */
  public abstract int stateSize();

  /** Return an initial state used for the start of epoch-wise training or for
    * training on trajectories.
    * This might not be a single state but could be a set of starting states.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void initialState(MatrixD state,Random random) throws MatrixException;

  /** Return the next state to be used for training in an epoch-wise system.
    * This method is different than nextState() in that nextState() returns the state
    * transitioned to as a function of the dynamics of the system.  This object simply
    * returns another state to be trained upon when performing epoch-wise training.  This
    * method should incrementally return unique states until all states in an epoch have
    * been used for training.  For example: if state space consists of 20 unique states, then
    * this method will return a unique state until all 20 states have been return.  The method
    * would then start over in a new series of the same 20 states.  The parameters are the last
    * state used and a time step size.  In short, this is an iterator over all states in state space.
    * If state space is infinite this method should not be used and is not meaningful.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void getState(MatrixD state, PDouble dt,Random random) throws MatrixException;

  /** Return the number of elements in the action vector. */
  public abstract int actionSize();

  /** Return the initial action possible in a state. This method is used when one has to iterate
    * over all possible actions in a given state.  Given a state, this method should return the
    * initial action possible in the given state.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException;

  /** Return the next action possible in a state given the last action performed.
    * This performs the same function as that of getState() in the sense that this serves
    * as an iterator over actions instead of states.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException;

  /** Return the number of actions in a given state. For simplicity this should be the same
    * for all states.  However, the state is being passed in to this method so that future
    * code can take advantage of this parameter if necessary. */
  public abstract int numActions(MatrixD state);

  /** Return the number of state/action pairs in the MDP for a given dt. This is used for epoch-wise
    * training.  An epoch would  consist of all state/action pairs for a given MDP and is a function
    * of the step size dt. */
  public abstract int numPairs(PDouble dt);


  /** Generates a random action from those possible.  Accepts a state and passes back an action.
    * Each action variable should be on a seperate row.  action should be a vector (single-column matrix): Nx1
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException;

  /** Generates a random state from those possible and returns it in the vector passed in.
    * This should NOT include terminal states where the value is known.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public abstract void randomState(MatrixD state,Random random) throws MatrixException;

  /** Find a (possibly stochastic) next state given a state and action,
    * and return the (possibly stochastic) reinforcement received.
    * All 3 should be vectors (single-column matrices).
    * The duration of the time step, dt, is also returned.  Most MDPs
    * will generally make this a constant, given in the parsed string.
    * If the resulting states value is perfectly known then the flag valueKnown should be
    * set to true.
    * @exception MatrixException if sizes aren't right.
    */
  public abstract double nextState(MatrixD state,MatrixD action,
                         MatrixD newState,PDouble dt,PBoolean valueKnown,Random random) throws MatrixException;

  /** Find the value and best action of this state.  This returns the value of a given state as a double.
    * This also destroys the action that is passed in by replacing it with the best action.  This
    * method always returns a value that is a function of state/action pairs.  The value associated with
    * these state/action pairs might be Q-values or advantages, but it is not important to know which
    * learning algorithm is being used.  This method should simply find the min or max value as a function
    * of the state/action pairs in the given state.  For example, if Q-learning is the learning algorithm,
    * then one would find the max Q-value for the given state and return that value.
    * The action associated with that Q-value would be passed back.  The state/action pair with the
    * max Q-value should be evaluated last so that findGradients() can be called from within
    * the learning algorithm without having to call function.evaluate().
    * @exception MatrixException column vectors are wrong size or shape
    */
  public abstract double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown)
  throws MatrixException;

  /** Find the max over action for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  This method is used in
    * the object ValIteration (value iteration). The max value over actions (<R+gammaV(x')>) is returned.
    * The state reached after performing the optimal action should be returned 'explorationFactor' percent of
    * the time in the parameter 'state'.  The state resulting from a random action will be returned
    * 1-explorationFactor percent of the time.  The possibility of explorationFactor==null must be handled.
    * The action parameter must be checked for a null value before implementing.  The learning
    * object 'ValueIteration' passes in a null in the place 'action'.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public abstract double findValue(MatrixD state,MatrixD action,PDouble gamma,FunApp f,PDouble dt,MatrixD outputs,
                                   PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random)
  throws MatrixException;

 // public abstract void demo(MatrixD state,MatrixD action,MatrixD newState,FunApp function,PDouble dt,MatrixD outputs)
 // throws MatrixException; /**/

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public abstract String BNF(int lang);

  /** Output a description of this object that can be parsed with parse().
    * Also creates the state/action/nextState vectors
    * @see Parsable
    */
  public abstract void unparse(Unparser u, int lang);

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public abstract Object parse(Parser p,int lang) throws ParserException;

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
  }
}
