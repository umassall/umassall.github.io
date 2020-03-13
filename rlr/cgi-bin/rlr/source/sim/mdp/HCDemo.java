package sim.mdp;
import watch.*;
import parse.*;
import matrix.*;
import pointer.*;
import Random;
import sim.funApp.*;
import expression.*;

/** A Markov Decision Process that takes a state and action
  * and returns a new state and a reinforcement.  This is a module used to demonstrate
  * the capabilities of the VRMLInterface module.  This module was created by changing
  * the HC mdp module.  The policy was hardcoded so that the missile move directly toward
  * the plane and the plane would move at a right angle to the missile.
  *
  *
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:mharmon@acm.org>mharmon@acm.org</a>>,
  *    <a href=http://eureka1.aa.wpafb.af.mil>http://eureka1.aa.wpafb.af.mil</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.0, 13 May 97
  * @author Mance Harmon
  */
public class HCDemo extends MDP {

  /** Size of the epoch.  Only needed when doing epochwise training on continuous state space */
  protected IntExp epochSize=new IntExp(-1);

  private static final double pi = 3.1415926;
  private static final double deg2rad = pi/180;
  private static final int MAction = 0;
  private static final int PAction = 1;
  private static final double speed = 1;

  private PDouble missileX = new PDouble(0); //position of missile
  private PDouble missileY = new PDouble(0);
  private PDouble vmx = new PDouble(0); //velocity of missile
  private PDouble vmy = new PDouble(0);

  private PDouble planeX = new PDouble(0);
  private PDouble planeY = new PDouble(0);
  private PDouble vpx = new PDouble(0);
  private PDouble vpy = new PDouble(0);

  private PDouble thetaP = new PDouble(0); //heading of plane
  private PDouble thetaM = new PDouble(0);


  /** Register all variables with this WatchManager.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"missileX",  missileX,      this);
    wm.registerVar(name+"missileY",  missileY,      this);
    wm.registerVar(name+"planeX",    planeX,        this);
    wm.registerVar(name+"planeY",    planeY,        this);
    wm.registerVar(name+"vmx",       vmx,           this);
    wm.registerVar(name+"vmy",       vmy,           this);
    wm.registerVar(name+"vpx",       vpx,           this);
    wm.registerVar(name+"vpy",       vpy,           this);
    wm.registerVar(name+"thetaP",    thetaP,        this);
    wm.registerVar(name+"thetaM",    thetaM,        this);
  }


  /** Return the number of states in this MDP.  This will always be epochSize because state space is continuous. */
  public int numStates(PDouble dt) {
    return epochSize.val;
  }

  /** Return the number of elements in the state vector. */
  public int stateSize() {
    return 4;
  }

  /** Return a start state for epoch-wise training.  This is actually NOT the state, but rather the difference
    * in the state variables of the two players.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialState(MatrixD state,Random random) throws MatrixException {
    randomState(state,random);
  }

  /** Return the next state when doing epoch-wise training.
    * Because this MDP is defined with continuous state space, this simply returns a random state.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getState(MatrixD state, PDouble dt,Random random) throws MatrixException {
    randomState(state,random);
  }

  /** Return the number of elements in the action vector. */
  public int actionSize() {
    return 2;
  }

  /** Return the number of actions in each state. */
  public int numActions(MatrixD state) {
    return 4;
  }

  /** Return an initial action possible in a given state.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void initialAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    action.set(MAction,-0.1);
    action.set(PAction,-0.1);
  }

  /** Return the next possible action in a state given an action.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void getAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
    if ((action.val(MAction)==-0.1) && (action.val(PAction)==-0.1)) action.set(PAction,0.1);
    else if ((action.val(MAction)==-0.1) && (action.val(PAction)==0.1)) {action.set(MAction,0.1); action.set(PAction,-0.1);}
    else if ((action.val(MAction)==0.1) && (action.val(PAction)==-0.1)) action.set(PAction,0.1);
    else if ((action.val(MAction)==0.1) && (action.val(PAction)==0.1)) {action.set(MAction,-0.1); action.set(PAction,-0.1);}
  }

  /** Return the number of state/action pairs for a given dt.
    * Because we have continuous states, this returns the number of actions in a given state (4)
    * times the pseudo-epoch size passed in to this as a parameter.*/
  public int numPairs(PDouble dt) {
    return 4*epochSize.val;
  }

  /** Generates a random action from those possible: (missile,plane) {(-1,-1),(-1,1),(1,-1),(1,1)}
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomAction(MatrixD state,MatrixD action,Random random) throws MatrixException {
  	double dt = 0.1;
  	double angle=Math.atan(state.val(1)/(state.val(0)==0?0.001:state.val(0)));

  	action.set(MAction,(angle - thetaM.val)/(2*pi));
  	while(action.val(MAction)>1) action.set(MAction,action.val(MAction)-1);
  	while(action.val(MAction)<-1) action.set(MAction,action.val(MAction)+1);
  	action.set(MAction,action.val(MAction)/dt);

  	action.set(PAction,(angle - thetaP.val + pi/2)/(2*pi));
  	while(action.val(PAction)>1) action.set(PAction,action.val(PAction)-1);
  	while(action.val(PAction)<-1) action.set(PAction,action.val(PAction)+1);
  	action.set(PAction,action.val(PAction)/dt);
  }

  /** Generates a random state from those possible.
    * @exception matrix.MatrixException Vector is wrong length. */
  public void randomState(MatrixD state,Random random) throws MatrixException {
    missileX.val = random.nextDouble()*100;
    missileY.val = random.nextDouble()*100;
    planeX.val = random.nextDouble()*100;
    planeY.val = random.nextDouble()*100;

    thetaP.val = random.nextDouble()*180*deg2rad;
    thetaM.val = random.nextDouble()*180*deg2rad;

    vmx.val = speed*Math.cos(thetaM.val);
    vmy.val = speed*Math.sin(thetaM.val);
    vpx.val = speed*Math.cos(thetaP.val)/2;
    vpy.val = speed*Math.sin(thetaP.val)/2;

    state.set(0,(missileX.val-planeX.val)/2);
    state.set(1,(missileY.val-planeY.val)/2);
    state.set(2,(vmx.val-vpx.val)/1.5/speed);
    state.set(3,(vmy.val-vpy.val)/1.5/speed);

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
    double reinforcement = 0;
    double x,y;

    //state.set(0,1); state.set(1,0);

  	double angle=Math.atan(state.val(1)/(state.val(0)==0?0.001:state.val(0)));
  	if (state.val(0)>0) angle += pi;

  	action.set(MAction,(angle - thetaM.val)/(2*pi));
  	while(action.val(MAction)>1) action.set(MAction,action.val(MAction)-1);
  	while(action.val(MAction)<-1) action.set(MAction,action.val(MAction)+1);
  	action.set(MAction,action.val(MAction)/dt.val);

  	action.set(PAction,(angle - thetaP.val + pi/2)/(2*pi));
  	while(action.val(PAction)>1) action.set(PAction,action.val(PAction)-1);
  	while(action.val(PAction)<-1) action.set(PAction,action.val(PAction)+1);
  	action.set(PAction,action.val(PAction)/dt.val);

    thetaM.val = -action.val(MAction)*180*deg2rad*dt.val;
thetaM.val = angle;

    thetaP.val = action.val(PAction)*180*deg2rad*dt.val;
thetaP.val = angle+pi/2;

    while(thetaP.val>(2*pi)) thetaP.val -= (2*pi);
    while(thetaP.val<0) thetaP.val += (2*pi);
    while(thetaM.val>(2*pi)) thetaM.val -= (2*pi);
    while(thetaM.val<0) thetaM.val += (2*pi);

    //update the velocities of the players
    vmx.val = speed*Math.cos(thetaM.val);
    vmy.val = speed*Math.sin(thetaM.val);
    vpx.val = (speed*Math.cos(thetaP.val)/2);
    vpy.val = (speed*Math.sin(thetaP.val)/2);

    //update the positions of the players
    missileX.val += vmx.val*dt.val;
    missileY.val += vmy.val*dt.val;
    planeX.val += vpx.val*dt.val;
    planeY.val += vpy.val*dt.val;

    x = missileX.val-planeX.val;
    y = missileY.val-planeY.val;

    newState.set(0,x/2);
    newState.set(1,y/2);
    newState.set(2,(vmx.val-vpx.val)/1.5/speed);
    newState.set(3,(vmy.val-vpy.val)/1.5/speed);


    if((x*x+y*y)>6000) { reinforcement=1; valueKnown.val=true;}
    if((x*x+y*y)<50) {reinforcement=-1; valueKnown.val=true;
       missileY.val = random.nextDouble()*100;
       missileX.val = random.nextDouble()*100;
       planeX.val = random.nextDouble()*100;
       planeY.val = random.nextDouble()*100;
       newState.set(0,planeX.val-missileX.val);
       newState.set(1,planeY.val-missileY.val);}

    return reinforcement;
  }

  /** Find the value and best action of this state.  This corrupts the original action passed in
    * by returning in its place the best action for the given state.
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValAct(MatrixD state,MatrixD action,FunApp f,MatrixD outputs,PBoolean valueKnown) throws MatrixException {
    double max1,max2,max3,max4,firstPossibility,secondPossibility,value=0;
    int choice1,choice2;


  //find Q value for action (-0.1,-0.1)
    action.set(MAction,-0.1); action.set(PAction,-0.1);
    f.evaluate();
    max1 = outputs.val(0);
  //find Q value for action (-0.1,0.1)
    action.set(PAction,0.1);
    f.evaluate();
    max2 = outputs.val(0);
  //find Q value for action (0.1,-0.1)
    action.set(MAction,0.1); action.set(PAction,-0.1);
    f.evaluate();
    max3 = outputs.val(0);
  //find Q value for action (-0.1,-0.1)
    action.set(MAction,-0.1);
    f.evaluate();
    max4 = outputs.val(0);

    if (max1>max2) {firstPossibility=max1; choice1=1;}
    else {firstPossibility=max2; choice1=2;}

    if (max3>max4) {secondPossibility=max3; choice2=3;}
    else {secondPossibility=max4; choice2=4;}

    if (firstPossibility<secondPossibility)
    switch (choice1) {
        case 1:  action.set(PAction,-0.1);
                 action.set(MAction,-0.1);
                 value = max1;
                 break;
        case 2:  action.set(PAction,0.1);
                 action.set(MAction,-0.1);
                 value = max2;
                 break;
        default: break;
    } else
    switch (choice2) {
        case 3:  action.set(PAction,-0.1);
                 action.set(MAction,0.1);
                 value = max3;
                 break;
        case 4:  action.set(PAction,0.1);
                 action.set(MAction,0.1);
                 value = max4;
                 break;
        default: break;
    }

    return value;
  }

  /** Find the max over action for <R+gammaV(x')> where V(x') is the value of the successor state
    * given state x, R is the reinforcement, gamma is the discount factor.  This method is used in
    * the object ValIter (value iteration).
    * @exception MatrixException column vectors are wrong size or shape
    */
  public double findValue(MatrixD state,MatrixD optAction,PDouble gamma,FunApp f,PDouble dt,MatrixD outputs,
  PDouble reinforcement,PBoolean valueKnown,NumExp explorationFactor,Random random) throws MatrixException {
    /* PBoolean vKnown=new PBoolean(false);
    MatrixD newX=new MatrixD(stateSize());
    MatrixD action=new MatrixD(actionSize());
    MatrixD originalX=new MatrixD(stateSize());
    MatrixD minX=new MatrixD(stateSize());
    double r;
    double Vx=0;
    double minV=1e20;

    originalX.replace(state);
    initialAction(state,action);
    for(int j=0; j<numActions(state); j++){
       r=nextState(originalX,action,newX,dt,vKnown);
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
       getAction(state,action);
    }
    state.replace(minX);
    if((minX.val(0)>-1e-5) && (minX.val(0)<1e-5)) valueKnown.val=true;
    return minV; */
    return 0;
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
        return "('epochSize' IntExp)*"+
        "//Linear-Quadratic Regulator. epochSize must "+
        "be set if doing epochwise training.";
  }

  /** Output a description of this object that can be parsed with parse().
    * Also creates the state/action/nextState vectors
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if(epochSize.val!=-1) {
        u.indent();
        u.emitLine();
        u.emit("epochSize ");
        u.emitUnparse(epochSize,lang);
        u.unindent();
    }
    u.emitLine();

  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    while (true) { //parse whatever parameters are there
      if (p.tID.equals("epochSize")) {
        p.parseID("epochSize",true);
        epochSize=(IntExp)p.parseClass("IntExp",lang,true);
      }
      else break;
    }
      return this;
  }
}
