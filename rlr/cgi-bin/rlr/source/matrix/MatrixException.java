package matrix;

/** This exception is raised when incompatable matrices are multiplied,
  * or a subset of a matrix is asked for that is larger than the matrix,
  * or some other error occurs when dealing with matrices.
  *    <p>This code is (c) 1996 Leemon Baird
  *    <<a href=mailto:leemon@cs.cmu.edu>leemon@cs.cmu.edu</a>>,
  *    <a href=http://www.cs.cmu.edu/~baird>http://www.cs.cmu.edu/~baird</a><br>
  *    The source and object code may be redistributed freely.
  *    If the code is modified, please state so in the comments.
  * @version 1.01, 9 August 96
  * @author Leemon Baird
  */
public class MatrixException extends Exception {
  /** print out the error and stack to System.out */
  public void print() {
    System.out.println(this.getMessage());
    this.printStackTrace();
  }
  /** define a message string when creating the exception */
  public MatrixException(String s) {
    super(s);
  }
}
