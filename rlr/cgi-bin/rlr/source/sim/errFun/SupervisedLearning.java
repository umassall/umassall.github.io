package sim.errFun;
import parse.*;
import watch.*;
import sim.data.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import Random;

/** Perform supervised learning with the given data, function approximator,
  * and gradient-descent algorithm.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.08, 21 July 97
  * @author Leemon Baird
  * @author Mance Harmon
  */
public class SupervisedLearning extends ErrFun {
    //revision history:
    // 1.08 added dEdOutdOut, dEdIndIn, dEdWeightsdWeights - Scott Weaver 10 June 97
    // 1.07 corrects minor bug in unparse - MH
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
    u.emitLine();
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
    dEdIndIn          =new MatrixD(inputs.size,inputs.size);
    dEdOutdOut        =new MatrixD(outputs.size,outputs.size); //also gradient of err wrt outputs
    dEdWeightsdWeights=new MatrixD(weights.size,weights.size);
    dEdOut            =new MatrixD(outputs.size); //also gradient of err wrt outputs
    dEdWeights        =new MatrixD(weights.size);
    dEdWeightsSum     =new MatrixD(weights.size);
    dEdInSum          =new MatrixD(inputs.size);
    try {
      function.setIO(inputs,outputs,weights,dEdIn,dEdOut,dEdWeights,dEdIndIn,dEdOutdOut,dEdWeightsdWeights);
    } catch (MatrixException e) {
      e.print();
    }
    return this;
  }//end parse

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
        data.getData(inputs.data,dEdOut.data,rnd); //get desired outputs
        function.evaluate();                               //calculate actual outputs
        dEdOut.subFrom(outputs);                   //actual-desired=gradient of error wrt outputs
        return dEdOut.dot(dEdOut)          //mean squared output error
               / dEdOut.size;
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
      if (incremental==true)
        function.findGradients(); //used for incremental training
      else { //used for epoch-wise training
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
}//end class SupervisedLearning
