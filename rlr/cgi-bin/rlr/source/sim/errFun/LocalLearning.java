package sim.errFun;
import parse.*;
import watch.*;
import sim.data.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import Random;

/** Perform local learning with the given data (only looks at input data)
  *    Gradient descent is performed on interference, I(x,x'), where x and
  *    x' are chosen randomly from the training data with the exception
  *    that x and x' never represent the same sample (otherwise interference
  *    couldn't be reduced for that error surface produced by x=x').
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.09, 23 July 97
  * @author Leemon Baird
  * @author Mance Harmon
  * @author Scott Weaver
  */
public class LocalLearning extends ErrFun {
    //revision history:
    // 1.09 changed interface to match ErrFun - MH
    // 1.08 added evaluateAgain() declaration, Not yet implemented - MH
    // 1.07 New class (LocalLearning) based on SupervisedLearning 1.06
    // 1.06 corrects bug introduced in 1.05 limiting it to Single Outputs
    // 1.05 changed to extend errFun
    // 1.04 changed to use Random rather than Math.Random or java.lang.Random
    // 1.02 changes the parameter indicating epoch-wise or incremental from mode
    //      to the boolean type 'incremental' -Mance Harmon

    // 1.01 adds ability to train on epochs. evaluate() and findGradient()
    //      are changed. Parameter mode has been added, telling whether
    //      to do incremental or epoch. -Mance Harmon

    /** the input/output pairs for the training set */
    protected Data data=null;
    /** the mode of learning: incremental or epoch-wise*/
    protected boolean incremental=true;
    /** This is used to sum the dEdIn when doing epoch-wise training */
    MatrixD dEdInSum=null;
    /** The sum of the errors for use in returning the actual MSE */
    double errorSum=0;

    /** */
    MatrixD dEdIndIn=null;
    /** */
    MatrixD dEdOutdOut=null;

    protected double N,D;

    /** hessian of mean squared error for 1 training example wrt weights */
    MatrixD dedWdW_x =null;
    /** other dedWdW_x (x') */
    MatrixD dedWdW_xp=null;

    /** other gradient (x) */
    MatrixD dedW_x=null;
    /** other gradient (x') */
    MatrixD dedW_xp=null;

    /** holding variable of size weights*/
    MatrixD holdWeightSize=null;
    /** other derivitive of I wrt W  */
    MatrixD dIdWeights=null;
    /** pointer to interference */
    protected PDouble interference=new PDouble(0);

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    data.setWatchManager(wm,name+"data/");
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{' 'incremental' <boolean> 'data' <sim.data.Data> "+
           "'funApp' <sim.funApp.FunApp> '}'"+
           "//learn a input/output mapping. "+
           "If incremental=false, then it does epochwise training. "+
           "The 'data' object may represent a finite or infinite "+
           "training set.  If infinite, then 'epochwise' will be a "+
           "pseudoepoch, where a large set of points are grouped "+
           "together and treated as if they were a full epoch.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      if (incremental)
        u.emit("incremental true ");
      else
        u.emit("incremental false ");
      u.emitLine();
      u.emit("data ");
      u.emitUnparseWithClassName(data,lang,false);
      u.emitLine();
      u.emit("funApp ");
      u.emitUnparseWithClassName(function,lang,false);
    u.unindent();
    u.emit("}");
    u.emitLine();
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    p.parseID("incremental",true);
    incremental=p.parseBoolean(true);
    p.parseID("data",true);
    data=(Data)p.parseType("sim.data.Data",lang,true);
    p.parseID("funApp",true);
    function=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
    p.parseChar('}',true);

    inputs            =new MatrixD(data.inSize()); //create vectors of appropriate sizes
    outputs           =new MatrixD(data.outSize());
    weights           =new MatrixD(function.nWeights(inputs.size,outputs.size));
    dEdIn             =new MatrixD(inputs.size);
    dEdOut            =new MatrixD(outputs.size); //also gradient of err wrt outputs
    dEdWeights        =new MatrixD(weights.size);
    dEdWeightsSum     =new MatrixD(weights.size);
    dEdInSum          =new MatrixD(inputs.size);
    dEdIndIn          =new MatrixD(inputs.size,inputs.size);
    dEdOutdOut        =new MatrixD(outputs.size,outputs.size); //also gradient of err wrt outputs
    hessian           =new MatrixD(weights.size,weights.size);
    dIdWeights        =new MatrixD(weights.size);
    holdWeightSize    =new MatrixD(weights.size);
    dedW_x            =new MatrixD(weights.size);
    dedW_xp           =new MatrixD(weights.size);
    dedWdW_x          =new MatrixD(weights.size,weights.size);
    dedWdW_xp         =new MatrixD(weights.size,weights.size);
    try {
      function.setIO(inputs,outputs,weights,dEdIn,dEdOut,dEdWeights,dEdIndIn,dEdOutdOut,hessian);
    } catch (MatrixException e) {
      e.print();
    }
    return this;
  }//end parse

  /** The gradient of f(x) with respect to x (a column vector)
    * this should override ErrFun.java which returns dEdWeights by default */
  public MatrixD getGradient() {
    return dIdWeights;
  }

  // the following are called by the gradient descent algorithm.
  // f(x) is the mean squared error for a single training example
  // if doing incremental training.  If doing epoch-wise training
  // f(x) is calculated using a single training example, but the
  // weights are updated only after averaging the weight changes
  // for all input-output pairs.
  // for a given weight vector x.

  /** return the scalar output for the current weight vector  x*/
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    int j,pairs=data.nPairs();
    try {
      if (incremental) { //incremental training
        dEdOutdOut.mult(0);   // regardless of cf we init like this

        int r=rnd.nextInt(0,pairs-1);  // make sure x' is the other point
        data.getData(r,inputs.data,dEdOut.data);  // I don't care about desiredOutputs
        dEdOut.mult(0);       // want gradient and hessian with respect to
        dEdOut.add(1);        // output not some cost function (cf).  Therefore
        function.evaluate();
        function.findHessian();
        dedW_xp.replace(dEdWeights);
        dedWdW_xp.replace(hessian);

        int rr=rnd.nextInt(0,pairs-2);  // make sure x is the other point
        if(rr==r)rr++;
        data.getData(rr,inputs.data,dEdOut.data);
        dEdOut.mult(0);       // want gradient and hessian with respect to
        dEdOut.add(1);        // output not some cost function (cf).  Therefore
        function.evaluate();
        function.findHessian();
        dedW_x.replace(dEdWeights);
        dedWdW_x.replace(hessian);

        D=dedW_x.dot(dedW_x);
        N=dedW_x.dot(dedW_xp);

        if(D<java.lang.Double.MIN_VALUE) {
            interference.val=1.0; // shouldn't be 0 since that will screw up average
            System.out.println("interference set to 1.0");
            }
        else
            interference.val=N/D;
        return (.5*interference.val*interference.val); // return (1/2) I^2
      } else { //epochwise training
        errorSum=0;
        dEdWeightsSum.mult(0); //initialize the sum matrices
        dEdInSum.mult(0);
        for (j=0; j<pairs; j++) {
          data.getData(j,inputs.data,dEdOut.data); //get desired outputs
          function.evaluate();  //calculate actual output for given fInput vector
          dEdOut.subFrom(outputs); //find actual-desired
          errorSum+=dEdOut.dot(dEdOut)/dEdOut.size;  //sum the MSEs
          function.findGradients();               // calculate dEdWeights and dEdIn for the given i-o pair
          dEdWeightsSum.add(dEdWeights);
          dEdInSum.add(dEdIn);
        }
        return errorSum/pairs;     //sum squared error for all inputs
      }//end else epochwise training
    } catch (MatrixException e) {
      e.print();
    }
    return 0;
  }//end evaluate

  /** update the fGradient vector based on the dEdOutput */
  public void findGradient() {
        int pairs=data.nPairs();
        double inverse_pairs=(double)(1.0/(double)pairs);

        try {
            if (incremental==true) {
                holdWeightSize.mult(dedWdW_x, dedW_xp); // M.vp
                dIdWeights.mult    (dedWdW_xp,dedW_x ); // Mp.v
                dIdWeights.add(holdWeightSize);         // M.vp+Mp.v
                dIdWeights.mult(1/D);                   // (M.vp+Mp.v)/D

                holdWeightSize.mult(dedWdW_x,dedW_x);   // M.v
                holdWeightSize.mult(-2*N/D/D);          // -2(N/D/D)(M.v)

                dIdWeights.add(holdWeightSize); //         -2(N/D/D)(M.v) + (M.vp+Mp.v)/D
                dIdWeights.mult(N/D);  //   (N/D)     [-2(N/D/D)(M.v) + (M.vp+Mp.v)/D]
                } else { //used for epoch-wise training
                System.out.println("LocalLearning shouldn't be doing batch");
                dEdWeightsSum.mult(inverse_pairs); //find the average of the gradient
                dEdWeights.mult(0); // zero out the old gradient
                dEdWeights.add(dEdWeightsSum); //set dEdWeghts equal to the new gradient
                dEdInSum.mult(inverse_pairs);
                dEdIn.mult(0);
                dEdIn.add(dEdInSum);
            }
        } catch (MatrixException e) {
                e.print();
        }
  }

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {
    super.initialize(level);
    data.initialize(level);
  }
}//end class LocalLearning
