import parse.*;
import java.awt.*;

/** Display credits.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 24 July 96
  * @author Leemon Baird
  */
public class Credits extends Project {
  /* Return the BNF description of how to parse the parameters of this object. */
  public String BNF(int lang) {
    return "//print the credits.";
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

  /** Print the credits */
  public void paint(Graphics g) {
    System.out.println("WebSim, (c)1996, 1997 Leemon Baird");
    Logo.credits(g,5,5);
  }

  /** This project does nothing, so the thread dies immediately */
  public void run() {}
} // end class Credits
