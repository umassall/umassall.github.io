package sim;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import Random;
import expression.*;

/** Perform Temporal Difference learning, TD(lambda), with a given Markov Decision
  * Process or Markov chain and function approximator.  If the MDP is a Markov chain, then one
  * can set the exploration factor to 0 and perform standard TD(lambda) for predicting the
  * value of the states.  Given an MDP then the object implements TD(lambda) such that anytime
  * the system explores the trace is set to 0.  This object has a decay factor for the
  * exploration rate, so that one can explore extensively in the initial stages of learning
  * and reduce the exploration rate in latter stages of learning.  The derivative
  * calculations with respect to the inputs have not been fully implemented here.
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird/java>http://www.cs.cmu.edu/~baird/java</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  *
  * @version 1.05, 17 June 97
  * @author Mance E. Harmon
  */
public class TDLambda extends Experiment {
//Version 1.05 17 June 97: changed the call to findValue() to reflect the new method definition in MDP

//Revision 1.04 4 June 97 : Moved matrix setup statements from beginning of run() to end of parse.  This was needed
//so that Graph3D's parse routine would not be parsing a function approximator with null parameters.

//Revision 1.03 14 May 97
//Revision 1.02 15 November 96: Added a decay rate for the exploration rate. - Mance Harmon

//Revision 1.01 30 October 96: Changed the structure of the state submatrix from a 1XN vector to an
//NX1 vector to match the oldState vector.

    /** the mdp to control */
    protected MDP mdp=null;
    /** the function approximator whose weights will be trained */
    protected FunApp function=null;
    /** the random number seed */
    protected IntExp seed=new IntExp(0);
    /** all the weights in the function approximator as a column vector */
    protected MatrixD weights=null;
    /** gradient of mean squared error wrt weights*/
    protected MatrixD dEdWeights=null;
    /** gradient of mean squared error summed for all training examples */
    protected MatrixD dEdWeightsSum=null;
    /** gradient of mean squared error  wrt inputs*/
    protected MatrixD dEdIn=null;
    /** gradient of mean squared error wrt weights of maximum advantage in successor state*/
    protected MatrixD dEdWeightsV1=null;
    /** The weighted average of the gradients.  The weighting factor is lambda. */
    protected MatrixD trace=null;
    /** hessian of mean squared error wrt weights */
    protected MatrixD hessian=null;
    /** The input vector to the function approximator */
    protected MatrixD inputs=null;
    /** The output vector from the function approximator */
    protected MatrixD outputs=null;
    /** The state of the MDP */
    protected MatrixD state=null;
    /** An action possible in the MDP */
    protected MatrixD action=null;
    /** The exploration rate */
    protected NumExp explore=new NumExp(0.3);
    /** The exploration decay rate. A value of 0.9 means a half-life of approximately 7, and
      * a value 0.99 means a half-life of approximately 70. */
    protected NumExp expDecay=new NumExp(0.99);
    /** The discount factor */
    protected NumExp gamma=new NumExp(0.9);
    /** The correct output that the function approximator learns to give */
    protected MatrixD desiredOutputs=null;
    /** The time step size used in transitioning from state x(t) to x(t+1) */
    protected NumExp dt=new NumExp(0);
    /** A copy of the original state.  */
    protected MatrixD oldState=null;
    /** The mode of learning: incremental or epoch-wise. */
    protected boolean incremental=true;
    /** A flag stating whether or not we know for certain the value of a state. */
    protected PBoolean valueKnown=new PBoolean(false);
    /** The weighting factor for gradients. */
    protected NumExp lambda=new NumExp(0.5);
    /** The random number generator */
    protected Random random=new Random(0);
    /** a noisy estimate of the error being gradient descended on */
    protected PDouble error=new PDouble(1);
    /** an exponentially smoothed estimate of the error */
    protected PDouble smoothedError=new PDouble(1);
    /** the constant used to smooth the error (near 1 = long halflife)*/
    protected NumExp smoothingFactor=new NumExp(0.9);
    /** stop learning when smoothed error < tolerance */
    protected NumExp tolerance=new NumExp(0);
    /** log base 10 of the smoothed error */
    protected PDouble logSmoothedError=new PDouble(1);
    /** current time (increments once per weight change */
    protected PInt time=new PInt(0);
    /** the learning rate, a small positive number */
    protected NumExp rate=new NumExp(0.1);

    /** When doing epoch-wise training (not updating the weights until the end of a trajectory,
      * this variable keeps track of the number of transitions.
      */
    protected int tcounter=0;


  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    function.setWatchManager(wm,name+"function/");
    mdp.setWatchManager(wm,name+"mdp/");
    wm.registerVar(name+"dt",      dt,      this);
    wm.registerVar(name+"seed",    seed,    this);
    wm.registerVar(name+"lambda",  lambda,  this);
    wm.registerVar(name+"gamma",   gamma,   this);
    wm.registerVar(name+"explore", explore, this);
    wm.registerVar(name+"expDecay", expDecay, this);
    wm.registerVar(name+"error",        error,           this);
    wm.registerVar(name+"avg error",    smoothedError,   this);
    wm.registerVar(name+"log error",    logSmoothedError,this);
    wm.registerVar(name+"avg const",    smoothingFactor, this);
    wm.registerVar(name+"time",         time,            this);
    wm.registerVar(name+"learning rate",rate,            this);
    wm.registerVar(name+"tolerance",    tolerance,       this);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' ("+
           "('mdp' <sim.mdp.MDP>) | "+
           "('funApp' <sim.funApp.FunApp>) | "+
           "('dt' NumExp)  | "+
           "('incremental' <boolean>) | "+
           "('seed' IntExp) | "+
           "('lambda' NumExp) | "+
           "('gamma' NumExp) | "+
           "('explore' NumExp) |"+
           "('rate' NumExp) | "+
           "('smooth' NumExp) | "+
           "('tolerance' NumExp) | "+
           "('expDecay' NumExp)"+
           ")* '}'"+
           "//TD(lambda)."+
           "If 'incremental' is false, then it does epochwise training. "+
           "'seed' is for the random number generator. 'explore' is the percentage"+
           " of time that the agent performs an action other than the action thought optimal. "+
           "'expDecay' is the decay rate of the exploration rate.  If expDecay=0.9 then explore has "+
           "a half-life of 6.57 iterations (expDecay=0.99 => explore half-life of 65.7 iterations). "+
           "In this case an iteration is completed when a terminal state is reached."+
           "DEFAULTS: lambda=0.5, gamma=0.9, dt=1, incremental=true, seed=0, rate=0.1, tolerance=1, "+
           "smooth=0.9, explore=0.3, expDecay=0.99";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emitLine();
    u.emit("{ ");
    u.emit("mdp ");
      u.emitUnparseWithClassName(mdp,lang,false);
    u.emit("funApp ");
      u.emitUnparseWithClassName(function,lang,false);
    u.emit("incremental ");
      u.emit(incremental);
      u.emitLine();
    u.emit("rate ");
      u.emitUnparse(rate,lang);
      u.emitLine();
    u.emit("smooth ");
      u.emitUnparse(smoothingFactor,lang);
      u.emitLine();
    u.emit("tolerance ");
      u.emitUnparse(tolerance,lang);
      u.emitLine();
    u.emit("gamma ");
      u.emitUnparse(gamma,lang);
      u.emitLine();
    u.emit("explore ");
      u.emitUnparse(explore,lang);
      u.emitLine();
    u.emit("expDecay ");
      u.emitUnparse(expDecay,lang);
      u.emitLine();
    u.emit("lambda ");
      u.emitUnparse(lambda,lang);
      u.emitLine();
    u.emit("dt ");
      u.emitUnparse(dt,lang);
      u.emitLine();
    u.emit("seed ");
      u.emitUnparse(seed,lang);
      u.emitLine();
    u.emit("} ");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    while (true) { //parse whatever parameters are there
      if (p.parseID("dt",false))
        dt=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("mdp",false))
        mdp=(MDP)p.parseType("sim.mdp.MDP",lang,true);
      else if (p.parseID("funApp",false))
        function=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
      else if (p.parseID("gamma",false))
        gamma=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("explore",false))
        explore=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("expDecay",false))
        expDecay=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("seed",false))
        seed=(IntExp)p.parseClass("IntExp",lang,true);
      else if (p.parseID("lambda",false))
        lambda=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("incremental",false))
        incremental=p.parseBoolean(true);
      else if (p.parseID("rate",false))
        rate=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("tolerance",false))
        tolerance=(NumExp)p.parseClass("NumExp",lang,true);
      else if (p.parseID("smooth",false))
        smoothingFactor=(NumExp)p.parseClass("NumExp",lang,true);
      else
        break;
    } //end while(true)

    p.parseChar('}',true);

    inputs        =new MatrixD(mdp.stateSize()+1); //create vectors of appropriate sizes
    dEdIn         =new MatrixD(inputs.size);
    outputs       =new MatrixD(1);
    desiredOutputs=new MatrixD(outputs.size); //also gradient of err wrt outputs
    weights       =new MatrixD(function.nWeights(inputs.size,outputs.size));
    dEdWeights    =new MatrixD(weights.size);
    dEdWeightsSum =new MatrixD(weights.size);
    dEdWeightsV1  =new MatrixD(weights.size);
    trace         =new MatrixD(weights.size);
    oldState      =new MatrixD(mdp.stateSize());
    action        =new MatrixD(mdp.actionSize());
    try{
      state		=((MatrixD)inputs.clone()).submatrix(1,mdp.stateSize(),1);
      function.setIO(inputs,outputs,weights,dEdIn,desiredOutputs,dEdWeights,null,null,null);
    } catch (MatrixException e) {
      e.print();
    }

    return this;
  }

  /** This runs the simulation.  The function returns when the simulation
    * is completely done.  As the simulation is running, it should call
    * the watchManager.update() function periodically so all the display
    * windows can be updated.
    */
  public void run() {
      try {
        trace.mult(0);
        inputs.set(0,1); //this is the bias
        mdp.initialState(state,random); //set up the initial state
        weights.setRandom((double)-1,(double)1,new Random(seed.val));//seed=0

        error.val=1;
        smoothedError.val=1;
        time.val=0;
        while (smoothedError.val>tolerance.val) {
          time.val++;
          error.val=evaluate();
          smoothedError.val=   smoothingFactor.val *smoothedError.val +
                          (1-smoothingFactor.val)*error.val;
          logSmoothedError.val=(double)Math.log(smoothedError.val) /
                             (double)Math.log(10);
          findGradient();
          dEdWeights.mult(-rate.val);
          weights.add(dEdWeights);
          watchManager.update();
        }

      } catch (MatrixException e) {
        e.print();
      }
  }//end method run

  /** The input x sent to the function f(x) (a column vector)*/
  public MatrixD getInput() {
    return weights;
  }

  /** The gradient of f(x) with respect to x (a column vector)*/
  public MatrixD getGradient() {
    return dEdWeights;
  }

  /** return the scalar output for the current dInput vector */
  public double evaluate() {
    PDouble r=new PDouble(0); //reinforcement
    double maxV1=0; //successor state value: maximum value in X(t+1)
 	double error=0,errorSum=0,errorSum2=0;
 	double V0=0;
    MatrixD newState=new MatrixD(state.size);

    try {
      if(incremental) {  //  Incremental training
          oldState.replace(state);

          if(random.nextDouble()>explore.val){ // calculate the max(R+gammaV(x')) over actions
	        maxV1=mdp.findValue(state,null,gamma,function,dt,outputs,r,valueKnown,null,random);
	        newState.replace(state);
	        state.replace(oldState);
          } else { //explore
            mdp.randomAction(state,action,random);
            r.val=mdp.nextState(state,action,newState,dt,valueKnown,random);
            state.replace(newState);
            function.evaluate();
            maxV1=r.val+gamma.val*outputs.val(0);
            trace.mult(0);
            state.replace(oldState);
          } //end if

	    // evaluate the original state to find V0
	      function.evaluate();
	      V0=outputs.val(0);

        // calculate the bellman residual: this is the equivalent of dEdOut and has to be done before calling findGradients().
          desiredOutputs.set(0,maxV1-V0);
		  error=desiredOutputs.dot(desiredOutputs);  //return the squared error

	    // find the derivative vector
	      function.findGradients();

        // set the state to the successor state for the next iteration
          if(valueKnown.val==false) state.replace(newState);
          else {mdp.initialState(state,random); explore.val*=expDecay.val;}

          return error;

        } else {    //epoch-wise training: update weights after a full trajectory

	      errorSum=0;
	      errorSum2=0;
	      dEdWeightsSum.mult(0);

	      mdp.initialState(state,random);  //set up the initial state: this might not be the same state every time

          while(valueKnown.val==false){
             ++tcounter;
             oldState.replace(state);

             if(random.nextDouble()>explore.val){ // calculate the max(R+gammaV(x')) over actions
	           maxV1=mdp.findValue(state,null,gamma,function,dt,outputs,r,valueKnown,null,random);
	           newState.replace(state);
	           state.replace(oldState);
             } else { //explore
               mdp.randomAction(state,action,random);
               r.val=mdp.nextState(state,action,newState,dt,valueKnown,random);
               state.replace(newState);
               function.evaluate();
               maxV1=r.val+gamma.val*outputs.val(0);
               trace.mult(0);
               state.replace(oldState);
             } //end if

	        // evaluate the original state to find V0
	         function.evaluate();
	         V0=outputs.val(0);

            // calculate the bellman residual: this is the equivalent of dEdOut and has to be done before calling findGradients().
             desiredOutputs.set(0,maxV1-V0);
		     error=desiredOutputs.dot(desiredOutputs);  //return the squared error
             errorSum2+=(maxV1-V0);
             errorSum+=desiredOutputs.dot(desiredOutputs);
                    //accumulate the squared error: used to monitor the MSE

	        // set up the derivative vector
	         function.findGradients();

	        //update the trace
	         dEdWeights.mult(-1); // -1*lambda
	         trace.multAdd(lambda.val,dEdWeights);
             dEdWeights.replace(trace);

  	        // calculate the dE/dw
	         dEdWeightsSum.add(dEdWeights);
            // set the state to the successor state for the next iteration
             state.replace(newState);
	         } //end while
	      desiredOutputs.set(0,(double)(errorSum2/tcounter));  //this sets up dEdOutputs
             //desiredOutputs is also gradient of error wrt outputs
          return (double)(errorSum/tcounter);     //sum squared error: used to monitor the progress
       } //end else
    } catch (MatrixException e) {
      e.print();
    }
    return 0;
  }

  /** update the fGradient vector based on the current fInput vector */
  public void findGradient() {
  double normFactor=0;

  	try {
  	  if(incremental){  // incremental training
        dEdWeights.mult(-1); // -1*lambda
	    trace.multAdd(lambda.val,dEdWeights);
	    dEdWeights.replace(trace);
	    valueKnown.val=false;
	  } else {  //epoch-wise training
        normFactor=(double)(1.0/(double)tcounter);
        dEdWeights.replace(dEdWeightsSum);
	    dEdWeights.mult(normFactor);
	    tcounter=0; //reset the iteration counter for the next trajectory
	    valueKnown.val=false; //reset valueKnown
	    explore.val*=expDecay.val;
	  }
	} catch (MatrixException e) {
          e.print();
	}
  }

  /** The hessian of f(x) with respect to x (a square matrix)*/
  public MatrixD getHessian() { //if hessian is desired, create it
    if (hessian!=null)
      return hessian;
    hessian=new MatrixD(weights.size,weights.size);
    return hessian;
  }

  /** update the fHessian vector based on the current fInput vector */
  public void findHessian() {
   //this is not yet implemented
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    function.initialize(level);
    mdp.initialize(level);
  }
}//end class TDLambda

