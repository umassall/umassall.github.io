package sim.errFun;
import parse.*;
import watch.*;
import sim.data.*;
import sim.funApp.*;
import pointer.*;
import matrix.*;
import expression.*;
import pointer.PDouble;
import Random;

/** Run the PDF Emulation algorithm to model an arbitrary PDF using an arbitrary function approximator.
  * and gradient-descent algorithm.
  *    <p>This code is (c) 1996,1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1, 21 July 97
  * @author Leemon Baird
  */
public class PDFE extends ErrFun {

    private Data     inputSource =null; // the random vector generator used as input to the function approx
    private Data     targetSource=null; // the random vector generator to be emulated
    private FunApp   function1=null; //function approximator mapping v  to f(v )
    private FunApp   function2=null; //function approximator mapping v' to f(v')
    private MatrixD  dEdWeights1 =null; // gradient of error for 1 training example wrt weights
    private MatrixD  dEdWeights2 =null; // gradient of error for 1 training example wrt weights
    private MatrixD  dEdIn       =null; // gradient of error for 1 training example wrt inputs
    private MatrixD  dEdOut1     =null; // gradient of error wrt output of first function approximator
    private MatrixD  dEdOut2     =null; // gradient of error wrt output of second function approximator
    private MatrixD  hessian     =null; // hessian of error for 1 training example wrt weights
    private MatrixD  inputs1     =null; // one input vector to the function approximator
    private MatrixD  inputs2     =null; // other input vector to the function approximator
    private MatrixD  outputs1    =null; // The output vector from the function approximator for one input
    private MatrixD  outputs2    =null; // The output vector from the function approximator for other input
    private MatrixD  targetOut1  =null; // The output of the generator being emulated (x)
    private MatrixD  targetOut2  =null; // The output of the generator being emulated (x prime)
    private MatrixD  diffVX      =null; // v-x
    private MatrixD  diffVpX     =null; // v'-x
    private MatrixD  diffVVp     =null; // v-v'
    private MatrixD  diffXpX     =null; // x'-x
    private PDouble  targX=new PDouble(.5); //(x,y) coordinate of target vector
    private PDouble  targY=new PDouble(.5);
    private PDouble  outX =new PDouble(.5); //(x,y) coordinate of output of net
    private PDouble  outY =new PDouble(.5);
    private PDouble  inX  =new PDouble(.5); //(x,y) coordinate of input to of net
    private PDouble  inY  =new PDouble(.5);
    private NumExp   c    =new NumExp (.1); //constant for the g() function
    private NumExp   minWeight,maxWeight; //range for initial random weights
    private double   gVX;  //gVX =g(v-x)
    private double   gVpX; //gVpX=g(v'-x)
    private double   gVVp; //gVVp=g(v-v')
    private double   gXpX; //gXpX=g(x'-x)
    private PDouble  weight=new PDouble(0); //to allow weight to be plotted over time

  /** Register all variables with this WatchManager.
    * This will be called after all parsing is done.
    * setWatchManager should be overridden and forced to
    * call the same method on all the other objects in the experiment.
    */
  public void setWatchManager(WatchManager wm,String name) {
    super.setWatchManager(wm,name);
    wm.registerVar(name+"target x", targX,   this);
    wm.registerVar(name+"target y", targY,   this);
    wm.registerVar(name+"input x",  inX,     this);
    wm.registerVar(name+"input y",  inY,     this);
    wm.registerVar(name+"output x", outX,    this);
    wm.registerVar(name+"output y", outY,    this);
    wm.registerVar(name+"c",        c,       this);
    wm.registerVar(name+"weight",   weight,  this);
  }

  /** Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'{"+
           "['c' NumExp] "+
           "'input'  <sim.data.Data> "+
           "'target' <sim.data.Data> "+
           "'funApp' <sim.funApp.FunApp> '}'"+
           "//emulate a target PDF. "+
           "After learning by observing iid vectors from the PDF, "+
           "this can emulate it by sending uniform iid vectors into the "+
           "function approximator, and the resulting vectors will be "+
           "distributed correctly.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{");
    u.indent();
      u.emit("c ");
      u.emitUnparse(c,lang);
      u.emit(" input ");
      u.emitUnparseWithClassName(inputSource,lang,false);
      u.emitLine();
      u.emit("target ");
      u.emitUnparseWithClassName(targetSource,lang,false);
      u.emitLine();
      u.emit("funApp ");
      u.emitUnparseWithClassName(function1,lang,false);
    u.unindent();
    u.emit("}");
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('{',true);
    if (p.parseID("c",false))
      c=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseID("input",true);
    inputSource=(Data)p.parseType("sim.data.Data",lang,true);
    p.parseID("target",true);
    targetSource=(Data)p.parseType("sim.data.Data",lang,true);
    p.parseID("funApp",true);
    function1=(FunApp)p.parseType("sim.funApp.FunApp",lang,true);
    function2=(FunApp)function1.clone();
    p.parseChar('}',true);

    inputs=inputs1  =new MatrixD(inputSource.outSize()+1); //input to net has a bias term too
    inputs2         =new MatrixD(inputSource.outSize()+1);
    dEdIn           =new MatrixD(inputSource.outSize()+1);
    outputs=outputs1=new MatrixD(targetSource.outSize());
    outputs2        =new MatrixD(targetSource.outSize());
    weights         =new MatrixD(function1.nWeights(inputs1.size,outputs.size));
    dEdOut1         =new MatrixD(outputs1.size);
    dEdOut2         =new MatrixD(outputs1.size);
    targetOut1      =new MatrixD(outputs1.size);
    targetOut2      =new MatrixD(outputs1.size);
    diffVX          =new MatrixD(outputs1.size);
    diffVpX         =new MatrixD(outputs1.size);
    diffVVp         =new MatrixD(outputs1.size);
    diffXpX         =new MatrixD(outputs1.size);
    dEdOut          =new MatrixD(outputs1.size);
    dEdIn           =new MatrixD(inputs.size);
    dEdWeights      =new MatrixD(weights.size);
    dEdWeights1     =new MatrixD(weights.size);
    dEdWeights2     =new MatrixD(weights.size);
    try {
      inputs1.set(inputs1.size-1,1); //set up bias.  Won't be changed
      inputs2.set(inputs2.size-1,1); //set up bias.  Won't be changed
      function1.setIO(inputs1,outputs1,weights,dEdIn,dEdOut1,dEdWeights1,null,null,null);
      function2.setIO(inputs2,outputs2,weights,dEdIn,dEdOut2,dEdWeights2,null,null,null);
    } catch (MatrixException e) {
      e.print();
    }
    return this;
  }//end parse

  /** The g() function maps a distance vector to a positive scalar.
    * Various g() functions can be used, but only the exponential
    * one is given here.
    * If either of g() or gPrime() is changed, the other should be changed too.
    */
  private final static double g(MatrixD v, PDouble c) {
    try {
      return -java.lang.Math.exp((-v.dot(v)) * c.val);
    } catch (MatrixException e) {
      e.print();
      return 0;
    }
  }

  /** Given a distance vector v, find the
    * gradient of g(v) with respect to v, and
    * return that gradient in v (overwriting the original v).
    * If either of g() or gPrime() is changed, the other should be changed too.
    */
  private final static void gPrime(MatrixD v,PDouble c) {
    try {
      v.mult(java.lang.Math.exp(-v.dot(v)*c.val)*2*c.val);
    } catch (MatrixException e) {
      e.print();
    }
  }

  /////////////////////////////////////////////////////////////////////

  // the following are called by the gradient descent algorithm.
  // evalutate() returns the error for a given weight vector x.
  // Depending on the algorithm and settings in the HTML file,
  // this error may be either the true error or an unbiased estimate of
  // the true error

  /** return the scalar output for the current weight vector  x.
    * err=g(x-f(v)) + g(x-f(v')) - g(f(v)-f(v'))
    */
  public double evaluate(Random rnd,boolean willFindDeriv,boolean willFindHess,boolean rememberNoise) {
    try {
      weight.val=weights.val(0);
      inputSource.getData (null,inputs1.data,   rnd);  //get v into input of funApp 1
      inputSource.getData (null,inputs2.data,   rnd);  //get vPrime into input of funApp 2
      targetSource.getData(null,targetOut1.data,rnd);  //get x
      targetSource.getData(null,targetOut2.data,rnd);  //get x prime
      function1.evaluate();       //put f(v) in outputs1
      function2.evaluate();       //put f(vPrime) in outputs2

      inX.val  =inputs1.data[0];
      inY.val  =inputs1.data[1];    //make v visible to watchers (v' is invisible)
      targX.val=targetOut1.data[0];
      targY.val=targetOut1.data[1]; //make x visible to watchers
      outX.val =outputs1.val(0);
      outY.val =outputs1.val(1);    //make f(v) visible to watchers

      diffVX.replace (outputs1);
      diffVX.sub     (targetOut1); //diffVX=v-x
      diffVpX.replace(outputs2);
      diffVpX.sub    (targetOut1); //diffVX=v'-x
      diffVVp.replace(outputs1);
      diffVVp.sub    (outputs2);   //diffVVp=v-v'
      diffXpX.replace(targetOut1);
      diffXpX.sub    (targetOut2); //diffXpX=x'-x

      gVX =g(diffVX ,c); //gVX =g(v -x)
      gVpX=g(diffVpX,c); //gVpX=g(v'-x)
      gVVp=g(diffVVp,c); //gVVp=g(v-v')
      gXpX=g(diffXpX,c); //gXpX=g(x'-x)

      //goal: minimize first two differences, maximize the third,
      // so f(v) and f(v') move toward x, repel each other.
      return gVX+gVpX-gVVp-gXpX;

    } catch (MatrixException e) {
      e.print();
      return 0;
    }
  }//end evaluate

  /** update the fGradient vector based on the dEdOutput.
    * err=g(x-f(v)) + g(x-f(v')) - g(f(v)-f(v'))
    * return gradient of err wrt weights.
    */
  public void findGradient() { //calculate gradient and put in dEdWeights1
    try {
      gPrime(diffVX ,c);  //diffVX =gradient of g(v -x) wrt to each element of (v -x)
      gPrime(diffVpX,c);  //diffVpX=gradient of g(v'-x) wrt to each element of (v'-x)
      gPrime(diffVVp,c);  //diffVVp=gradient of g(v-v') wrt to each element of (v-v')

      dEdOut1.replace(diffVX);
      dEdOut1.sub    (diffVVp);  //dEdOut1=gradient of error wrt each element of v
      dEdOut2.replace(diffVpX);
      dEdOut2.add    (diffVVp);  //dEdOut2=gradient of error wrt each element of v'

      function1.findGradients();
      function2.findGradients();
      dEdWeights.replace(dEdWeights1);
      dEdWeights.add    (dEdWeights2);  //dEdWeights=gradient of error wrt each weight
    } catch (MatrixException e) {
      e.print();
    }
  }//end findGradient
}//end class SupervisedLearning
