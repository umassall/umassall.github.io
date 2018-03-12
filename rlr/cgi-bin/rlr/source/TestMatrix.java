import parse.*;
import matrix.*;

/** Test the MatrixD object (useful for debugging native-code implementations).
  *    <p>This code is (c) 1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.               <p>
  *
  *    This was tested with Symantec Cafe 1.51 with JIT 3.0 beta 3, with
  *    both pure Java, and with the MatrixD.dll (compiled by Microsoft Visual C++
  *    Developer Studio, 1995 version).  The test was run under WinNT on
  *    a 200 MHz Pentium Pro.                                                  <p>
  *                                                                            <pre>
  *
  *    MatrixF: exp=0, iter=5000, size= 30 seconds(with/without DLL)= 3.484 17.735
  *    MatrixD: exp=0, iter=5000, size= 30 seconds(with/without DLL)= 3.906 18.062
  *
  *    MatrixF: exp=1, iter=5000, size= 30 seconds(with/without DLL)= 3.453 17.735
  *    MatrixD: exp=1, iter=5000, size= 30 seconds(with/without DLL)= 3.89  18.047
  *
  *    MatrixF: exp=2, iter=5000, size= 30 seconds(with/without DLL)= 3.344 17.39
  *    MatrixD: exp=2, iter=5000, size= 30 seconds(with/without DLL)= 3.719 17.625
  *
  *    MatrixF: exp=3, iter= 200, size= 30 seconds(with/without DLL)= 0.125  0.672
  *    MatrixD: exp=3, iter= 200, size= 30 seconds(with/without DLL)= 0.14   0.688
  *
  *    MatrixF: exp=0, iter=   4, size=300 seconds(with/without DLL)= 5.985 18.141
  *    MatrixD: exp=0, iter=   4, size=300 seconds(with/without DLL)=10.203 21.938
  *
  *    MatrixF: exp=1, iter=   4, size=300 seconds(with/without DLL)= 5.985 18.125
  *    MatrixD: exp=1, iter=   4, size=300 seconds(with/without DLL)=10.219 22.016
  *
  *    MatrixF: exp=2, iter=   4, size=300 seconds(with/without DLL)= 5.766 17.921
  *    MatrixD: exp=2, iter=   4, size=300 seconds(with/without DLL)= 9.969 21.844
  *
  *    MatrixF: exp=3, iter=   4, size=300 seconds(with/without DLL)= 5.719 17.875
  *    MatrixD: exp=3, iter=   4, size=300 seconds(with/without DLL)= 9.937 21.766
  *                                                                            </pre>
  *    MatrixD is used throughout WebSim.
  *    These numbers were checked with Cafe's compiler set for
  *    "debug" and "release", and the speed was equal in both cases.
  * @version 1.0, 15 Apr 97
  * @author Leemon Baird
  */
public class TestMatrix extends Project {
  private int line; //the line of text being written to now

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//Test the MatrixD object.  Useful for debugging native-code implementations.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    return this;
  } //end method parse

  /** Run the project in a separate thread, and let the thread die when done. */
  public void run() {
    try { //force a native load now so its message isn't interleaved with the numbers
      MatrixD a=new MatrixD(1);
      MatrixF b=new MatrixF(1);
    } catch (Throwable e) {
    }

    //experiment, iterations, size
    test(0,5000,30);
    test(1,5000,30);
    test(2,5000,30);
    test(3, 200,30);

    test(0,4,300);
    test(1,4,300);
    test(2,4,300);
    test(3,4,300);
  }

  /** run a series of tests comparing MatrixD and MatrixF with and without DLL */
  public void test(int exp,int iter, int size) {
    System.out.println(" ");
    System.out.print("MatrixF: exp="+exp+", iter="+iter+", size="+size+" seconds(with/without DLL)=");
    testF(exp,iter,size,true);
    testF(exp,iter,size,false);
    System.out.println(" ");
    System.out.print("MatrixD: exp="+exp+", iter="+iter+", size="+size+" seconds(with/without DLL)=");
    testD(exp,iter,size,true);
    testD(exp,iter,size,false);
    System.out.println(" ");
    System.out.println(" ");
  }

  /** test the MatrixD class. iter=# iterations for timing test,
    * exp=which experiment to run, size=size of arrays/vectors.
    */
  public void testD(int exp,int iter,int size,boolean useNative) {
    try {
//      System.out.println("==================== Testing MatrixD ====================");
      MatrixD a=new MatrixD(5,10);
      MatrixD b=a.submatrix(1,1,3,3);
      MatrixD c=a.submatrix(1,5,3,4);
      MatrixD d=((MatrixD)c.clone()).transpose();
      MatrixD e=b.duplicate();

      b.diag(3).add(1).mult(2);
      c.setRandom(0,1,new Random());
      e.mult(c,d);
      e.setRandom(0,1,new Random());
 //     System.out.println("a="+a.toString(true));
 //     System.out.println("b="+b);
 //     System.out.println("c="+c);
 //     System.out.println("d="+d);
 //     System.out.println("e="+e);
      if (!a.equalEls(a))
        System.out.println("*************** ERROR: a!=a ********************");
      if (b.equalEls(e))
        System.out.println("*************** ERROR: b==e ********************");
 //     System.out.println("d+e="+e.add(b));

      MatrixD b2=a.submatrix(1,1,3,2);
      MatrixD c2=a.submatrix(1,3,3,2);
      MatrixD d2=a.submatrix(1,5,2,2);
      b2.set(0,0,1);  b2.set(0,1,4);
      b2.set(1,0,2);  b2.set(1,1,5);
      b2.set(2,0,3);  b2.set(2,1,6);
      c2.set(0,0,.7); c2.set(0,1,.10);
      c2.set(1,0,.8); c2.set(1,1,.11);
      c2.set(2,0,.9); c2.set(2,1,.12);
      c2.transpose();
  //    System.out.println("b2="+b2);
  //    System.out.println("c2="+c2);
  //    System.out.println("d2="+d2);
  //    System.out.println("a="+a);
      d2.mult(c2,b2);
      double[] data={.7 *1+.8 *2+.9 *3,    .7 *4+.8 *5+.9 *6,
                     .10*1+.11*2+.12*3,    .10*4+.11*5+.12*6};
      MatrixD dg=new MatrixD(data,2,2);
  //    System.out.println("d2=c2*b2="+d2);
  //    System.out.println("dg="+dg);
      if (!d2.equalEls(dg))
        System.out.println("*************** ERROR: dg!=de ********************");
      dg.sub(d2);
  //      System.out.println("dg-d2=all zeros="+dg);
      dg.add(d2);
      dg.subFrom(d2);
  //    System.out.println("-dg+d2=all zeros="+dg);
  //    System.out.println("a="+a);
      MatrixD v1=a.submatrix(1,1,3,1);
      MatrixD v2=a.submatrix(1,2,3,1);
  //    System.out.println("v1="+v1);
  //    System.out.println("v2="+v2);
  //    System.out.println("v1.dot(v2)="+v1.dot(v2));
      if (v1.dot(v2)!=32)
        System.out.println("*************** ERROR: v1.dot(v2)!=32 ************");

      //benchmark test:
      MatrixD big1=new MatrixD(size,size);
      MatrixD big2=new MatrixD(size,size);
      MatrixD big3=new MatrixD(size,size);
      MatrixD vv  =new MatrixD(size,1);
      big1.useNative(useNative);
      big2.useNative(useNative);
      big3.useNative(useNative);
      vv.useNative(useNative);
      long startTime=System.currentTimeMillis();
      switch (exp) {
        case 0: { //multiply size*size matrix by 1.0
          for (int i=0;i<iter;i++)
            big1.mult(1);
        }
        case 1: { //multiply transposed size*size matrix by 1.0
          big1.transpose();
          startTime=System.currentTimeMillis();
          for (int i=0;i<iter;i++)
            big1.mult(1);
        }
        case 2: { //multiply size vector and size*size
          for (int i=0;i<iter;i++)
            vv.mult(big1,vv);
        }
        case 3: { //multiply two size*size matrices
          for (int i=0;i<iter;i++)
            big1.mult(big2,big3);
        }
      }
      System.out.print(" "+(System.currentTimeMillis()-startTime)/1000.);
      big1=big2=big3=null;
    } catch (MatrixException e) {
      e.print();
    }
  }//end testD

  /** test the MatrixF class. iter=# iterations for timing test,
    * exp=which experiment to run, size=size of arrays/vectors.
    */
  public void testF(int exp,int iter,int size,boolean useNative) {
    try {
//      System.out.println("==================== Testing MatrixF ====================");
      MatrixF a=new MatrixF(5,10);
      MatrixF b=a.submatrix(1,1,3,3);
      MatrixF c=a.submatrix(1,5,3,4);
      MatrixF d=((MatrixF)c.clone()).transpose();
      MatrixF e=b.duplicate();

      b.diag(3).add(1).mult(2);
      c.setRandom(0,1,new Random());
      e.mult(c,d);
      e.setRandom(0,1,new Random());
//      System.out.println("a="+a.toString(true));
//      System.out.println("b="+b);
//      System.out.println("c="+c);
//      System.out.println("d="+d);
//      System.out.println("e="+e);
      if (!a.equalEls(a))
        System.out.println("*************** ERROR: a!=a ********************");
      if (b.equalEls(e))
        System.out.println("*************** ERROR: b==e ********************");
//      System.out.println("d+e="+e.add(b));

      MatrixF b2=a.submatrix(1,1,3,2);
      MatrixF c2=a.submatrix(1,3,3,2);
      MatrixF d2=a.submatrix(1,5,2,2);
      b2.set(0,0,1);  b2.set(0,1,4);
      b2.set(1,0,2);  b2.set(1,1,5);
      b2.set(2,0,3);  b2.set(2,1,6);
      c2.set(0,0,(float).7); c2.set(0,1,(float).10);
      c2.set(1,0,(float).8); c2.set(1,1,(float).11);
      c2.set(2,0,(float).9); c2.set(2,1,(float).12);
      c2.transpose();
//      System.out.println("b2="+b2);
//      System.out.println("c2="+c2);
//      System.out.println("d2="+d2);
//      System.out.println("a="+a);
      d2.mult(c2,b2);
      float[] data={(float)(.7 *1+.8 *2+.9 *3),    (float)(.7 *4+.8 *5+.9 *6),
                    (float)(.10*1+.11*2+.12*3),    (float)(.10*4+.11*5+.12*6)};
      MatrixF dg=new MatrixF(data,2,2);
//      System.out.println("d2=c2*b2="+d2);
//      System.out.println("dg="+dg);
      if (!d2.equalEls(dg))
        System.out.println("*************** ERROR: dg!=de ********************");
      dg.sub(d2);
//      System.out.println("dg-d2=all zeros="+dg);
      dg.add(d2);
      dg.subFrom(d2);
//      System.out.println("-dg+d2=all zeros="+dg);
//      System.out.println("a="+a);
      MatrixF v1=a.submatrix(1,1,3,1);
      MatrixF v2=a.submatrix(1,2,3,1);
//      System.out.println("v1="+v1);
//      System.out.println("v2="+v2);
//      System.out.println("v1.dot(v2)="+v1.dot(v2));
      if (v1.dot(v2)!=32)
        System.out.println("*************** ERROR: v1.dot(v2)!=32 ************");

      //benchmark test:
      MatrixF big1=new MatrixF(size,size);
      MatrixF big2=new MatrixF(size,size);
      MatrixF big3=new MatrixF(size,size);
      MatrixF vv  =new MatrixF(size,1);
      big1.useNative(useNative);
      big2.useNative(useNative);
      big3.useNative(useNative);
      vv.useNative(useNative);
      long startTime=System.currentTimeMillis();
      switch (exp) {
        case 0: { //multiply size*size matrix by 1.0
          for (int i=0;i<iter;i++)
            big1.mult(1);
        }
        case 1: { //multiply transposed size*size matrix by 1.0
          big1.transpose();
          startTime=System.currentTimeMillis();
          for (int i=0;i<iter;i++)
            big1.mult(1);
        }
        case 2: { //multiply size vector and size*size
          for (int i=0;i<iter;i++)
            vv.mult(big1,vv);
        }
        case 3: { //multiply size vector and size*size
          for (int i=0;i<iter;i++)
            big1.mult(big2,big3);
        }
      }
      System.out.print(" "+(System.currentTimeMillis()-startTime)/1000.);
      big1=big2=big3=null;
    } catch (MatrixException e) {
      e.print();
    }
  }//end testF

} // end class TestMatrix
