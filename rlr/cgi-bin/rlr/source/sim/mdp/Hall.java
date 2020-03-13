package sim.mdp;
import watch.*;
import parse.*;
import matrix.*;
import pointer.*;
import Random;
import sim.funApp.*;
import expression.*;

/** A Markov Chain that takes a state and returns a new state and a reinforcement.
  * If the next state is fed back in as the state, it can run a simulation.  If the
  * state is repeatedly randomized, it can be used for learning
  * with random transitions. The chain has boundaries of [-1,1].  The number of
  * states is dependent upon dt.  The reinforcement on each transition is 1 and state
  * 1 is an absorbing state with a value of 0.
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.aa.wpafb.af.mil/~harmonme>http://www.aa.wpafb.af.mil/~harmonme</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.06, 17 June 97
  * @author Mance Harmon
  */
public class Hall extends MDP {
//Version 1.06 17 June 97: Added explorationFactor in findValue()
//Version 1.05 18 November 96: added state as a parameter to getAction() and nextAction().

//Version 1.04 6 November 96: changed dt from double to PDouble as a parameter.  Changed findValue()
//and findValAct() so that the function does not have to be evaluated as a last step before returning
//from method.

  //Version 1.03 23 October 96: Changed randomAction() to conform to new spec in MDP.
  //Version 1.02 22 October 96: Added the unimplemented method findValue(). - Mance Harmon

  //Version 1.01 11 October 96 Changed all parameters passed to methods from PFloat to PDouble.
  //This is in compliance with the new MDP specs. - Mance Harmon

  //Version 1.0 4 October 96

  /** The random number generator */
  protected Random random=new Random(0);

  /** Return the number of states in this LQR for a given dt. The state space is the number line
    * from [-1,1] and is discretized in units of dt.*/
  public int numStates(PDouble dt) {
    return (int)((2/dt.val)+0.5); //the 0.5 is to ensure that the truncated value
  }                                //is 2/dt.val

  /** Return the number of elements in the state vector. */
  public int stateSize() {
    return 1;
  }

  /** Return a start state for epoch-wise training.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void initialState(MatrixD state,Random random) throws MatrixException {
    state.set(0,-1);
  }

  /** Return the next state when doing epoch-wise training.
    * This functions as a circular queue.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void getState(MatrixD state, PDouble dt,Random random) throws MatrixException {
    state.set(0,state.val(0)+dt.val);
    if (state.val(0)>0.99999) state.set(0,-1);
  }

  /** Return the number of elements in the action vector. */
  public int actionSize() {
    return 1;
  }

  /** Return the number of actions in each state. */
  public int numActions(MatrixD state) {
    return 1;
  }

  /** Return an initial action possible in a given state.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    action.set(0,1);
  }

  /** Return the next possible action in a state given an action.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    action.set(0,1);
  }

  /** Return the number of state/action pairs for a given dt.
    * This only works for dt's in which 2 is evenly divisible by dt.
    */
  public int numPairs(PDouble dt) {
   return (int)((2/dt.val)+0.5); //1 action in each state -> numStates=1-(-1)/dt :don't count the absorbing state
  }

  /** Generates a random action from those possible.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
  	action.set(0,1);
  }

  /** Generates a random state from those possible.
    * @exception matrix.MatrixException Vector is the wrong length.
    */
  public void randomState(MatrixD state,Random random) throws MatrixException {
    state.set(0,(2*random.nextDouble())-1);  //generates a random state between [-1,1]
    state.mult(10);
    state.set(0,Math.floor(state.val(0)));
    state.set(0,state.val(0)/10);
  }

  /** Find the next state given a state and action,
    * and return the reinforcement received.
    * All 3 should be vectors (single-column matrices).
    * The duration of the time step, dt, is also returned.
    * @exception MatrixException if sizes aren't right.
    */
  public double nextState(MatrixD state,MatrixD action,
                         MatrixD newState,PDouble dt,PBoolean valueKnown,Random random) throws MatrixException {
    newState.replace(state);			//initialize newState
    if(newState.val(0)>0.999) {
        valueKnown.val=true;
        newState.addMult(dt.val,action);
        return 0;
    }
    else newState.addMult(dt.val,action);	//x(new) = x(old) + u*dt
    return 1;  //return the reinforcement
  }

  /** Find the value and best action of this state.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown) throws MatrixException {
    if(state.val(0)>0.999) {
        valueKnown.val=true;
        return 0;
    } else f.evaluate();
    return outputs.val(0);
  }


  /** Find the max over actions for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  Return <R+gammaV(x')>
    * This method is used in the object ValIteration (value iteration).  The last  performed in this
    * method should be done for the successor state associate with the optimal action.
    *
    * In this case there is only one action.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValue(MatrixD state,MatrixD action,PDouble gamma,FunApp f,PDouble dt,MatrixD outputs,
                          PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random)
  throws MatrixException {

    MatrixD optAction=new MatrixD(1);
    optAction.set(0,1);
    state.addMult(dt.val,optAction);
    f.evaluate();
    reinforcement.val=1;
    if(state.val(0)>0.999) {
        reinforcement.val=0;
        valueKnown.val=true;
        return 0;
    } else return reinforcement.val+gamma.val*outputs.val(0);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
        return "//A Hall Markov chain. Starts in state -1, "+
               "deterministically transitions from x to x+dt. "+
               "Final state 1 is absorbing. Reinforcement is always 0.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
  	return this;
  }
}
