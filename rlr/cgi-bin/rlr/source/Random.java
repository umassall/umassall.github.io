/** Random number generator.  This is a plug-in replacement for the
  * built-in generator in Java.  This is used instead to ensure
  * it is repeatable, won't change in future versions of Java, and
  * has known good security.  The algorithms used come from Numerical
  * Recipies in C (2nd ed), but the actual code is original, and so
  * not subject to their copyright.  The code here is often simpler
  * than the C code, since it can be written using 64-bit integers.
  *    <p>This code is (c) 1996,1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.1, 22 July 97
  * @author Leemon Baird
  */
public class Random {
  //version 1.1, 22 July 97 added clone()
  //version 1.0, 20 Oct 96
  private long    seed;          // Netscape bug seems to require this to be public (?)
  private long    seed2=0;       // used in algorithm 2
  private long    minLong;       //minimum returned by nextLong()
  private long    maxLong;       //maximum returned by nextLong()
  private long    rangeLong;     //# different values returnable by nextLong()
  private int     algorithm=1;   //which algorithm to use (1 is recommended)
  private boolean refill;        //should rnd # shuffle table be refilled?
  private int     inext, inextp; //used in algorithm 3 (Knuth's)
  private long    iy=0;          //used in algorithms 1 and 2
  private long    table []=new long[32]; //shuffles random numbers in algorithms 1 and 2
  private long    table3[]=new long[56]; //shuffles in algorithm 3

  /** Create a copy of this generator which will generate the same sequence. */
  public Object clone() {
    Random rnd=new Random();
    rnd.table =new long[table.length];
    rnd.table3=new long[table3.length];
    copyInto(rnd);
    return rnd;
  }

  /** Make the existing Random rnd into an exact clone of this Random.
    * It will then generate the same sequence as this.
    */
  public void copyInto(Random rnd) {
    rnd.seed     =seed;
    rnd.seed2    =seed2;
    rnd.minLong  =minLong;
    rnd.maxLong  =maxLong;
    rnd.rangeLong=rangeLong;
    rnd.algorithm=algorithm;
    rnd.refill   =refill;
    rnd.inext    =inext;
    rnd.inextp   =inextp;
    rnd.iy       =iy;
    System.arraycopy(table ,0,rnd.table ,0,table.length);
    System.arraycopy(table3,0,rnd.table3,0,table3.length);
  }

  /** creates a new random number generator and sets its seed
    * randomly according to the current time.
    */
  public Random() {
    setSeed();
    setAlgorithm(1);
  }

  /** creates a new random number generator and sets its seed */
  public Random(long s) {
    setSeed(s);
    setAlgorithm(1);
  }

  /** Sets which algorithm to use henceforth.
    * Compared to algorithm zero, 1 takes 30% longer to run, 2 takes
    * twice as long, and 3 takes 40% less time.  Numerical Recipes
    * in C recommends using 1 in most cases, or 2 for extremely good numbers,
    * and offers a $1000 prize for anyone finding flaws in 2.
    * If more than 100 million numbers are needed, it recommends
    * 2 rather than 1, even though it takes 50% longer.
    * Note that for 1, 2, and 3, if the random number generator is
    * seeded with a given number and then run, it will always generate
    * the same sequence.  But, if one looks at the seed halfway through
    * the sequence, reseeding with that seed in the future will *not*
    * make it generate the second half of the sequence.  This is because
    * the true "seed" is actually a large table which is regenerated
    * from scratch every time setSeed() is called.
    * To store the state of a sequence so it can be regenerated later,
    * use clone().                                                       <pre>
    * -1 = ANSI C "example" algorithm (is bad, and only creates 16-bit rnd #s)
    *  0 = Park and Miller Minimum Standard (CACM, 88, from Lewis,Goodman,Miller, 69)
    *  1 = alg 0 plus random shuffling
    *  2 = two LCGs combined with a shuffle
    *  3 = Knuth's subtractive algorithm (probably a good algorithm)     </pre>
    */
  public final void setAlgorithm(int alg) {
    if (alg>=-1 && alg<=3) {
      algorithm=alg;
      minLong=(alg==-1 ? 0 : 1);
      maxLong=(alg==-1 ? 0x7FFFL     : //15 bits, all ones
               alg<2   ? 0x7FFFFFFEL : //31 bits, all but LSB ones
               0xFFFFFFFFL);           //32 bits, all ones
      rangeLong=maxLong-minLong+1L;
      seed=((seed % maxLong) + maxLong) % maxLong; //keep seed in legal range
      if (seed==0) setSeed(123459876L); //zero seeds are sometimes bad
    }
    refill=true; //after changing seed or algorithm, must refill the tables
  } //end setAlgorithm

  /** Sets the random number generator seed */
  public final void setSeed(long s) {
    seed=s;
    setAlgorithm(algorithm); //force seed into range [0..maxLong]
  }

  /** Sets the random number generator seed from the system clock*/
  public final void setSeed() {
    setSeed((new java.util.Random()).nextLong());
  }

  /** return the minimum value ever returned by nextLong() */
  public final long minLong() {
    return minLong;
  }

  /** return the maximum value ever returned by nextLong() */
  public final long maxLong() {
    return maxLong;
  }

  private final static long c1 =1103515245L;  //used in alg -1
  private final static long c2 =12345;        //used in alg -1
  private final static long c3 =(1L<<32);     //used in alg -1
  private final static long c4 =16807;        //used in alg 0 and 1
  private final static long c5 =(1L<<31)-1;   //used in alg 0 and 1 (=2147483647)
  private final static long c6 =(1L<<31)-2;   //used in alg 1
  private final static long c7 =(1L<<31)-85;  //used in alg 2
  private final static long c8 =(1L<<31)-249; //used in alg 2
  private final static long c9 =40014;        //used in alg 2
  private final static long c10=40692;        //used in alg 2
  private final static long c11=161803398L;   //used in alg 3
  private final static long c12=1000000000L;  //used in alg 3 (=10**9)

  /** Generates a pseudorandom, uniformly-distributed, long value.
    * The number is between minLong() and maxLong() inclusive, which
    * depends on which algorithm is currently selected.
    */
  public final long nextLong() {
    long mj,mk,MZ=0;
    int k,j,ii,i;

    switch (algorithm) {
      case -1 : return seed=(seed * c1 + c2) % c3;
      case  0 : return seed=(seed * c4) % c5;
      case  1 : {
        if (refill) {
          for (j=table.length+7;j>=0;j--) { //skip first 7 numbers, then fill table
            seed=(seed * c4) % c5;  //algorithm 0
            if (j<table.length) table[j]=seed;
          }
          iy=table[0];
        }
        seed=(seed * c4) % c5;  //algorithm 0
        j=(int)(iy/(1L+c6/table.length)); //pick an element of the table
        iy=table[j];
        table[j]=seed;
        return iy;
      } //end case 1
      case 2 : {
        if (refill) {
          seed2=seed;
          for (j=table.length+7;j>=0;j--) { //skip first 7 numbers, then fill table
            seed=(seed * c9) % c7;
            if (j<table.length) table[j]=seed;
          }
          iy=table[0];
        }
        seed =(seed  * c9) % c7;
        seed2=(seed2 * c10) % c8;
        j=(int)(iy/(1+(c7-1L)/table.length));
        iy=table[j]-seed2;
        table[j]=seed;
        if (iy<1)iy+=c7-1;
        return iy;
      }//end case 2
      case 3 : {
        if (refill) {
          mj=(c11-seed)%c12;
          table3[55]=mj;
          mk=1;
          for (i=1;i<=54;i++) {
            ii=(21*i)%55;
            table3[ii]=mk;
            mk=mj-mk;
            if (mk<MZ) mk+=c12;
            mj=table3[ii];
          }
          for (k=1;k<=4;k++)
            for (i=1;i<=55;i++) {
              table3[i] -= table3[1+(i+30)%55];
              if (table3[i]<MZ) table3[i]+=c12;
            }
          inext=0;
          inextp=31;
          seed=1;
        }//end if refill
        if (++inext  == 56) inext =1;
        if (++inextp == 56) inextp=1;
        mj=table3[inext]-table3[inextp];
        if (mj<MZ) mj+=c12;
        table3[inext]=mj;
        return mj;
      }
      default: return 0;
    } //end switch
  } //end nextLong

  /** Generates a pseudorandom, uniformly-distributed, double value
    * in the range [0.0,1.0)
    */
  public final double nextDouble() { //return a random double in the range [0,1)
    return ((double)nextLong()-minLong)/rangeLong;
  }

  /** Generates a pseudorandom, uniformly-distributed, float value
    * in the range [0.0,1.0)
    */
  public final float nextFloat() {
    return (float)nextDouble();
  }

  /** Generates a pseudorandom, uniformly-distributed, int value in [min,max].*/
  public final int nextInt(int min,int max) {
    return (int)(nextDouble()*(max-min+1))-min;
  }

  /** Generates a pseudorandom, uniformly-distributed, int value.*/
  public final int nextInt() {
    return nextInt(Integer.MIN_VALUE,Integer.MAX_VALUE);
  }
}//end class Random
