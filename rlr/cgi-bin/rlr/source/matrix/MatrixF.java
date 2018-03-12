package matrix;  //matrix classes differ on "MatrixF","nextFloat","float"
import Random;
import parse.*;
import java.util.Vector;
import expression.*;

/** A MatrixF is a matrix where each element is a float
  * (a vector if it has only 1 row or column)
  * that can perform normal matrix/vector operations.  A MatrixF is
  * actually just a view on a subset of a 1D array, so a particular
  * array might have two different MatrixF objects pointing to it,
  * one of which treats it as a 2D matrix, and one of which treats
  * it as a 1D vector.  Also, a MatrixF may view part of an array,
  * so it is possible to define a matrix or vector that is a concatenation
  * of smaller matrices or vectors.  The constructor automatically
  * creates the 1D array and the MatrixF object pointing to it, unless
  * an existing array is passed in.  The
  * submatrix method returns another MatrixF object that points to the
  * same array.  The toVector method returns another MatrixF object that
  * points to the same array and treats it as a single vector (column matrix).
  *    <p>This code is (c) 1996,1997 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @author Leemon Baird
  * @author Mance Harmon
  * @author Scott Weaver
  * @version 1.08, 3 May 97
  */
public final class MatrixF implements Parsable {
  //revision history:
  //1.08 3 May 97 fixed bug in diag(v) and made mult(0) work for infinity and NaN-Leemon Baird
  //1.07 15 Apr 97 fixed bugs, finished native code for all but 3 functions that need it -Leemon Baird
  //1.06 15 Mar 97 made more efficient, fixed bugs, added comments, started native implementation - Leemon Baird
  //1.05 4 Nov 96 modified to use NumExp instead of parseNumber() - Leemon Baird
  //1.04 modified comments on replace()
  //1.03 adds parse/unparse/BNF
  //     Scott Weaver added multDiag multK multMatcol multColMat MatToScalar
  //     Scott Weaver added diag
  //1.02 adds replace(MatrixF m) and multAdd(MatrixF m, float x) and addMult(MatrixF m, float x)
  //1.01 adds duplicate()

  //the following 6 variables define a matrix, and can be
  //set in the constructor

  /** Is native code loaded? */
  public static boolean nativeCodeLoaded=false;
  /** Use the native code? */
  public static boolean useNativeCode=false;

  /** The 1D array that holds the data for this MatrixF, and perhaps others too. */
  public float[] data=null;
  /** # rows in the matrix */
  public int nRows=0;
  /** # columns in the matrix */
  public int nCols=0;
  /** amount to add to an index to move to the next row */
  public int nextRow=0;
  /** amount to add to an index to move to the next column */
  public int nextCol=0;
  /** element of array holding upper-left corner of matrix */
  public int first=0;

  //the following are determined by the above, but are calculated
  //and stored anyway to save time during matrix multiplication, etc.

  /** element of array holding lower-right corner of matrix */
  public int last=0;
  /** amount to add to an index to go from past the end of one row to start of next*/
  public int newRow=0;
  /** amount to add to an index to go from past the end of one column to top of next*/
  public int newCol=0;
  /** amount to add to an index to go from past the end of one row back to its start*/
  public int restartRow=0;
  /** amount to add to an index to go from past the end of one column back to its start*/
  public int restartCol=0;
  /** # elements in this vector (-1 if not a vector) */
  public int size=-1;
  /** amount to add to index to get next element of this vector */
  public int next=0;

  static { //load native code if possible, else remember there is none. Use native code if it loads.
    useNativeCode=nativeCodeLoaded=true;
    try {  //should add a check for security model before trying this, to avoid extra errors to std out.
      System.loadLibrary("MatrixF");
      System.out.println("MatrixF native code loaded");
      if (native_version()!=1)
        System.out.println("Incompatible native code: "+
                           "is version "+native_version()+
                           "but should be version 1");
      else
        useNativeCode=nativeCodeLoaded=true;
    } catch (Throwable e) {//java.lang.UnsatisfiedLinkError
      useNativeCode=nativeCodeLoaded=false; //if can't load native library, then don't use it
    }
  }

  /** If a matrix is created by constructors, or is created by parse()
    * but every element was a simple number (no expression) then
    * expression=null.  Otherwise expression is an array the same size
    * as data, and has nonnull entries for those elements that were
    * expressions.
    */
  NumExp[] expression=null;

  /** Initialize, either partially or completely.
    *@see parse.Parsable#initialize
    */
  public void initialize(int level) {}

  /** Stops using native code if parameter is false.  If parameter is true, it
    * will use native code if available.  The default for this class is as if
    * useNative(true) was called initially.
    */
  public void useNative(boolean useIt) {
    useNativeCode=useIt && nativeCodeLoaded;
  }

  /** Create a new scalar (a 1x1 matrix) */
  public MatrixF() {
    setVars(new float[1],0,1,1,1,1);
  }
  /** Create a new column vector with n elements */
  public MatrixF(int n) {
    setVars(new float[n],0,n,1,1,1);
  }
  /** Create a new 2D matrix with size (rows,cols) */
  public MatrixF(int rows, int cols) {
    setVars(new float[rows*cols],0,rows,cols,cols,1);
  }
  /** View an existing array as a 1D vector (column matrix) */
  public MatrixF(float[] fArray) {
    setVars(fArray,0,fArray.length,1,1,1);
  }

  /** View an existing array as a 2D matrix with size (rows,cols)
    * All elements in the same row are adjacent in the array.
    * @exception MatrixException array wasn't same size as matrix
    */
  public MatrixF(float[] fArray,int rows,int cols) throws MatrixException {
    if (fArray.length!=rows*cols)
      throw new MatrixException("Array with "+fArray.length+
        "elements does not exactly fit a "+rows+"x"+cols+" matrix.");
    setVars(fArray,0,rows,cols,cols,1);
  }

  /** Directly set 6 data fields in an object. Avoid using this if possible.*/
  public MatrixF(float[] data,int first,
                       int nRows,  int nCols,
                       int nextRow,int nextCol) {
    setVars(data,first,nRows,nCols,nextRow,nextCol);
  }

  /* this sets the 6 data fields and recalculates the others */
  private final void setVars(float[] _data,int _first,
                             int _nRows,  int _nCols,
                             int _nextRow,int _nextCol) {
    expression=null;
    data   =_data;
    first  =_first;
    nRows  =_nRows;
    nCols  =_nCols;
    nextRow=_nextRow;
    nextCol=_nextCol;
    last       = first+nextRow*(nRows-1)+nextCol*(nCols-1);
    restartRow = -nextCol*nCols;
    restartCol = -nextRow*nRows;
    newRow     = nextRow + restartRow;
    newCol     = nextCol + restartCol;
    if (nCols==1) {        //this is a column vector
      size=nRows;
      next=nextRow;
    } else if (nRows==1) { //this is a row vector
      size=nCols;
      next=nextCol;
    } else {               //this is not a vector
      size=-1;
      next=0;
    }
  }//end setVars

  /** Return a new MatrixF object that is a single vector (column matrix)
    * pointing to the entire data array of this object.  If this object
    * represents only a subset of that array, the vector returned will
    * still represent the entire array, not the subset.
    */
  public MatrixF toVector() {
    return new MatrixF(data);
  } //end toVector

  /** Return a new MatrixF that is a contiguous, rectangular subset of this one.    * The data in the matrix is not copied.
    * The MatrixF object returned contains a pointer to the same data.
    * The new matrix has shape (rows,cols), and its (0,0) element is
    * element (row,col) in the source matrix.
    * @exception MatrixException if called on a 2D matrix, or index out of bounds
    */
  public MatrixF submatrix(int row, int col, int rows, int cols) throws MatrixException {
    if (row<0 || rows<0 || row+rows-1>nRows)
      throw new MatrixException("submatrix rows must be in range 0.."+(nRows-1));
    if (col<0 || cols<0 || col+cols-1>nCols)
      throw new MatrixException("submatrix cols must be in range 0.."+(nCols-1));
    return new MatrixF(data,                          //data
                       first+row*nextRow+col*nextCol, //first
                       rows,                          //nRows
                       cols,                          //nCols
                       nextRow,                       //nextRow
                       nextCol);                      //nextCol
  } //end submatrix

  /** Return a new MatrixF that views some of the elements of this vector
    * (elements n to n+rows*cols-1 inclusive, zero based)
    * as a matrix of shape (rows,cols).
    * @exception MatrixException if this is not a vector or has too few elements
    */
  public MatrixF submatrix(int n, int rows, int cols) throws MatrixException {
    if (n<0 || n+rows*cols-1>=size)
      throw new MatrixException("vector has "+size+" elements, too few to form a "+
                                rows+"x"+cols+" matrix starting with element "
                                +n);
    if (rows<0 || cols<0)
      throw new MatrixException("can't convert a vector to a "+
                                rows+"x"+cols+" matrix");
    return new MatrixF(data,                          //data
                       first+n*next,                  //first
                       rows,                          //nRows
                       cols,                          //nCols
                       next*cols,                     //nextRow
                       next);                         //nextCol
  } //end submatrix

  /** return the only element of a 1x1 MatrixF object
    * @exception MatrixException if called if not 1x1
    */
  public final float MatToScalar() throws MatrixException {
    if (this.nRows == 1 && this.nCols == 1)
      return data[this.first];
    else
      throw new MatrixException("Can't convert "+nRows+"x"+nCols+" matrix to scalar");
  } //end MatToScalar

  /** return the nth element of a 1D vector (column or row vector)
    * @exception MatrixException if called on a 2D matrix, or index out of bounds
    */
  public final float val(int n) throws MatrixException {
    if (n>=0 && n<size) //this is a vector, and n is legal
      return data[first+n*next];
    else
      throw new MatrixException("Can't return element "+n+
                                " of a "+nRows+"x"+nCols+" vector");
  } //end val

  /** return a particular element of a matrix
    * @exception MatrixException if index out of bounds
    */
  public final float val(int row,int col) throws MatrixException {
    if (row>=0 && col>=0 && row<nRows && col<nCols)
      return data[first+row*nextRow+col*nextCol];
    else
      throw new MatrixException("Can't return element ("+row+","+col+
                                ") of a "+nRows+"x"+nCols+" array.");
  } //end val

  /** set the nth element of a 1D vector (row or column vector)
    * @exception MatrixException if called on a 2D matrix, or index out of bounds
    */
  public final void set(int n,float val) throws MatrixException {
    if (n>=0 && n<size)  //this is a vector and n is legal
      data[first+n*next]=val;
    else
      throw new MatrixException("Can't set element"+n+
                                "of a "+nRows+"x"+nCols+" vector");
  } //end set

  /** set a particular element of a matrix to a particular value
    * @exception MatrixException if index out of bounds
    */
  public final void set(int row,int col,float val) throws MatrixException {
    if (row>=0 && col>=0 && row<nRows && col<nCols)
      data[first+row*nextRow+col*nextCol]=val;
    else
      throw new MatrixException("Can't return element ("+row+","+col+
                                ") of a "+nRows+"x"+nCols+" array.");
  } //end set

  /** Transpose this matrix or vector, and return this. */
  public MatrixF transpose() {
    int temp;
    temp=nRows;        nRows     =nCols;        nCols     =temp;
    temp=nextRow;      nextRow   =nextCol;      nextCol   =temp;
    temp=newCol;       newCol    =newRow;       newRow    =temp;
    temp=restartRow;   restartRow=restartCol;   restartCol=temp;
    return this;
  } //end transpose

  /** Duplicate this object/associated data array and return a
    * pointer to the copy.
    */
  public final MatrixF duplicate() {
        MatrixF x=null;
        if (data==null)
          return new MatrixF();
        x=new MatrixF(new float[data.length],
                      first,nRows,nCols,nextRow,nextCol);
        for(int i=0;i<data.length;i++)
                x.data[i]=data[i];
        return x;
  }

  /** Clone this object.  The data array it points to is not cloned. */
  public Object clone() {
    return (Object)new MatrixF(data,first,nRows,nCols,nextRow,nextCol);
  }

  /** Return a parameter array if BNF(), parse(), and unparse() are to be automated, null otherwise.
    * @see parse.Parsable#getParameters
    */
  public Object[][] getParameters(int lang) {
    return null;
  }

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "( ('[' [(NumExp [','])* NumExp] ']') | "+
             "('[' ('[' [(NumExp [','])* NumExp] ']' [','])* "+
                  "('[' [(NumExp [','])* NumExp] ']') "+
              "']' )) "+
             "['transpose']"+
           "//A matrix or vector. Could be transposed.  Can either "+
           "be a list of vectors [[1 2][3 4]]  or a single vector "+
           "[5 6].  Commas are optional throughout.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    if (size>-1) {//it's a vector
      u.emit("[");
      for (int i=first;i<=last;i+=next) {
        if (expression!=null && expression[i]!=null)
          u.emitUnparse(expression[i],lang);
        else {
          u.emit(data[i]);
          u.emit(" ");
        }
      }
      u.emit("] ");
      if (nCols==1 && nRows!=1) //it's really a column vector, so transpose it
        u.emit("transpose ");
    } else {//it's a matrix
      u.emit("[");
      u.indent(1);
      for (int i=first,s1=first-restartCol;i<s1;i+=newRow) { //for each row
        u.emit("[");
        for (int s2=i-restartRow;i<s2;i+=nextCol) {              //for each column
          if (expression!=null && expression[i]!=null)
            u.emitUnparse(expression[i],lang);
          else {
            u.emit(data[i]);
            u.emit(" ");
          }
        }
        u.emitLine();
      }
      u.unindent();
      u.emitLine("]");
    }
  } //end unparse

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    Vector   v  =new Vector();
    float[] va =null;
    NumExp[] exp=null;
    boolean  isMatrix=false;
    int i=0,r=1,c; //index, # rows, # cols
    boolean  allSimple=true; //should expression=null because all are simple?

    p.parseChar('[',true);
    isMatrix=p.parseChar('[',false);
    while (!p.parseChar(']',false))
      v.addElement(p.parseClass("NumExp",lang,true));
    c=v.size();
    if (!isMatrix) {//it's a vector, and it's all read in now
      va =new float[c];
      exp=new NumExp[c];
      v.copyInto(exp);
      for (i=0;i<c;i++) {
        va[i]=(float)exp[i].val;
        if (exp[i].isSimple()) //no need to store whole objects for simpe #s
          exp[i]=null;
        else
          allSimple=false; //at least one is not simple, so don't kill the array
      }
      if (allSimple)
        exp=null;
      setVars(va,0,va.length,1,1,1); //make it a column vector
      expression=exp;
      transpose(); //now it's a row vector, as it appeared in the input
    } else { //it's a matrix with just the top row read in so far
      while (p.parseChar('[',false)) { //repeatedly read a row, add to v
        r++;
        for (i=0;i<c;i++)
          v.addElement(p.parseClass("NumExp",lang,true));
        p.parseChar(']',true);
      }
      p.parseChar(']',true);
      exp=new NumExp[v.size()]; //exp is 1D array holding entire matrix expresions
      va =new float[v.size()]; //exp is 1D array holding entire matrix values
      v.copyInto(exp);
      for (i=0;i<v.size();i++) {
        va[i]=(float)exp[i].val;
        if (exp[i].isSimple()) //no need to store whole objects for simpe #s
          exp[i]=null;
        else
          allSimple=false; //at least one is not simple, so don't kill the array
      }
      if (allSimple)
        exp=null;
      setVars(va,0,r,c,c,1);
      expression=exp;
    }
    if (p.parseID("transpose",false))
      transpose();
    return this;
  } //end parse

  /** a rectangle of numbers (using newlines) representing the matrix */
  public String toString() {
    StringBuffer s=new StringBuffer();
    int i,s1,s2;
    s.append("[");
    if (nCols!=1 || nRows==1) { //if not a column vector (is a row vect or matrix)
      for (i=first,s1=first-restartCol;i<s1;i+=newRow) { //for each row
        for (s2=i-restartRow;i<s2;i+=nextCol)             //for each column
///**/          s.append(float.toString(data[i])+" "); //Netscape 4.0 beta 3 bug: must use explicit toString here
          s.append(fix.Util.toString(data[i],15,10)+" ");
///**/          s.append("# "); //Netscape 4.0 beta 3 bug: must use explicit toString here
        if (i+newRow<s1)
          s.append("\n ");
      }
      s.append("]");
    } else { //print this column vector on its side followed by "(transposed)"
      for (i=first;i<=last;i+=next) //for each row
///**/        s.append(float.toString(data[i])+" ");  //Netscape 4.0 beta 3 bug: must use explicit toString here
          s.append(fix.Util.toString(data[i],15,10)+" ");
///**/        s.append("# ");  //Netscape 4.0 beta 3 bug: must use explicit toString here
      s.append("] (transposed)");
    }
    return s.toString();
  } //end toString

  /** All fields plus the rectangle*/
  public String toString(boolean full) {
    if (full) {
      return " first="     +first     +
             " nRows="     +nRows     +
             " nCols="     +nCols     +
             " nextRow="   +nextRow   +
             " nextCol="   +nextCol   +
             " restartRow="+restartRow+
             " restartCol="+restartCol+
             " newRow="    +newRow    +
             " newCol="    +newCol    +
             " last="      +last      +
             " next="      +next      +
             " size="      +size      +
             "\n"+toString();
    }
    return toString();
  } //end toString(boolean)


  /** set every element of this matrix to a random number between
    * min and max using the random number generator r.
    */
  public MatrixF setRandom(float min, float max, Random r) {
    int i,s1,s2;
    float diff=max-min;
    for (i=first,s1=first-restartCol;i<s1;i+=newRow)  //for each row
      for (s2=i-restartRow;i<s2;i+=nextCol)           //for each column
        data[i]=r.nextFloat()*diff+min;
    return this;
  }

  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  ///   The following are implemented as both Java and native code  /////
  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////

  /** Return whether this and m are the same size and shape and have the same
    * numbers for corresponding elements.  Two matrices are still equal
    * if their data is stored in different arrays, and if corresponding
    * elements uparse to different symbolic expressions, as long as the
    * numeric values are the same.
    */
  public final boolean equalEls(MatrixF m) {
    if (m==null || m.nRows!=nRows || m.nCols!=nCols) //not same size and shape
      return false;

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;
    if (useNativeCode)
      return native_equalEls(this,m); //use native code if available

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol) //for each column
        if (data[i]!=mData[mi])
          return false;  //false if any element doesn't match
    return true;         //true if no mismatches found
  } //end equalEls

  /** this=this+m,   return this
    * @exception MatrixException if matrices are different sizes
    */
  public final MatrixF add(MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't add null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't add "+nRows+"x"+nCols+" matrix to "+
                                 m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_add1(this,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol) //for each column
        data[i]+=mData[mi];        //add corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end add

  /** this=this-m,    return this
    * @exception MatrixException if matrices are different shapes
    */
  public final MatrixF sub(MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't subtract null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't subtract "+nRows+"x"+nCols+
                         " matrix minus "+m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_sub(this,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol) //for each column
        data[i]-=mData[mi];        //subtract corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end sub

  /** this=m-this,    return this
    * @exception MatrixException if matrices are different shapes
    */
  public final MatrixF subFrom(MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't subtract null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't subtract "+nRows+"x"+nCols+
                         " matrix minus "+m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_subFrom(this,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol) //for each column
        data[i]=mData[mi]-data[i];   //subtract corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end subFrom

  /** this=this*k+m,   return this.
    * @exception MatrixException if matrices are different sizes
    */
  public final MatrixF multAdd(float k,MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't add "+nRows+"x"+nCols+" matrix to "+
                                 m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_multAdd(this,k,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol)    //for each column
        data[i]=data[i]*k+mData[mi];   //add corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end multAdd

  /** this=this+k*m,   return this.
    * @exception MatrixException if matrices are different sizes
    */
  public final MatrixF addMult(float k,MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't add "+nRows+"x"+nCols+" matrix to "+
                                 m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_addMult(this,k,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;          //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;
         i<s1;
         i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;
           i<s2;
           i+=nextCol,mi+=mNextCol) //for each column
        data[i]+=mData[mi]*k;        //add corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end addMult

  /** Return dot product of this vector with vector v.
    * Both are row vectors, or both columns, or one of each.
    * @exception MatrixException different # elements, or 1 isn't a vector
    */
  public final float dot(MatrixF v) throws MatrixException {
    if (v==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (size==-1 || v.size==-1)
      throw new MatrixException("cannot take the dot product of a matrix, "
                                +" only vectors (row or column)");
    else if (size!=v.size)
      throw new MatrixException("Cannot dot different-sized vectors with "
                                 +size+" and "+v.size+" elements.");

    if (useNativeCode)
      return native_dot(this,v); //use native code if available

    float prod=0;   //the dot product

    for (int i=first,vi=v.first;i<=last;i+=next,vi+=v.next)
      prod+=data[i]*v.data[vi];
    return prod;
  }//end dot

  /** this=m,   return this.
    * Replace this object's data with the data of matrix m,
    * then returns this.  This copies the data in the array itself,
    * not just pointers to it.
    *@exception MatrixException if matricies are different shapes
    */
  public final MatrixF replace(MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't replace a "+nRows+"x"+nCols+
                         " matrix with a "+m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_replace(this,m); //use native code if available

    int  i, s1, s2;  //indices for this matrix
    int mi;                   //index for matrix m
    float[] mData=m.data;
    int mNextCol=m.nextCol, mRestartRow=m.restartRow, mNewRow=m.newRow;

    for (i=first,mi=m.first,s1=first-restartCol;i<s1;i+=newRow,mi+=mNewRow) //for each row
      for (s2=i-restartRow;i<s2;i+=nextCol,mi+=mNextCol) //for each column
        data[i]=mData[mi];   //subtract corresponding elements
        return this; //return pointer to this object, which contains the answer
  }  //end replace

  /** each_element_of_this += k,    return this */
  public final MatrixF add(float k) {
    if (useNativeCode)
      return native_add2(this,k); //use native code if available

    int i,s1,s2;
    for (i=first,s1=first-restartCol;i<s1;i+=newRow) //for each row
      for (s2=i-restartRow;i<s2;i+=nextCol)          //for each column
        data[i]+=k;                                  //add k to this[row][col]
    return this; //return pointer to this object, which contains the answer
  } //end add

  /** replaces all elements of this with zeros, except for all k's
    * on the diagonal.  this must be square.
    *@exception MatrixException if matrix is not square
    */
  public final MatrixF diag(float k) throws MatrixException {
    if(this.nCols!=this.nRows)
      throw new MatrixException("matrix "+nRows+"x"+nCols+" is not square");

    if (useNativeCode)
      return native_diag1(this,k); //use native code if available

    mult((float)0);
    for(int i=0;i<nRows;i++)
        this.set(i,i,k);
    return this; //return pointer to this object, which contains the answer
  } //end diag(k)

  /** replaces all elements of this with zeros, except for the
    * diagonal which will be copied from the elements of row or column
    * vector v.  this must be square.
    *@exception MatrixException if matrix is not square or v is not a vector of that size
    */
  public final MatrixF diag(MatrixF v) throws MatrixException {
    if(this.nCols!=this.nRows || this.nCols!=v.size)
      throw new MatrixException("matrix "+nRows+"x"+nCols+" is not square"+
                          "or matrix "+v.nRows+"x"+v.nCols+
                          " is not a vector of the same dimensionality");

    if (useNativeCode)
      return native_diag2(this,v); //use native code if available

    mult((float)0);
    for(int i=first, vi=v.first;
        i<=last;
        i+=nextRow+nextCol, vi+=v.next)
      data[i]=v.data[vi];
    return this; //return pointer to this object, which contains the answer
  } //end diag

  /** this=x*y,  return this.
    * x and y are matrices of appropriate shapes.
    * This object must already be the right shape to hold the answer.
    * Returns pointer to this object.
    * @exception MatrixException if shapes are wrong
    */
  public final MatrixF mult(MatrixF x, MatrixF y) throws MatrixException {
    if (x==null || y==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");

    if (nRows!=x.nRows || x.nCols!=y.nRows || y.nCols!=nCols)
      throw new MatrixException("error trying to multiply matrices "+
          nRows+"x"+  nCols+"="+x.nRows+"x"+x.nCols+"*"+y.nRows+"x"+y.nCols);

    if (useNativeCode)
      return native_mult1(this,x,y); //use native code if available

    int xi,yi,ai; //index in array for x, y, answer
    int s1,s2,s3; //index at which each FOR loop should stop
    float dot;    //dot product of one row of x and one column of y

    xi=x.first;
    s1=first-restartCol;
    for (ai=first;ai<s1;ai+=newRow) {  //for each row of answer and x
      yi=y.first;
      s2=ai-restartRow;
      for (;ai<s2;ai+=nextCol) { //for each column of answer and y
        dot=0;
        s3=xi-x.restartRow;
        for (;xi<s3;xi+=x.nextCol) { //for each col of x and row of y
          dot+=x.data[xi]*y.data[yi];
          yi+=y.nextRow;
        }
        data[ai]=dot;
        yi+=y.newCol;
        xi+=x.restartRow;
      }
      xi+=x.nextRow;
    }
    return this; //return pointer to this object, which contains the answer
  } //end mult

  /** this=this*k,    return this.  If k=0, then it sets this equal to all zeros,
    * even if this contains some infinites or Not-a-Numbers. */
  public final MatrixF mult(float k) {
    if (useNativeCode)
      return native_mult2(this,k); //use native code if available

    int i,s1,s2;
    if (k==0) {
      for (i=first,s1=first-restartCol;i<s1;i+=newRow) //for each row
        for (s2=i-restartRow;i<s2;i+=nextCol)          //for each column
          data[i]=0;                                   //zero it out
    } else {
      for (i=first,s1=first-restartCol;i<s1;i+=newRow) //for each row
        for (s2=i-restartRow;i<s2;i+=nextCol)          //for each column
          data[i]*=k;                                  //multiply this[row][col] by x
    }
    return this; //return pointer to this object, which contains the answer
  } //end mult

  /** this=k*m,   return this.
    *@exception MatrixException if this and m are different sizes
    */
  public final MatrixF multK(float k,MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't put a "+m.nRows+"x"+m.nCols+
                         " matrix times a constant into a "+
                         nRows+"x"+nCols+" matrix.  Shapes must be identical.");

    if (useNativeCode)
      return native_multK(this,k,m); //use native code if available

    replace(m);
    mult(k);
    return this; //return pointer to this object, which contains the answer
  } //end MultK


  /** multiply corresponding elements of this matrix and matrix m,
    * store answer in this matrix, and return this matrix
    * @exception MatrixException if this and m are different sizes
    */
  public final MatrixF multEl(MatrixF m) throws MatrixException {
    if (m==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");
    if (m.nRows!=nRows || m.nCols!=nCols)
      throw new MatrixException("can't multiply corresponding elements of "+nRows+"x"+nCols+" matrix and "+
                                 m.nRows+"x"+m.nCols+" matrix.");

    if (useNativeCode)
      return native_multEl(this,m); //use native code if available

    int i,mi,s1,s2;

    mi=m.first;
    s1=first-restartCol;
    for (i=first; i<s1; i+=newRow,mi+=m.newRow) //for each row
      for (s2=i-restartRow;i<s2;i+=nextCol,mi+=m.nextCol) //for each column
        data[i]*=m.data[mi];       //multiply corresponding elements
    return this; //return pointer to this object, which contains the answer
  } //end multEl

  /** this=((col)th column, zero based, of x)*v,  return this
    * (which will be a matrix).
    * x is a matrix and v is a row vector.
    * This object must already be the right shape to hold the answer
    * (row(x) by col(v)).
    * Returns pointer to this object.
    * @exception MatrixException if shapes are wrong
    */
  public final MatrixF multColMat(MatrixF x, int col, MatrixF v) throws MatrixException {
    if (x==null || v==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");

    if (nRows!=x.nRows || 1!=v.nRows || v.nCols!=nCols || col<0 || col>=x.nCols)
      throw new MatrixException("error trying to multiply col x matrix "+
          nRows+"x"+  nCols+"="+x.nRows+"x"+x.nCols+"*"+v.nRows+"x"+v.nCols);

///**/    if (useNativeCode)
///**/      return native_multColMat(this,x,col,v); //use native code if available

    int xi,vi,ai; //index in array for x, y, answer
    int s1,s2,s3; //index at which each FOR loop should stop

    xi=x.first + col*x.nextCol;
    s3=xi-x.restartRow;
    s1=first-restartCol;
    for (ai=first;ai<s1;ai+=newRow) {  //for each row of answer and x
      vi=v.first;
      s2=ai-restartRow;
      for (;ai<s2;ai+=nextCol) { //for each column of answer and y
        data[ai]=x.data[xi]*v.data[vi];
        vi+=v.nextCol;
      }
      xi+=x.nextRow;
    }
    return this; //return pointer to this object, which contains the answer
  } //end multColMat

  /** this=x*((col)th column, zero based, of y),  return this
    * (which will be a column).
    * x and y are matrices of appropriate shapes.
    * This object must already be the right shape to hold the answer.
    * Returns pointer to this object.
    * @exception MatrixException if shapes are wrong
    */
  public final MatrixF multMatCol(MatrixF x, MatrixF y, int col) throws MatrixException {
    if (x==null || y==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");

    if (nRows!=x.nRows || x.nCols!=y.nRows || col<0 || col>=y.nCols)
      throw new MatrixException("error trying to multiply Matrix x Col "+
          nRows+"x"+  nCols+"="+x.nRows+"x"+x.nCols+"*"+y.nRows+"x1");

///**/    if (useNativeCode)
///**/      return native_multMatCol(this,x,y,col); //use native code if available

    int xi,yi,ai; //index in array for x, y, answer
    int s1,s3;    //index at which each FOR loop should stop
    float dot;    //dot product of one row of x and one column of y

    xi=x.first;
    s1=first-restartCol;
    for (ai=first;ai<s1;ai+=nextRow) {
      yi=y.first + col*y.nextCol;
      dot=0;
      s3=xi-x.restartRow;
      for (;xi<s3;xi+=x.nextCol) { // dot of the col'th column of y and a row of x
        dot+=x.data[xi]*y.data[yi];
        yi+=y.nextRow;
      }
      data[ai]=dot;
      xi+=x.newRow;
    }
    return this; //return pointer to this object, which contains the answer
  } //end multMatCol

  /** this=diag(v)*y,  return this.
    * diag(v) is a diagonal matrix whose diagonal
    * is the elements of the column or row vector v.
    * y and this are matrices of the appropriate shape.
    * this must already be the right shape to hold the answer.
    * Returns pointer to this object.
    * @exception MatrixException if shapes are wrong
    */
  public final MatrixF multDiag(MatrixF v, MatrixF y) throws MatrixException {
    if (v==null || y==null)
      throw new MatrixException("Can't operate on null MatrixF pointer");

    if (nRows!=v.size || nRows!=y.nRows || nCols!=y.nCols)
      throw new MatrixException("error trying to make vector the diagonal of a matrix"+
                 "to multiply times the other matrix "+
          nRows+"x"+  nCols+"="+v.nRows+"x"+v.nCols+"x"+y.nRows+"x"+y.nCols);

///**/    if (useNativeCode)
///**/      return native_multDiag(this,v,y); //use native code if available

    int vi,yi,ai; //index in array for v, y, answer
    int s1,s2; //index at which each FOR loop should stop

    vi=v.first;
    yi=y.first;
    s1=first-restartCol;
    for (ai=first;ai<s1;ai+=newRow) { //for ai=start of each row of this
      s2=ai-restartRow;
      for (;ai<s2;ai+=nextCol) { //for ai=each col of this (within curr row)
          data[ai]=v.data[vi]*y.data[yi];
          yi+=y.nextCol;
      }
      vi+=v.next;
      yi+=y.newRow;
    }
    return this; //return pointer to this object, which contains the answer
  } //end multDiag

  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////
  ///////////////////////////////////////////////////////////////////////

  static private native final long    native_version    ();
  static private native final boolean native_equalEls   (MatrixF t, MatrixF m);
  static private native final MatrixF native_add1       (MatrixF t, MatrixF m);
  static private native final MatrixF native_sub        (MatrixF t, MatrixF m);
  static private native final MatrixF native_subFrom    (MatrixF t, MatrixF m);
  static private native final MatrixF native_multAdd    (MatrixF t, float k,MatrixF m);
  static private native final MatrixF native_addMult    (MatrixF t, float k,MatrixF m);
  static private native final float  native_dot        (MatrixF t, MatrixF v);
  static private native final MatrixF native_replace    (MatrixF t, MatrixF m);
  static private native final MatrixF native_add2       (MatrixF t, float k);
  static private native final MatrixF native_diag1      (MatrixF t, float k);
  static private native final MatrixF native_diag2      (MatrixF t, MatrixF v);
  static private native final MatrixF native_mult1      (MatrixF t, MatrixF x,MatrixF y);
  static private native final MatrixF native_mult2      (MatrixF t, float k);
  static private native final MatrixF native_multK      (MatrixF t, float k,MatrixF m);
  static private native final MatrixF native_multEl     (MatrixF t, MatrixF m);
  static private native final MatrixF native_multColMat (MatrixF t, MatrixF x, int col, MatrixF v);
  static private native final MatrixF native_multMatCol (MatrixF t, MatrixF x, MatrixF y, int col);
  static private native final MatrixF native_multDiag   (MatrixF t, MatrixF v, MatrixF y);
}//end MatrixF
