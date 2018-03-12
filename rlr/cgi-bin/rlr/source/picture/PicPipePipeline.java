package picture;
import java.util.Vector;
import parse.*;

/**
  * A sequence of PicPipes hooked together to define a single picture.
  * This could also be something that is not a viewable picture,
  * such as a #define statement.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 27 April 96
  * @author Leemon Baird
  */
public class PicPipePipeline extends PicPipe {
  /** is this a picture as opposed to something like a #define ? */
  public boolean viewable=false;
  /** linked list of pipelines that are pictures (not including @defines, etc.) */
  public PicPipeList source=null;
  /** the last pipeline on the linked list */
  public PicPipeList sourceLast=null;

  protected PicPipe[]  pic=null; //all the pictures parsed

  /** Return the BNF description of this object's parameters. */
  public String BNF(int lang) {
    return "'{' picture.PicPipe * '}'//A Picture pipeline. "+
           "The first Picture is the source for the second, "+
           "which is the source for the third, etc. "+
           "If you ask a Picture for a pixel's color, it either "+
           "replies immediately, or asks it's source (1 or more times) "+
           "and returns some function of the answer (or answers). "+
           "Sources can take/return scalars (doubles) instead of "+
           "colors.  This allows a fractal object to be a scalar-valued "+
           "function that is then fed in to another function that remaps "+
           "the scalars (e.g. to do edge detection), which then feeds in "+
           "to a third PicPipe that maps the resulting numbers to colors, "+
           "which then feeds into a fourth PicPipe that does antialiasing "+
           "by asking the third PicPipe for many colors then averaging them.";
  }

  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit("{ ");
    u.indent(2); //first one is indented, but on same line as "{ "
    u.emitUnparseWithClassName(pic[0],lang,false);
    for (int i=1;i<pic.length;i++) {
      u.emitLine();
      u.emitUnparseWithClassName(pic[i],lang,false);
    }
    u.unindent();
    u.emitLine();
    u.emit("} ");
  } //end method unparse

  /** Parse this object's parameters and initialize it.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p, int lang) throws ParserException {
    Vector vect;
    PicPipeList temp;
    p.parseChar('{',true);
    vect=p.parseTypeList("PicPipe",lang,false);
    p.parseChar('}',true);
    pic=new PicPipe[vect.size()];
    vect.copyInto(pic);
    if (pic.length>0) {
      source=new PicPipeList(pic[0],null); //this will be the end of the list
      source.first.init(source);
      sourceLast=source; //remember the last PicPipe on the list (first in the source file)
    }
    for (int i=1;i<pic.length;i++) { //create linked list of PicPipes,
      source=new PicPipeList(pic[i],source); //order is reverse of order parsed
      source.first.init(source); //let the object remove itself from the list, if desired
    }
    if (source!=null)
      viewable=true; //the list is nonempty, so I'm viewable
    return this;
  } //end method parse
} //end class PicPipePipeline
