package sim.funApp;
import parse.*;
import matrix.*;
import pointer.*;
import watch.*;
import java.util.*;
import expression.*;

/** A standard lookup table of n elements (weights).
  * IMPORTANT NOTES AND REMINDERS:
  * 1) dEdOut must be a single element MatrixD object.
  * 2) There should not be a "bias".  This is not a neural network.
  * 3) There should be a 3 tuple passed as a parameter to this object for each dimension
  *    of the lookup table.  The first element is the minimum value, followed by the
  *    maximum value, and finally the discretization factor (the number of elements in that
  *    dimension).
  * 4) It is not necessary for the inputs to be rounded to discrete values before being passed
  *    to this object.  The object will discretize the inputs automatically.  The input vector
  *    is treated as being a vector of indices to the lookup table.  Any index that is below or
  *    above the minimum or maximum index for that dimension will be clipped.
  * 5) The formula for calculating an index for a given input variable a is:
  *    floor[(n(a-min))/(max-min)]  where n is the number of elements for this dimension.
  * 6) Hessian calculations are not yet implemented.
  *
  *    <p>This code is (c) 1997 Mance Harmon
  *    <<a href=mailto:harmonme@aa.wpafb.af.mil>harmonme@aa.wpafb.af.mil</a>>,
  *    <a href=http://www.aa.wpafb.af.mil/~harmonme>http://www.aa.wpafb.af.mil/~harmonme</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 2.0, 8 May 97
  * @author Mance Harmon
  * @author Leemon Baird
  */
public class LookupTable extends FunApp {
  //v. 2.0 8 May 97 added multiple outputs, completely reorganized all
  //                variables and data structures to be much faster,
  //                more memory efficient, and readable (e.g. put comments on global
  //                variables, eliminated unneeded global variables, and lengthened
  //                global variable names like "a"). Also made it deal with
  //                infinite or NaN values for inputs by treating them as -infinity -Leemon Baird
  //v. 1.0 21 January 97 -Mance Harmon


  private MatrixD   wMatrix=null;    //access a particular weight as wMatrix.val(whichOutput,whichInput)
  private double[]  min;             //min value of discretization for each dimension
  private double[]  max;             //max value of discretization for each dimension
  private int[]     numLevels;       //how many levels of discretization for each dimension
  private NumExp[]  minExp;          //an unparsable version of min[]
  private NumExp[]  maxExp;          //an unparsable version of max[]
  private IntExp[]  numLevelsExp;    //an unparsable version of numLevels[]
  private int       index;           //the index of the last bin outputted
  private int       inSize;          //number of elements in input vector
  private int       outSize;         //number of elements in output vector
  private int       numBins;         //number of hyperrectangular bins that input spaces is divided into
  private MatrixD[] bin;             //the vector of output values for each bin.
  private MatrixD[] dEdBin;          //vector which is partial of error wrt output vector for each bin

  /** Define the MatrixD objects that will be used by evaluate()
    * and findGradients().  All 6 should be column vectors (n by 1 matrices).
    * All the MatrixD objects are copied, but the pointers still point
    * to the same data arrays.
    * @exception MatrixException if vector shapes don't match
    */
  public void setIO(MatrixD inVect,   MatrixD outVect,    MatrixD weights,
                    MatrixD dEdIn,    MatrixD dEdOut,     MatrixD dEdWeights,
                    MatrixD dEdIndIn, MatrixD dEdOutdOut, MatrixD dEdWeightsdWeights)
                 throws MatrixException {
    super.setIO(inVect, outVect, weights, dEdIn, dEdOut, dEdWeights,  dEdIndIn, dEdOutdOut, dEdWeightsdWeights);

    if ((dEdIn     !=null && inVect.size !=dEdIn.size) ||
        (dEdOut    !=null && outVect.size!=dEdOut.size) ||
        (dEdWeights!=null && weights.size!=dEdWeights.size))
      throw new MatrixException ("LookupTable.java: vector sizes don't match");

    if (inSize!=inVect.size)
      throw new MatrixException ("LookupTable.java: input vector has "+
                                 inVect.size+" elements, but there are "+
                                 inSize+" triplets, which is a different number.");

    wMatrix=((MatrixD)weights.clone());

    outSize=outVect.size;
    for (int i=0;i<wMatrix.size/outSize;i++) { //break up weight vector into bins
      bin   [i]=   wMatrix.submatrix(i*outSize,outSize,1);
      if (dEdWeights!=null)
        dEdBin[i]=dEdWeights.submatrix(i*outSize,outSize,1);
    }
  }//end setIO

  /** calculate the output for the given input */
  public void evaluate() {
    int    level=0; //the discretized level of each element of the input vector
    double input=0; //each element of input vector
    index=0;
    try {
      for(int i=0; i<inSize; i++){
        input=inVect.val(i);
        level=(int)Math.floor(numLevels[i]*(input-min[i])/(max[i]-min[i]));

        if (level<0)
          level=0;
        if (level>numLevels[i]-1)
          level=numLevels[i]-1;

        index=index*numLevels[i] + level;
      }
      outVect.replace(bin[index]);
    } catch (MatrixException e) {
      e.print();
    }
  }

  /** Calculate the output and gradient for a given input.
    * This does everything evaluate() does, plus it calculates
    * the gradient of the error with respect to the inputs and
    * weights, dEdx and dEdw,
    * User must set dEdOut before calling.
    */
  public void findGradients() {
    try {
      dEdWeights.mult(0);
      dEdBin[index].replace(dEdOut);
    } catch (MatrixException e) {
      e.print();
    }
  }

  /** Calculate the output, gradient, and Hessian for a given input.
    * This does everything evaluate() and findGradients() do, plus
    * it calculates the Hessian of the error with resepect to the
    * the weights and inputs, dEdxdx, dEdwdx, and dEdwdw.
    */
  public void findHessian(){
    //this method not yet implemented
    //dEdWdW is block diagonal with block number index equalling dEdOutdOut and other blocks zero.
    //dEdWdIn and dEdIndIn are all zeros.
  }

  /** Return # elements (weights) in the lookup table.
    */
  public int nWeights(int nIn,int nOut) {
    return numBins*nOut;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "('{' NumExp NumExp IntExp '}')+ "+
           "//Lookup Table. "+
           "The triplets are ordered {min max levels} where min and max "+
           "refer to the variable, and levels is the number of discretizations "+
           "in that dimension.  The min and max can be real valued.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emitLine();
    for(int n=0; n<inSize; n++){
       u.emit(" { ");
       u.emitUnparse(minExp[n],lang); u.emit(" ");
       u.emitUnparse(maxExp[n],lang); u.emit(" ");
       u.emitUnparse(numLevelsExp[n],lang);
       u.emit("}");
       u.emitLine();
    }
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector v          = new Vector();
    Vector vMin       = new Vector();
    Vector vMax       = new Vector();
    Vector vNumLevels = new Vector();
    NumExp numD;
    IntExp numI;
    inSize=0;
    while (true) { //parse whatever parameters are there
      if (p.parseChar('{',false)) {
        inSize++;
        numD=(NumExp)p.parseClass("NumExp",lang,true);
        vMin.addElement(numD);
        numD=(NumExp)p.parseClass("NumExp",lang,true);
        vMax.addElement(numD);
        numI=(IntExp)p.parseClass("IntExp",lang,true);
        vNumLevels.addElement(numI);
        p.parseChar('}',true);
      } else
        break;
    }
    minExp      = new NumExp[inSize]; vMin.copyInto(minExp);
    maxExp      = new NumExp[inSize]; vMax.copyInto(maxExp);
    numLevelsExp= new IntExp[inSize]; vNumLevels.copyInto(numLevelsExp);
    min         = new double[inSize];
    max         = new double[inSize];
    numLevels   = new int   [inSize];

    for (int i=0;i<inSize;i++) {
      min[i]=minExp[i].val;
      max[i]=maxExp[i].val;
      numLevels[i]=numLevelsExp[i].val;
    }

    numBins=1; //number of bins in the lookup table (each is a vector of outputs)
    for (int j=0; j<inSize; j++)
      numBins*=numLevels[j];
    bin   =new MatrixD[numBins];
    dEdBin=new MatrixD[numBins];

    return this;
  } //parse

  /** Make an exact duplicate of this class.  For objects it contains, it
    * only duplicates the pointers, not the objects they point to.  For a
    * new FunApp called MyFunApp, the code in this method should be the
    * single line: return cloneVars(new MyFunApp());
    */
  public Object clone() {
    return cloneVars(new LookupTable());
  }

  /** After making a copy of self during a clone(), call cloneVars() to
    * copy variables into the copy, then return super.cloneVars(copy).
    * The variables copied are just those set in parse() and
    * setWatchManager().  The caller will be required to call
    * setIO to set up the rest of the variables.
    */
  public Object cloneVars(FunApp copy) {
    LookupTable c=(LookupTable)copy;
    c.wMatrix      = wMatrix;
    c.min          = min;
    c.max          = max;
    c.numLevels    = numLevels;
    c.minExp       = minExp;
    c.maxExp       = maxExp;
    c.numLevelsExp = numLevelsExp;
    c.index        = index;
    c.inSize       = inSize;
    c.outSize      = outSize;
    c.numBins      = numBins;
    c.bin          = bin;
    c.dEdBin       = dEdBin;
    return super.cloneVars(copy);
  }
} //LookupTable
