package sim.mdp;
import watch.*;
import parse.*;
import matrix.*;
import pointer.*;
import Random;
import sim.funApp.*;
import expression.*;

/** A Markov Decision Process that takes a state and action
  * and returns a new state and a reinforcement.  This MDP is deterministic.
  * The state space consists of 4 states: {[0,0],[0,1],[1,0],[1,1]}. In state
  * [0,0] there are two possible actions: go left and transition to state [0,1], or
  * go right and transition to state [1,0].  Each action returns a reinforcement of -1.
  * Both states [0,1] and [1,0] have a single action that transitions to state [1,1] and
  * returns a reinforcement of 1.  State [1,1] is defined to have a value of 0.  This
  * is the analog of the XOR problem for supervised learning systems.
  *
  *
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.aa.wpafb.af.mil/~harmonme>http://www.aa.wpafb.af.mil/~harmonme</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.01, 17 June 97
  * @author Mance Harmon
  */
public class XORmdp extends MDP {
  //Version 1.01 17 June 97: Added explorationFactor to findValue()
  //Version 1.00 8 January 97

  /** Return the number of states for this mdp. This does not include the terminal state [1,1].*/
  public int numStates(PDouble dt) {
  return 4;
  }

  /** Return the number of elements in the state vector. */
  public int stateSize() {
    return 2;
  }

  /** Return a start state for epoch-wise training.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialState(MatrixD state,Random random) throws MatrixException {
    state.set(0,0);  state.set(1,0);
  }

  /** Return the next state when doing epoch-wise training.
    * If the state passed in is [1,1] then the next state is [0,0].
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getState(MatrixD state, PDouble dt,Random random) throws MatrixException {
    if((state.val(0)==1) && (state.val(1)==1)) {state.set(0,0); state.set(1,0);
    } else if((state.val(0)==0) && (state.val(1)==1)) {state.set(0,1); state.set(1,0); //if in 00 goto 01
    } else if((state.val(0)==1) && (state.val(1)==0)) {state.set(0,1); state.set(1,1);
    } else if((state.val(0)==0) && (state.val(1)==0)) {state.set(0,0); state.set(1,1);
    }

  }

  /** Return the number of elements in the action vector. */
  public int actionSize() {
    return 1;
  }

  /** Return the number of actions in each state. */
  public int numActions(MatrixD state) {
    try{
      if((state.val(0)==0) && (state.val(1)==0))
        return 2;
      else
        return 1;
    } catch (MatrixException e) {
        e.print();
    }
    System.out.println("Error in mdp.numActions");
    return 0;
  }

  /** Return an initial action possible in a given state.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    action.set(0,1);
  }

  /** Return the next possible action in a state given an action.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    if ((state.val(0)==0) && (state.val(1)==0))
      if (action.val(0)==1) action.set(0,-1); else action.set(0,1);
    else action.set(0,1);
  }

  /** Return the number of state/action pairs for a given dt.*/
  public int numPairs(PDouble dt) {
    return 5;
  }

  /** Generates a random action from those possible.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    if ((state.val(0)==0) && (state.val(1)==0))
      if (random.nextDouble()>0.5) action.set(0,-1); else action.set(0,1);
    else action.set(0,1);
  }

  /** Generates a random state from those possible.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomState(MatrixD state,Random random) throws MatrixException {
    if(random.nextDouble()>0.75) {state.set(0,1); state.set(1,1);}
    else if(random.nextDouble()>0.50) {state.set(0,0); state.set(1,1);}
    else if(random.nextDouble()>0.25) {state.set(0,1); state.set(1,0);}
    else {state.set(0,0); state.set(1,0);}
  }

  /** Find a next state given a state and action,
    * and return the reinforcement received.
    * All 3 should be vectors (single-column matrices).
    * The duration of the time step, dt, is also returned.  Most MDPs
    * will generally make this a constant, given in the parsed string.
    * @exception MatrixException if sizes aren't right.
    */
  public double nextState(MatrixD state,MatrixD action,
                         MatrixD newState,PDouble dt,PBoolean valueKnown,Random random) throws MatrixException {
    if((state.val(0)==0) && (state.val(1)==0) && (action.val(0)==1)) {
      newState.set(0,0); newState.set(1,1); return -1;}
    if((state.val(0)==0) && (state.val(1)==0) && (action.val(0)==-1)) {
      newState.set(0,1); newState.set(1,0); return -1;}
    if(((state.val(0)==1) && (state.val(1)==0)) || ((state.val(0)==0) && (state.val(1)==1))) {
      newState.set(0,1); newState.set(1,1); return 1;}
    if((state.val(0)==1) && (state.val(1)==1)) {
      newState.replace(state); return 0;}
    System.out.println("Error in nextState"); return 0;
  }

  /** Find the value and best action of this state.  This corrupts the original action passed in
    * by returning in its place the best action for the given state.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown)
  throws MatrixException {
    double left, right;

    if((state.val(0)==0) && (state.val(1)==0)){
      //find Q value for action(left)
        action.set(0,-1);
        f.evaluate();
        left=outputs.val(0);
      //find Q value for action(right)
        action.set(0,1);
        f.evaluate();
        right=outputs.val(0);
      //find minimum, set action, and return Qmin value
        if (left<right) {
        	action.set(0,-1);
        	return left;
        } else return right;
     } else
        if((state.val(0)==1) && (state.val(1)==1)){
          valueKnown.val=true;
          action.set(0,1);
          return 0;
        } else {
          action.set(0,1);
          f.evaluate();
          return outputs.val(0);}
  }

  /** Find the max over action for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  This method is used in
    * the object ValueIteration.  The new state is returned in the state variable, reinforcemet is
    * returned in the reinforcement parameter, the optimal action is returned as a parameter, and
    * the max value is returned.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValue(MatrixD state,MatrixD optAction,PDouble gamma,FunApp f,PDouble dt,
  MatrixD outputs,PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random) throws MatrixException {
    MatrixD action=new MatrixD(actionSize());
    MatrixD newX=new MatrixD(stateSize());
    PBoolean vKnown=new PBoolean(false);
    MatrixD originalX=new MatrixD(stateSize());
    double r;
    double left,right;

    if(optAction!=null) optAction.set(0,1);
    if((state.val(1)==1) && (state.val(0)==1)) { // state [1,1]
        valueKnown.val=true;
        reinforcement.val=0;
        return 0;
    } else
    if((state.val(1)==1) || (state.val(0)==1)) { // states [1,0] and [0,1]
        reinforcement.val=1;
        state.set(0,1);
        state.set(1,1);
        f.evaluate();
        return reinforcement.val+gamma.val*outputs.val(0);
    }

    originalX.replace(state);
    if((state.val(0)==0) && (state.val(1)==0)) { //state [0,0]
      action.set(0,-1);
      r=nextState(state,action,newX,dt,vKnown,random);
      state.replace(newX);
      f.evaluate();
      left=r+gamma.val*outputs.val(0);
      state.replace(originalX);
      action.set(0,1);
      r=nextState(state,action,newX,dt,vKnown,random);
      state.replace(newX);
      f.evaluate();
      right=r+gamma.val*outputs.val(0);
      state.replace(originalX);
      reinforcement.val=r;
      if(left>right) {
        if(optAction!=null) optAction.set(0,1);

        if (explorationFactor!=null)
        if (random.nextDouble()<explorationFactor.val)
          if (random.nextDouble()<0.5) {state.set(0,0); state.set(1,1);}
          else {state.set(0,1); state.set(1,0);}
        else {state.set(0,0);state.set(1,1);}

        return right;
      } else {
        if(optAction!=null) optAction.set(0,-1);

        if (explorationFactor!=null)
        if (random.nextDouble()<explorationFactor.val)
          if (random.nextDouble()<0.5) {state.set(0,0); state.set(1,1);}
          else {state.set(0,1); state.set(1,0);}
        else {state.set(0,1);state.set(1,0);}

        return left;
      }
    }

    System.out.println("Error in findValue"); return 0;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
        return "//XOR Markov Decision Process. ";
  }

  /** Output a description of this object that can be parsed with parse().
    * Also creates the state/action/nextState vectors
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
