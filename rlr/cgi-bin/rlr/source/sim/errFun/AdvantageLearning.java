package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import Random;

/** Perform Advantage learning with a given Markov Decision
  * Process, function approximator, and gradient-descent algorithm.  The derivative
  * calculations with respect to the inputs have not been fully implemented here.  This code
  * does work with both stochastic and deterministic systems.
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird/java>http://www.cs.cmu.edu/~baird/java</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  *
  * @version 1.03, 21 July 97
  * @author Mance E. Harmon
  */
public class AdvantageLearning extends RLErrFun {
//Version 1.03 21 July 97: Moved all gradient calculations to findGradients() - MH
//Version 1.02 25 June 97: Corrected bug in 'direct' implementation.  When valueKnown=true the dEdWeights was being set
//to 0 even when method=direct.
//Version 1.01 23 June 97: Optimized so that when phi=0 extra deriv calcs will not be done - MH
//Version 1.00 6 June 97

//Variables used only by ALearing
  /** gradient of mean squared error wrt weights of maximum advantage in successor state*/
  protected MatrixD dEdWeightsA1=null;
  /** gradient of mean squared error wrt weights of maximum advantage in original state */
  protected MatrixD dEdWeightsA0=null;
  /** gradient of mean squared error wrt weights of advantage */
  protected MatrixD dEdWeightsA=null;
  /** The scaling factor used in the advantage learning algorithm */
  protected NumExp k=new NumExp(1);
  /** A copy of the original state.  */
  protected MatrixD oldState=null;
  /** A copy of the original action.  */
  protected MatrixD oldAction=null;
  /** A copy of the action considered best at time t */
  private MatrixD maxActionA0=null;
  /** The random number generator that will be used for this object.  This is a copy of the generator passed to evaluate() */
  protected Random rnd=new Random(0);

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"k", k, this);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' 'k' NumExp '}'"+
           "//Advantage Learning."+
           "k is the scaling factor is normally proportional in size to dt. "+
           "DEFAULTS: k=1 ";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit(" {");
      u.emit(" k ");
        u.emitUnparse(k,lang);
    u.emit("} ");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    while (true) { //parse whatever parameters are there
      if (p.parseID("k",false))
        k=(NumExp)p.parseClass("NumExp",lang,true);
      else
        break;
    }
    p.parseChar('}',true);
    return this;
  }//end parse

  /** Create inputs, state, and action vectors.  Also, create any vectors that might be specific to this module. */
  public void initVects(MDP mdp,RLErrFun rl) {

    inputs  =new MatrixD(mdp.stateSize()+mdp.actionSize()+1); //create appropriately sized input vector to funApp

    int numWeights = rl.function.nWeights(inputs.size,1); //specific to ALearning
    dEdWeightsA1  =new MatrixD(numWeights); //specific to ALearning
    dEdWeightsA0  =new MatrixD(numWeights); //specific to ALearning
    dEdWeightsA   =new MatrixD(numWeights); //specific to ALearning

    try {

      //set up the state vector and action vector
      state  =((MatrixD)inputs.clone()).submatrix(1,mdp.stateSize(),1);
      action =((MatrixD)inputs.clone()).submatrix(mdp.stateSize()+1,1,mdp.actionSize()).transpose();
      newState = new MatrixD(state.size);
      mdp.initialState(newState,rnd);
      mdp.initialState(state,rnd);

      oldState =new MatrixD(state.size);  //specific to ALearning
      oldAction =new MatrixD(action.size);//specific to ALearning


    } catch (MatrixException e) {e.print();}

  }

  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    double r=0; //reinforcement
    double maxA1=0; //successor state value: maximum advantage in X(t+1)
    double maxA0=0; //initial state value: maximum advantage in X(t)
    double A=0; //advantage associated with the given state action pair
    maxActionA0=new MatrixD(mdp.actionSize());
    rnd.copyInto(this.rnd);

    try {

	  // make copies of the original state and action
	      oldState.replace(state);  //store a copy of the current state for later use
	      oldAction.replace(action); //store a copy of the current action for later use

	  // calculate the next state, reinforcement, maxA1
	      r=mdp.nextState(state,action,newState,dt,valueKnown,rnd); //find the next state x' and reinforcement R
	      state.replace(newState);  //set up the state vector for calculating the maxA1 value
	      maxA1=mdp.findValAct(state,action,function,outputs,valueKnown);	//find the value of the successor state x'

      // calculate maxA0
	      state.replace(oldState);  //reset the state vector to the original state
	      maxA0=mdp.findValAct(state,action,function,outputs,valueKnown);	//find the value of the successor state x'
	      maxActionA0.replace(action);

	  // calculate the advantage A for the chosen action
	      action.replace(oldAction);
	      function.evaluate();      //evaluate the network for the given state/action pair
	      A=outputs.val(0);	//find the advantage for the given action

      // calculate the bellman residual: this is the equivalent of dEdOut and has to be done before calling findGradients().
          dEdOut.set(0,((r+gamma.val*maxA1)/(dt.val*k.val)+maxA0-maxA0/(dt.val*k.val)-A));
      return dEdOut.dot(dEdOut);  //return the squared error

    } catch (MatrixException e) {e.print();}
    return 0;
  }//end evaluate

  /** update the fGradient vector based on the current fInput vector */
  public void findGradient() {
  double normFactor=0;
  	try {
	  // calculate dA/dw
        function.findGradients();      //find the gradients in this for A(x,u) wrt the weight vector
        if((method.val<2) && (phi.val!=0)){ //if residual gradient or residual (not direct)
	      dEdWeightsA.replace(dEdWeights);

      // calculate dmaxA1/dw
          mdp.nextState(state,action,newState,dt,valueKnown,rnd); //find an independent next state x'
          state.replace(newState);
          mdp.findValAct(state,action,function,outputs,valueKnown);	//find the value of the successor state x'
          function.evaluate();
          function.findGradients();
        //valueKnown is a flag that determines if we have entered an absorbing state where we know
        //the actual value
	      if(valueKnown.val==true) {
	        endTrajectory.val=true;
	        dEdWeights.mult(0);
	        dEdIn.mult(0);
	        valueKnown.val=false;
	      }
          dEdWeightsA1.replace(dEdWeights);

      // calculate dmaxA0/dw
          state.replace(oldState);
          action.replace(maxActionA0);
          function.evaluate();
          function.findGradients();
          dEdWeightsA0.replace(dEdWeights);
      // reset the action to be the original action passed in
          action.replace(oldAction);

  	      dEdWeightsA1.mult(phi.val*gamma.val/(dt.val*k.val));
	      dEdWeightsA0.mult(phi.val*(1-1/(dt.val*k.val)));
	      dEdWeights.replace(dEdWeightsA1);
	      dEdWeights.add(dEdWeightsA0);
	      dEdWeights.sub(dEdWeightsA);
	    } else { // if direct method
	      if (valueKnown.val) {
            endTrajectory.val=true;
            valueKnown.val=false;
          }
	      dEdWeights.mult(-1);
	    }
	} catch (MatrixException e) {e.print();}
  }//end findGradient
}//end class ALearning

