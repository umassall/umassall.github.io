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
  * The state space is the interval [-1,1], discretized by the time step dt.
  * An agent sits on one of the discrete states defined by dt.  The agent can
  * perform 2 possible actions: go left (-1), and go right (1).  The reinforcement
  * returned after performing an action is the new position on the number line squared.
  * The object of this MDP is to minimize total discounted reinforcement.
  *
  * If the parameter discrete is not set to true, then epochSize should be
  * set if the experiment is using epochwise training.  This might be the
  * case if, for example, an inherently epochwise method such as conjugate gradient
  * is the learning algorithm.  In that case, the incremental parameter for the experiment,
  * assuming one exists, would be set to false.  However, the discrete parameter of LQR
  * could be set to false and a number of gradients to average over would be defined using
  * epochSize.  If discrete is set to false then epochSize is ignored.
  *
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.aa.wpafb.af.mil/~harmonme>http://www.aa.wpafb.af.mil/~harmonme</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.27, 21 July 97
  * @author Mance Harmon
  */
public class LQR extends MDP {
//Version 1.27 21 July 97: deleted global Random and added Random as parameter to method calls. - MH
//Version 1.26 17 June 97: added explorationFactor to findValue()

//Version 1.25 18 November 96: added state as a parameter to getAction() and nextAction().

//Version 1.24 13 November 96: added the action parameter to findValue().  This allows findValue() to
//pass back the action associated with the optimal value.

//Version 1.23 6 November 96: changed dt from double to PDouble as a parameter.  Changed findValue()
//and findValAct() so that the function does not have to be evaluated as a last step before returning
//from method.

  //Version 1.22 24 October 96: 1) Changed the initialState() method to pass back either a -1 or a 1
  //instead of always passing back a -1 for the initial state. 2) gamma is added as a
  //parameter passed into findValue()- Mance Harmon

  //Version 1.21 23 October 96: Changed randomAction() to conform to new spec in MDP. - Mance Harmon

  //Version 1.20 21 October 96: Added the findValue() method to address the needs of value
  //iteration. -Mance Harmon

  //Version 1.12 17 October 96 Fixed bug in getState.  Look in method for details.  Also, added
  //boundary checking for state space.  If an action causes a transition across a boundary it now leads
  //back to the state from which it left.

  //Version 1.11 11 October 96 Changed all parameters to methods to PDouble from PFloat.  This is
  //in compliance with the new MDP spec.

  //Version 1.10 9 October 96 Adds the parameters discrete and epochSize.  This changes the MDP
  //from one with discrete states to continuous states.

  //Version 1.01 4 October 96 Adds 0.5 to numPairs() and numStates methods to ensure that the
  //cast from float to int will give the desired number.

  /** is the state space continuous or discrete */
  protected boolean discrete=true;
  /** Size of the epoch.  Only needed when doing epochwise training on continuous state space */
  protected IntExp epochSize=new IntExp(100);

  /** Register all variables with this WatchManager.
    * Override this if there are internal variables that
    * should be registered here.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    watchManager=wm;
  }

  /** Return the number of states in this LQR for a given dt.  dt must evenly divide 2. */
  public int numStates(PDouble dt) {
    if(discrete) return (int)((2/dt.val)+1.5);
    else return epochSize.val;
  }

  /** Return the number of elements in the state vector. */
  public int stateSize() {
    return 1;
  }

  /** Return a start state for epoch-wise training.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialState(MatrixD state,Random random) throws MatrixException {
    if(random.nextDouble()>0.5) state.set(0,-1); else state.set(0,1);
  }

  /** Return the next state when doing epoch-wise training.
    * If the state passed in is 1 then the next state is -1.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getState(MatrixD state, PDouble dt,Random random) throws MatrixException {
    if(discrete) {
      state.set(0,state.val(0)+dt.val);
      if (state.val(0)>1.01) state.set(0,-1); //this test must be 1.01 and not 1.0 because the internal representation
                                               //used for MatrixD gives a 1>1.0 in this test.
    }
    else randomState(state,random);
  }

  /** Return the number of elements in the action vector. */
  public int actionSize() {
    return 1;
  }

  /** Return the number of actions in each state. */
  public int numActions(MatrixD state) {
    return 2;
  }

  /** Return an initial action possible in a given state.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    action.set(0,-1);
  }

  /** Return the next possible action in a state given an action.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    if (action.val(0)==-1) action.set(0,1); else action.set(0,-1);
  }

  /** Return the number of state/action pairs for a given dt.
    * This only works for dt's in which 2 is evenly divisible by dt. */
  public int numPairs(PDouble dt) {
    if(discrete) return (int)((4/dt.val)+0.5); //2 action in each state -> numStates=1-(-1)/dt -> 2*numStates=4/dt
    else return 2*epochSize.val;
  }

  /** Generates a random action from those possible.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
  	if (random.nextDouble()<0.5) action.set(0,1); else action.set(0,-1);
  }

  /** Generates a random state from those possible.
    * This doesn't generate random states with uniform probability.  States
    * -1 and 1 are half as likely as the other states.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomState(MatrixD state,Random random) throws MatrixException {
 //   if(discrete){
      state.set(0,(2*random.nextDouble())-1);  //generates a random state between [-1,1]
      state.mult(10);
      state.set(0,Math.round(state.val(0)));
      state.set(0,state.val(0)/10);
 //   } else state.set(0,(2*random.nextDouble())-1); //generates a random state between [-1,1]
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
    newState.replace(state);			//initialize newState
    newState.addMult(dt.val,action);	//x(new) = x(old) + u*dt
    if((newState.val(0)>1.0) || (newState.val(0)<-1.0)) { //check for boundary conditions
        newState.replace(state);
    }
    if((newState.val(0)>-1e-5) && (newState.val(0)<1e-5)) valueKnown.val=true; /**/
    return newState.val(0)*newState.val(0);  //return the reinforcement (new position squared)

    /*newState.replace(state);			//initialize newState
    if((newState.val(0)>-1e-5) && (newState.val(0)<1e-5)) valueKnown.val=true;

    newState.addMult(dt.val,action);	//x(new) = x(old) + u*dt
    if(newState.val(0)<-1.0) newState.set(0,-1.0);
    if(newState.val(0)>1.0)  newState.set(0,1.0);

    return newState.val(0)*newState.val(0);  //return the reinforcement (new position squared) */
  }

  /** Find the value and best action of this state.  This corrupts the original action passed in
    * by returning in its place the best action for the given state.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown) throws MatrixException {
    double left, right;


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
  }

  /** Find the max over action for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  This method is used in
    * the object ValIter (value iteration).
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValue(MatrixD state,MatrixD optAction,PDouble gamma,FunApp f,PDouble dt,MatrixD outputs,
                          PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random) throws MatrixException {
    PBoolean vKnown=new PBoolean(false);
    MatrixD newX=new MatrixD(stateSize());
    MatrixD action=new MatrixD(actionSize());
    MatrixD originalX=new MatrixD(stateSize());
    MatrixD minX=new MatrixD(stateSize());
    double r;
    double Vx=0;
    double minV=1e20;

    originalX.replace(state);
    initialAction(state,action,random);
    for(int j=0; j<numActions(state); j++){
       r=nextState(originalX,action,newX,dt,vKnown,random);
       state.replace(newX);
       f.evaluate();
       Vx=outputs.val(0);
       if(minV>r+gamma.val*Vx) {
         minV=r+gamma.val*Vx;
         minX.replace(newX);
         reinforcement.val=r;
         minX.replace(newX);
         if(optAction!=null) optAction.replace(action);
       }
       getAction(state,action,random);
    }
    if (explorationFactor!=null)
    if (random.nextDouble()<explorationFactor.val) {
        randomAction(originalX,action,random);
        nextState(originalX,action,newX,dt,vKnown,random);
        state.replace(newX);
    } else state.replace(minX);
    if((minX.val(0)>-1e-5) && (minX.val(0)<1e-5)) valueKnown.val=true;
    return minV;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
        return "('discrete' <boolean> 'epochSize' IntExp)*"+
        "//Linear-Quadratic Regulator. epochSize must "+
        "be set if discrete=false and doing epochwise training.";
  }

  /** Output a description of this object that can be parsed with parse().
    * Also creates the state/action/nextState vectors
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if(!discrete){
    u.indent();
    u.emitLine();
    u.emit("discrete ");
      u.emit(discrete);
      u.emitLine();
    u.emit("epochSize ");
      u.emitUnparse(epochSize,lang);
    u.unindent();}
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    while (true) { //parse whatever parameters are there
      if (p.tID.equals("discrete")) {
        p.parseID("discrete",true);
        discrete=p.parseBoolean(true);
      } else if (p.tID.equals("epochSize")) {
        p.parseID("epochSize",true);
        epochSize=(IntExp)p.parseClass("IntExp",lang,true);
      } else break;
    }
      return this;
  }
}
