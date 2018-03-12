package picture;
import parse.*;

/** Put "Description <String>" before or after a PicPipe to
  * say what the picture is
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 11 May 96
  * @author Leemon Baird
  */
public class Description extends PicPipe {
  protected String desc=null; //a string describing the picture

  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "<string>"+
           "//A string describing the picture being drawn.";
  }
  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit('\'');
    u.emit(desc);
    u.emit('\'');
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    desc=null;
    if (!p.isString)
      p.error("String"); //complain that a string was expected
    desc=p.tString;
    p.getToken();
    return this;
  } //end method parse

  /** Initialize the description of this object
    * and remove this object from the linked list of objects
    * to call while drawing the image.  When called, list.first
    * is this object, and list.rest.first is the source object.
    * If this object is the first pipeline in the sequence, then
    * it will never be called by get() anyway, so it can stay on
    * the linked list, and can call setDescription on itself.
    */
  public PicPipeList init(PicPipeList list) {
    if (list.rest==null) { //if it's the first pipeline with no source
      this.setDescription(desc);
      return list; //remain on the list
    }
    list.rest.first.setDescription(desc); //setup source
    list.first = list.rest.first; //remove self
    list.rest  = list.rest.rest;
    return list;
  }
} //end class Description
