package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import Random;

/** Perform Q learning, either residual gradient, residual or direct, with a given Markov Decision
  * Process, function approximator, and gradient-descent algorithm.  This code works
  * with both stochastic and deterministic systems.
  *    <p>This code is (c) 1996 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely provided
  *    no fee is charged.  If the code is modified, please state so
  *    in the comments.
  * @version 1.03, 21 July 97
  * @author Mance E. Harmon
  */
public class QLearning extends RLErrFun {
//Version 1.03 21 July 97: Moved all gradient calculations to findGradients() - MH
//Version 1.02 25 June 97: Corrected bug in 'direct' implementation.  When valueKnown=true the dEdWeights was being set
//to 0 even when method=direct.
//Version 1.01 23 June 97: Optimized so that when phi=0 extra deriv calcs will not be done - MH
//Version 1.00 17 June 97
  /** gradient of mean squared error for 1 training example wrt weights of original state*/
  protected MatrixD dEdWeightsQ1=null;
  /** gradient of mean squared error for 1 training example wrt inputs of original state*/
  protected MatrixD dEdInQ1=null;
  /** A copy of the original state.  */
  protected MatrixD oldState=null;
  /** A copy of the original action.  */
  protected MatrixD oldAction=null;
  /** The random number generator that will be used for this object.  This is a copy of the generator passed to evaluate() */
  protected Random rnd=new Random(0);

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//Q Learning. ";
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

  /** Create inputs, state, and action vectors. Also, create any vectors that might be specific to this module. */
  public void initVects(MDP mdp,RLErrFun rl) {

    inputs        =new MatrixD(mdp.stateSize()+mdp.actionSize()+1); //create vectors of appropriate sizes

    int numWeights = rl.function.nWeights(inputs.size,1); //specific to QLearning

    dEdInQ1       =new MatrixD(inputs.size);
    dEdWeightsQ1  =new MatrixD(numWeights); //specific to QLearning

    try {
      state     =((MatrixD)inputs.clone()).submatrix(1,mdp.stateSize(),1);
      action    =((MatrixD)inputs.clone()).submatrix(mdp.stateSize()+1,1,mdp.actionSize()).transpose();
      newState = new MatrixD(state.size);
      mdp.initialState(newState,rnd);
      mdp.initialState(state,rnd);

      oldState      =new MatrixD(state.size);
      oldAction     =new MatrixD(action.size);

    } catch (MatrixException e) {e.print();}
  }//end initVects


  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    double  r=0; //reinforcement
    double  q2=0; //successor state value: Q(t+1)
    double  q1=0; //initial state value: Q(t)

    rnd.copyInto(this.rnd);

    try {
       // make copies of the original state and action
	      oldState.replace(state);  //store a copy of the current state for later use
	      oldAction.replace(action); //store a copy of the current action for later use

      // calculate the next state, reinforcement, q2 (max q in x')
	      r=mdp.nextState(state,action,newState,dt,valueKnown,rnd); //find the next state x' and reinforcement R
	      state.replace(newState);  //set up the state vector for calculating the value of the successor Q value
	      q2=mdp.findValAct(state,action,function,outputs,valueKnown);	//find the value of the successor state x'

     // calculate q1 (value of q(x,u))
	      state.replace(oldState);  //reset the state vector to the original state
	      action.replace(oldAction); //reset the action vector to the original action
	      function.evaluate();      //evaluate the network for the given state/action pair
	      q1=outputs.val(0);	//find the q value given x,u

      // calculate the bellman residual: this is the equivalent of dEdOut and has to be done before calling findGradients().
          dEdOut.set(0,(r+gamma.val*q2-q1));  //set up dEdOut:  needed before calling findGradients().
          return dEdOut.dot(dEdOut);  //return the squared error

    } catch (MatrixException e) {e.print();}
    return 0;
  }//end evaluate

  /** update the fGradient vector based on the current fInput vector */
  public void findGradient() {
  double normFactor=0;
  	try {
      // calculate dq1/dw
          function.findGradients();         //find the gradients in this for Q(x,u) wrt the weight vector

          if((method.val<2) && (phi.val!=0)){ //residual gradient or residual Q learning

            dEdWeightsQ1.replace(dEdWeights); //store the gradients for the initial Q value
            dEdInQ1.replace(dEdIn);
            mdp.nextState(state,action,newState,dt,valueKnown,rnd); //generate an independent successor state
            state.replace(newState);  //set up the state vector with the successor state
            mdp.findValAct(state,action,function,outputs,valueKnown);	//find the value of the successor state x'
            function.evaluate();
            function.findGradients(); //calculate the gradients with respect to the new state

      // reset the action to be the original action passed in
            action.replace(oldAction);

      //valueKnown is a flag that determines if we have entered an absorbing state where we know
      //the actual value
          if(valueKnown.val) {
            endTrajectory.val=true;
            dEdWeights.mult(0);
            dEdIn.mult(0);
            valueKnown.val=false;
          }

 	      dEdWeights.mult(phi.val*gamma.val);
	      dEdIn.mult(gamma.val);
	      dEdWeights.sub(dEdWeightsQ1);
	      dEdIn.sub(dEdInQ1);
	    } else {    //direct Q learning
	      if (valueKnown.val) {
            endTrajectory.val=true;
            valueKnown.val=false;
          }
	      dEdWeights.mult(-1);
	      dEdIn.mult(-1);
	    }
	} catch (MatrixException e) {e.print();}
  } //end findGradient
}//end class QLearning

