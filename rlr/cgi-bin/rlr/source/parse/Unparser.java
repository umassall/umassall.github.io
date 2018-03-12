package parse;
import java.io.*;
import java.util.Hashtable;

/** Allows Parsable objects to output a description of themselves in
  * a format such that the Parser can later read them in and recreate
  * them.  Every parsable object has an unparse() method.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 10 March 96
  * @author Leemon Baird
  * @see Parser
  */
public class Unparser {
  protected int indent=3;     //# spaces for each level of indentation
  protected int currIndent=0; //# spaces currently indenting
  protected int maxIndent=-1; //max indenting allowed (-1 means infinite)
  protected int maxLen=78;    //max length for a line
  protected PrintStream out;  //stream that should be unparsed to
  protected int currCol=0;    //column for next char to print (zero based)
  protected Hashtable labels=null; //#DEF/#USE labels
  protected Hashtable labelUnparsed=null; //which labels have been unparsed already

  /** Unparse with each indent being ind spaces, up to a maximum of max spaces.
    * If max=-1, then there is no limit on how far it can indent.
    * Lines wrap after len columns.  If len=-1, then lines do not automatically
    * wrap.  defUseLabels is the hash table created by the parser that tells
    * about when #DEF and #USE were used.
    */
  public Unparser(PrintStream s,int ind, int max, int len, Hashtable defUseLabels) {
    labelUnparsed=new Hashtable(); //initially empty, so no labels yet unparsed, all labels will unparse as #DEF the first time
    labels    =defUseLabels;
    maxLen    =len;
    out       =s;
    indent    =ind;
    maxIndent =max;
    currIndent=0;
    currCol   =0;
  }

  /** Output the string to whatever output is being unparsed to */
  public void emit (String  x) {
    if (currCol+x.length()>maxLen && maxLen>0) //line too long: wrap
      if (x.length()+currIndent<maxLen) //break the line, indent, and try again
        emitLine();
      else {            //if x is extremely long, just line break and print without indent
        out.println();
        currCol=0;
      }
    out.print(x);
    currCol+=x.length();
  }

  /** End the current line and indent the next line appropriately. */
  public void emitLine() {
    out.println();
    currCol=0;
    for (int i=0;i<currIndent && (i<maxIndent || maxIndent<0);i++)
      emit(" ");
  }

  /** Close the output file. */
  public void close() {
    emitLine();
  }

  /** Indent all future lines, and also emit n less than a normal indent right now. */
  public void indent(int n) { //should combine lines in an indent if short enough /**/
    for (int i=0;i<indent-n;i++)
      emit(" ");
    indent();
  }

  /** Cause all following emitLine() calls to indent the following line more. */
  public void indent() {
    currIndent+=indent;
  }

  /** Cause all following emitLine() calls to indent the following line less. */
  public void unindent() {
    currIndent-=indent;
  }

  /** Output x. */
  public void emit    (int     x) {emit(Integer.toString(x));}
  /** Output x. */
  public void emit    (long    x) {emit(Long.toString(x));}
  /** Output x. */
  public void emit    (float   x) {emit(Float.toString(x));}
  /** Output x. */
  public void emit    (double  x) {emit(Double.toString(x));}
  /** Output x. */
  public void emit    (char    x) {emit(new Character(x).toString());}
  /** Output "true" or "false".*/
  public void emit    (boolean x) {emit(x?"true":"false");}
  /** Output a string representing this object.*/
  public void emit    (Object  x) {emit(x.toString());}
  /** Output x and start a new line. */
  public void emitLine(int     x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(long    x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(float   x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(double  x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(char    x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(boolean x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(String  x) {emit(x);emitLine();}
  /** Output x and start a new line. */
  public void emitLine(Object  x) {emit(x);emitLine();}

  /** Output the class name of the object, then unparse it.
    * If not showPackage, then don't output the package name prefix.
    */
  public void emitUnparseWithClassName(Parsable obj,int lang, boolean showPackage) {
    String clss=(obj.getClass()).getName();
    int i=clss.lastIndexOf(".");
    String id=(String)labels.get(obj);

    if (id==null) { //this is not a #DEF or #USE object
      if (showPackage || i==-1)
        emit(clss);
      else
        emit(clss.substring(i+1));
      emit(" ");
      unparseObject(obj,lang);
    } else { //this is one, so output the #DEF or #USE
      if (labelUnparsed.get(obj)!=null)  //I've seen this before, so #USE it
        emit("#USE "+id+" ");
      else {                  //I've never seen this before, so #DEF it
        labelUnparsed.put(obj,""); //remember that this has been unparsed before
        emit("#DEF "+id+" {");
        if (showPackage || i==-1)
          emit(clss);
        else
          emit(clss.substring(i+1));
        emit(" ");
        unparseObject(obj,lang);
        emit("}");
      }
    }
  }//end emitUnparseWithClassName

  /** unparse a class by calling its unparse method.  If the class was
    * within a #DEF or #USE statement, emit that too.
    */
  public void emitUnparse(Parsable obj,int lang) {
    String id=(String)labels.get(obj);
    if (id==null) { //this is not a #DEF or #USE object
      unparseObject(obj,lang);
    } else { //this is one, so output the #DEF or #USE
      if (labelUnparsed.get(obj)!=null)  //I've seen this before, so #USE it
        emit("#USE "+id+" ");
      else {   //I've never seen this before, so #DEF it
        labelUnparsed.put(obj,""); //remember that it's been seen
        emit("#DEF "+id+" {");
        unparseObject(obj,lang);
        emit("}");
      }
    }
  }//end emitUnparse



  //Unparse parameters for a Parsable object.
  //This works by either using the parameters array that the object returns, or by
  //calling its unparse() method.  The unparse() method is called only if the parameters
  //returned were null.
  private final void unparseObject(Parsable obj,int lang) {
    Object[][] par=obj.getParameters(lang);
    if (par==null) //no automatic parsing, so just call its unparse() method
      ((Parsable)obj).unparse(this, lang);
    else { //the object wants automatic parsing
      indent();
      emit(" {");
      int i=0;
      boolean skipNextLine=true;
      if (par[2].length>0 && lang<0) {
        emit("{");
        for (i=0;i<par[2].length;i++) {//unparse the saved state variables
          unparseParameter("",par[2][i],lang);
          emit(",");
        }
        if (par[2].length>0)
          emit("}");
      }
      for (i=0;i<par[1].length;i+=3) { //unparse each normal parameter (not a saved state variable)
        if (skipNextLine)
          emitLine();
        skipNextLine=unparseParameter((String)par[1][i],par[1][i+1],lang);
      }//end for
      unindent();
      emitLine();
      emit("}");
    }
  }//end unparseObject

  //unparse the parameter described by obj. Returns false iff parameter was null
  private boolean unparseParameter(String parName,Object obj,int lang) {
    try { //if it's Parsable, a class, then unparse it
      Parsable temp;
      temp=(Parsable)obj;
      if (temp!=null) {
        emit(parName);  //emit the name of the parameter
        emit(" ");
        emitUnparse(temp,lang);
        return true;
      }
    } catch (ClassCastException e) { //it's not a Parsable, so check if it's a Parsable[]  (a type)
      try {
        Parsable[] x;
        x=((Parsable[])obj); //if it's a Parsable[]   (a type)
        if (x!=null && x.length>0 && x[0]!=null) {
          emit(parName);  //emit the name of the parameter
          emit(" ");
          emitUnparseWithClassName(x[0],lang,false);
          return true;
        }
      } catch (ClassCastException ee) {
        try {
          Parsable[][] x;
          x=((Parsable[][])obj); //if it's a Parsable[][]  (a list of classes or types)
          if (x[0]!=null && x[0].length!=0) {
            emit(parName);  //emit the name of the parameter
            indent();
            emitLine("{");
            if (x.length==2)
              for (int i=0;i<x[0].length;i++)
                emitUnparse(x[0][i],lang);
            else
              for (int i=0;i<x[0].length;i++)
                emitUnparseWithClassName(x[0][i],lang,false);
            unindent();
            emitLine();
            emit("}");
            return true;
          }
        } catch (ClassCastException eee) {
            System.out.println("error in parameters array: parameter '"+parName+"' not a Parsable or Parsable[]");
            throw eee; //quit if an error in the parameters array in this object
        }
      }
    }
    return false;  //this parameter was null, so wasn't printed out
  }
} //end class Unparser
