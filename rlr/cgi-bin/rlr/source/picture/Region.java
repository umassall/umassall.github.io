package picture;
import parse.*;
import expression.*;

/** Put "Region (-1 -1) (1 1)" before or after a PicPipe
  * in the source to zoom in to that region.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 21 April 96
  * @author Leemon Baird
  */
public class Region extends PicPipe {
  protected NumExp minX,maxX,minY,maxY; //region to zoom in on
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "'(' NumExp [','] NumExp ')' [','] "+
           "'(' NumExp [','] NumExp ')'"+
           "//A region to zoom into. "+
           "Zoom into a region from (minX,minY) to (maxX,maxY)";
  }
  /** Output a description of this object that can be parsed with parse().
    * @see Parsable
    */
  public void unparse(Unparser u, int lang) {
    u.emit('(');
    u.emitUnparse(minX,lang);
    u.emit(',');
    u.emitUnparse(minY,lang);
    u.emit(") (");
    u.emitUnparse(maxX,lang);
    u.emit(',');
    u.emitUnparse(maxY,lang);
    u.emit(')');
  }

  /** Parse the input file to get the parameters for this object.
    * @exception parse.ParserException parser didn't find the required token
    */
  public Object parse(Parser p,int lang) throws ParserException {
    p.parseChar('(',true);
    minX=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(',',false);
    minY=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(')',true);
    p.parseChar(',',false);
    p.parseChar('(',true);
    maxX=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(',',false);
    maxY=(NumExp)p.parseClass("NumExp",lang,true);
    p.parseChar(')',true);
    return this;
  } //end method parse

  /** Initialize the zoom region of the source of this object,
    * and remove this object from the linked list of objects
    * to call while drawing the image.  When called, list.first
    * is this object, and list.rest.first is the source object.
    * If this object is the first pipeline in the sequence, then
    * it will never be called by get() anyway, so it can stay on
    * the linked list, and can call setRegion on itself.
    */
  public PicPipeList init(PicPipeList list) {
    if (list.rest==null) { //if it's the first pipeline with no source
      this.setRegion(minX.val,minY.val,maxX.val,maxY.val);
      return list; //remain on the list
    }
    list.rest.first.setRegion(minX.val,minY.val,maxX.val,maxY.val); //setup source
    list.first = list.rest.first; //remove self
    list.rest  = list.rest.rest;
    return list;
  }
} //end class Region
