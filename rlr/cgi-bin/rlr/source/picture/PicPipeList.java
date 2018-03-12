package picture;

/** A linked list describing the order of evaluation for a PicPipe.
  * This list is passed to a PicPipe when it is evaluated,
  * and it allows it to know the address of the source from which
  * it gets the values and colors on which it operates.  It passes
  * the list without its head to the next PicPipe on the pipeline.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @see PicPipe
  * @version 1.0, 28 March 96
  * @author Leemon Baird
  */
public class PicPipeList {
  public PicPipe     first=null; //first PicPipe to call
  public PicPipeList rest =null; //all PicPipes feeding into that one

  /** Create a list with specified first element (CAR) and rest (CDR) */
  public PicPipeList(PicPipe firstElement, PicPipeList restList) {
    first=firstElement;
    rest=restList;
  }

  /** Return a new copy of the entire linked list starting with this node.
    * The list is duplicated, but the PicPipes it points to are not. */
  public PicPipeList cloneList() {
    return new PicPipeList(first,(rest==null)?null:rest.cloneList());
  }
  /** Return the first Gallery on the pipeline, if any, that is acting
    * in Gallery mode.  That means it displays a tiling of pictures,
    * and a single click on one picture zooms in on it.
    */
  public Gallery getGallery() {
    Gallery g=null;
    try { //is this PicPipe a gallery?
      g=(Gallery)first;
      g=g.getGallery(); //is it in gallery mode, or is one of its pictures a Galllery in gallery mode?
    } catch (ClassCastException e) {
    }
    if (g==null && rest!=null)
      return rest.getGallery();
    else
      return g;
  }
  /** Return all the Galleries on the pipeline, if any, to gallery mode,
    * where no individual picture is selected.
    */
  public void resetGalleries() {
    Gallery g=null;
    try { //is this PicPipe a gallery?
      g=(Gallery)first;
      g.resetGalleries();  //reset this gallery and its child galleries
    } catch (ClassCastException e) {
    }
    if (rest!=null)
      rest.resetGalleries(); //recursively reset all other galleries
  }
} //end class PicPipeList
