package sim.mdp;
import watch.*;
import parse.*;
import matrix.*;
import pointer.*;
import Random;
import sim.funApp.*;
import expression.*;

/** A Markov Decision Process or Markov Game that takes a state and action
  * and returns a new state and a reinforcement.  It can be either
  * deterministic or nondeterministic.  If the next state is
  * fed back in as the state, it can run a simulation.  If the
  * state is repeatedly randomized, it can be used for learning
  * with random transitions.
  *    <p>This code is (c) 1996 Leemon Baird and Mance Harmon
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.04, 25 June 97
  * @author Mance Harmon
  */
public class GridWorld extends MDP {
//Version 1.04 25 June 97: Made state [1,1] truly absorbing.  All actions in [1,1] lead back to [1,1].
//Version 1.03 17 June 97: Added explorationFactor to findValue().
//Version 1.02 18 November 96: added state as a parameter to getAction() and nextAction().
//Version 1.01 14 November 96: Added functionality to findValue().
//Version 1.00 24 October 96 - Mance Harmon

  /** the WatchManager that variables here may be registered with*/
  protected WatchManager watchManager=null;
  /** a state vector (created in parse())*/
  protected MatrixD state=null;
  /** an action vector (created in parse())*/
  protected MatrixD action=null;
  /** the state vector resulting from doing action in state (created in parse())*/
  protected MatrixD nextState=null;
  /** The random number generator */
  protected Random random=new Random(0);
  /** The depth in both the x and y dimension of the gridworld. */
  protected IntExp granFactor=new IntExp(10);


   /** Counters used in epoch-wise training */
  protected int count1;
  protected int count2;

  static final double eps=1e-5;
  static final double infinity=1e20;

  /** Register all variables with this WatchManager.
    * Override this if there are internal variables that
    * should be registered here.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    watchManager=wm;
    wm.registerVar(name+"granularity", granFactor,this);
  }

  /** Return the WatchManager set by setWatchManager(). */
  public WatchManager getWatchManager() {
    return watchManager;
  }

  /** The number of states for this MDP is determined by the granularity factor that is passed in as
    * a  parameter.  A granularity of 10 would produce a state space containing 121 states:
    * sqr(granularity+1) */
  public int numStates(PDouble dt) {
    return (int)((1.0/dt.val+1)*(1.0/dt.val+1)+0.5);
  }

  /** Return the number of elements in the state vector. In this case the state is a point
    * (x,y) in a 2D Euclidean space.*/
  public int stateSize(){
    return 2;
  }

  /** Return an initial state used for the start of epoch-wise training or for
    * training on trajectories. The start state for this MDP is the lower left
    * corner of the 2D grid (0,0).
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void initialState(MatrixD state,Random random) throws MatrixException{
    state.set(0,0);
    state.set(1,0);
  }

  /** Return the next state to be used for training in an epoch-wise system.
    * This method is different than nextState() in that nextState() returns the state
    * transitioned to as a function of the dynamics of the system.  This object simply
    * returns another state to be trained upon when performing epoch-wise training.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void getState(MatrixD state, PDouble dt,Random random) throws MatrixException {
    if(count2++<(1.0/dt.val))
      state.set(1,count2*dt.val);
    else {
      count2=0;
      if(count1++<(1.0/dt.val))
        state.set(0,count1*dt.val);
      else {state.set(0,0); count1=0;}
      state.set(1,0);
    }
  }

  /** Return the number of elements in the action vector. The action vector is of length 1 and
    * has 4 possible values: 0 - East, 0.25 - North, 0.5 - West, 0.75 - South. */
  public int actionSize(){
    return 1;
  }

  /** Return the initial action possible in a state. This method is used when one has to iterate
    * over all possible actions in a given state.  Given a state, this method should return the
    * initial action possible in the given state.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException{
    action.set(0,0);
  }

  /** Return the next action possible in a state given the last action performed.
    * This performs the same function as that of getState() in the sense that this serves
    * as an iterator over actions instead of states.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException{
    double oldAction=action.val(0);
    if(oldAction>0.5) action.set(0,0.0);
    else action.set(0,oldAction+0.25);
  }

  /** Return the number of actions in a given state. For this MDP this number is constant for
    * all states.  There are 4 actions possible in each state:
    * 0 - East, 0.25 - North, 0.5 - West, 0.75 - South. */
  public int numActions(MatrixD state){
    return 4;
  }

  /** Return the number of state/action pairs in the MDP for a given dt. This is used for epoch-wise
    * training.  An epoch would consist of all state/action pairs for a given MDP and is a function
    * of the step size dt. For this MDP we have a continuum of state/action pairs because we have
    * a continuum of states.  The value returned from this method will be the pseudo-epoch size
    * passed in to this MDP is the parameter called epochSize.*/
  public int numPairs(PDouble dt){
    return numStates(dt)*4;
  }

  /** Generates a random action from those possible.  Accepts a state and passes back an action.
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException{
    double rand;
    rand=random.nextDouble();
    if(rand>0.75) action.set(0,0.75);
    else if(rand>0.5) action.set(0,0.5);
    else if(rand>0.25) action.set(0,0.25);
    else action.set(0,0.0);
  }

  /** Generates a random state from those possible and returns it in the vector passed in.
    * This returns a vector of length 2.  Each element is in the range [0,1].
    * @exception matrix.MatrixException Vector passed in was wrong length.
    */
  public void randomState(MatrixD state,Random random) throws MatrixException {
    state.set(0,random.nextDouble());
    state.set(1,random.nextDouble());
    state.mult(granFactor.val);
    state.set(0,Math.round(state.val(0)));
    state.set(1,Math.round(state.val(1)));
    state.mult(1.0/granFactor.val);
  }

  /** Find a next state given a state and action, and return the reinforcement received.
    * All 3 should be vectors (single-column matrices).
    * The duration of the time step, dt, is also returned.  Most MDPs
    * will generally make this a constant, given in the parsed string.
    * The goal state is the upper right corner of the grid world (x>1-dt, y>1-dt).
    * @exception MatrixException if sizes aren't right.
    */
  public double nextState(MatrixD state,MatrixD action,
                         MatrixD newState,PDouble dt,PBoolean valueKnown,Random random) throws MatrixException {

    double x,y;
    x=state.val(0);
    y=state.val(1);
    newState.replace(state);

    //East
    if(action.val(0)<0.24){
      if((x>(1-dt.val)+eps)&&(y>(1-dt.val)+eps)) {valueKnown.val=true; return -1;}
      else {
        if((x+dt.val>(1-dt.val)+eps)&&(y>(1-dt.val)+eps)) {newState.set(0,1.0); valueKnown.val=true; return -1;} //enter goal state
        if((x+dt.val>(1-dt.val)+eps)&&(y<=(1-dt.val)+eps)) {newState.set(0,1.0); return 1;} //not in goal state but on boundary
        if(x+dt.val<=(1-dt.val)+eps) {newState.set(0,x+dt.val); return 1;} //not in goal state or on boundary
      }
    }

    //North
    if(action.val(0)<0.49){
      if((x>(1-dt.val)+eps)&&(y>(1-dt.val)+eps)) {valueKnown.val=true; return -1;}
      else {
        if((y+dt.val>(1-dt.val)+eps)&&(x>(1-dt.val)+eps)) {newState.set(1,1.0); valueKnown.val=true; return -1;} //enter goal state
        if((y+dt.val>(1-dt.val)+eps)&&(x<=(1-dt.val)+eps)) {newState.set(1,1.0); return 1;} //not in goal state but on boundary
        if(y+dt.val<=(1-dt.val)+eps) {newState.set(1,y+dt.val); return 1;} //not in goal state or on boundary
      }
    }

    //West
    if(action.val(0)<0.74){
      if((x>(1-dt.val)+eps)&&(y>(1-dt.val)+eps)) {valueKnown.val=true; return -1;}
      else {
        if (x-dt.val<dt.val-eps) {newState.set(0,0.0); return 1;}
        else {newState.set(0,x-dt.val); return 1;}
      }
    }

    //South
    if(action.val(0)<0.99){
      if((x>(1-dt.val)+eps)&&(y>(1-dt.val)+eps)) {valueKnown.val=true; return -1;}
      else {
        if(y-dt.val<dt.val-eps) {newState.set(1,0.0); return 1;}
        else {newState.set(1,y-dt.val); return 1;}
      }
    }
    System.out.println("GridWorld.nextState: OOPS");
    return 0; //this should never be returned.
  }


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
  public double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown)
  throws MatrixException{
    double min1,min2,north,south,east,west;

    if(state.val(0)<1-eps) {
        action.set(0,0);
        f.evaluate();
        east=outputs.val(0);}
    else east=infinity;

    if(state.val(1)<1-eps) {
        action.set(0,0.25);
        f.evaluate();
        north=outputs.val(0);}
    else north=infinity;

    if(state.val(0)>0+eps) {
        action.set(0,0.5);
        f.evaluate();
        west=outputs.val(0);}
    else west=infinity;

    if(state.val(1)>0+eps) {
        action.set(0,0.75);
        f.evaluate();
        south=outputs.val(0);}
    else south=infinity;

    if(east<north) min1=east; else min1=north;
    if(west<south) min2=west; else min2=south;
    if(min1<min2)
      if(east<north)  {action.set(0,0); return east; }
      else            {action.set(0,0.25); return north; }
    else
      if(west<south)  {action.set(0,0.5); return west; }
      else            {action.set(0,0.75); return south; }
  }

  /** Find the max over action for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  This method is used in
    * the object ValIteration (value iteration). The max value over actions (<R+gammaV(x')>) is returned.
    * The state associated with the optimal action is return 1-explorationFactor percent of the time.
    * Otherwise, a random next state is returned.  The next state is passed back in state.
    *
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValue(MatrixD state,MatrixD action,PDouble gamma,FunApp f,PDouble dt,MatrixD outputs,
                          PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random)
  throws MatrixException {

    double min1,min2,north,south,east,west,r,rnd;
    MatrixD newState=new MatrixD(stateSize());
    MatrixD localAction=null;
    MatrixD origX=new MatrixD(stateSize());

    localAction =(action==null) ? new MatrixD(actionSize()): (MatrixD)action.clone();
    origX.replace(state);
    if((state.val(0)>1-eps) && (state.val(1)>1-eps)) {
      localAction.set(0,0);
      valueKnown.val=true;
      return -1;
    } else {
        if(state.val(0)<1-eps) {
            localAction.set(0,0);
            r=nextState(state,localAction,newState,dt,valueKnown,random);
            state.replace(newState);
            f.evaluate();
            east=r+gamma.val*outputs.val(0);
            state.replace(origX);
        } else east=infinity;

        if(state.val(1)<1-eps) {
            localAction.set(0,0.25);
            r=nextState(state,localAction,newState,dt,valueKnown,random);
            state.replace(newState);
            f.evaluate();
            north=r+gamma.val*outputs.val(0);
            state.replace(origX);
        } else north=infinity;

        if(state.val(0)>0+eps) {
            localAction.set(0,0.5);
            r=nextState(state,localAction,newState,dt,valueKnown,random);
            state.replace(newState);
            f.evaluate();
            west=r+gamma.val*outputs.val(0);
            state.replace(origX);
        } else west=infinity;

        if(state.val(1)>0+eps) {
            localAction.set(0,0.75);
            r=nextState(state,localAction,newState,dt,valueKnown,random);
            state.replace(newState);
            f.evaluate();
            south=r+gamma.val*outputs.val(0);
            state.replace(origX);
        } else south=infinity;

        valueKnown.val=false;
        if(east<north) min1=east; else min1=north;
        if(west<south) min2=west; else min2=south;
        if(min1<min2)
          if(east<north)  {
            localAction.set(0,0);
            reinforcement.val=nextState(state,localAction,newState,dt,valueKnown,random);
            //Choose a random nextState
            if (explorationFactor!=null)
            if (random.nextDouble()<explorationFactor.val) {
              rnd = random.nextDouble();
              if (rnd>0.75) localAction.set(0,0.75);
              else if (rnd>0.5) localAction.set(0,0.5);
              else if (rnd>0.25) localAction.set(0,0.25);
              else localAction.set(0,0);
              nextState(state,localAction,newState,dt,valueKnown,random);
            }
            state.replace(newState);
            return east;
          } else {
            localAction.set(0,0.25);
            reinforcement.val=nextState(state,localAction,newState,dt,valueKnown,random);
            //Choose a random nextState
            if (explorationFactor!=null)
            if (random.nextDouble()<explorationFactor.val) {
              rnd = random.nextDouble();
              if (rnd>0.75) localAction.set(0,0.75);
              else if (rnd>0.5) localAction.set(0,0.5);
              else if (rnd>0.25) localAction.set(0,0.25);
              else localAction.set(0,0);
              nextState(state,localAction,newState,dt,valueKnown,random);
            }
            state.replace(newState);
            return north;
        } else if(west<south) {
            localAction.set(0,0.5);
            reinforcement.val=nextState(state,localAction,newState,dt,valueKnown,random);
            //Choose a random nextState
            if (explorationFactor!=null)
            if (random.nextDouble()<explorationFactor.val) {
              rnd = random.nextDouble();
              if (rnd>0.75) localAction.set(0,0.75);
              else if (rnd>0.5) localAction.set(0,0.5);
              else if (rnd>0.25) localAction.set(0,0.25);
              else localAction.set(0,0);
              nextState(state,localAction,newState,dt,valueKnown,random);
            }
            state.replace(newState);
            return west;
          } else {
            localAction.set(0,0.75);
            reinforcement.val=nextState(state,localAction,newState,dt,valueKnown,random);
            //Choose a random nextState
            if (explorationFactor!=null)
            if (random.nextDouble()<explorationFactor.val) {
              rnd = random.nextDouble();
              if (rnd>0.75) localAction.set(0,0.75);
              else if (rnd>0.5) localAction.set(0,0.5);
              else if (rnd>0.25) localAction.set(0,0.25);
              else localAction.set(0,0);
              nextState(state,localAction,newState,dt,valueKnown,random);
            }
            state.replace(newState);
            return south;
            }
    }
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "('granularity' IntExp)"+
    "//2D Continuous Grid World. 'granularity' defines the number of states. "+
    "The size of the grid is sqr(granularity+1) states.";
  }

  /** Output a description of this object that can be parsed with parse().
    * Also creates the state/action/nextState vectors
    * @see Parsable
    */
  public void unparse(Unparser u, int lang){
    u.indent();
    u.emitLine();
    u.emit("granularity ");
      u.emit(granFactor.val);
    u.unindent();
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException{
    if(p.tID.equals("granularity")) {
      p.parseID("granularity",true);
      granFactor=(IntExp)p.parseClass("IntExp",lang,true);
    }
    return this;
  }

}
