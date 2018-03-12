package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import Random;

/** Used to define a reinforcement learning experiment.  The parameters passed to this define
  * the exploration policy, the funApp parameter update policy (incremental or epoch), the mdp,
  * the function approximator, and the reinforcement learning algorithm to be used.
  * NOTES:  Use caution when training on trajectories with a low exploration factor.  This could lead to
  * very long trajectories that could cause the system to appear hung.
  * The exploration factor is handled in this code for all cases except when 'statesOnly' is true while training on
  * trajectories.  In this case, the rlAlgorithm must use the class variable 'exploration' passed into this to implement
  * the exploration strategy.
  *
  * After a policy has been learned it can be observed by setting incremental=true, trajectories=true, and exploration=0.
  *
  *    <p>This code is (c) 1997 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.02, 26 June 97
  * @author Mance E. Harmon
  */
public class ReinforcementLearning extends RLErrFun {
  //Version 1.02 26 June 97: Corrected bug in calcPhi() - MH
  //Version 1.01 23 June 97: Added code to calculate an adaptive phi - MH
  //Version 1.00 16 June 97

  /** The RL algorithm to use */
  protected RLErrFun rlAlgorithm=null;
  /** Used to cache the number of states in the given mdp */
  protected int numberOfStates;
  /** which of the batch elements is currently being processed. */
  protected PInt batchIndex=new PInt(0);
  /** the gradient from a single call to rlAlgorithm */
  protected MatrixD rlAlgorithmGradient=null;
  /** the average of the gradients from all numberOfStates calls to RLerrFun */
  protected MatrixD gradient=null;
  /** The number of state/action pairs in the mdp for a given dt (assuming statesOnly=false). */
  protected int saPairs;
  /** The gradient associated with the direct method update.  Used in calculating an adaptive phi. */
  protected MatrixD directVector=null;
  /** The gradient associated with the residual gradient method update.  Used in calculating and adaptive phi. */
  protected MatrixD resGradVector=null;
  /** Flag set to true if phi is adaptive. */
  protected boolean adaptivePhi=false;
  /** A copy of the random number generator passed into evaluate() */
  protected Random rnd=new Random(0);

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    mdp.setWatchManager(wm,name+"mdp/");
    rlAlgorithm.setWatchManager(wm,name+"rlAlgorithm/");
    wm.registerVar(name+"gamma",  gamma,     this);
    wm.registerVar(name+"dt",     dt,        this);
    wm.registerVar(name+"phi",    phi,       this);
    wm.registerVar(name+"mu",     mu,        this);
    wm.registerVar(name+"method", methodStr, this);
    wm.registerVar(name+"trajectories", trajectories, this);
    wm.registerVar(name+"incremental",  incremental,  this);
    wm.registerVar(name+"exploration",  exploration,  this);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ('MDP' <sim.mdp.MDP> | 'funApp' <sim.funApp.FunApp> "+
           "'dt' NumExp | 'gamma' NumExp | 'incremental' <boolean> | 'trajectories' <boolean>"+
           "'exploration' NumExp | 'algorithm' <sim.errFun.RLErrFun> | "+
           "'method' ('resGrad' | 'direct' | ('residual' (NumExp | 'adapt' NumExp*))))* '}'"+
           "//Reinforcement Learning Experiment Wrapper."+
           "With tag 'method direct', this is the direct method. With 'method resGrad', this "+
           "is the residual gradient method (the default). With 'method residual .7' this is "+
           "the residual method with phi=.7 (0=direct,1=residual gradient,between=residual). "+
           "With 'method adapt 0.99' phi is adaptive and calculated autmatically.  The NumExp after "+
           "'adapt' is the decay factor for the update traces used in calculating phi. "+
           "If incremental=false and trajectories=false then the NumExp after 'adapt' is ignored and "+
           "the proper phi is calculated exactly.  If incremental=false and trajectories=true, or "+
           "incremental=true then the NumExp after 'adapt' is used for an approximate solution to phi. "+
           "If 'trajectories' is true, then it does training on a trajectory through state space, "+
           "as opposed to randomly choosing a state on which to train.  'incremental' determines if the "+
           "parameters of the function approximator will be updated after every transition in state space "+
           "or after a sequence of transitions.  If incrementa=false and trajectories=true the parameters of "+
           "the function approximator will be updated when an absorbing state is entered.  This is an analog to "+
           "epochwise training in supervised learning. 'exploration' is the percentage of "+
           "time a random action is chosen rather than the action considered best. "+
           "'gamma' is the discount factor.  'dt' is the time step size. "+
           "'algorithm' is the reinforcement learning algorithm to be used. "+
           "DEFAULTS: method=resGrad, gamma=0.9, exploration=1, dt=1, "+
           "incremental=true, trajectories=false. ";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(" {");
    u.indent();
      u.emitLine();
      u.emit("algorithm ");
      u.emitUnparseWithClassName(rlAlgorithm,lang,false);
      u.emitLine();
      u.emit("mdp ");
        u.emitUnparseWithClassName(mdp,lang,false);
      u.emit("incremental ");
        u.emit(incremental);
        u.emitLine();
      u.emit("trajectories ");
        u.emit(trajectories);
        u.emitLine();
      u.emit("dt ");
        u.emitUnparse(dt,lang);
        u.emitLine();
      u.emit("gamma ");
        u.emitUnparse(gamma,lang);
        u.emitLine();
      u.emit("exploration ");
        u.emitUnparse(exploration,lang);
        u.emitLine();
      u.emit("method ");
        u.emit(methodStr.val);
        u.emit(" ");
        if (method.val==0) { //residual method
          if (adaptivePhi) {u.emit("adapt "); u.emitUnparse(mu,lang);}
          else u.emitUnparse(phi,lang);
          u.emit(" ");
        }
        u.emitLine();
      u.emit("funApp ");
        u.emitUnparseWithClassName(function,lang,false);
        u.emitLine();

    u.unindent();
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    while (true) {
        if (p.parseID("algorithm",false))
          rlAlgorithm   =(RLErrFun)p.parseType("sim.errFun.RLErrFun",lang,true);
        else if (p.parseID("mdp",false))
          mdp=(MDP)p.parseType("sim.mdp.MDP",lang,true);
        else if (p.parseID("gamma",false))
          gamma=(NumExp)p.parseClass("NumExp",lang,true);
        else if (p.parseID("incremental",false))
          incremental.val =p.parseBoolean(true);
        else if (p.parseID("exploration",false))
          exploration=(NumExp)p.parseClass("NumExp",lang,true);
        else if (p.parseID("trajectories",false))
          trajectories.val=p.parseBoolean(true);
        else if (p.parseID("dt",false))
          dt=(NumExp)p.parseClass("NumExp",lang,true);
        else if (p.parseID("funApp",false))
          function=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
        else if (p.parseID("method",false)) {
          if (p.parseID("resGrad",false)) {
            methodStr.val="resGrad";
            method.val=1;
            phi.val=1;
          } else if (p.parseID("direct",false)) {
            methodStr.val="direct";
            method.val=2;
            phi.val=0;
          } else if (p.parseID("residual",true)) {
            methodStr.val="residual";
            method.val=0;
            if (p.parseID("adapt",false)) {
                adaptivePhi=true;
                mu=(NumExp)p.parseClass("NumExp",lang,false);
            } else phi=(NumExp)p.parseClass("NumExp",lang,true);
          }
        } else break;
    }
    p.parseChar('}',true);


    rlAlgorithm.initVects(mdp,this); //Set up inputs, state, and action vectors
    state = rlAlgorithm.state;    //The rlAlgorithm will set up the state vector (submatrix of inputs)
    action = rlAlgorithm.action;  //The rlAlgorithm will set up that action vector (submatrix of inputs)
    newState = rlAlgorithm.newState; //The rlAlgorithm will set up the newState vector

    inputs          = rlAlgorithm.inputs; //The rlAlgorithm set up the inputs vector in initialize
    outputs         =new MatrixD(1);
    weights         =new MatrixD(function.nWeights(inputs.size,outputs.size));
    dEdWeights      =new MatrixD(weights.size);
    dEdOut          =new MatrixD(outputs.size);
    dEdIn           =new MatrixD(inputs.size);

    if (adaptivePhi) {
      directVector    =new MatrixD(weights.size);
      resGradVector   =new MatrixD(weights.size);
    }

    //Make the parameters of ReinforcementLearning visible to the rlAlgorithm
    rlAlgorithm.mdp = mdp;
    rlAlgorithm.gamma = gamma;
    rlAlgorithm.dt = dt;
    rlAlgorithm.phi = phi;
    rlAlgorithm.method = method;
    rlAlgorithm.methodStr = methodStr;
    rlAlgorithm.action = action;
    rlAlgorithm.trajectories = trajectories;
    rlAlgorithm.endTrajectory = endTrajectory;
    rlAlgorithm.incremental = incremental;
    rlAlgorithm.exploration = exploration;

    rlAlgorithm.outputs = outputs;
    rlAlgorithm.weights = weights;
    rlAlgorithm.function = function;
    rlAlgorithm.dEdIn = dEdIn;
    rlAlgorithm.dEdOut = dEdOut;
    rlAlgorithm.dEdWeights = dEdWeights;

    statesOnly= rlAlgorithm.statesOnly;

    try {
      inputs.set(0,1);  //this sets the bias to 1.  This should be handled in Net (not here) /**/
      function.setIO(inputs,outputs,weights,dEdIn,dEdOut,dEdWeights,null,null,null);
      if (!statesOnly) saPairs=mdp.numPairs(dt);  //stores the number of state/action pairs in the mdp
    } catch (MatrixException e) {e.print();}

    return this;
  }//end parse

  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    int count=0; //The number of states in a given trajectory
    int numIterations; //The number of total iterations in an epoch.
    double error=0;
    rnd.copyInto(this.rnd);

    try {
      double err=0;
      gradient.mult(0);
   //INCREMENTAL
      if(incremental.val) { //incremental training
        //Setup of inputs to funApp
        if (trajectories.val) state.replace(newState);
        else mdp.randomState(state,rnd); //set up the state
      if (!statesOnly)
          if (rnd.nextDouble()<exploration.val)
            mdp.randomAction(state,action,rnd); //set up a random action
          else
            mdp.findValAct(state,action,function,outputs,valueKnown); //set up the current best action
        if(adaptivePhi) calcPhi(false,rnd);//calculate phi if required

        err=rlAlgorithm.evaluate(rnd,true,false,false); //evaluate and find error
        rlAlgorithm.findGradient(); //find gradient
        gradient.add(rlAlgorithmGradient);
        if (endTrajectory.val) {mdp.initialState(newState,rnd); endTrajectory.val=false;}
        return err;

      } else { //batch mode training
    //BATCH
     //NO TRAJECTORIES
        if(!trajectories.val) {
          numberOfStates = mdp.numStates(dt); //find the number states in an epoch
          mdp.initialState(state,rnd);  //set up the initial state
          if (statesOnly) { //rlAlgorithm uses only states as opposed to state/action pairs
            if (adaptivePhi) calcPhi(true,rnd); //calculate phi
            numIterations = numberOfStates;
            for (batchIndex.val=0;batchIndex.val<numberOfStates;batchIndex.val++) {
              err+=rlAlgorithm.evaluate(rnd,true,false,false);
              rlAlgorithm.findGradient();
              gradient.addMult(1./numIterations,rlAlgorithmGradient);
              mdp.getState(state,dt,rnd);
            }
            watchManager.update();
          } else { //rlAlgorithm uses state/action pairs
            if (adaptivePhi) calcPhi(true,rnd);
            numIterations = saPairs;
            mdp.initialAction(state,action,rnd);
            for (batchIndex.val=0;batchIndex.val<numberOfStates;batchIndex.val++) { //iterate over states
              for (int j=0; j<mdp.numActions(state); j++) { //iterate over actions
                err+=rlAlgorithm.evaluate(rnd,true,false,false);
                rlAlgorithm.findGradient();
                gradient.addMult(1./numIterations,rlAlgorithmGradient);
                mdp.getAction(state,action,rnd);
              }
              mdp.getState(state,dt,rnd);
            }
            watchManager.update();
          }
          return err/numIterations;
        } else { //training on trajectories
       //TRAJECTORIES
          if (statesOnly) { //rlAlgorithm uses only states as opposed to state/action pairs
           mdp.initialState(state,rnd);  //EXPLORATION FOR THIS CASE (TRAJECTORIES, STATESONLY) MUST BE HANDLED IN rlAlgorithm
           while (!endTrajectory.val) {
              if(adaptivePhi) calcPhi(false,rnd);//calculate phi if required
              err+=rlAlgorithm.evaluate(rnd,true,false,false);
              rlAlgorithm.findGradient();
              gradient.add(rlAlgorithmGradient);
              state.replace(newState);
              count++;
              watchManager.update();
            }
            gradient.mult(1./count);
          } else { //rlAlgorithm uses state/action pairs
            mdp.initialState(state,rnd);
            mdp.initialAction(state,action,rnd);
            while (!endTrajectory.val) {
              if(adaptivePhi) calcPhi(false,rnd);//calculate phi if required
              err+=rlAlgorithm.evaluate(rnd,true,false,false); //evaluate state/action
              rlAlgorithm.findGradient(); //find gradient
              gradient.add(rlAlgorithmGradient);  //sum with previous gradients in this trajectory
              state.replace(newState);  //setup next state as input
              if (rnd.nextDouble()<exploration.val)
                mdp.randomAction(state,action,rnd); //set up a random action
              else
                mdp.findValAct(state,action,function,outputs,valueKnown); //set up the current best action
              count++;
              watchManager.update();
            } //end while
            gradient.mult(1./count);
          }

          error = err/count;
          count=0;
          endTrajectory.val=false;
          return error;

          } //end training on trajectories code


      } //end if/else

    } catch (MatrixException e) {
      e.print();
    }
    return 0;
  }//end evaluate

  /** update the gradient vector based on the current fInput vector.
    * Assumes that evaluate() was already called on this vector.
    */
  public void findGradient() {  //gradient is updated in evaluate()
  } //end findGradient

  /** The gradient of f(x) with respect to x (a column vector)*/
  public MatrixD getGradient() {
    rlAlgorithmGradient=rlAlgorithm.getGradient();
    gradient=rlAlgorithmGradient.duplicate();
    return gradient;
  }

  public void initVects(MDP mpd,RLErrFun rl) {} //not needed

  public void calcPhi(boolean batch,Random rnd) {
    double drg,rgrg; //used for adaptive phi calculations
    int numIterations;
    MatrixD oldState=new MatrixD(state.size);

    try {
      if(!batch) { //incremental training
        oldState.replace(state);
        //Update the trace of weight updates for residual gradient method
        phi.val=1; //residual gradient
        rlAlgorithm.evaluate(rnd,true,false,false);
        rlAlgorithm.findGradient(); //find the gradient for residual gradient
        rlAlgorithmGradient.mult(-mu.val);
        resGradVector.multAdd(1-mu.val,rlAlgorithmGradient);
        state.replace(oldState);

        //Update the trace of weight updates for direct method
        phi.val=0; //direct method
        rlAlgorithm.evaluate(rnd,true,false,false);
        rlAlgorithm.findGradient();
        rlAlgorithmGradient.mult(-mu.val);
        directVector.multAdd(1-mu.val,rlAlgorithmGradient);
        state.replace(oldState);

        drg  =resGradVector.dot(directVector); //calculate the parts of phi equation
        rgrg =resGradVector.dot(resGradVector);
        phi.val = drg/(drg-rgrg); //update phi

        if((phi.val>1) || (phi.val<0)) phi.val = 0; //if outside of range [0,1] then direct vector is best
        if(phi.val!=0) {phi.val+=0.1; if(phi.val>1) phi.val = 1;}

      } else { //epoch wise training
        if (statesOnly) { //rlAlgorithm uses only states as opposed to state/action pairs
          resGradVector.mult(0);
          directVector.mult(0);
          numIterations = numberOfStates;
          for (batchIndex.val=0;batchIndex.val<numberOfStates;batchIndex.val++) {

            oldState.replace(state); //make a copy of the original state

            phi.val=1;
            rlAlgorithm.evaluate(rnd,true,false,false);
            rlAlgorithm.findGradient();
            resGradVector.add(rlAlgorithmGradient);
            state.replace(oldState);

            phi.val=0;
            rlAlgorithm.evaluate(rnd,true,false,false);
            rlAlgorithm.findGradient();
            directVector.add(rlAlgorithmGradient);
            state.replace(oldState);

            mdp.getState(state,dt,rnd);
          }
          resGradVector.mult(1./numIterations);
          directVector.mult(1./numIterations);
          drg  =resGradVector.dot(directVector); //calculate the parts of phi equation
          rgrg =resGradVector.dot(resGradVector);

          phi.val = drg/(drg-rgrg); //update phi

          if((phi.val>1) || (phi.val<0)) phi.val = 0; //if outside of range [0,1] then direct vector is best
          if(phi.val!=0) {phi.val+=0.1; if(phi.val>1) phi.val = 1;}

          mdp.initialState(state,rnd); //reset initial state

        } else { //rlAlgorithm uses state/action pairs
          numIterations = saPairs;
          mdp.initialAction(state,action,rnd);
          for (batchIndex.val=0;batchIndex.val<numberOfStates;batchIndex.val++) { //iterate over states
            oldState.replace(state);
            for (int j=0; j<mdp.numActions(state); j++) { //iterate over actions

              phi.val=1;
              rlAlgorithm.evaluate(rnd,true,false,false);
              rlAlgorithm.findGradient();
              resGradVector.add(rlAlgorithmGradient);
              state.replace(oldState);

              phi.val=0;
              rlAlgorithm.evaluate(rnd,true,false,false);
              rlAlgorithm.findGradient();
              directVector.add(rlAlgorithmGradient);
              state.replace(oldState);

              mdp.getAction(state,action,rnd);
            }
            mdp.getState(state,dt,rnd);
          }
          resGradVector.mult(1./numIterations);
          directVector.mult(1./numIterations);
          drg  =resGradVector.dot(directVector); //calculate the parts of phi equation
          rgrg =resGradVector.dot(resGradVector);

          phi.val = drg/(drg-rgrg); //update phi

          if((phi.val>1) || (phi.val<0)) phi.val = 0; //if outside of range [0,1] then direct vector is best
          if(phi.val!=0) {phi.val+=0.1; if(phi.val>1) phi.val = 1;}

          mdp.initialState(state,rnd); //reset initial state
          mdp.initialAction(state,action,rnd); //reset initial action

        }
      }


    } catch (MatrixException e) {
      e.print();
    }
  }//end calcPhi

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    mdp.initialize(level);
    rlAlgorithm.initialize(level);
  }
}//end ReinforcementLearning