package parse;

/** When the parser detects an error, it throws a ParserException
  * which contains the parser.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.0, 10 March 96
  * @author Leemon Baird
  */
public class ParserException extends Exception {
  public ParserException(String s) {
    super(s);
  }
  /** this is the parser raising the exception */
  public Parser parser;
  /** Remember the parser that found the error. */
  public void remember(Parser p) {
    parser=p;
  }
  /** print out the error and stack to System.out */
  public void print() {
    System.out.println(this.getMessage());
    this.printStackTrace();
  }
} //end class ParserException
