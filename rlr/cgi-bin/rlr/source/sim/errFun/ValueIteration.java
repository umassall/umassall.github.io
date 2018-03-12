package sim.errFun;
import parse.*;
import watch.*;
import sim.mdp.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import Random;

/** Perform Value Iteration with a given Markov Decision
  * Process, function approximator, and gradient-descent algorithm.  The derivative
  * calculations with respect to the inputs have not been fully implemented here.
  *
  *    <p>This code is (c) 1997 Mance E. Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  *
  * @version 1.03, 21 July 97
  * @author Mance E. Harmon
  */
public class ValueIteration extends RLErrFun {
//Version 1.03 21 July 97: Moved all gradient calculations to findGradients() - MH
//Version 1.02 25 June 97: Corrected bug in 'direct' implementation.  When valueKnown=true the dEdWeights was being set
//to 0 even when method=direct.
//Version 1.01 23 June 97: Optimized so that when phi=0 extra deriv calcs are not performed.
//Version 1.0 16 June 97

  /** gradient of mean squared error wrt weights of maximum advantage in successor state*/
  protected MatrixD dEdWeightsV1=null;
  /** A copy of the original state.  */
  protected MatrixD oldState=null;
  /** The random number generator that will be used for this object.  This is a copy of the generator passed to evaluate() */
  protected Random rnd=new Random(0);

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//Value Iteration.";
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
  } //No parameters to parse


  /** Create inputs and state vectors.  Also, create any vectors that might be specific to this module. */
  public void initVects(MDP mdp,RLErrFun rl) {
    statesOnly=true;
    inputs         =new MatrixD(mdp.stateSize()+1); //create vectors of appropriate sizes
    int numWeights = rl.function.nWeights(inputs.size,1); //specific to ALearning
    dEdWeightsSum  =new MatrixD(numWeights);
    dEdWeightsV1   =new MatrixD(numWeights);

    try {
      state       =((MatrixD)inputs.clone()).submatrix(1,mdp.stateSize(),1);
      oldState = new MatrixD(state.size);  //specific to Value Iteration
      newState = new MatrixD(state.size);
      mdp.initialState(newState,rnd);
      mdp.initialState(state,rnd);

    } catch (MatrixException e) {
      e.print();
    }

  }//end initVects()

  /** return the scalar output for the current dInput vector */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    PDouble r=new PDouble(0); //reinforcement
    double maxV1=0; //successor state value: maximum value in X(t+1)
    double V0=0;
    rnd.copyInto(this.rnd);

    try {
        // make a copy of the original state
          oldState.replace(state);

        // evaluate the original state to find V0
          function.evaluate();
          V0=outputs.val(0);

        // calculate the max(R+gammaV(x')) over actions and save gradient
          maxV1=mdp.findValue(state,null,gamma,function,dt,outputs,r,valueKnown,exploration,rnd);
          newState.replace(state);  //the next state is passed back from mdp.findvalue() in state
          function.evaluate();

        // calculate the bellman residual: this is the equivalent of dEdOut and has to be done before calling findGradients().
          dEdOut.set(0,maxV1-V0);
          return dEdOut.dot(dEdOut);  //return the squared error

    } catch (MatrixException e) {
      e.print();
    }
    return 0;
  }//end Evaluate

  /** update the fGradient vector based on the current fInput vector */
  public void findGradient() {
  double normFactor=0;

    try {
        if((method.val<2) && (phi.val!=0)){ //if residual gradient or residual (not direct)
          if(valueKnown.val==true) {
	        endTrajectory.val=true;
            valueKnown.val=false;
          } else {
            function.findGradients();
            dEdWeightsV1.replace(dEdWeights);
          }
      // evaluate the original state again to set up the derivative vector
          state.replace(oldState);
          function.evaluate();
          function.findGradients();

          dEdWeightsV1.mult(phi.val*gamma.val);
          dEdWeightsV1.sub(dEdWeights);
          dEdWeights.replace(dEdWeightsV1);
        } else { //direct method
          if (valueKnown.val) {
            endTrajectory.val=true;
            valueKnown.val=false;
          }
       // evaluate the original state again to set up the derivative vector
          state.replace(oldState);
          function.evaluate();
          function.findGradients();

          dEdWeights.mult(-1);
        }
    } catch (MatrixException e) {
        e.print();
    }
  }//end findGradient
}//end class ValIteration

